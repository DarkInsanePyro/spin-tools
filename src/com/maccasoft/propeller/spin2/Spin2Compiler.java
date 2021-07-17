/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.maccasoft.propeller.CompilerMessage;
import com.maccasoft.propeller.expressions.Add;
import com.maccasoft.propeller.expressions.Addbits;
import com.maccasoft.propeller.expressions.Addpins;
import com.maccasoft.propeller.expressions.And;
import com.maccasoft.propeller.expressions.CharacterLiteral;
import com.maccasoft.propeller.expressions.ContextLiteral;
import com.maccasoft.propeller.expressions.Divide;
import com.maccasoft.propeller.expressions.Expression;
import com.maccasoft.propeller.expressions.HubContextLiteral;
import com.maccasoft.propeller.expressions.Identifier;
import com.maccasoft.propeller.expressions.LocalVariable;
import com.maccasoft.propeller.expressions.Method;
import com.maccasoft.propeller.expressions.Modulo;
import com.maccasoft.propeller.expressions.Multiply;
import com.maccasoft.propeller.expressions.NumberLiteral;
import com.maccasoft.propeller.expressions.Or;
import com.maccasoft.propeller.expressions.Register;
import com.maccasoft.propeller.expressions.ShiftLeft;
import com.maccasoft.propeller.expressions.ShiftRight;
import com.maccasoft.propeller.expressions.Subtract;
import com.maccasoft.propeller.expressions.Trunc;
import com.maccasoft.propeller.expressions.Variable;
import com.maccasoft.propeller.expressions.Xor;
import com.maccasoft.propeller.model.ConstantAssignEnumNode;
import com.maccasoft.propeller.model.ConstantAssignNode;
import com.maccasoft.propeller.model.ConstantSetEnumNode;
import com.maccasoft.propeller.model.ConstantsNode;
import com.maccasoft.propeller.model.DataLineNode;
import com.maccasoft.propeller.model.DataNode;
import com.maccasoft.propeller.model.LocalVariableNode;
import com.maccasoft.propeller.model.MethodNode;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.model.NodeVisitor;
import com.maccasoft.propeller.model.ObjectNode;
import com.maccasoft.propeller.model.ObjectsNode;
import com.maccasoft.propeller.model.ParameterNode;
import com.maccasoft.propeller.model.StatementNode;
import com.maccasoft.propeller.model.Token;
import com.maccasoft.propeller.model.VariableNode;
import com.maccasoft.propeller.model.VariablesNode;
import com.maccasoft.propeller.spin2.Spin2Bytecode.Descriptor;
import com.maccasoft.propeller.spin2.Spin2Object.LongDataObject;
import com.maccasoft.propeller.spin2.bytecode.Bytecode;
import com.maccasoft.propeller.spin2.bytecode.CaseJmp;
import com.maccasoft.propeller.spin2.bytecode.CaseRangeJmp;
import com.maccasoft.propeller.spin2.bytecode.Constant;
import com.maccasoft.propeller.spin2.bytecode.Djnz;
import com.maccasoft.propeller.spin2.bytecode.Jmp;
import com.maccasoft.propeller.spin2.bytecode.Jnz;
import com.maccasoft.propeller.spin2.bytecode.Jz;
import com.maccasoft.propeller.spin2.bytecode.MathOp;
import com.maccasoft.propeller.spin2.bytecode.MemoryOp;
import com.maccasoft.propeller.spin2.bytecode.RegisterOp;
import com.maccasoft.propeller.spin2.bytecode.Tjz;
import com.maccasoft.propeller.spin2.bytecode.VariableOp;
import com.maccasoft.propeller.spin2.instructions.Org;
import com.maccasoft.propeller.spin2.instructions.Orgh;

public class Spin2Compiler {

    Spin2Context scope = new Spin2GlobalContext();
    List<Spin2PAsmLine> source = new ArrayList<Spin2PAsmLine>();
    List<Spin2Method> methods = new ArrayList<Spin2Method>();
    List<Spin2Object> objects = new ArrayList<Spin2Object>();

    int varOffset = 4;
    int labelCounter;

    boolean errors;
    List<CompilerMessage> messages = new ArrayList<CompilerMessage>();

    Spin2Object object = new Spin2Object();

    public Spin2Compiler() {

    }

