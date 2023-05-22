/*
 * Copyright (c) 2021-23 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin1;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.commons.collections4.map.ListOrderedMap;

import com.maccasoft.propeller.Compiler.ObjectInfo;
import com.maccasoft.propeller.CompilerException;
import com.maccasoft.propeller.ObjectCompiler;
import com.maccasoft.propeller.SpinObject.LinkDataObject;
import com.maccasoft.propeller.SpinObject.LongDataObject;
import com.maccasoft.propeller.SpinObject.WordDataObject;
import com.maccasoft.propeller.expressions.Add;
import com.maccasoft.propeller.expressions.BinaryOperator;
import com.maccasoft.propeller.expressions.Context;
import com.maccasoft.propeller.expressions.ContextLiteral;
import com.maccasoft.propeller.expressions.Expression;
import com.maccasoft.propeller.expressions.LocalVariable;
import com.maccasoft.propeller.expressions.Method;
import com.maccasoft.propeller.expressions.Multiply;
import com.maccasoft.propeller.expressions.NumberLiteral;
import com.maccasoft.propeller.expressions.SpinObject;
import com.maccasoft.propeller.expressions.Variable;
import com.maccasoft.propeller.model.ConstantNode;
import com.maccasoft.propeller.model.ConstantsNode;
import com.maccasoft.propeller.model.DataLineNode;
import com.maccasoft.propeller.model.DataNode;
import com.maccasoft.propeller.model.DirectiveNode;
import com.maccasoft.propeller.model.ErrorNode;
import com.maccasoft.propeller.model.MethodNode;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.model.NodeVisitor;
import com.maccasoft.propeller.model.ObjectNode;
import com.maccasoft.propeller.model.StatementNode;
import com.maccasoft.propeller.model.Token;
import com.maccasoft.propeller.model.VariableNode;
import com.maccasoft.propeller.model.VariablesNode;
import com.maccasoft.propeller.spin1.Spin1Object.Spin1LinkDataObject;
import com.maccasoft.propeller.spin1.bytecode.Address;
import com.maccasoft.propeller.spin1.bytecode.Bytecode;
import com.maccasoft.propeller.spin1.bytecode.CaseJmp;
import com.maccasoft.propeller.spin1.bytecode.CaseRangeJmp;
import com.maccasoft.propeller.spin1.bytecode.Constant;
import com.maccasoft.propeller.spin1.bytecode.Djnz;
import com.maccasoft.propeller.spin1.bytecode.Jmp;
import com.maccasoft.propeller.spin1.bytecode.Jnz;
import com.maccasoft.propeller.spin1.bytecode.Jz;
import com.maccasoft.propeller.spin1.bytecode.RepeatLoop;
import com.maccasoft.propeller.spin1.bytecode.Tjz;
import com.maccasoft.propeller.spin1.bytecode.VariableOp;
import com.maccasoft.propeller.spin1.instructions.Org;
import com.maccasoft.propeller.spin1.instructions.Res;

public class Spin1ObjectCompiler extends Spin1BytecodeCompiler implements ObjectCompiler {

    List<Variable> variables = new ArrayList<>();
    List<Spin1Method> methods = new ArrayList<>();
    Map<String, ObjectInfo> objects = ListOrderedMap.listOrderedMap(new HashMap<>());

    int objectVarSize;

    boolean errors;
    List<CompilerException> messages = new ArrayList<>();

    Map<String, Expression> publicSymbols = new HashMap<>();
    List<LinkDataObject> objectLinks = new ArrayList<>();

    public Spin1ObjectCompiler(Spin1Compiler compiler) {
        super(new Spin1GlobalContext(compiler.isCaseSensitive()), compiler);

        scope.addDefinition("__P1__", new NumberLiteral(1));
        scope.addDefinition("__P2__", new NumberLiteral(0));
        scope.addDefinition("__SPINTOOLS__", new NumberLiteral(1));
        scope.addDefinition("__debug__", new NumberLiteral(0));
    }

    @Override
    public Spin1Object compileObject(Node root) {
        compile(root);
        return generateObject();
    }

    @Override
    public void compile(Node root) {
        objectVarSize = 0;

        root.accept(new NodeVisitor() {

            @Override
            public void visitDirective(DirectiveNode node) {
                compileDirective(node);
            }

            @Override
            public boolean visitConstants(ConstantsNode node) {
                enumValue = new NumberLiteral(0);
                enumIncrement = new NumberLiteral(1);
                return true;
            }

            @Override
            public void visitConstant(ConstantNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
                if (!skipNode(node)) {
                    compileConstant(node);
                }
            }

            @Override
            public void visitVariable(VariableNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
            }

            @Override
            public void visitObject(ObjectNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
                if (!skipNode(node)) {
                    compileObject(node);
                }
            }

            @Override
            public boolean visitMethod(MethodNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
                return true;
            }

            @Override
            public boolean visitStatement(StatementNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
                return true;
            }

            @Override
            public void visitDataLine(DataLineNode node) {
                node.setData("__skip__", !conditionStack.isEmpty() && conditionStack.peek().skip);
            }

        });

        while (!conditionStack.isEmpty()) {
            Condition c = conditionStack.pop();
            logMessage(new CompilerException("unbalanced conditional directive", c.node));
        }

        try {
            determineClock();
        } catch (CompilerException e) {
            logMessage(e);
        }

        Iterator<Entry<String, Expression>> iter = publicSymbols.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Expression> entry = iter.next();
            try {
                Expression expression = entry.getValue().resolve();
                if (!expression.isConstant()) {
                    logMessage(new CompilerException("expression is not constant", expression.getData()));
                }
            } catch (CompilerException e) {
                logMessage(e);
                iter.remove();
            } catch (Exception e) {
                logMessage(new CompilerException(e, entry.getValue().getData()));
                iter.remove();
            }
        }

        for (Node node : root.getChilds()) {
            if (node instanceof VariablesNode) {
                compileVarBlock(node);
            }
        }

        Collections.sort(variables, new Comparator<Variable>() {

            @Override
            public int compare(Variable o1, Variable o2) {
                return o2.getTypeSize() - o1.getTypeSize();
            }

        });
        for (Variable var : variables) {
            var.setOffset(objectVarSize);
            objectVarSize += var.getTypeSize() * var.getSize().getNumber().intValue();
        }

        objectVarSize = (objectVarSize + 3) & ~3;

        for (Node node : root.getChilds()) {
            if (node instanceof DataNode) {
                compileDatBlock(node);
            }
        }

        for (Node node : root.getChilds()) {
            if (skipNode(node)) {
                continue;
            }
            if ((node instanceof MethodNode) && "PUB".equalsIgnoreCase(((MethodNode) node).getType().getText())) {
                try {
                    Spin1Method method = compileMethod((MethodNode) node);
                    if (method != null) {
                        Method exp = new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount()) {

                            @Override
                            public int getIndex() {
                                return methods.indexOf(method);
                            }

                        };
                        exp.setData(method.getClass().getName(), method);

                        try {
                            scope.addSymbol(method.getLabel(), exp);
                            scope.addSymbol("@" + method.getLabel(), exp);
                            publicSymbols.put(method.getLabel(), exp);
                        } catch (Exception e) {
                            logMessage(new CompilerException(e.getMessage(), node));
                        }

                        methods.add(method);
                    }
                } catch (CompilerException e) {
                    logMessage(e);
                } catch (Exception e) {
                    logMessage(new CompilerException(e.getMessage(), node));
                }
            }
        }
        for (Node node : root.getChilds()) {
            if (skipNode(node)) {
                continue;
            }
            if ((node instanceof MethodNode) && "PRI".equalsIgnoreCase(((MethodNode) node).getType().getText())) {
                try {
                    Spin1Method method = compileMethod((MethodNode) node);
                    if (method != null) {
                        Method exp = new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount()) {

                            @Override
                            public int getIndex() {
                                return methods.indexOf(method);
                            }

                        };
                        exp.setData(method.getClass().getName(), method);

                        try {
                            scope.addSymbol(method.getLabel(), exp);
                            scope.addSymbol("@" + method.getLabel(), exp);
                        } catch (Exception e) {
                            logMessage(new CompilerException(e.getMessage(), node));
                        }

                        methods.add(method);
                    }
                } catch (CompilerException e) {
                    logMessage(e);
                } catch (Exception e) {
                    logMessage(new CompilerException(e.getMessage(), node));
                }
            }
        }

        for (Entry<String, ObjectInfo> infoEntry : objects.entrySet()) {
            ObjectInfo info = infoEntry.getValue();
            String name = infoEntry.getKey();

            try {
                int count = info.count.getNumber().intValue();

                LinkDataObject linkData = new Spin1LinkDataObject(info.compiler, info.compiler.getVarSize());
                for (Entry<String, Expression> objEntry : info.compiler.getPublicSymbols().entrySet()) {
                    if (objEntry.getValue() instanceof Method) {
                        String qualifiedName = name + "." + objEntry.getKey();
                        Method objectMethod = (Method) objEntry.getValue();
                        Method method = new Method(objectMethod.getName(), objectMethod.getArgumentsCount(), objectMethod.getReturnsCount()) {

                            @Override
                            public int getIndex() {
                                return objectMethod.getIndex();
                            }

                            @Override
                            public int getObjectIndex() {
                                return objectLinks.indexOf(linkData) + methods.size() + 1;
                            }

                        };
                        method.setData(Spin1Method.class.getName(), objectMethod.getData(Spin1Method.class.getName()));
                        scope.addSymbol(qualifiedName, method);
                    }
                }
                scope.addSymbol(name, new SpinObject(name, count) {

                    @Override
                    public int getIndex() {
                        return objectLinks.indexOf(linkData) + methods.size() + 1;
                    }

                });
                objectLinks.add(linkData);

                for (int i = 1; i < count; i++) {
                    objectLinks.add(new Spin1LinkDataObject(info.compiler, info.compiler.getVarSize()));
                }

            } catch (CompilerException e) {
                logMessage(e);
            } catch (Exception e) {
                logMessage(new CompilerException(e, info.count.getData()));
            }
        }

        for (Spin1Method method : methods) {
            Node node = (Node) method.getData();
            for (Spin1MethodLine line : compileStatement(method.getScope(), method, null, node)) {
                method.addSource(line);
            }
            if (compiler.isOpenspinCompatible() || method.getLines().size() == 0 || !"RETURN".equals(method.getLines().get(method.getLines().size() - 1).getStatement())) {
                Spin1MethodLine line = new Spin1MethodLine(method.getScope(), "RETURN");
                line.addSource(new Bytecode(line.getScope(), 0b00110010, line.getStatement()));
                method.addSource(line);
            }

            if (compiler.isOpenspinCompatible()) {
                List<Spin1Bytecode> data = getStringData();
                if (data.size() != 0) {
                    Spin1MethodLine stringDataLine = new Spin1MethodLine(method.getScope());
                    stringDataLine.setText("(string data)");
                    stringDataLine.addSource(data);
                    method.addSource(stringDataLine);
                    data.clear();
                }
            }
        }
    }

    @Override
    public void compilePass2() {

        for (Variable var : variables) {
            if (!var.isReferenced() && var.getData() != null) {
                logMessage(new CompilerException(CompilerException.WARNING, "variable \"" + var.getName() + "\" is not used", var.getData()));
            }
        }

        if (methods.size() != 0) {
            if (compiler.isRemoveUnusedMethods()) {
                boolean loop;
                do {
                    loop = false;
                    Iterator<Spin1Method> methodsIterator = methods.iterator();
                    methodsIterator.next();
                    while (methodsIterator.hasNext()) {
                        Spin1Method method = methodsIterator.next();
                        if (!method.isReferenced()) {
                            MethodNode node = (MethodNode) method.getData();
                            logMessage(new CompilerException(CompilerException.WARNING, "method \"" + method.getLabel() + "\" is not used", node.getName()));
                            for (Variable var : method.getParameters()) {
                                if (!var.isReferenced() && var.getData() != null) {
                                    logMessage(new CompilerException(CompilerException.WARNING, "parameter \"" + var.getName() + "\" is not used", var.getData()));
                                }
                            }
                            for (Variable var : method.getLocalVariables()) {
                                if (!var.isReferenced() && var.getData() != null) {
                                    logMessage(new CompilerException(CompilerException.WARNING, "local variable \"" + var.getName() + "\" is not used", var.getData()));
                                }
                            }
                            method.remove();
                            methodsIterator.remove();
                            loop = true;
                        }
                    }
                } while (loop);
            }

            Iterator<Spin1Method> methodsIterator = methods.iterator();

            Spin1Method method = methodsIterator.next();
            for (Variable var : method.getParameters()) {
                if (!var.isReferenced() && var.getData() != null) {
                    logMessage(new CompilerException(CompilerException.WARNING, "parameter \"" + var.getName() + "\" is not used", var.getData()));
                }
            }
            for (Variable var : method.getLocalVariables()) {
                if (!var.isReferenced() && var.getData() != null) {
                    logMessage(new CompilerException(CompilerException.WARNING, "local variable \"" + var.getName() + "\" is not used", var.getData()));
                }
            }

            while (methodsIterator.hasNext()) {
                method = methodsIterator.next();
                if (!method.isReferenced()) {
                    MethodNode node = (MethodNode) method.getData();
                    logMessage(new CompilerException(CompilerException.WARNING, "method \"" + method.getLabel() + "\" is not used", node.getName()));
                }
                for (Variable var : method.getParameters()) {
                    if (!var.isReferenced() && var.getData() != null) {
                        logMessage(new CompilerException(CompilerException.WARNING, "parameter \"" + var.getName() + "\" is not used", var.getData()));
                    }
                }
                for (Variable var : method.getLocalVariables()) {
                    if (!var.isReferenced() && var.getData() != null) {
                        logMessage(new CompilerException(CompilerException.WARNING, "local variable \"" + var.getName() + "\" is not used", var.getData()));
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Expression> getPublicSymbols() {
        return publicSymbols;
    }

    @Override
    public int getVarSize() {
        int linkedVarOffset = objectVarSize;
        for (LinkDataObject linkData : objectLinks) {
            linkedVarOffset += linkData.getVarSize();
        }
        return linkedVarOffset;
    }

    @Override
    public List<LinkDataObject> getObjectLinks() {
        return objectLinks;
    }

    @Override
    public Spin1Object generateObject() {
        return generateObject(0);
    }

    @Override
    public Spin1Object generateObject(int memoryOffset) {
        int address = 0, hubAddress = 0;
        Spin1Object object = new Spin1Object();

        object.setClkFreq(scope.getLocalSymbol("CLKFREQ").getNumber().intValue());
        object.setClkMode(scope.getLocalSymbol("CLKMODE").getNumber().intValue());

        object.writeComment("Object header (var size " + objectVarSize + ")");

        WordDataObject objectSize = object.writeWord(0, "Object size");
        object.writeByte(methods.size() + 1, "Method count + 1");

        int count = 0;
        for (Entry<String, ObjectInfo> infoEntry : objects.entrySet()) {
            ObjectInfo info = infoEntry.getValue();
            try {
                count += info.count.getNumber().intValue();
            } catch (Exception e) {
                // Do nothing, error throw in compile
            }
        }
        object.writeByte(count, "Object count");

        List<LongDataObject> methodData = new ArrayList<>();
        if (methods.size() != 0) {
            Iterator<Spin1Method> methodsIterator = methods.iterator();
            while (methodsIterator.hasNext()) {
                Spin1Method method = methodsIterator.next();
                LongDataObject dataObject = new LongDataObject(0, "Function " + method.getLabel());
                object.write(dataObject);
                methodData.add(dataObject);
            }
            object.setDcurr(methods.get(0).getStackSize());
        }

        int linkedVarOffset = objectVarSize;
        for (LinkDataObject linkData : objectLinks) {
            linkData.setVarOffset(linkedVarOffset);
            object.write(linkData);
            linkedVarOffset += linkData.getVarSize();
        }
        object.setVarSize(linkedVarOffset);

        object.addAllSymbols(publicSymbols);

        hubAddress = object.getSize();

        for (Spin1PAsmLine line : source) {
            if (line.getInstructionFactory() instanceof com.maccasoft.propeller.spin1.instructions.Long) {
                hubAddress = (hubAddress + 3) & ~3;
            }
            line.getScope().setObjectAddress(hubAddress);
            if ((line.getInstructionFactory() instanceof Org) || (line.getInstructionFactory() instanceof Res)) {
                hubAddress = (hubAddress + 3) & ~3;
            }
            try {
                address = line.resolve(address, memoryOffset + hubAddress);
                hubAddress += line.getInstructionObject().getSize();
                for (CompilerException msg : line.getAnnotations()) {
                    logMessage(msg);
                }
            } catch (CompilerException e) {
                logMessage(e);
            } catch (Exception e) {
                logMessage(new CompilerException(e, line.getData()));
            }
        }

        for (Spin1PAsmLine line : source) {
            hubAddress = line.getScope().getObjectAddress();
            if (object.getSize() < hubAddress) {
                object.writeBytes(new byte[hubAddress - object.getSize()], "(filler)");
            }
            try {
                object.writeBytes(line.getScope().getAddress(), line.getInstructionObject().getBytes(), line.toString());
            } catch (CompilerException e) {
                logMessage(e);
            } catch (Exception e) {
                logMessage(new CompilerException(e, line.getData()));
            }
        }

        Spin1MethodLine stringDataLine = null;
        if (!compiler.isOpenspinCompatible() && getStringData().size() != 0) {
            stringDataLine = new Spin1MethodLine(scope);
            stringDataLine.setText("(string data)");
            stringDataLine.addSource(getStringData());
        }

        if (methods.size() != 0) {
            boolean loop;
            do {
                loop = false;
                address = object.getSize();
                for (Spin1Method method : methods) {
                    address = method.resolve(address);
                    loop |= method.isAddressChanged();
                }
                if (stringDataLine != null) {
                    address = stringDataLine.resolve(address);
                    loop |= stringDataLine.isAddressChanged();
                }
            } while (loop);

            int index = 0;
            for (Spin1Method method : methods) {
                methodData.get(index).setValue((method.getLocalsSize() << 16) | object.getSize());
                methodData.get(index).setText(String.format("Function %s @ $%04X (local size %d)", method.getLabel(), object.getSize(), method.getLocalsSize()));
                method.writeTo(object);
                index++;
            }

            if (stringDataLine != null) {
                stringDataLine.writeTo(object);
            }

            object.alignToLong();
        }

        objectSize.setValue(object.getSize());

        return object;
    }

    Expression enumValue = new NumberLiteral(0);
    Expression enumIncrement = new NumberLiteral(1);

    void compileConstant(ConstantNode node) {
        try {
            Iterator<Token> iter = node.getTokens().iterator();

            Token token = iter.next();
            do {
                if ("#".equals(token.getText())) {
                    Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                    while (iter.hasNext()) {
                        token = iter.next();
                        if ("[".equals(token.getText())) {
                            break;
                        }
                        builder.addToken(token);
                    }
                    try {
                        enumValue = builder.getExpression();
                    } catch (CompilerException e) {
                        logMessage(e);
                    } catch (Exception e) {
                        logMessage(new CompilerException("expression syntax error", builder.getTokens()));
                    }
                    if ("[".equals(token.getText())) {
                        builder = new Spin1ExpressionBuilder(scope);
                        while (iter.hasNext()) {
                            token = iter.next();
                            if ("]".equals(token.getText())) {
                                try {
                                    enumIncrement = builder.getExpression();
                                } catch (CompilerException e) {
                                    logMessage(e);
                                } catch (Exception e) {
                                    logMessage(new CompilerException(e, builder.getTokens()));
                                }
                                break;
                            }
                            builder.addToken(token);
                        }
                        if (!"]".equals(token.getText())) {
                            logMessage(new CompilerException("expecting '['", token));
                        }
                    }
                }
                else {
                    if (token.type != 0) {
                        logMessage(new CompilerException("expecting identifier", token));
                        break;
                    }
                    String identifier = token.getText();
                    if (!iter.hasNext()) {
                        try {
                            scope.addSymbol(identifier, enumValue);
                            publicSymbols.put(identifier, enumValue);
                        } catch (CompilerException e) {
                            logMessage(e);
                        } catch (Exception e) {
                            logMessage(new CompilerException(e, token));
                        }
                        if (enumValue instanceof BinaryOperator) {
                            enumValue = Expression.fold(new Add(((BinaryOperator) enumValue).getTerm1(), new Add(((BinaryOperator) enumValue).getTerm2(), enumIncrement)));
                        }
                        else {
                            enumValue = Expression.fold(new Add(enumValue, enumIncrement));
                        }
                    }
                    else {
                        token = iter.next();
                        if ("[".equals(token.getText())) {
                            try {
                                scope.addSymbol(identifier, enumValue);
                                publicSymbols.put(identifier, enumValue);
                            } catch (CompilerException e) {
                                logMessage(e);
                            } catch (Exception e) {
                                logMessage(new CompilerException(e, token));
                            }

                            Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                            while (iter.hasNext()) {
                                token = iter.next();
                                if ("]".equals(token.getText())) {
                                    break;
                                }
                                builder.addToken(token);
                            }
                            try {
                                Expression expression = builder.getExpression();
                                if (enumValue instanceof BinaryOperator) {
                                    Expression increment = new Multiply(enumIncrement, expression);
                                    enumValue = Expression.fold(new Add(((BinaryOperator) enumValue).getTerm1(), new Add(((BinaryOperator) enumValue).getTerm2(), increment)));
                                }
                                else {
                                    enumValue = Expression.fold(new Add(enumValue, new Multiply(enumIncrement, expression)));
                                }
                            } catch (CompilerException e) {
                                logMessage(e);
                            } catch (Exception e) {
                                logMessage(new CompilerException(e, builder.getTokens()));
                            }
                        }
                        else if ("=".equals(token.getText())) {
                            Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                            while (iter.hasNext()) {
                                token = iter.next();
                                if ("]".equals(token.getText())) {
                                    break;
                                }
                                builder.addToken(token);
                            }
                            try {
                                Expression expression = builder.getExpression();
                                try {
                                    scope.addSymbol(identifier, expression);
                                    publicSymbols.put(identifier, expression);
                                } catch (CompilerException e) {
                                    logMessage(e);
                                } catch (Exception e) {
                                    logMessage(new CompilerException(e, node));
                                }
                            } catch (CompilerException e) {
                                logMessage(e);
                            } catch (Exception e) {
                                if (builder.getTokens().size() == 0) {
                                    logMessage(new CompilerException("expecting expression", token));
                                }
                                else {
                                    logMessage(new CompilerException("expression syntax error", builder.getTokens()));
                                }
                            }
                        }
                        else {
                            logMessage(new CompilerException("unexpected '" + token.getText() + "'", token));
                            break;
                        }
                    }
                }
            } while (iter.hasNext());

        } catch (CompilerException e) {
            logMessage(e);
        } catch (Exception e) {
            logMessage(new CompilerException(e, node));
        }
    }

    void compileVarBlock(Node parent) {
        String type = "LONG";

        for (Node node : parent.getChilds()) {
            try {
                if (skipNode(node)) {
                    continue;
                }
                if (node instanceof VariableNode) {
                    Iterator<Token> iter = node.getTokens().iterator();

                    Token token = iter.next();
                    if (Spin1Model.isType(token.getText())) {
                        type = token.getText().toUpperCase();
                        if (!iter.hasNext()) {
                            logMessage(new CompilerException("expecting identifier", token));
                            return;
                        }
                        token = iter.next();
                    }

                    Token identifier = token;
                    Expression size = new NumberLiteral(1);

                    if (iter.hasNext()) {
                        token = iter.next();
                        if ("[".equals(token.getText())) {
                            if (!iter.hasNext()) {
                                logMessage(new CompilerException("expecting expression", token));
                                return;
                            }
                            Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                            while (iter.hasNext()) {
                                token = iter.next();
                                if ("]".equals(token.getText())) {
                                    try {
                                        size = builder.getExpression();
                                    } catch (CompilerException e) {
                                        logMessage(e);
                                    } catch (Exception e) {
                                        logMessage(new CompilerException(e, builder.tokens));
                                    }
                                    break;
                                }
                                builder.addToken(token);
                            }
                            if (!"]".equals(token.getText())) {
                                logMessage(new CompilerException("expecting '['", token));
                                return;
                            }
                        }
                        else {
                            Node error = new Node();
                            iter.forEachRemaining(t -> error.addToken(t));
                            logMessage(new CompilerException("unexpected '" + error + "'", error));
                        }
                    }

                    try {
                        Variable var = new Variable(type, identifier.getText(), size);
                        scope.addSymbol(identifier.getText(), var);
                        variables.add(var);
                        var.setData(identifier);
                    } catch (Exception e) {
                        logMessage(new CompilerException(e, identifier));
                    }

                    if (iter.hasNext()) {
                        Node error = new Node();
                        iter.forEachRemaining(t -> error.addToken(t));
                        logMessage(new CompilerException("unexpected '" + error + "'", error));
                    }
                }
            } catch (CompilerException e) {
                logMessage(e);
            } catch (Exception e) {
                logMessage(new CompilerException(e, node));
            }
        }
    }

    void compileObject(ObjectNode node) {
        try {
            Iterator<Token> iter = node.getTokens().iterator();

            Token token = iter.next();
            String name = token.getText();
            Expression count = new NumberLiteral(1);

            if (!iter.hasNext()) {
                logMessage(new CompilerException("syntax error", node));
                return;
            }
            token = iter.next();

            if ("[".equals(token.getText())) {
                if (!iter.hasNext()) {
                    logMessage(new CompilerException("syntax error", node));
                    return;
                }
                Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                while (iter.hasNext()) {
                    token = iter.next();
                    if ("]".equals(token.getText())) {
                        try {
                            count = builder.getExpression();
                            count.setData(node.count);
                        } catch (CompilerException e) {
                            logMessage(e);
                        } catch (Exception e) {
                            logMessage(new CompilerException(e, builder.tokens));
                        }
                        break;
                    }
                    builder.addToken(token);
                }
                if (!"]".equals(token.getText())) {
                    logMessage(new CompilerException("expecting '['", token));
                    return;
                }
                if (!iter.hasNext()) {
                    logMessage(new CompilerException("expecting object file", token));
                    return;
                }
                token = iter.next();
            }

            if (!":".equals(token.getText())) {
                logMessage(new CompilerException("expecting ':'", token));
                return;
            }
            if (!iter.hasNext()) {
                logMessage(new CompilerException("syntax error", node));
                return;
            }
            token = iter.next();
            if (token.type != Token.STRING) {
                logMessage(new CompilerException("expecting file name", token));
                return;
            }
            String fileName = token.getText().substring(1, token.getText().length() - 1);

            ObjectInfo info = compiler.getObjectInfo(fileName, Collections.emptyMap());
            if (info == null) {
                logMessage(new CompilerException("object " + token + " not found", token));
                return;
            }
            if (info.hasErrors()) {
                logMessage(new CompilerException("object " + token + " has errors", token));
                return;
            }

            objects.put(name, new ObjectInfo(info, count));

            for (Entry<String, Expression> entry : info.compiler.getPublicSymbols().entrySet()) {
                if (!(entry.getValue() instanceof Method)) {
                    String qualifiedName = name + "#" + entry.getKey();
                    scope.addSymbol(qualifiedName, entry.getValue());
                }
            }

            if (iter.hasNext()) {
                Node error = new Node();
                iter.forEachRemaining(t -> error.addToken(t));
                logMessage(new CompilerException("unexpected '" + error + "'", error));
            }

        } catch (CompilerException e) {
            logMessage(e);
        } catch (Exception e) {
            logMessage(new CompilerException(e, node));
        }
    }

    @Override
    protected void compileDatInclude(Node root) {
        for (Node node : root.getChilds()) {
            if (!(node instanceof ConstantsNode) && !(node instanceof DataNode)) {
                throw new RuntimeException("only CON and DAT sections allowed in included files");
            }
        }
        for (Node node : root.getChilds()) {
            if (node instanceof ConstantsNode) {
                for (Node child : node.getChilds()) {
                    if (child instanceof ConstantNode) {
                        compileConstant((ConstantNode) child);
                    }
                }
            }
        }
        for (Node node : root.getChilds()) {
            if (node instanceof DataNode) {
                compileDatBlock(node);
            }
        }
    }

    Spin1Method compileMethod(MethodNode node) {
        Context localScope = new Context(scope);

        Iterator<Token> iter = node.getTokens().iterator();
        Token token = iter.next(); // First token is PUB/PRI already checked

        if (!iter.hasNext()) {
            logMessage(new CompilerException("expecting method name", token));
            return null;
        }
        token = iter.next();
        if (token.type != 0) {
            logMessage(new CompilerException("expecting identifier", token));
            return null;
        }

        Spin1Method method = new Spin1Method(localScope, token.getText());
        method.setComment(node.getText());
        method.setData(node);

        for (Node child : node.getParameters()) {
            token = child.getToken(0);
            if (token.type == 0) {
                Token identifier = token;
                Expression expression = localScope.getLocalSymbol(identifier.getText());
                if (expression instanceof LocalVariable) {
                    logMessage(new CompilerException("symbol '" + identifier + "' already defined", child));
                }
                else {
                    if (expression != null) {
                        logMessage(new CompilerException(CompilerException.WARNING, "parameter '" + identifier + "' hides global variable", child));
                    }
                    LocalVariable var = method.addParameter("LONG", identifier.getText(), new NumberLiteral(1));
                    var.setData(identifier);
                }
            }
            else {
                logMessage(new CompilerException("expecting identifier", token));
            }
            if (child.getTokenCount() > 1) {
                Node error = new Node(child.getTokens().subList(1, child.getTokenCount()));
                logMessage(new CompilerException("unexpected '" + error + "'", error));
            }
        }

        if (iter.hasNext()) {
            token = iter.next();
            if ("(".equals(token.getText())) {
                if (method.getParametersCount() == 0) {
                    logMessage(new CompilerException("expecting parameter name", token));
                }
                else {
                    while (iter.hasNext()) {
                        token = iter.next();
                        if (")".equals(token.getText())) {
                            break;
                        }
                    }
                    if (!")".equals(token.getText())) {
                        logMessage(new CompilerException("expecting ',' or ')'", token));
                    }
                }
            }
        }

        for (Node child : node.getReturnVariables()) {
            token = child.getToken(0);
            if (token.type == 0) {
                Token identifier = token;
                if ("RESULT".equalsIgnoreCase(identifier.getText())) {
                    continue;
                }
                Expression expression = localScope.getLocalSymbol(identifier.getText());
                if (expression instanceof LocalVariable) {
                    logMessage(new CompilerException("symbol '" + identifier.getText() + "' already defined", child));
                }
                else {
                    if (expression != null) {
                        logMessage(new CompilerException(CompilerException.WARNING, "return variable '" + identifier.getText() + "' hides global variable", child));
                    }
                    LocalVariable var = method.addReturnVariable(identifier.getText());
                    var.setData(identifier);
                }
            }
            else {
                logMessage(new CompilerException("expecting identifier", token));
            }
            if (child.getTokenCount() > 1) {
                Node error = new Node(child.getTokens().subList(1, child.getTokenCount()));
                logMessage(new CompilerException("unexpected '" + error + "'", error));
            }
        }

        for (MethodNode.LocalVariableNode child : node.getLocalVariables()) {
            String type = "LONG";
            iter = child.getTokens().iterator();

            token = iter.next();
            if (Spin1Model.isType(token.getText())) {
                type = token.getText().toUpperCase();
                if (!iter.hasNext()) {
                    logMessage(new CompilerException("expecting identifier", token));
                    continue;
                }
                token = iter.next();
            }
            if (token.type == 0) {
                Token identifier = token;
                Expression size = new NumberLiteral(1);

                if (iter.hasNext()) {
                    token = iter.next();
                    if ("[".equals(token.getText())) {
                        if (!iter.hasNext()) {
                            logMessage(new CompilerException("expecting expression", token));
                            continue;
                        }
                        Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                        while (iter.hasNext()) {
                            token = iter.next();
                            if ("]".equals(token.getText())) {
                                try {
                                    size = builder.getExpression();
                                    size.getNumber().intValue();
                                } catch (CompilerException e) {
                                    logMessage(e);
                                } catch (Exception e) {
                                    logMessage(new CompilerException(e, builder.tokens));
                                }
                                break;
                            }
                            builder.addToken(token);
                        }
                        if (!"]".equals(token.getText())) {
                            logMessage(new CompilerException("expecting '['", token));
                            continue;
                        }
                    }
                    else {
                        Node error = new Node();
                        error.addToken(token);
                        iter.forEachRemaining(t -> error.addToken(t));
                        logMessage(new CompilerException("unexpected '" + error + "'", error));
                    }
                }

                Expression expression = localScope.getLocalSymbol(identifier.getText());
                if (expression instanceof LocalVariable) {
                    logMessage(new CompilerException("symbol '" + identifier + "' already defined", child));
                }
                else {
                    if (expression != null) {
                        logMessage(new CompilerException(CompilerException.WARNING, "local variable '" + identifier + "' hides global variable", child));
                    }
                    LocalVariable var = method.addLocalVariable(type, identifier.getText(), size); // new LocalVariable(type, identifier.getText(), size, offset);
                    var.setData(identifier);
                }

                if (iter.hasNext()) {
                    Node error = new Node();
                    iter.forEachRemaining(t -> error.addToken(t));
                    logMessage(new CompilerException("unexpected '" + error + "'", error));
                }
            }
            else {
                logMessage(new CompilerException("expecting identifier", token));
            }
        }

        for (Node child : node.getChilds()) {
            if (child instanceof ErrorNode) {
                logMessage(new CompilerException("unexpected '" + child + "'", child));
            }
        }

        return method;
    }

    List<Spin1MethodLine> compileStatement(Context context, Spin1Method method, Spin1MethodLine parent, Node statementNode) {
        List<Spin1MethodLine> lines = new ArrayList<>();

        Spin1MethodLine previousLine = null;

        Iterator<Node> nodeIterator = new ArrayList<>(statementNode.getChilds()).iterator();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            try {
                if (skipNode(node)) {
                    continue;
                }
                if (node instanceof StatementNode) {
                    Spin1MethodLine line = compileStatement(context, method, parent, node, previousLine);
                    if (line != null) {
                        lines.add(line);
                        previousLine = line;
                    }
                }
            } catch (CompilerException e) {
                logMessage(e);
            } catch (Exception e) {
                logMessage(new CompilerException(e, node));
            }
        }

        return lines;
    }

    Spin1MethodLine compileStatement(Context context, Spin1Method method, Spin1MethodLine parent, Node node, Spin1MethodLine previousLine) {
        Spin1MethodLine line = null;

        try {
            Iterator<Token> iter = node.getTokens().iterator();
            if (!iter.hasNext()) {
                throw new RuntimeException("syntax error");
            }

            Token token = iter.next();
            String text = token.getText().toUpperCase();

            if ("ABORT".equals(text)) {
                line = new Spin1MethodLine(context, parent, token.getText(), node);
                if (iter.hasNext()) {
                    Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                    while (iter.hasNext()) {
                        builder.addToken(iter.next());
                    }
                    line.addSource(compileBytecodeExpression(context, method, builder.getRoot(), true));
                    line.addSource(new Bytecode(line.getScope(), 0b00110001, text));
                }
                else {
                    line.addSource(new Bytecode(line.getScope(), 0b00110000, text));
                }
            }
            else if ("IF".equals(text) || "IFNOT".equals(text)) {
                line = new Spin1MethodLine(context, parent, token.getText(), node);

                Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                while (iter.hasNext()) {
                    builder.addToken(iter.next());
                }
                line.addSource(compileBytecodeExpression(context, method, builder.getRoot(), true));

                Spin1MethodLine falseLine = new Spin1MethodLine(context);
                if ("IF".equals(text)) {
                    line.addSource(new Jz(line.getScope(), new ContextLiteral(falseLine.getScope())));
                }
                else {
                    line.addSource(new Jnz(line.getScope(), new ContextLiteral(falseLine.getScope())));
                }

                line.addChilds(compileStatement(new Context(context), method, line, node));

                line.addChild(falseLine);
                line.addChild(new Spin1MethodLine(context));
            }
            else if ("ELSE".equals(text) || "ELSEIF".equals(text) || "ELSEIFNOT".equals(text)) {
                if (previousLine == null || (!previousLine.getStatement().toUpperCase().startsWith("IF") && !previousLine.getStatement().toUpperCase().startsWith("ELSEIF"))) {
                    throw new CompilerException("misplaced " + token.getText().toLowerCase(), node);
                }

                line = new Spin1MethodLine(context, parent, text, node);
                Spin1MethodLine falseLine = new Spin1MethodLine(context);
                Spin1MethodLine exitLine = previousLine.getChilds().remove(previousLine.getChilds().size() - 1);

                if ("ELSEIF".equals(text) || "ELSEIFNOT".equals(text)) {
                    Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                    while (iter.hasNext()) {
                        builder.addToken(iter.next());
                    }
                    line.addSource(compileBytecodeExpression(context, method, builder.getRoot(), true));
                    if ("ELSEIF".equals(text)) {
                        line.addSource(new Jz(line.getScope(), new ContextLiteral(falseLine.getScope())));
                    }
                    else {
                        line.addSource(new Jnz(line.getScope(), new ContextLiteral(falseLine.getScope())));
                    }
                }

                line.addChilds(compileStatement(new Context(context), method, line, node));
                line.addChild(falseLine);
                line.addChild(exitLine);

                Spin1MethodLine jmpLine = new Spin1MethodLine(context);
                jmpLine.addSource(new Jmp(line.getScope(), new ContextLiteral(exitLine.getScope())));
                previousLine.addChild(previousLine.getChilds().size() - 1, jmpLine);
            }
            else if ("RETURN".equals(text)) {
                line = new Spin1MethodLine(context, parent, text, node);

                if (iter.hasNext()) {
                    Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                    while (iter.hasNext()) {
                        builder.addToken(iter.next());
                    }
                    line.addSource(compileBytecodeExpression(line.getScope(), method, builder.getRoot(), true));
                    line.addSource(new Bytecode(line.getScope(), 0b00110011, text));
                }
                else {
                    line.addSource(new Bytecode(line.getScope(), 0b00110010, text));
                }
            }
            else if ("REPEAT".equals(text)) {
                line = new Spin1MethodLine(context, parent, text, node);

                Spin1MethodLine quitLine = new Spin1MethodLine(context);
                line.setData("quit", quitLine);

                if (iter.hasNext()) {
                    token = iter.next();
                    if ("WHILE".equalsIgnoreCase(token.getText()) || "UNTIL".equalsIgnoreCase(token.getText())) {
                        line.setStatement(text + " " + token.getText().toUpperCase());
                        line.setData("next", line);

                        Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                        while (iter.hasNext()) {
                            builder.addToken(iter.next());
                        }
                        line.addSource(compileBytecodeExpression(line.getScope(), method, builder.getRoot(), true));

                        if ("WHILE".equalsIgnoreCase(token.getText())) {
                            line.addSource(new Jz(line.getScope(), new ContextLiteral(quitLine.getScope())));
                        }
                        else {
                            line.addSource(new Jnz(line.getScope(), new ContextLiteral(quitLine.getScope())));
                        }

                        line.addChilds(compileStatement(new Context(context), method, line, node));

                        Spin1MethodLine loopLine = new Spin1MethodLine(context);
                        loopLine.addSource(new Jmp(loopLine.getScope(), new ContextLiteral(line.getScope())));
                        line.addChild(loopLine);
                    }
                    else {
                        Spin1StatementNode counter = null;
                        Spin1StatementNode from = null;
                        Spin1StatementNode to = null;
                        Spin1StatementNode step = null;

                        Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                        builder.addToken(token);
                        while (iter.hasNext()) {
                            token = iter.next();
                            if ("FROM".equalsIgnoreCase(token.getText())) {
                                break;
                            }
                            builder.addToken(token);
                        }
                        counter = builder.getRoot();

                        if ("FROM".equalsIgnoreCase(token.getText())) {
                            if (counter.getChildCount() != 0) {
                                throw new CompilerException("syntax error", builder.getTokens());
                            }

                            builder = new Spin1TreeBuilder(context);
                            while (iter.hasNext()) {
                                token = iter.next();
                                if ("TO".equalsIgnoreCase(token.getText())) {
                                    break;
                                }
                                builder.addToken(token);
                            }
                            from = builder.getRoot();

                            if ("TO".equalsIgnoreCase(token.getText())) {
                                builder = new Spin1TreeBuilder(context);
                                while (iter.hasNext()) {
                                    token = iter.next();
                                    if ("STEP".equalsIgnoreCase(token.getText())) {
                                        break;
                                    }
                                    builder.addToken(token);
                                }
                                to = builder.getRoot();

                                if ("STEP".equalsIgnoreCase(token.getText())) {
                                    builder = new Spin1TreeBuilder(context);
                                    while (iter.hasNext()) {
                                        builder.addToken(iter.next());
                                    }
                                    step = builder.getRoot();
                                }
                            }
                        }

                        if (from != null && to != null) {
                            line.addSource(compileConstantExpression(line.getScope(), method, from));

                            Expression expression = line.getScope().getLocalSymbol(counter.getText());
                            if (expression == null) {
                                throw new CompilerException("undefined symbol " + counter.getText(), counter.getToken());
                            }
                            else if (expression instanceof Variable) {
                                line.addSource(new VariableOp(line.getScope(), VariableOp.Op.Write, (Variable) expression));
                            }
                            else {
                                throw new RuntimeException("unsupported " + counter);
                            }

                            Spin1MethodLine nextLine = new Spin1MethodLine(context);
                            line.setData("next", nextLine);

                            Spin1MethodLine loopLine = new Spin1MethodLine(context);
                            line.addChild(loopLine);
                            line.addChilds(compileStatement(new Context(context), method, line, node));

                            if (step != null) {
                                nextLine.addSource(compileConstantExpression(line.getScope(), method, step));
                            }
                            nextLine.addSource(compileConstantExpression(line.getScope(), method, from));
                            nextLine.addSource(compileConstantExpression(line.getScope(), method, to));

                            nextLine.addSource(new VariableOp(line.getScope(), VariableOp.Op.Assign, (Variable) expression));
                            nextLine.addSource(new RepeatLoop(line.getScope(), step != null, new ContextLiteral(loopLine.getScope())));
                            line.addChild(nextLine);
                        }
                        else {
                            line.addSource(compileConstantExpression(line.getScope(), method, counter));
                            line.addSource(new Tjz(line.getScope(), new ContextLiteral(quitLine.getScope())));
                            line.setData("pop", Integer.valueOf(4));

                            Spin1MethodLine nextLine = new Spin1MethodLine(context);
                            line.setData("next", nextLine);

                            Spin1MethodLine loopLine = new Spin1MethodLine(context);
                            line.addChild(loopLine);
                            line.addChilds(compileStatement(new Context(context), method, line, node));

                            nextLine.addSource(new Djnz(nextLine.getScope(), new ContextLiteral(loopLine.getScope())));
                            line.addChild(nextLine);
                        }
                    }
                }
                else {
                    Spin1MethodLine nextLine = new Spin1MethodLine(context);
                    line.setData("next", nextLine);

                    line.addChild(nextLine);
                    line.addChilds(compileStatement(new Context(context), method, line, node));

                    Spin1MethodLine loopLine = new Spin1MethodLine(context);
                    loopLine.addSource(new Jmp(loopLine.getScope(), new ContextLiteral(line.getScope())));
                    line.addChild(loopLine);
                    line.setData("loop", loopLine);
                }

                line.addChild(quitLine);
            }
            else if ("WHILE".equals(text) || "UNTIL".equals(text)) {
                if (previousLine == null || (!"REPEAT".equalsIgnoreCase(previousLine.getStatement()))) {
                    throw new CompilerException("misplaced " + token.getText().toLowerCase(), node);
                }

                previousLine.getChilds().remove(previousLine.getData("loop"));

                line = new Spin1MethodLine(context, parent, text, node);

                Spin1MethodLine nextLine = (Spin1MethodLine) previousLine.getData("next");
                previousLine.getChilds().remove(nextLine);
                line.addChild(nextLine);

                Spin1MethodLine conditionLine = new Spin1MethodLine(context);

                Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                while (iter.hasNext()) {
                    builder.addToken(iter.next());
                }
                conditionLine.addSource(compileConstantExpression(line.getScope(), method, builder.getRoot()));

                if ("WHILE".equals(text)) {
                    conditionLine.addSource(new Jnz(previousLine.getScope(), new ContextLiteral(previousLine.getScope())));
                }
                else {
                    conditionLine.addSource(new Jz(previousLine.getScope(), new ContextLiteral(previousLine.getScope())));
                }

                line.addChild(conditionLine);

                Spin1MethodLine quitLine = (Spin1MethodLine) previousLine.getData("quit");
                previousLine.getChilds().remove(quitLine);
                line.addChild(quitLine);
            }
            else if ("NEXT".equals(text)) {
                line = new Spin1MethodLine(context, parent, text, node);

                while (parent != null && parent.getData("next") == null) {
                    parent = parent.getParent();
                }
                if (parent == null || parent.getData("next") == null) {
                    throw new CompilerException("misplaced next", node);
                }

                Spin1MethodLine nextLine = (Spin1MethodLine) parent.getData("next");
                line.addSource(new Jmp(line.getScope(), new ContextLiteral(nextLine.getScope())));
            }
            else if ("QUIT".equalsIgnoreCase(text)) {
                int pop = 0;

                line = new Spin1MethodLine(context, parent, text, node);

                while (parent != null) {
                    if (parent.getData("pop") != null) {
                        pop += ((Integer) parent.getData("pop")).intValue();
                    }
                    if (parent.getData("quit") != null) {
                        break;
                    }
                    parent = parent.getParent();
                }
                if (parent == null || parent.getData("quit") == null) {
                    throw new CompilerException("misplaced quit", node);
                }

                Spin1MethodLine nextLine = (Spin1MethodLine) parent.getData("quit");
                if ("REPEAT".equals(parent.getStatement()) && pop == 4) {
                    line.addSource(new Jnz(line.getScope(), new ContextLiteral(nextLine.getScope())));
                }
                else {
                    if (pop != 0) {
                        try {
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            os.write(new Constant(line.getScope(), new NumberLiteral(pop), compiler.isOpenspinCompatible()).getBytes());
                            os.write(0x14);
                            line.addSource(new Bytecode(line.getScope(), os.toByteArray(), String.format("POP %d", pop)));
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }
                    line.addSource(new Jmp(line.getScope(), new ContextLiteral(nextLine.getScope())));
                }
            }
            else if ("CASE".equalsIgnoreCase(text)) {
                line = new Spin1MethodLine(context, parent, text, node);
                line.setData("pop", Integer.valueOf(8));

                Spin1MethodLine endLine = new Spin1MethodLine(context);
                line.addSource(new Address(line.getScope(), new ContextLiteral(endLine.getScope())));

                Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                while (iter.hasNext()) {
                    builder.addToken(iter.next());
                }
                line.addSource(compileBytecodeExpression(context, method, builder.getRoot(), true));

                boolean hasOther = false;
                for (Node child : node.getChilds()) {
                    if (skipNode(node)) {
                        continue;
                    }
                    if (child instanceof StatementNode) {
                        Spin1MethodLine caseLine = new Spin1MethodLine(context);
                        caseLine.addChilds(compileStatement(new Context(context), method, line, child));

                        Iterator<Token> childIter = child.getTokens().iterator();
                        token = childIter.next();

                        if ("OTHER".equalsIgnoreCase(token.getText())) {
                            line.addChild(0, caseLine);
                            hasOther = true;
                        }
                        else {
                            builder = new Spin1TreeBuilder(context);
                            builder.addToken(token);
                            while (childIter.hasNext()) {
                                token = childIter.next();
                                if (":".equals(token.getText())) {
                                    break;
                                }
                                builder.addToken(token);
                            }
                            compileCase(method, line, builder.getRoot(), caseLine);

                            line.addChild(caseLine);
                        }

                        Spin1MethodLine doneLine = new Spin1MethodLine(context);
                        doneLine.addSource(new Bytecode(line.getScope(), 0x0C, "CASE_DONE"));
                        caseLine.addChild(doneLine);
                    }
                }

                if (!hasOther) {
                    line.addSource(new Bytecode(line.getScope(), 0x0C, "CASE_DONE"));
                }

                line.addChild(endLine);
            }
            else {
                Spin1TreeBuilder builder = new Spin1TreeBuilder(context);
                builder.addToken(token);
                while (iter.hasNext()) {
                    builder.addToken(iter.next());
                }
                line = new Spin1MethodLine(context, parent, null, node);
                line.addSource(compileBytecodeExpression(context, method, builder.getRoot(), false));
            }

        } catch (CompilerException e) {
            logMessage(e);
        } catch (Exception e) {
            logMessage(new CompilerException(e, node));
        }

        return line;
    }

    void compileCase(Spin1Method method, Spin1MethodLine line, Spin1StatementNode arg, Spin1MethodLine target) {
        if (",".equals(arg.getText())) {
            for (Spin1StatementNode child : arg.getChilds()) {
                compileCase(method, line, child, target);
            }
        }
        else if ("..".equals(arg.getText())) {
            line.addSource(compileConstantExpression(line.getScope(), method, arg.getChild(0)));
            line.addSource(compileConstantExpression(line.getScope(), method, arg.getChild(1)));
            if (target != null) {
                line.addSource(new CaseRangeJmp(line.getScope(), new ContextLiteral(target.getScope())));
            }
        }
        else {
            line.addSource(compileConstantExpression(line.getScope(), method, arg));
            if (target != null) {
                line.addSource(new CaseJmp(line.getScope(), new ContextLiteral(target.getScope())));
            }
        }
    }

    class Condition {

        final Node node;
        final boolean evaluated;
        final boolean skip;

        public Condition(Node node, boolean evaluated, boolean skip) {
            this.node = node;
            this.evaluated = evaluated;
            this.skip = skip;
        }

    }

    Stack<Condition> conditionStack = new Stack<>();

    void compileDirective(Node node) {
        Token token;
        boolean skip = !conditionStack.isEmpty() && conditionStack.peek().skip;

        Iterator<Token> iter = node.getTokens().iterator();
        token = iter.next();
        if (!iter.hasNext()) {
            throw new CompilerException("expecting directive", new Token(token.getStream(), token.stop));
        }
        token = iter.next();
        if ("define".equals(token.getText())) {
            if (!skip) {
                if (!iter.hasNext()) {
                    throw new CompilerException("expecting identifier", new Token(token.getStream(), token.stop));
                }
                token = iter.next();
                if (token.type != 0 && token.type != Token.KEYWORD) {
                    throw new CompilerException("invalid identifier", token);
                }
                String identifier = token.getText();
                if (scope.hasSymbol(identifier) || scope.isDefined(identifier)) {
                    logMessage(new CompilerException(CompilerException.WARNING, "duplicated definition: " + identifier, token));
                }

                List<Token> list = new ArrayList<>();
                if (iter.hasNext()) {
                    while (iter.hasNext()) {
                        list.add(iter.next());
                    }
                }
                scope.addDefinition(identifier, list);
            }
        }
        else if ("ifdef".equals(token.getText())) {
            if (!skip) {
                if (!iter.hasNext()) {
                    throw new CompilerException("expecting identifier", new Token(token.getStream(), token.stop));
                }
                Token identifier = iter.next();
                if (token.type != 0 && token.type != Token.KEYWORD) {
                    throw new CompilerException("invalid identifier", identifier);
                }
                skip = !(scope.isDefined(identifier.getText()) || scope.hasSymbol(identifier.getText()));
                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("elifdef".equals(token.getText()) || "elseifdef".equals(token.getText())) {
            if (conditionStack.isEmpty()) {
                throw new CompilerException("misplaced #" + token.getText(), token);
            }
            if (conditionStack.peek().evaluated) {
                conditionStack.pop();

                Token identifier = iter.next();
                if (token.type != 0 && token.type != Token.KEYWORD) {
                    throw new CompilerException("invalid identifier", identifier);
                }
                skip = !(scope.isDefined(identifier.getText()) || scope.hasSymbol(identifier.getText()));

                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("ifndef".equals(token.getText())) {
            if (!skip) {
                if (!iter.hasNext()) {
                    throw new CompilerException("expecting identifier", new Token(token.getStream(), token.stop));
                }
                Token identifier = iter.next();
                if (token.type != 0 && token.type != Token.KEYWORD) {
                    throw new CompilerException("invalid identifier", identifier);
                }
                skip = scope.isDefined(identifier.getText()) || scope.hasSymbol(identifier.getText());
                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("elifndef".equals(token.getText()) || "elseifndef".equals(token.getText())) {
            if (conditionStack.isEmpty()) {
                throw new CompilerException("misplaced #" + token.getText(), token);
            }
            if (conditionStack.peek().evaluated) {
                conditionStack.pop();

                Token identifier = iter.next();
                if (token.type != 0 && token.type != Token.KEYWORD) {
                    throw new CompilerException("invalid identifier", identifier);
                }
                skip = scope.isDefined(identifier.getText()) || scope.hasSymbol(identifier.getText());

                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("else".equals(token.getText())) {
            if (conditionStack.isEmpty()) {
                throw new CompilerException("misplaced #" + token.getText(), token);
            }
            if (conditionStack.peek().evaluated) {
                skip = !conditionStack.pop().skip;
                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("if".equals(token.getText())) {
            if (!skip) {
                Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                while (iter.hasNext()) {
                    token = iter.next();
                    if ("defined".equals(token.getText())) {
                        builder.addToken(token);
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                    }
                    else {
                        builder.addToken(token);
                    }
                }
                Expression expression = builder.getExpression();
                if (!expression.isConstant()) {
                    throw new RuntimeException("expression is not constant");
                }
                skip = expression.getNumber().intValue() == 0;
                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("elif".equals(token.getText()) || "elseif".equals(token.getText())) {
            if (conditionStack.isEmpty()) {
                throw new CompilerException("misplaced #" + token.getText(), token);
            }
            if (conditionStack.peek().evaluated) {
                conditionStack.pop();

                Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder(scope);
                while (iter.hasNext()) {
                    token = iter.next();
                    if ("defined".equals(token.getText())) {
                        builder.addToken(token);
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                        if (iter.hasNext()) {
                            builder.addTokenLiteral(iter.next());
                        }
                    }
                    else {
                        builder.addToken(token);
                    }
                }
                Expression expression = builder.getExpression();
                if (!expression.isConstant()) {
                    throw new RuntimeException("expression is not constant");
                }
                skip = expression.getNumber().intValue() == 0;

                conditionStack.push(new Condition(node, true, skip));
            }
            else {
                conditionStack.push(new Condition(node, false, skip));
            }
        }
        else if ("endif".equals(token.getText())) {
            if (conditionStack.isEmpty()) {
                throw new CompilerException("misplaced #" + token.getText(), token);
            }
            conditionStack.pop();
        }
        else {
            logMessage(new CompilerException("unsupported directive", token));
        }
    }

    void determineClock() {
        Expression _clkmode = scope.getLocalSymbol("_CLKMODE");
        if (_clkmode == null) {
            _clkmode = scope.getLocalSymbol("_clkmode");
        }

        Expression _clkfreq = scope.getLocalSymbol("_CLKFREQ");
        if (_clkfreq == null) {
            _clkfreq = scope.getLocalSymbol("_clkfreq");
        }

        Expression _xinfreq = scope.getLocalSymbol("_XINFREQ");
        if (_xinfreq == null) {
            _xinfreq = scope.getLocalSymbol("_xinfreq");
        }

        if (_clkmode == null && _clkfreq == null && _xinfreq == null) {
            scope.addSymbol("CLKMODE", new NumberLiteral(0));
            scope.addSymbol("CLKFREQ", new NumberLiteral(12_000_000));
            return;
        }

        if (_clkmode == null && (_clkfreq != null || _xinfreq != null)) {
            throw new CompilerException("_CLKFREQ / _XINFREQ specified without _CLKMODE", _clkfreq != null ? _clkfreq.getData() : _xinfreq.getData());
        }
        if (_clkfreq != null && _xinfreq != null) {
            throw new CompilerException("either _CLKFREQ or _XINFREQ must be specified, but not both", _clkfreq != null ? _clkfreq.getData() : _xinfreq.getData());
        }

        int mode = _clkmode.getNumber().intValue();
        if (mode == 0 || (mode & 0xFFFFF800) != 0 || (((mode & 0x03) != 0) && ((mode & 0x7FC) != 0))) {
            throw new CompilerException("invalid _CLKMODE specified", _clkmode.getData());
        }
        if ((mode & 0x03) != 0) { // RCFAST or RCSLOW
            if (_clkfreq != null || _xinfreq != null) {
                throw new CompilerException("_CLKFREQ / _XINFREQ not allowed with RCFAST / RCSLOW", _clkfreq != null ? _clkfreq.getData() : _xinfreq.getData());
            }

            scope.addSymbol("CLKMODE", new NumberLiteral(mode == 2 ? 1 : 0));
            scope.addSymbol("CLKFREQ", new NumberLiteral(mode == 2 ? 20_000 : 12_000_000));
            return;
        }

        if (_clkfreq == null && _xinfreq == null) {
            throw new CompilerException("_CLKFREQ or _XINFREQ must be specified", _clkmode.getData());
        }

        try {
            int bitPos = getBitPos((mode >> 2) & 0x0F);
            int clkmode = (bitPos << 3) | 0x22;

            int freqshift = 0;
            if ((mode & 0x7C0) != 0) {
                freqshift = getBitPos(mode >> 6);
                clkmode += freqshift + 0x41;
            }
            if (_xinfreq != null) {
                scope.addSymbol("CLKFREQ", new NumberLiteral(_xinfreq.getNumber().intValue() << freqshift));
            }
            else {
                scope.addSymbol("CLKFREQ", _clkfreq);
            }
            scope.addSymbol("CLKMODE", new NumberLiteral(clkmode));
        } catch (Exception e) {
            throw new CompilerException(e, _clkmode.getData());
        }
    }

    int getBitPos(int value) {
        int bitCount = 0;
        int bitPos = 0;

        for (int i = 32; i > 0; i--) {
            if ((value & 0x01) != 0) {
                bitPos = 32 - i;
                bitCount++;
            }
            value >>= 1;
        }

        if (bitCount != 1) {
            throw new RuntimeException("invalid _CLKMODE specified");
        }

        return bitPos;
    }

    @Override
    public Context getScope() {
        return scope;
    }

    @Override
    protected void logMessage(CompilerException message) {
        if (message.hasChilds()) {
            for (CompilerException msg : message.getChilds()) {
                if (msg.type == CompilerException.ERROR) {
                    errors = true;
                }
            }
        }
        else {
            if (message.type == CompilerException.ERROR) {
                errors = true;
            }
        }
        messages.add(message);
    }

    @Override
    public boolean hasErrors() {
        return errors;
    }

    public List<CompilerException> getMessages() {
        return messages;
    }

}
