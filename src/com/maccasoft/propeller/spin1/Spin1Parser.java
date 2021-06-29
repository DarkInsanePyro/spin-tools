/*
 * Copyright (c) 2021 Marco Maccaferri and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.maccasoft.propeller.spin1;

import java.util.ArrayList;
import java.util.List;

import com.maccasoft.propeller.model.ConstantAssignEnumNode;
import com.maccasoft.propeller.model.ConstantAssignNode;
import com.maccasoft.propeller.model.ConstantSetEnumNode;
import com.maccasoft.propeller.model.ConstantsNode;
import com.maccasoft.propeller.model.DataLineNode;
import com.maccasoft.propeller.model.DataNode;
import com.maccasoft.propeller.model.ErrorNode;
import com.maccasoft.propeller.model.ExpressionNode;
import com.maccasoft.propeller.model.LocalVariableNode;
import com.maccasoft.propeller.model.MethodNode;
import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.model.ObjectNode;
import com.maccasoft.propeller.model.ObjectsNode;
import com.maccasoft.propeller.model.ParameterNode;
import com.maccasoft.propeller.model.StatementNode;
import com.maccasoft.propeller.model.Token;
import com.maccasoft.propeller.model.VariableNode;
import com.maccasoft.propeller.model.VariablesNode;
import com.maccasoft.propeller.spin2.Spin2Model;

public class Spin1Parser {

    final Spin1TokenStream stream;

    Node root;

    public Spin1Parser(Spin1TokenStream stream) {
        this.stream = stream;
    }

    public Node parse() {
        root = new Node();

        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                root.addToken(token);
                break;
            }
            if (token.type == Token.NL) {
                continue;
            }
            if (!parseSection(token)) {
                ConstantsNode node = new ConstantsNode(root);
                parseConstants(node, token);
            }
        }

        return root;
    }

    boolean parseSection(Token token) {
        if (token.type == Token.EOF) {
            return false;
        }
        if ("VAR".equalsIgnoreCase(token.getText())) {
            parseVar(token);
            return true;
        }
        if ("OBJ".equalsIgnoreCase(token.getText())) {
            parseObj(token);
            return true;
        }
        if ("PUB".equalsIgnoreCase(token.getText())) {
            parseMethod(token);
            return true;
        }
        if ("PRI".equalsIgnoreCase(token.getText())) {
            parseMethod(token);
            return true;
        }
        if ("DAT".equalsIgnoreCase(token.getText())) {
            parseDat(token);
            return true;
        }
        if ("CON".equalsIgnoreCase(token.getText())) {
            ConstantsNode node = new ConstantsNode(root, token);
            node.addToken(token);
            parseConstants(node, stream.nextToken());
            return true;
        }
        return false;
    }

    void parseConstants(ConstantsNode parent, Token token) {
        List<Token> list = new ArrayList<Token>();

        int state = 1;
        while (true) {
            switch (state) {
                case 0:
                    if (parseSection(token)) {
                        return;
                    }
                    state = 1;
                    // fall-through
                case 1:
                    if (token.type == Token.NL || token.type == Token.EOF || ",".equals(token.getText())) {
                        Node child = null;

                        if (list.size() == 1) {
                            new ConstantAssignEnumNode(parent, list);
                        }
                        else if (list.size() >= 2) {
                            if ("#".equals(list.get(0).getText())) {
                                new ConstantSetEnumNode(parent, list);
                            }
                            else if (list.size() >= 3) {
                                if ("=".equals(list.get(1).getText())) {
                                    new ConstantAssignNode(parent, list);
                                }
                                else if ("[".equals(list.get(1).getText())) {
                                    new ConstantAssignEnumNode(parent, list);
                                }
                                else {
                                    child = new ErrorNode(parent, "Syntax error");
                                }
                            }
                            else {
                                child = new ErrorNode(parent, "Syntax error");
                            }
                        }

                        if (child != null) {
                            child.addAllTokens(list);
                        }
                        list.clear();

                        if (token.type == Token.EOF) {
                            return;
                        }
                        if (token.type == Token.NL) {
                            state = 0;
                        }
                        break;
                    }
                    list.add(token);
                    break;
            }
            token = stream.nextToken();
        }
    }

    void parseVar(Token start) {
        Node parent = new VariablesNode(root);
        parent.addToken(start);

        ErrorNode error = null;
        VariableNode node = null;
        int state = 1;
        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                return;
            }
            if (token.type == Token.NL) {
                node = null;
                state = 0;
                continue;
            }
            switch (state) {
                case 0:
                    if (parseSection(token)) {
                        return;
                    }
                    state = 1;
                    // fall-through
                case 1:
                    if (Spin2Model.isType(token.getText())) {
                        node = new VariableNode(parent);
                        node.addToken(token);
                        node.type = token;
                        state = 2;
                        break;
                    }
                    // fall-through
                case 2:
                    if (token.type == 0) {
                        if (node == null) {
                            node = new VariableNode(parent);
                        }
                        node.addToken(token);
                        node.identifier = token;
                        state = 3;
                        break;
                    }
                    error = new ErrorNode(parent);
                    state = 9;
                    break;

                case 3:
                    if ("[".equals(token.getText())) {
                        node.size = new ExpressionNode(node);
                        state = 5;
                        break;
                    }
                    // fall-through
                case 4:
                    if (",".equals(token.getText())) {
                        node = null;
                        state = 1;
                        break;
                    }
                    error = new ErrorNode(parent);
                    state = 9;
                    break;

                case 5:
                    if ("]".equals(token.getText())) {
                        state = 4;
                        break;
                    }
                    node.size.addToken(token);
                    break;

                case 9:
                    error.addToken(token);
                    break;
            }
        }
    }

    void parseObj(Token start) {
        Node parent = new ObjectsNode(root);
        parent.addToken(start);

        ErrorNode error = null;
        ObjectNode object = null;
        int state = 1;
        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                return;
            }
            if (token.type == Token.NL) {
                state = 0;
                continue;
            }
            switch (state) {
                case 0:
                    if (parseSection(token)) {
                        return;
                    }
                    state = 1;
                    // fall-through
                case 1:
                    object = new ObjectNode(parent);
                    object.addToken(token);
                    object.name = token;
                    state = 2;
                    break;
                case 2:
                    if ("[".equals(token.getText())) {
                        object.count = new Node(object);
                        state = 5;
                        break;
                    }
                    // fall-through
                case 3:
                    if (":".equals(token.getText())) {
                        state = 4;
                        break;
                    }
                    error = new ErrorNode(object, "Syntax error");
                    error.addToken(token);
                    state = 9;
                    break;
                case 4:
                    object.addToken(token);
                    object.file = token;
                    state = 8;
                    break;

                case 5:
                    if ("]".equals(token.getText())) {
                        state = 3;
                        break;
                    }
                    object.count.addToken(token);
                    break;

                case 8:
                    error = new ErrorNode(object, "Syntax error");
                    state = 9;
                    // fall-through
                case 9:
                    error.addToken(token);
                    break;
            }
        }
    }

    void parseMethod(Token start) {
        MethodNode node = new MethodNode(root, start);

        int state = 2;
        Node parent = node;
        Node child = node;
        Node param = null;
        Node ret = null;
        LocalVariableNode local = null;

        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                return;
            }
            if (token.type == Token.NL) {
                state = 0;
                continue;
            }
            switch (state) {
                case 0:
                    if (parseSection(token)) {
                        return;
                    }

                    if (child.getTokens().size() != 0) {
                        while (token.column < child.getToken(0).column && child.getParent() != node) {
                            child = child.getParent();
                            parent = child.getParent();
                        }

                        if (token.column > child.getToken(0).column) {
                            if (Spin1Model.isBlockStart(child.getToken(0).getText())) {
                                parent = child;
                            }
                            else if (child.getText().endsWith(":")) {
                                parent = child;
                            }
                        }
                    }

                    child = parseStatement(parent, token);
                    break;

                case 1:
                    child.addToken(token);
                    break;

                case 2:
                    if (token.type == 0) {
                        node.name = token;
                        node.addToken(token);
                        state = 3;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;

                case 3:
                    if ("(".equals(token.getText())) {
                        node.addToken(token);
                        state = 4;
                        break;
                    }
                    else if (":".equals(token.getText())) {
                        node.addToken(token);
                        state = 7;
                        break;
                    }
                    else if ("|".equals(token.getText())) {
                        node.addToken(token);
                        state = 9;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;

                case 4:
                    if (")".equals(token.getText())) {
                        node.addToken(token);
                        state = 6;
                        break;
                    }
                    if (Spin1Model.isType(token.getText())) {
                        child = new ErrorNode(parent);
                        child.addToken(token);
                        state = 1;
                        break;
                    }
                    param = new Node(node);
                    param.addToken(token);
                    node.parameters.add(param);
                    state = 5;
                    break;
                case 5:
                    if (",".equals(token.getText())) {
                        node.addToken(token);
                        state = 4;
                        break;
                    }
                    if (")".equals(token.getText())) {
                        node.addToken(token);
                        state = 6;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;

                case 6:
                    if ("|".equals(token.getText())) {
                        node.addToken(token);
                        state = 9;
                        break;
                    }
                    else if (":".equals(token.getText())) {
                        node.addToken(token);
                        state = 7;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;

                case 7:
                    if (Spin1Model.isType(token.getText())) {
                        child = new ErrorNode(parent);
                        child.addToken(token);
                        state = 1;
                        break;
                    }
                    ret = new Node(node);
                    ret.addToken(token);
                    node.returnVariables.add(ret);
                    state = 8;
                    break;
                case 8:
                    if ("|".equals(token.getText())) {
                        node.addToken(token);
                        state = 9;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;

                case 9:
                    local = new LocalVariableNode(node);
                    node.localVariables.add(local);
                    if (Spin1Model.isType(token.getText())) {
                        local.type = new Node(local);
                        local.type.addToken(token);
                        state = 10;
                        break;
                    }
                    // fall-through
                case 10:
                    if (token.type == 0) {
                        local.identifier = new Node(local);
                        local.identifier.addToken(token);
                        state = 11;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;
                case 11:
                    if (",".equals(token.getText())) {
                        node.addToken(token);
                        state = 9;
                        break;
                    }
                    if ("[".equals(token.getText())) {
                        node.addToken(token);
                        local.size = new ExpressionNode(local);
                        state = 12;
                        break;
                    }
                    if (":".equals(token.getText())) {
                        node.addToken(token);
                        state = 7;
                        break;
                    }
                    child = new ErrorNode(parent);
                    child.addToken(token);
                    state = 1;
                    break;
                case 12:
                    if ("]".equals(token.getText())) {
                        node.addToken(token);
                        state = 11;
                        break;
                    }
                    local.size.addToken(token);
                    break;
            }
        }
    }

    Node parseStatement(Node parent, Token token) {
        Node statement = new StatementNode(parent);

        while (true) {
            statement.addToken(token);
            if (statement.getTokens().size() == 2) {
                if (":".equals(token.getText())) {
                    break;
                }
            }
            if ("(".equals(token.getText())) {
                token = parseSubStatement(statement, token);
                if (token.type == Token.NL || token.type == Token.EOF) {
                    break;
                }
                if (")".equals(token.getText())) {
                    statement.addToken(token);
                }
                token = stream.nextToken();
            }
            else {
                token = stream.nextToken();
            }
            if (token.type == Token.NL || token.type == Token.EOF) {
                break;
            }
        }

        return statement;
    }

    Token parseSubStatement(Node parent, Token token) {
        Node node = new Node(parent);

        while (true) {
            token = stream.nextToken();
            if (token.type == Token.NL || token.type == Token.EOF) {
                return token;
            }
            if ("(".equals(token.getText())) {
                token = parseSubStatement(node, token);
                if (token.type == Token.NL || token.type == Token.EOF) {
                    return token;
                }
                if (")".equals(token.getText())) {
                    parent.addToken(token);
                    continue;
                }
            }
            if (",".equals(token.getText())) {
                parent.addToken(token);
                node = new Node(parent);
                continue;
            }
            else if (")".equals(token.getText())) {
                if (node.getParent() != parent) {
                    node = node.getParent();
                }
                return token;
            }
            node.addToken(token);
        }
    }

    void parseDat(Token start) {
        Node node = new DataNode(root);
        node.addToken(start);

        Node parent = node;
        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                return;
            }
            if (token.type == Token.NL) {
                continue;
            }
            if (parseSection(token)) {
                return;
            }
            //if ("ORG".equalsIgnoreCase(token.getText())) {
            //    parent = node;
            //}
            parseDatLine(parent, token);
            //if ("ORG".equalsIgnoreCase(token.getText())) {
            //    parent = node.getChild(node.getChilds().size() - 1);
            //}
        }
    }

    void parseDatLine(Node node, Token token) {
        int state = 0;
        DataLineNode parent = new DataLineNode(node);
        ParameterNode parameter = null;
        Node child = null;

        while (true) {
            if (state != 0) {
                token = stream.nextToken();
            }
            if (token.type == Token.EOF || token.type == Token.NL) {
                return;
            }
            switch (state) {
                case 0:
                    state = 1;
                    // fall-through
                case 1:
                    if (token.column == 0) {
                        parent.label = new Node(parent);
                        parent.label.addToken(token);
                        state = 2;
                        break;
                    }
                    // fall-through
                case 2:
                    if (Spin1Model.isCondition(token.getText())) {
                        parent.condition = new Node(parent);
                        parent.condition.addToken(token);
                        state = 3;
                        break;
                    }
                    // fall-through
                case 3:
                    if (Spin1Model.isInstruction(token.getText()) || Spin1Model.isType(token.getText())) {
                        parent.instruction = new Node(parent);
                        parent.instruction.addToken(token);
                        state = 4;
                        break;
                    }
                    child = new ErrorNode(parent, parent.condition == null ? "Invalid condition or instruction" : "Invalid instruction");
                    child.addToken(token);
                    child = parent;
                    state = 9;
                    break;
                case 4:
                    if (Spin1Model.isModifier(token.getText())) {
                        parent.modifier = new Node(parent);
                        parent.modifier.addToken(token);
                        state = 5;
                        break;
                    }
                    parameter = new ParameterNode(parent);
                    parameter.addToken(token);
                    parent.parameters.add(parameter);
                    state = 7;
                    break;
                case 5:
                    if (",".equals(token.getText())) {
                        parent.modifier.addToken(token);
                        state = 6;
                        break;
                    }
                    child = new ErrorNode(parent, "Syntax error");
                    child.addToken(token);
                    state = 9;
                    break;
                case 6:
                    if (Spin1Model.isModifier(token.getText())) {
                        parent.modifier.addToken(token);
                        state = 5;
                        break;
                    }
                    child = new ErrorNode(parent, "Invalid modifier");
                    child.addToken(token);
                    state = 9;
                    break;
                case 7:
                    if (",".equals(token.getText())) {
                        parameter = new ParameterNode(parent);
                        parent.parameters.add(parameter);
                        break;
                    }
                    if (Spin1Model.isModifier(token.getText())) {
                        parent.modifier = new Node(parent);
                        parent.modifier.addToken(token);
                        state = 5;
                        break;
                    }
                    if ("[".equals(token.getText())) {
                        parameter.count = new ExpressionNode();
                        parameter.addChild(parameter.count);
                        state = 10;
                        break;
                    }
                    parameter.addToken(token);
                    break;
                case 9:
                    child.addToken(token);
                    break;
                case 10:
                    if ("]".equals(token.getText())) {
                        parent.addToken(token);
                        state = 7;
                        break;
                    }
                    parameter.count.addToken(token);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        String text = ""
            + "CON\n"
            + "\n"
            + "    _XINFREQ = 5_000_000\n"
            + "    _CLKMODE = XTAL1 + PLL16X\n"
            + "\n"
            + "VAR\n"
            + "\n"
            + "OBJ\n"
            + "\n"
            + "PUB start | a\n"
            + "\n"
            + "    ' initialization\n"
            + "    a := 1 + 2 * 3\n"
            + "\n"
            + "    repeat\n"
            + "        ' loop\n"
            + "";

        try {
            Spin1TokenStream stream = new Spin1TokenStream(text);
            Spin1Parser subject = new Spin1Parser(stream);
            Node root = subject.parse();
            print(root, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void print(Node node, int indent) {
        if (indent != 0) {
            for (int i = 1; i < indent; i++) {
                System.out.print("|    ");
            }
            System.out.print("+--- ");
        }

        System.out.print(node.getClass().getSimpleName());
        //for (Token token : node.tokens) {
        //    System.out.print(" [" + token.getText().replaceAll("\n", "\\n") + "]");
        //}
        System.out.println(" [" + node.getText().replaceAll("\n", "\\\\n") + "]");

        for (Node child : node.getChilds()) {
            print(child, indent + 1);
        }
    }

}