    public Spin2Object compile(Node root) {
        boolean hubMode = false;
        int address = 0, hubAddress = 0;

        for (Node node : root.getChilds()) {
            if (node instanceof ConstantsNode) {
                compileConBlock(node);
            }
        }

        for (Node node : root.getChilds()) {
            if (node instanceof VariablesNode) {
                compileVarBlock(node);
            }
        }

        object.setVarSize(varOffset);

        int _clkfreq = 20000000;
        if (scope.hasSymbol("_clkfreq")) {
            _clkfreq = scope.getSymbol("_clkfreq").getNumber().intValue();
        }
        else if (scope.hasSymbol("_CLKFREQ")) {
            _clkfreq = scope.getSymbol("_CLKFREQ").getNumber().intValue();
        }

        if (!scope.hasSymbol("_clkfreq")) {
            scope.addSymbol("_clkfreq", new NumberLiteral(_clkfreq));
        }
        if (!scope.hasSymbol("_CLKFREQ")) {
            scope.addSymbol("_CLKFREQ", new NumberLiteral(_clkfreq));
        }

        int _clkmode = getClockMode(20000000, _clkfreq);
        if (!scope.hasSymbol("clkmode_")) {
            scope.addSymbol("clkmode_", new NumberLiteral(_clkmode));
        }
        if (!scope.hasSymbol("CLKMODE_")) {
            scope.addSymbol("CLKMODE_", new NumberLiteral(_clkmode));
        }

        object.setClkFreq(_clkfreq);
        object.setClkMode(_clkmode);

        for (Node node : root.getChilds()) {
            if (node instanceof ObjectsNode) {
                compileObjBlock(node);
            }
        }

        for (Node node : root.getChilds()) {
            if (node instanceof DataNode) {
                compileDatBlock(node);
            }
        }

        for (Node node : root.getChilds()) {
            if ((node instanceof MethodNode) && "PUB".equalsIgnoreCase(((MethodNode) node).getType().getText())) {
                Spin2Method method = compileMethod((MethodNode) node);
                scope.addSymbol(method.getLabel(), new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount(), methods.size()));
                scope.addSymbol("@" + method.getLabel(), new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount(), methods.size()));
                method.register();
                methods.add(method);
            }
        }

        for (Node node : root.getChilds()) {
            if ((node instanceof MethodNode) && "PRI".equalsIgnoreCase(((MethodNode) node).getType().getText())) {
                Spin2Method method = compileMethod((MethodNode) node);
                scope.addSymbol(method.getLabel(), new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount(), methods.size()));
                scope.addSymbol("@" + method.getLabel(), new Method(method.getLabel(), method.getParametersCount(), method.getReturnsCount(), methods.size()));
                method.register();
                methods.add(method);
            }
        }

        for (Spin2Method method : methods) {
            for (Spin2MethodLine line : method.getLines()) {
                try {
                    compileLine(line);
                } catch (CompilerMessage e) {
                    logMessage(e);
                    e.printStackTrace();
                } catch (Exception e) {
                    logMessage(new CompilerMessage(e.getMessage(), (Node) line.getData()));
                    e.printStackTrace();
                }
            }
        }

        LongDataObject[] ldo = new LongDataObject[objects.size() * 2];

        if (objects.size() != 0) {
            int index = 0;
            for (int i = 0; i < objects.size(); i++) {
                ldo[index] = object.writeLong(0, "");
                index++;
                ldo[index] = object.writeLong(0, "");
                index++;
            }
        }

        if (methods.size() != 0) {
            scope.addSymbol("@CLKMODE", new NumberLiteral(0x40));
            scope.addSymbol("@clkmode", new NumberLiteral(0x40));
            scope.addSymbol("@CLKFREQ", new NumberLiteral(0x44));
            scope.addSymbol("@clkfreq", new NumberLiteral(0x44));
            hubAddress = (methods.size() + 1) * 4;
        }

        for (Spin2PAsmLine line : source) {
            line.getScope().setHubAddress(hubAddress);
            if (line.getInstructionFactory() instanceof Orgh) {
                hubMode = true;
            }
            if (line.getInstructionFactory() instanceof Org) {
                hubMode = false;
                hubAddress = (hubAddress + 3) & ~3;
            }
            boolean isCogCode = address < 0x200;
            address = line.resolve(hubMode ? hubAddress : address);
            if (line.getInstructionFactory() instanceof Org) {
                isCogCode = address < 0x200;
            }
            if (hubMode) {
                hubAddress += line.getInstructionObject().getSize();
                if (address > hubAddress) {
                    hubAddress = address;
                }
                else {
                    address = hubAddress;
                }
            }
            else {
                if (isCogCode && address > 0x1F0) {
                    throw new RuntimeException("cog code limit exceeded by " + (address - 0x1F0) + " long(s)");
                }
                else if (!isCogCode && address > 0x400) {
                    throw new RuntimeException("lut code limit exceeded by " + (address - 0x400) + " long(s)");
                }
                hubAddress += line.getInstructionObject().getSize();
            }
        }

        LongDataObject[] ld = new LongDataObject[methods.size() + 1];

        if (methods.size() != 0) {
            int index = 0;
            for (Spin2Method method : methods) {
                ld[index] = object.writeLong(0, "Method " + method.getLabel());
                index++;
            }
            ld[index] = object.writeLong(0, "End");
        }

        for (Spin2PAsmLine line : source) {
            hubAddress = line.getScope().getHubAddress();
            if (object.getSize() < hubAddress) {
                object.writeBytes(new byte[hubAddress - object.getSize()], "(filler)");
            }
            object.writeBytes(line.getScope().getAddress(), line.getInstructionObject().getBytes(), line.toString());
        }
        object.alignToLong();

        if (methods.size() != 0) {
            address = object.getSize();
            for (Spin2Method method : methods) {
                address++;
                address = method.resolve(address);
            }

            address = object.getSize();
            for (Spin2Method method : methods) {
                address++;
                address = method.resolve(address);
            }

            int index = 0;
            for (Spin2Method method : methods) {
                int value = 0;

                value = Spin2Method.address_bit.setValue(value, object.getSize());
                value = Spin2Method.returns_bit.setValue(value, method.getReturnsCount());
                value = Spin2Method.parameters_bit.setValue(value, method.getParametersCount());

                ld[index].setValue(value | 0x80000000L);
                ld[index].setText(String.format("Method %s @ $%05X (%d parameters, %d returns)", method.getLabel(), object.getSize(), method.getParametersCount(), method.getReturnsCount()));
                method.writeTo(object);
                index++;
            }
            ld[index].setValue(object.getSize());

            object.alignToLong();

            if (objects.size() != 0) {
                index = 0;
                ldo[index].setValue(object.getSize());
                ldo[index].setText(String.format("Object @ $%05X", object.getSize()));
                index++;
                ldo[index].setValue(varOffset);
                ldo[index].setText(String.format("Variables @ $%05X", varOffset));
                index++;

                Iterator<Spin2Object> iter = objects.iterator();
                while (iter.hasNext()) {
                    Spin2Object obj = iter.next();
                    object.writeObject(obj);
                    varOffset += obj.getVarSize();
                    if (iter.hasNext()) {
                        ldo[index].setValue(object.getSize());
                        ldo[index].setText(String.format("Object @ PBASE+$%05X", object.getSize()));
                        index++;
                        ldo[index].setValue(varOffset);
                        ldo[index].setText(String.format("Variables @ VBASE+$%05X", varOffset));
                        index++;
                    }
                }
            }

            Spin2Interpreter interpreter = new Spin2Interpreter();
            interpreter.setVBase(interpreter.getPBase() + object.getSize());
            interpreter.setDBase(interpreter.getPBase() + object.getSize() + varOffset);
            interpreter.setClearLongs(255 + ((varOffset + 3) / 4));
            object.setInterpreter(interpreter);
        }

        return object;
    }

    public Spin2Object getObject() {
        return object;
    }

    void compileConBlock(Node parent) {

        parent.accept(new NodeVisitor() {
            int enumValue = 0, enumIncrement = 1;

            @Override
            public void visitConstantAssign(ConstantAssignNode node) {
                String name = node.identifier.getText();
                try {
                    Expression expression = buildExpression(node.expression, scope);
                    expression.setData(node);
                    try {
                        scope.addSymbol(name, expression);
                    } catch (CompilerMessage e) {
                        logMessage(e);
                    } catch (Exception e) {
                        logMessage(new CompilerMessage(e.getMessage(), node.identifier));
                        e.printStackTrace();
                    }
                } catch (CompilerMessage e) {
                    logMessage(e);
                } catch (Exception e) {
                    logMessage(new CompilerMessage(e.getMessage(), node.expression));
                    e.printStackTrace();
                }
            }

            @Override
            public void visitConstantSetEnum(ConstantSetEnumNode node) {
                Expression expression = buildExpression(node.start, scope);
                enumValue = expression.getNumber().intValue();
                if (node.step != null) {
                    expression = buildExpression(node.step, scope);
                    enumIncrement = expression.getNumber().intValue();
                }
                else {
                    enumIncrement = 1;
                }
            }

            @Override
            public void visitConstantAssignEnum(ConstantAssignEnumNode node) {
                try {
                    scope.addSymbol(node.identifier.getText(), new NumberLiteral(enumValue));
                } catch (Exception e) {
                    logMessage(new CompilerMessage(e.getMessage(), node.identifier));
                    e.printStackTrace();
                    return;
                }
                if (node.multiplier != null) {
                    Expression expression = buildExpression(node.multiplier, scope);
                    enumValue += enumIncrement * expression.getNumber().intValue();
                }
                else {
                    enumValue += enumIncrement;
                }
            }

        });
    }

    void compileVarBlock(Node parent) {

        for (Node child : parent.getChilds()) {
            VariableNode node = (VariableNode) child;
            if (node.identifier == null) {
                continue;
            }

            String type = "LONG";
            if (node.type != null) {
                type = node.type.getText().toUpperCase();
            }

            Expression size = new NumberLiteral(1);
            if (node.size != null) {
                size = buildExpression(node.size.getTokens(), scope);
            }

            try {
                scope.addSymbol(node.identifier.getText(), new Variable(type, node.identifier.getText(), size, varOffset));
                scope.addSymbol("@" + node.identifier.getText(), new Variable(type, node.identifier.getText(), size, varOffset));
            } catch (Exception e) {
                logMessage(new CompilerMessage(e.getMessage(), node.identifier));
                e.printStackTrace();
                continue;
            }

            int varSize = size.getNumber().intValue();
            if ("WORD".equalsIgnoreCase(type)) {
                varSize = varSize * 2;
            }
            else if (!"BYTE".equalsIgnoreCase(type)) {
                varSize = varSize * 4;
            }
            varOffset += varSize;
        }
    }

    void compileObjBlock(Node parent) {

        for (Node child : parent.getChilds()) {
            ObjectNode node = (ObjectNode) child;
            if (node.name != null && node.file != null) {
                String text = getObjectSource(node.file.getText());

                Spin2TokenStream stream = new Spin2TokenStream(text);
                Spin2Parser subject = new Spin2Parser(stream);
                Node root = subject.parse();

                Spin2Compiler compiler = new Spin2Compiler();
                objects.add(compiler.compile(root));
            }

        }
    }

    protected String getObjectSource(String fileName) {
        return "";
    }

    void compileDatBlock(Node parent) {
        Spin2Context savedContext = scope;

        for (Node child : parent.getChilds()) {
            DataLineNode node = (DataLineNode) child;
            Spin2PAsmLine pasmLine = compileDataLine(node);
            pasmLine.setData(node);
            source.addAll(pasmLine.expand());
        }

        scope = savedContext;
    }

    Spin2PAsmLine compileDataLine(DataLineNode node) {
        String label = node.label != null ? node.label.getText() : null;
        String condition = node.condition != null ? node.condition.getText() : null;
        String mnemonic = node.instruction != null ? node.instruction.getText() : null;
        String modifier = node.modifier != null ? node.modifier.getText() : null;
        List<Spin2PAsmExpression> parameters = new ArrayList<Spin2PAsmExpression>();

        Spin2Context localScope = new Spin2Context(scope);

        for (ParameterNode param : node.parameters) {
            int index = 0;
            String prefix = null;
            Expression expression = null, count = null;

            if (param.getText().toUpperCase().contains("PTRA") || param.getText().toUpperCase().contains("PTRB")) {
                expression = new Identifier(param.getText(), scope);
            }
            else {
                Token token;
                if (index < param.getTokens().size()) {
                    token = param.getToken(index);
                    if (token.getText().startsWith("#")) {
                        prefix = (prefix == null ? "" : prefix) + token.getText();
                        index++;
                    }
                }
                if (index < param.getTokens().size()) {
                    token = param.getToken(index);
                    if ("\\".equals(token.getText())) {
                        prefix = (prefix == null ? "" : prefix) + token.getText();
                        index++;
                    }
                }
                if (index < param.getTokens().size()) {
                    try {
                        expression = buildExpression(param.getTokens().subList(index, param.getTokens().size()), localScope);
                    } catch (Exception e) {
                        for (Token t : param.getTokens().subList(index, param.getTokens().size())) {
                            System.err.print(" " + t.getText());
                        }
                        System.err.println();
                        throw e;
                    }
                }
            }
            if (param.count != null) {
                try {
                    count = buildExpression(param.count, localScope);
                } catch (Exception e) {
                    for (Token t : param.count.getTokens()) {
                        System.err.print(" " + t.getText());
                    }
                    System.err.println();
                    throw e;
                }
            }
            parameters.add(new Spin2PAsmExpression(prefix, expression, count));
        }

        Spin2PAsmLine pasmLine = new Spin2PAsmLine(localScope, label, condition, mnemonic, parameters, modifier);
        if (pasmLine.getLabel() != null) {
            try {
                if (!pasmLine.isLocalLabel() && scope.getParent() != null) {
                    scope = scope.getParent();
                }
                scope.addSymbol(pasmLine.getLabel(), new ContextLiteral(pasmLine.getScope()));
                scope.addSymbol("@" + pasmLine.getLabel(), new HubContextLiteral(pasmLine.getScope()));
                if (!pasmLine.isLocalLabel()) {
                    scope = new Spin2Context(scope);
                }
            } catch (RuntimeException e) {
                System.err.println(pasmLine);
                e.printStackTrace();
            }
        }

        return pasmLine;
    }

    Spin2Method compileMethod(MethodNode node) {
        Spin2Context localScope = new Spin2Context(scope);
        List<LocalVariable> parameters = new ArrayList<LocalVariable>();
        List<LocalVariable> returns = new ArrayList<LocalVariable>();
        List<LocalVariable> localVariables = new ArrayList<LocalVariable>();

        //print(node, 0);

        localScope.addSymbol("recv", new Register(0x1D2));
        localScope.addSymbol("send", new Register(0x1D3));

        localScope.addSymbol("pr0", new Register(0x1D8));
        localScope.addSymbol("pr1", new Register(0x1D9));
        localScope.addSymbol("pr2", new Register(0x1DA));
        localScope.addSymbol("pr3", new Register(0x1DB));
        localScope.addSymbol("pr4", new Register(0x1DC));
        localScope.addSymbol("pr5", new Register(0x1DD));
        localScope.addSymbol("pr6", new Register(0x1DE));
        localScope.addSymbol("pr7", new Register(0x1DF));

        localScope.addSymbol("RECV", new Register(0x1D2));
        localScope.addSymbol("SEND", new Register(0x1D3));

        localScope.addSymbol("PR0", new Register(0x1D8));
        localScope.addSymbol("PR1", new Register(0x1D9));
        localScope.addSymbol("PR2", new Register(0x1DA));
        localScope.addSymbol("PR3", new Register(0x1DB));
        localScope.addSymbol("PR4", new Register(0x1DC));
        localScope.addSymbol("PR5", new Register(0x1DD));
        localScope.addSymbol("PR6", new Register(0x1DE));
        localScope.addSymbol("PR7", new Register(0x1DF));

        int offset = 0;
        for (Node child : node.getParameters()) {
            String name = child.getText();
            Expression expression = localScope.getLocalSymbol(name);
            if (expression instanceof LocalVariable) {
                logMessage(new CompilerMessage("symbol " + name + " already defined", child));
                continue;
            }
            else if (expression != null) {
                logMessage(new CompilerMessage(CompilerMessage.WARNING, "parameter " + name + " hides global variable", child));
            }
            LocalVariable var = new LocalVariable("LONG", name, new NumberLiteral(1), offset);
            localScope.addSymbol(name, var);
            localScope.addSymbol("@" + name, var);
            parameters.add(var);
            offset += 4;
        }

        for (Node child : node.getReturnVariables()) {
            String name = child.getText();
            Expression expression = localScope.getLocalSymbol(name);
            if (expression instanceof LocalVariable) {
                logMessage(new CompilerMessage("symbol " + name + " already defined", child));
                continue;
            }
            else if (expression != null) {
                logMessage(new CompilerMessage(CompilerMessage.WARNING, "return variable " + name + " hides global variable", child));
            }
            LocalVariable var = new LocalVariable("LONG", name, new NumberLiteral(1), offset);
            localScope.addSymbol(name, var);
            localScope.addSymbol("@" + name, var);
            returns.add(var);
            offset += 4;
        }

        for (LocalVariableNode child : node.getLocalVariables()) {
            String name = child.getIdentifier().getText();
            Expression expression = localScope.getLocalSymbol(name);
            if (expression instanceof LocalVariable) {
                logMessage(new CompilerMessage("symbol " + name + " already defined", child.getIdentifier()));
                continue;
            }
            else if (expression != null) {
                logMessage(new CompilerMessage(CompilerMessage.WARNING, "local variable " + name + " hides global variable", child.getIdentifier()));
            }

            String type = "LONG";
            if (child.type != null) {
                type = child.type.getText().toUpperCase();
            }
            Expression size = null;
            if (child.size != null) {
                try {
                    size = buildExpression(child.size.getTokens(), scope);
                    if (!size.isConstant()) {
                        logMessage(new CompilerMessage("expression is not constant", child.size));
                        continue;
                    }
                } catch (Exception e) {
                    logMessage(new CompilerMessage("expression syntax error", child.size));
                    continue;
                }
            }
            LocalVariable var = new LocalVariable(type, child.getText(), size, offset);
            localScope.addSymbol(child.getIdentifier().getText(), var);
            localScope.addSymbol("@" + child.getIdentifier().getText(), var);
            localVariables.add(var);

            int count = 4;
            if ("BYTE".equalsIgnoreCase(type)) {
                count = 1;
            }
            if ("WORD".equalsIgnoreCase(type)) {
                count = 2;
            }
            if (size != null) {
                count = count * size.getNumber().intValue();
            }
            offset += ((count + 3) / 4) * 4;
        }

        Spin2Method method = new Spin2Method(localScope, node.name.getText(), parameters, returns, localVariables);
        method.setComment(node.getText());

        List<Spin2MethodLine> childs = compileStatements(localScope, node.getChilds());
        childs.add(new Spin2MethodLine(localScope, String.format(".label_" + labelCounter++), "RETURN", Collections.emptyList()));
        for (Spin2MethodLine line : childs) {
            method.addSource(line);
        }

        return method;
    }

    List<Spin2MethodLine> compileStatements(Spin2Context context, List<Node> childs) {
        List<Spin2MethodLine> lines = new ArrayList<Spin2MethodLine>();

        for (Node node : childs) {
            if (node instanceof StatementNode) {
                try {
                    Iterator<Token> iter = node.getTokens().iterator();
                    if (!iter.hasNext()) {
                        throw new RuntimeException("syntax error");
                    }

                    Token token = iter.next();
                    if ("ABORT".equalsIgnoreCase(token.getText())) {
                        if (iter.hasNext()) {
                            Spin2TreeBuilder builder = new Spin2TreeBuilder();
                            while (iter.hasNext()) {
                                builder.addToken(iter.next());
                            }
                            Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), builder.getRoot());
                            line.setText(node.getText());
                            line.setData(node);
                            lines.add(line);
                        }
                        else {
                            Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), Collections.emptyList());
                            line.setText(node.getText());
                            line.setData(node);
                            lines.add(line);
                        }
                    }
                    else if ("IF".equalsIgnoreCase(token.getText()) || "IFNOT".equalsIgnoreCase(token.getText())) {
                        if (!iter.hasNext()) {
                            throw new RuntimeException("expected expression");
                        }

                        Spin2TreeBuilder builder = new Spin2TreeBuilder();
                        while (iter.hasNext()) {
                            builder.addToken(iter.next());
                        }

                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), builder.getRoot());
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);

                        line.addChilds(compileStatements(context, node.getChilds()));

                        Spin2MethodLine falseLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                        lines.add(falseLine);
                        line.setData("false", falseLine);

                        Spin2MethodLine exitLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                        lines.add(exitLine);
                    }
                    else if ("ELSEIF".equalsIgnoreCase(token.getText()) || "ELSEIFNOT".equalsIgnoreCase(token.getText())) {
                        if (!iter.hasNext()) {
                            throw new RuntimeException("expected expression");
                        }

                        Spin2TreeBuilder builder = new Spin2TreeBuilder();
                        while (iter.hasNext()) {
                            builder.addToken(iter.next());
                        }
                        Spin2StatementNode argument = builder.getRoot();

                        Spin2MethodLine exitLine = lines.remove(lines.size() - 1);
                        lines.get(lines.size() - 2).setData("exit", exitLine);

                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), argument);
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);

                        line.addChilds(compileStatements(context, node.getChilds()));

                        Spin2MethodLine falseLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                        lines.add(falseLine);
                        line.setData("false", falseLine);

                        lines.add(exitLine);
                    }
                    else if ("ELSE".equalsIgnoreCase(token.getText())) {
                        if (iter.hasNext()) {
                            throw new RuntimeException("syntax error");
                        }

                        Spin2MethodLine exitLine = lines.remove(lines.size() - 1);
                        lines.get(lines.size() - 2).setData("exit", exitLine);

                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), Collections.emptyList());
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);

                        line.addChilds(compileStatements(context, node.getChilds()));

                        lines.add(exitLine);
                    }
                    else if ("REPEAT".equalsIgnoreCase(token.getText())) {
                        List<Spin2StatementNode> arguments = new ArrayList<Spin2StatementNode>();

                        String text = token.getText();

                        if (iter.hasNext()) {
                            token = iter.next();
                            if ("WHILE".equalsIgnoreCase(token.getText()) || "UNTIL".equalsIgnoreCase(token.getText())) {
                                text += " " + token.getText();
                                Spin2TreeBuilder builder = new Spin2TreeBuilder();
                                while (iter.hasNext()) {
                                    builder.addToken(iter.next());
                                }
                                arguments.add(builder.getRoot());
                            }
                            else {
                                Spin2TreeBuilder builder = new Spin2TreeBuilder();
                                builder.addToken(token);

                                while (iter.hasNext()) {
                                    token = iter.next();
                                    if ("FROM".equalsIgnoreCase(token.getText())) {
                                        break;
                                    }
                                    builder.addToken(token);
                                }
                                arguments.add(builder.getRoot());

                                if ("FROM".equalsIgnoreCase(token.getText())) {
                                    builder = new Spin2TreeBuilder();
                                    while (iter.hasNext()) {
                                        token = iter.next();
                                        if ("TO".equalsIgnoreCase(token.getText())) {
                                            break;
                                        }
                                        builder.addToken(token);
                                    }
                                    arguments.add(builder.getRoot());

                                    if ("TO".equalsIgnoreCase(token.getText())) {
                                        builder = new Spin2TreeBuilder();
                                        while (iter.hasNext()) {
                                            token = iter.next();
                                            if ("STEP".equalsIgnoreCase(token.getText())) {
                                                break;
                                            }
                                            builder.addToken(token);
                                        }
                                        arguments.add(builder.getRoot());

                                        if ("STEP".equalsIgnoreCase(token.getText())) {
                                            builder = new Spin2TreeBuilder();
                                            while (iter.hasNext()) {
                                                builder.addToken(iter.next());
                                            }
                                            arguments.add(builder.getRoot());
                                        }
                                    }
                                }
                            }
                        }

                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), text, arguments);
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);

                        Spin2MethodLine loopLine = line;
                        if (arguments.size() == 1) {
                            loopLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                            line.addChild(loopLine);
                        }

                        line.addChilds(compileStatements(context, node.getChilds()));

                        if (arguments.size() == 3 || arguments.size() == 4) {
                            loopLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), "REPEAT-LOOP", Collections.emptyList());
                            line.addChild(loopLine);
                        }
                        else {
                            line.addChild(new Spin2MethodLine(context, String.format(".label_" + labelCounter++), "NEXT", Collections.emptyList()));
                        }

                        line.setData("next", loopLine);

                        Spin2MethodLine quitLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                        lines.add(quitLine);
                        line.setData("quit", quitLine);
                    }
                    else if ("WHILE".equalsIgnoreCase(token.getText()) || "UNTIL".equalsIgnoreCase(token.getText())) {
                        if (!iter.hasNext()) {
                            throw new RuntimeException("expected expression");
                        }
                        Spin2TreeBuilder builder = new Spin2TreeBuilder();
                        while (iter.hasNext()) {
                            builder.addToken(iter.next());
                        }
                        Spin2StatementNode argument = builder.getRoot();

                        Spin2MethodLine repeatLine = lines.get(lines.size() - 2);
                        repeatLine.childs.remove(repeatLine.childs.size() - 1);

                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), argument);
                        line.setText(node.getText());
                        line.setData(node);
                        line.setData("true", repeatLine);
                        lines.add(lines.size() - 1, line);

                        repeatLine.setData("next", line);
                    }
                    else if ("QUIT".equalsIgnoreCase(token.getText()) || "NEXT".equalsIgnoreCase(token.getText())) {
                        if (iter.hasNext()) {
                            throw new RuntimeException("syntax error");
                        }
                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), Collections.emptyList());
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);
                    }
                    else if ("CASE".equalsIgnoreCase(token.getText())) {
                        List<Spin2StatementNode> arguments = new ArrayList<Spin2StatementNode>();
                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), token.getText(), arguments);
                        line.setText(node.getText());

                        if (!iter.hasNext()) {
                            throw new RuntimeException("expected expression");
                        }
                        Spin2TreeBuilder builder = new Spin2TreeBuilder();
                        while (iter.hasNext()) {
                            builder.addToken(iter.next());
                        }
                        arguments.add(builder.getRoot());

                        for (Node child : node.getChilds()) {
                            if (child instanceof StatementNode) {
                                Iterator<Token> childIter = child.getTokens().iterator();
                                if (!childIter.hasNext()) {
                                    throw new RuntimeException("expected expression");
                                }

                                while (childIter.hasNext()) {
                                    token = childIter.next();
                                    if (":".equals(token.getText())) {
                                        break;
                                    }
                                    builder.addToken(token);
                                }
                                if (childIter.hasNext()) {
                                    throw new RuntimeException("syntax error");
                                }

                                Spin2MethodLine targetLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                                targetLine.addChilds(compileStatements(context, child.getChilds()));
                                targetLine.addChild(new Spin2MethodLine(context, String.format(".label_" + labelCounter++), "CASE_DONE", Collections.emptyList()));

                                Spin2StatementNode expression = builder.getRoot();
                                if ("OTHER".equalsIgnoreCase(expression.getText())) {
                                    line.childs.add(0, targetLine);
                                }
                                else {
                                    expression.setData("true", targetLine);
                                    line.addChild(targetLine);
                                    arguments.add(expression);
                                }
                            }
                        }

                        Spin2MethodLine doneLine = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, Collections.emptyList());
                        line.addChild(doneLine);
                        line.setData("end", doneLine);
                        line.setData(node);
                        lines.add(line);
                    }
                    else {
                        Spin2TreeBuilder builder = new Spin2TreeBuilder();
                        iter = node.getTokens().iterator();
                        while (iter.hasNext()) {
                            token = iter.next();
                            builder.addToken(token);
                        }
                        Spin2MethodLine line = new Spin2MethodLine(context, String.format(".label_" + labelCounter++), null, builder.getRoot());
                        line.setText(node.getText());
                        line.setData(node);
                        lines.add(line);
                    }

                } catch (CompilerMessage e) {
                    logMessage(e);
                    throw e;
                } catch (Exception e) {
                    logMessage(new CompilerMessage(e.getMessage(), node));
                    e.printStackTrace();
                }
            }
        }

        return lines;
    }

    void compileLine(Spin2MethodLine line) {
        String text = line.getStatement();

        if ("ABORT".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                line.addSource(new Bytecode(line.getScope(), 0x06, text));
            }
            else if (line.getArgumentsCount() == 1) {
                line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
                line.addSource(new Bytecode(line.getScope(), 0x07, text));
            }
            else {
                throw new RuntimeException("expected 0 or 1 argument(s), found " + line.getArgumentsCount());
            }
        }
        else if ("IF".equalsIgnoreCase(text) || "ELSEIF".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("false");
            line.addSource(new Jz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("IFNOT".equalsIgnoreCase(text) || "ELSEIFNOT".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("false");
            line.addSource(new Jnz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("ELSE".equalsIgnoreCase(text)) {

        }
        else if ("REPEAT".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 1) {
                try {
                    Expression expression = buildConstantExpression(line.getScope(), line.getArgument(0));
                    line.addSource(new Constant(line.getScope(), expression));
                } catch (Exception e) {
                    line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
                    Spin2MethodLine target = (Spin2MethodLine) line.getData("quit");
                    line.addSource(new Tjz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
                }
            }
            else if (line.getArgumentsCount() == 3 || line.getArgumentsCount() == 4) {
                Spin2MethodLine end = line.getChilds().get(0);
                line.addSource(new Constant(line.getScope(), new Identifier(end.getLabel(), end.getScope())));

                try {
                    Expression from = buildConstantExpression(line.getScope(), line.getArgument(1));
                    Expression to = buildConstantExpression(line.getScope(), line.getArgument(2));
                    if (to.isConstant() && from.isConstant()) {
                        if (to.getNumber().intValue() < from.getNumber().intValue()) {
                            line.addSource(new Constant(line.getScope(), new NumberLiteral(from.getNumber().intValue())));
                            if (line.getArgumentsCount() == 4) {
                                line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(3), true));
                            }
                            line.addSource(new Constant(line.getScope(), new NumberLiteral(to.getNumber().intValue())));
                        }
                        else {
                            line.addSource(new Constant(line.getScope(), new NumberLiteral(to.getNumber().intValue())));
                            if (line.getArgumentsCount() == 4) {
                                line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(3), true));
                            }
                            line.addSource(new Constant(line.getScope(), new NumberLiteral(from.getNumber().intValue())));
                        }
                    }
                    else {
                        line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(2), true));
                        if (line.getArgumentsCount() == 4) {
                            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(3), true));
                        }
                        line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(1), true));
                    }
                } catch (Exception e) {
                    line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(2), true));
                    if (line.getArgumentsCount() == 4) {
                        line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(3), true));
                    }
                    line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(1), true));
                }

                String varText = line.getArgument(0).getText();
                Expression expression = line.getScope().getLocalSymbol(varText);
                if (expression == null) {
                    throw new CompilerMessage("undefined symbol " + varText, line.getArgument(0).getToken());
                }
                else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                    line.addSource(new VariableOp(line.getScope(), VariableOp.Op.Setup, false, (Variable) expression));
                    line.addSource(new Bytecode(line.getScope(), line.getArgumentsCount() == 4 ? 0x7D : 0x7C, "REPEAT"));
                }
                else {
                    throw new RuntimeException("unsupported " + line.getArgument(0));
                }
            }
            else if (line.getArgumentsCount() != 0) {
                throw new RuntimeException("unsupported");
            }
        }
        else if ("REPEAT WHILE".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("quit");
            line.addSource(new Jz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("REPEAT UNTIL".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("quit");
            line.addSource(new Jnz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("WHILE".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("true");
            line.addSource(new Jnz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("UNTIL".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                throw new RuntimeException("expected expression");
            }
            if (line.getArgumentsCount() != 1) {
                throw new RuntimeException("syntax error");
            }
            line.addSource(compileBytecodeExpression(line.getScope(), line.getArgument(0), true));
            Spin2MethodLine target = (Spin2MethodLine) line.getData("true");
            line.addSource(new Jz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("NEXT".equalsIgnoreCase(text)) {
            Spin2MethodLine repeat = line.getParent();
            while (repeat != null) {
                if ("REPEAT".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                if ("REPEAT WHILE".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                if ("REPEAT UNTIL".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                repeat = repeat.getParent();
            }

            Spin2MethodLine target = (Spin2MethodLine) repeat.getData("next");

            if ("REPEAT WHILE".equalsIgnoreCase(repeat.getStatement()) || "REPEAT UNTIL".equalsIgnoreCase(repeat.getStatement())) {
                line.addSource(new Jmp(line.getScope(), new Identifier(repeat.getLabel(), repeat.getScope())));
            }
            else if (repeat.getArgumentsCount() == 1) {
                line.addSource(new Djnz(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
            }
            else {
                line.addSource(new Jmp(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
            }
        }
        else if ("REPEAT-LOOP".equalsIgnoreCase(text)) {
            Spin2MethodLine repeat = line.getParent();
            while (repeat != null) {
                if ("REPEAT".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                repeat = repeat.getParent();
            }

            String varText = repeat.getArgument(0).getText();
            Expression expression = line.getScope().getLocalSymbol(varText);
            if (expression == null) {
                throw new CompilerMessage("undefined symbol " + varText, repeat.getArgument(0).getToken());
            }
            else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                line.addSource(new VariableOp(line.getScope(), VariableOp.Op.Setup, false, (Variable) expression));
                line.addSource(new Bytecode(line.getScope(), 0x7E, "REPEAT_LOOP"));
            }
            else {
                throw new RuntimeException("unsupported " + line.getArgument(0));
            }
        }
        else if ("QUIT".equalsIgnoreCase(text)) {
            Spin2MethodLine repeat = line.getParent();
            while (repeat != null) {
                if ("REPEAT".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                if ("REPEAT WHILE".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                if ("REPEAT UNTIL".equalsIgnoreCase(repeat.getStatement())) {
                    break;
                }
                repeat = repeat.getParent();
            }
            Spin2MethodLine target = (Spin2MethodLine) repeat.getData("quit");
            line.addSource(new Jmp(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
        }
        else if ("RETURN".equalsIgnoreCase(text)) {
            if (line.getArgumentsCount() == 0) {
                line.addSource(new Bytecode(line.getScope(), 0x04, text));
            }
            else {
                for (Spin2StatementNode arg : line.getArguments()) {
                    line.addSource(compileBytecodeExpression(line.getScope(), arg, true));
                }
                line.addSource(new Bytecode(line.getScope(), 0x05, text));
            }
        }
        else if ("CASE".equalsIgnoreCase(text)) {
            Spin2MethodLine end = (Spin2MethodLine) line.getData("end");
            line.addSource(new Constant(line.getScope(), new Identifier(end.getLabel(), end.getScope())));

            for (Spin2StatementNode arg : line.getArguments()) {
                Spin2MethodLine target = (Spin2MethodLine) arg.getData("true");
                if ("..".equals(arg.getText())) {
                    line.addSource(compileBytecodeExpression(line.getScope(), arg.getChild(0), false));
                    line.addSource(compileBytecodeExpression(line.getScope(), arg.getChild(1), false));
                    if (target != null) {
                        line.addSource(new CaseRangeJmp(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
                    }
                }
                else {
                    line.addSource(compileBytecodeExpression(line.getScope(), arg, false));
                    if (target != null) {
                        line.addSource(new CaseJmp(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
                    }
                }
            }
        }
        else if ("CASE_DONE".equalsIgnoreCase(text)) {
            line.addSource(new Bytecode(line.getScope(), 0x1E, text));
        }
        else if (text != null) {
            Descriptor desc = Spin2Bytecode.getDescriptor(text);
            if (desc != null) {
                if (line.getArgumentsCount() != desc.parameters) {
                    throw new CompilerMessage("expected " + desc.parameters + " argument(s), found " + line.getArgumentsCount(), (Node) line.getData());
                }
                for (Spin2StatementNode arg : line.getArguments()) {
                    List<Spin2Bytecode> list = compileBytecodeExpression(line.getScope(), arg, true);
                    line.addSource(list);
                }
                line.addSource(new Bytecode(line.getScope(), desc.code, text));
            }
            else {
                //throw new RuntimeException("unknown " + text);
            }
        }
        else {
            for (Spin2StatementNode arg : line.getArguments()) {
                line.addSource(compileBytecodeExpression(line.getScope(), arg, false));
            }
        }

        Spin2MethodLine target = (Spin2MethodLine) line.getData("exit");
        if (target != null) {
            Spin2MethodLine newLine = new Spin2MethodLine(line.getScope(), null, null, Collections.emptyList());
            newLine.addSource(new Jmp(line.getScope(), new Identifier(target.getLabel(), target.getScope())));
            line.addChild(newLine);
        }

        for (Spin2MethodLine child : line.getChilds()) {
            compileLine(child);
        }
    }

    List<Spin2Bytecode> compileBytecodeExpression(Spin2Context context, Spin2StatementNode node, boolean push) {
        List<Spin2Bytecode> source = new ArrayList<Spin2Bytecode>();

        Descriptor desc = Spin2Bytecode.getDescriptor(node.getText());
        if (desc != null) {
            if (node.getChildCount() != desc.parameters) {
                throw new RuntimeException("expected " + desc.parameters + " argument(s), found " + node.getChildCount());
            }
            for (int i = 0; i < desc.parameters; i++) {
                source.addAll(compileBytecodeExpression(context, node.getChild(i), true));
            }
            source.add(new Bytecode(context, desc.code, node.getText()));
        }
        else if (node.getType() == Token.NUMBER) {
            Expression expression = new NumberLiteral(node.getText());
            source.add(new Constant(context, expression));
        }
        else if (node.getType() == Token.STRING) {
            String s = node.getText().substring(1);
            s = s.substring(0, s.length() - 1);
            if (s.length() == 1) {
                Expression expression = new CharacterLiteral(s.charAt(0));
                source.add(new Constant(context, expression));
            }
            else {
                byte[] code = new byte[s.length() + 3];
                int index = 0;
                code[index++] = (byte) 0x9E;
                code[index++] = (byte) (s.length() + 1);
                for (int i = 0; i < s.length(); i++) {
                    code[index++] = (byte) s.charAt(i);
                }
                code[index++] = (byte) 0x00;
                source.add(new Bytecode(context, code, "STRING".toUpperCase()));
            }
        }
        else if (":=".equals(node.getText())) {
            source.addAll(compileBytecodeExpression(context, node.getChild(1), true));

            Spin2StatementNode left = node.getChild(0);
            if (left.getType() == Token.OPERATOR) {
                source.addAll(compileBytecodeExpression(context, left, push));
            }
            else if ("BYTE".equalsIgnoreCase(left.getText()) || "WORD".equalsIgnoreCase(left.getText()) || "LONG".equalsIgnoreCase(left.getText())) {
                if (left.getChildCount() != 1) {
                    throw new RuntimeException("expression syntax error " + left.getText());
                }
                source.addAll(compileBytecodeExpression(context, left.getChild(0), true));
                if ("BYTE".equalsIgnoreCase(left.getText())) {
                    source.add(new Bytecode(context, new byte[] {
                        (byte) 0x65, push ? (byte) 0x82 : (byte) 0x81
                    }, left.getText().toUpperCase() + (push ? "_MODIFY" : "_WRITE")));
                }
                else if ("WORD".equalsIgnoreCase(left.getText())) {
                    source.add(new Bytecode(context, new byte[] {
                        (byte) 0x66, push ? (byte) 0x82 : (byte) 0x81
                    }, left.getText().toUpperCase() + (push ? "_MODIFY" : "_WRITE")));
                }
                else if ("LONG".equalsIgnoreCase(left.getText())) {
                    source.add(new Bytecode(context, new byte[] {
                        (byte) 0x67, push ? (byte) 0x82 : (byte) 0x81
                    }, left.getText().toUpperCase() + (push ? "_MODIFY" : "_WRITE")));
                }
            }
            else {
                Expression expression = context.getLocalSymbol(left.getText());
                if (expression instanceof Register) {
                    source.add(new RegisterOp(context, RegisterOp.Op.Write, expression));
                }
                else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                    source.add(new VariableOp(context, VariableOp.Op.Write, push, (Variable) expression));
                }
                else if (expression instanceof ContextLiteral) {
                    MemoryOp.Size ss = MemoryOp.Size.Long;
                    MemoryOp.Base bb = MemoryOp.Base.PBase;
                    if (expression instanceof LocalVariable) {
                        bb = MemoryOp.Base.DBase;
                    }
                    else if (expression instanceof Variable) {
                        bb = MemoryOp.Base.VBase;
                    }
                    source.add(new MemoryOp(context, ss, bb, MemoryOp.Op.Write, expression));
                }
                else {
                    throw new CompilerMessage("undefined symbol " + left.getText(), left.getToken());
                }
            }
        }
        else if (MathOp.isAssignMathOp(node.getText())) {
            source.addAll(compileBytecodeExpression(context, node.getChild(1), true));

            Spin2StatementNode left = node.getChild(0);
            if (left.getType() == Token.OPERATOR) {
                source.addAll(compileBytecodeExpression(context, left, true));
            }
            else {
                Expression expression = context.getLocalSymbol(left.getText());
                if (expression instanceof Register) {
                    throw new RuntimeException("unhandled register expression");
                }
                else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                    source.add(new VariableOp(context, VariableOp.Op.Setup, (Variable) expression));
                }
                else if (expression instanceof ContextLiteral) {
                    MemoryOp.Size ss = MemoryOp.Size.Long;
                    MemoryOp.Base bb = MemoryOp.Base.PBase;
                    if (expression instanceof LocalVariable) {
                        bb = MemoryOp.Base.DBase;
                    }
                    else if (expression instanceof Variable) {
                        bb = MemoryOp.Base.VBase;
                    }
                    source.add(new MemoryOp(context, ss, bb, MemoryOp.Op.Setup, expression));
                }
                else {
                    throw new CompilerMessage("undefined symbol " + left.getText(), left.getToken());
                }
            }
            source.add(new MathOp(context, node.getText(), push));
        }
        else if (MathOp.isMathOp(node.getText())) {
            if (node.getChildCount() != 2) {
                throw new RuntimeException("expression syntax error " + node.getText());
            }
            try {
                Expression expression = buildConstantExpression(context, node);
                source.add(new Constant(context, expression));
            } catch (Exception e) {
                source.addAll(compileBytecodeExpression(context, node.getChild(0), true));
                source.addAll(compileBytecodeExpression(context, node.getChild(1), true));
                source.add(new MathOp(context, node.getText(), false));
            }
        }
        else if ("BYTE".equalsIgnoreCase(node.getText()) || "WORD".equalsIgnoreCase(node.getText()) || "LONG".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() != 1) {
                throw new RuntimeException("expression syntax error " + node.getText());
            }
            source.addAll(compileConstantExpression(context, node.getChild(0)));
            if ("BYTE".equalsIgnoreCase(node.getText())) {
                source.add(new Bytecode(context, new byte[] {
                    (byte) 0x65, push ? (byte) 0x80 : (byte) 0x81
                }, node.getText().toUpperCase() + (push ? "_READ" : "_WRITE")));
            }
            else if ("WORD".equalsIgnoreCase(node.getText())) {
                source.add(new Bytecode(context, new byte[] {
                    (byte) 0x66, push ? (byte) 0x80 : (byte) 0x81
                }, node.getText().toUpperCase() + (push ? "_READ" : "_WRITE")));
            }
            else if ("LONG".equalsIgnoreCase(node.getText())) {
                source.add(new Bytecode(context, new byte[] {
                    (byte) 0x67, push ? (byte) 0x80 : (byte) 0x81
                }, node.getText().toUpperCase() + (push ? "_READ" : "_WRITE")));
            }
        }
        else if (":".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() != 2) {
                throw new RuntimeException("expression syntax error " + node.getText());
            }
            if (!"?".equals(node.getChild(0).getText())) {
                throw new RuntimeException("expression syntax error " + node.getText());
            }
            source.addAll(compileBytecodeExpression(context, node.getChild(0).getChild(0), true));
            source.addAll(compileBytecodeExpression(context, node.getChild(0).getChild(1), true));
            source.addAll(compileBytecodeExpression(context, node.getChild(1), true));
            source.add(new Bytecode(context, 0x6B, "TERNARY_IF_ELSE"));
        }
        else if ("(".equalsIgnoreCase(node.getText())) {
            source.addAll(compileBytecodeExpression(context, node.getChild(0), push));
        }
        else if (",".equalsIgnoreCase(node.getText())) {
            for (Spin2StatementNode child : node.getChilds()) {
                source.addAll(compileBytecodeExpression(context, child, push));
            }
        }
        else if ("ABORT".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() == 0) {
                source.add(new Bytecode(context, 0x06, node.getText().toUpperCase()));
            }
            else if (node.getChildCount() == 1) {
                source.addAll(compileBytecodeExpression(context, node.getChild(0), true));
                source.add(new Bytecode(context, 0x07, node.getText().toUpperCase()));
            }
            else {
                throw new RuntimeException("expected 0 or 1 argument(s), found " + node.getChildCount());
            }
        }
        else if ("RECV".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() != 0) {
                throw new RuntimeException("expected " + 0 + " argument(s), found " + node.getChildCount());
            }
            source.add(new Bytecode(context, 0x0C, node.getText().toUpperCase()));
        }
        else if ("SEND".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() == 0) {
                throw new RuntimeException("syntax error");
            }
            boolean bytes = true;
            for (Spin2StatementNode child : node.getChilds()) {
                if (child.getType() != Token.NUMBER) {
                    bytes = false;
                    break;
                }
            }
            if (bytes) {
                byte[] code = new byte[node.getChildCount() + 2];
                code[0] = 0x0E;
                code[1] = (byte) node.getChildCount();
                for (int i = 0; i < node.getChildCount(); i++) {
                    code[i + 2] = (byte) new NumberLiteral(node.getChild(i).getText()).getNumber().intValue();
                }
                source.add(new Bytecode(context, code, node.getText().toUpperCase()));
            }
            else {
                for (Spin2StatementNode child : node.getChilds()) {
                    source.addAll(compileBytecodeExpression(context, child, true));
                    source.add(new Bytecode(context, 0x0D, node.getText().toUpperCase()));
                }
            }
        }
        else if ("DEBUG".equalsIgnoreCase(node.getText())) {
            // Ignored
        }
        else if ("STRING".equalsIgnoreCase(node.getText())) {
            StringBuilder sb = new StringBuilder();
            for (Spin2StatementNode child : node.getChilds()) {
                if (child.getType() == Token.STRING) {
                    String s = child.getText().substring(1);
                    sb.append(s.substring(0, s.length() - 1));
                }
                else if (child.getType() == Token.NUMBER) {
                    NumberLiteral expression = new NumberLiteral(child.getText());
                    sb.append((char) expression.getNumber().intValue());
                }
            }
            byte[] code = new byte[sb.length() + 3];
            int index = 0;
            code[index++] = (byte) 0x9E;
            code[index++] = (byte) (sb.length() + 1);
            for (int i = 0; i < sb.length(); i++) {
                code[index++] = (byte) sb.charAt(i);
            }
            code[index++] = (byte) 0x00;
            source.add(new Bytecode(context, code, node.getText().toUpperCase()));
        }
        else {
            Expression expression = context.getLocalSymbol(node.getText());
            if (expression == null) {
                throw new CompilerMessage("undefined symbol " + node.getText(), node.getToken());
            }
            if (expression instanceof Register) {
                throw new RuntimeException("unhandled register expression");
            }
            else if (node.getText().startsWith("@")) {
                if (expression instanceof Method) {
                    source.add(new Bytecode(context, new byte[] {
                        (byte) 0x11,
                        (byte) ((Method) expression).getOffset()
                    }, "SUB (" + ((Method) expression).getOffset() + ")"));
                }
                else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                    source.add(new VariableOp(context, VariableOp.Op.Address, (Variable) expression));
                }
                else {
                    MemoryOp.Size ss = MemoryOp.Size.Long;
                    MemoryOp.Base bb = MemoryOp.Base.PBase;
                    if (expression instanceof LocalVariable) {
                        bb = MemoryOp.Base.DBase;
                    }
                    else if (expression instanceof Variable) {
                        bb = MemoryOp.Base.VBase;
                    }
                    source.add(new MemoryOp(context, ss, bb, MemoryOp.Op.Address, expression));
                }
            }
            else if (expression instanceof Method) {
                int parameters = ((Method) expression).getArgumentsCount();
                if (node.getChildCount() != parameters) {
                    throw new RuntimeException("expected " + parameters + " argument(s), found " + node.getChildCount());
                }
                source.add(new Bytecode(context, new byte[] {
                    (byte) ((Method) expression).getReturnsCount(),
                }, "RETURNS_COUNT (" + ((Method) expression).getReturnsCount() + ")"));
                for (int i = 0; i < parameters; i++) {
                    source.addAll(compileBytecodeExpression(context, node.getChild(i), true));
                }
                source.add(new Bytecode(context, new byte[] {
                    (byte) 0x0A,
                    (byte) ((Method) expression).getOffset()
                }, "CALL_SUB (" + ((Method) expression).getOffset() + ")"));
            }
            else if ((expression instanceof Variable) || (expression instanceof LocalVariable)) {
                source.add(new VariableOp(context, VariableOp.Op.Read, (Variable) expression));
            }
            else if (expression instanceof ContextLiteral) {
                MemoryOp.Size ss = MemoryOp.Size.Long;
                MemoryOp.Base bb = MemoryOp.Base.PBase;
                if (expression instanceof LocalVariable) {
                    bb = MemoryOp.Base.DBase;
                }
                else if (expression instanceof Variable) {
                    bb = MemoryOp.Base.VBase;
                }
                source.add(new MemoryOp(context, ss, bb, MemoryOp.Op.Read, expression));
            }
            else {
                throw new RuntimeException("unhandled " + node.getText());
            }
        }

        return source;
    }

    List<Spin2Bytecode> compileConstantExpression(Spin2Context context, Spin2StatementNode node) {
        try {
            Expression expression = buildConstantExpression(context, node);
            if (expression.isConstant()) {
                return Collections.singletonList(new Constant(context, expression));
            }
        } catch (Exception e) {

        }
        return compileBytecodeExpression(context, node, true);
    }

    Expression buildConstantExpression(Spin2Context context, Spin2StatementNode node) {
        if (node.getType() == Token.NUMBER) {
            return new NumberLiteral(node.getText());
        }
        else if (node.getType() == Token.STRING) {
            String s = node.getText().substring(1);
            s = s.substring(0, s.length() - 1);
            if (s.length() == 1) {
                return new CharacterLiteral(s.charAt(0));
            }
            throw new RuntimeException("string not allowed");
        }

        Expression expression = context.getLocalSymbol(node.getText());
        if (expression != null) {
            if (expression.isConstant()) {
                return expression;
            }
            throw new RuntimeException("not a constant (" + expression + ")");
        }

        if ("+".equals(node.getText())) {
            return new Add(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("-".equals(node.getText())) {
            return new Subtract(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("*".equals(node.getText())) {
            return new Multiply(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("/".equals(node.getText())) {
            return new Divide(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("//".equals(node.getText())) {
            return new Modulo(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("|".equals(node.getText())) {
            return new Or(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("&".equals(node.getText())) {
            return new And(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("^".equals(node.getText())) {
            return new Xor(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("<<".equals(node.getText())) {
            return new ShiftLeft(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if (">>".equals(node.getText())) {
            return new ShiftRight(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("ADDPINS".equalsIgnoreCase(node.getText())) {
            return new Addpins(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("ADDBITS".equalsIgnoreCase(node.getText())) {
            return new Addbits(buildConstantExpression(context, node.getChild(0)), buildConstantExpression(context, node.getChild(1)));
        }
        if ("TRUNC".equalsIgnoreCase(node.getText())) {
            if (node.getChildCount() != 1) {
                throw new RuntimeException("misplaced unary operator (" + node.getText() + ")");
            }
            return new Trunc(buildConstantExpression(context, node.getChild(0)));
        }

        throw new RuntimeException("unknown " + node.getText());
    }

    int getClockMode(int xinfreq, int clkfreq) {
        int divd;
        int zzzz = 11; // 0b10_11
        double e, post, mult, Fpfd, Fvco, Fout;
        double result_mult = 0;
        //double result_Fout = 0;
        int result_pppp = 0, result_divd = 0;
        double error = 1e9;

        for (int pppp = 0; pppp <= 15; pppp++) {
            if (pppp == 0) {
                post = 1.0;
            }
            else {
                post = pppp * 2.0;
            }
            for (divd = 64; divd >= 1; --divd) {
                Fpfd = Math.round(xinfreq / (double) divd);
                mult = Math.round(clkfreq * post / Fpfd);
                Fvco = Math.round(Fpfd * mult);
                Fout = Math.round(Fvco / post);
                e = Math.abs(Fout - clkfreq);
                if ((e <= error) && (Fpfd >= 250000) && (mult <= 1024) && (Fvco > 99e6) && ((Fvco <= 201e6) || (Fvco <= clkfreq + 1e6))) {
                    result_divd = divd;
                    result_mult = mult;
                    result_pppp = (pppp - 1) & 15;
                    //result_Fout = Fout;
                    error = e;
                }
            }
        }
        if (error > 100000.0) {
            throw new RuntimeException(String.format("Unable to find clock settings for freq %f Hz with input freq %f Hz", clkfreq, xinfreq));
        }

        int D = result_divd - 1;
        int M = ((int) result_mult) - 1;
        int clkmode = zzzz | (result_pppp << 4) | (M << 8) | (D << 18) | (1 << 24);

        //int finalfreq = (int) Math.round(result_Fout);

        return clkmode;
    }

    public List<Spin2PAsmLine> getSource() {
        return source;
    }

    protected void logMessage(CompilerMessage message) {
        if (message.type == CompilerMessage.ERROR) {
            errors = true;
        }
        messages.add(message);
    }

    public boolean hasErrors() {
        return errors;
    }

    public List<CompilerMessage> getMessages() {
        return messages;
    }

    Expression buildExpression(Node node, Spin2Context scope) {
        return buildExpression(node.getTokens(), scope);
    }

    Expression buildExpression(List<Token> tokens, Spin2Context scope) {
        Spin2ExpressionBuilder expressionBuilder = new Spin2ExpressionBuilder(scope, tokens);
        return expressionBuilder.getExpression();
    }

}
