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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maccasoft.propeller.model.Node;
import com.maccasoft.propeller.model.Token;

public class Spin1ExpressionBuilder {

    static final int LEFT_TO_RIGHT = 0;
    static final int RIGHT_TO_LEFT = 1;

    static Map<String, Integer> operatorPrecedence = new HashMap<String, Integer>();
    static {
        operatorPrecedence.put("(", -2);
        operatorPrecedence.put(")", 99);

        operatorPrecedence.put("[", -1);
        operatorPrecedence.put("]", 99);

        operatorPrecedence.put("^^", 1);
        operatorPrecedence.put("||", 1);
        operatorPrecedence.put("|<", 1);
        operatorPrecedence.put(">|", 1);
        operatorPrecedence.put("!", 1);

        operatorPrecedence.put("<-", 2);
        operatorPrecedence.put("->", 2);
        operatorPrecedence.put("<<", 2);
        operatorPrecedence.put(">>", 2);
        operatorPrecedence.put("~>", 2);
        operatorPrecedence.put("><", 2);

        operatorPrecedence.put("&", 3);

        operatorPrecedence.put("|", 4);
        operatorPrecedence.put("^", 4);

        operatorPrecedence.put("*", 5);
        operatorPrecedence.put("**", 5);
        operatorPrecedence.put("/", 5);
        operatorPrecedence.put("//", 5);

        operatorPrecedence.put("+", 6);
        operatorPrecedence.put("-", 6);

        operatorPrecedence.put("#>", 7);
        operatorPrecedence.put("<#", 7);

        operatorPrecedence.put("<", 8);
        operatorPrecedence.put(">", 8);
        operatorPrecedence.put("<>", 8);
        operatorPrecedence.put("==", 8);
        operatorPrecedence.put("=<", 8);
        operatorPrecedence.put("=>", 8);

        operatorPrecedence.put("NOT", 9);

        operatorPrecedence.put("AND", 10);

        operatorPrecedence.put("OR", 11);

        operatorPrecedence.put(":=", 12);
        operatorPrecedence.put("^^=", 12);
        operatorPrecedence.put("||=", 12);
        operatorPrecedence.put("|<=", 12);
        operatorPrecedence.put(">|=", 12);
        operatorPrecedence.put("!=", 12);
        operatorPrecedence.put("<-=", 12);
        operatorPrecedence.put("->=", 12);
        operatorPrecedence.put("<<=", 12);
        operatorPrecedence.put(">>=", 12);
        operatorPrecedence.put("~>=", 12);
        operatorPrecedence.put("><=", 12);
        operatorPrecedence.put("&=", 12);
        operatorPrecedence.put("|=", 12);
        operatorPrecedence.put("^=", 12);
        operatorPrecedence.put("*=", 12);
        operatorPrecedence.put("**=", 12);
        operatorPrecedence.put("/=", 12);
        operatorPrecedence.put("//=", 12);
        operatorPrecedence.put("+=", 12);
        operatorPrecedence.put("-=", 12);
        operatorPrecedence.put("#>=", 12);
        operatorPrecedence.put("<#=", 12);
        operatorPrecedence.put("<=", 12);
        operatorPrecedence.put(">=", 12);
        operatorPrecedence.put("<>=", 12);
        operatorPrecedence.put("===", 12);
        operatorPrecedence.put("=<=", 12);
        operatorPrecedence.put("=>=", 12);
        operatorPrecedence.put("NOT=", 12);
        operatorPrecedence.put("AND=", 12);
        operatorPrecedence.put("OR=", 12);

        operatorPrecedence.put(",", 98);
    }

    private static abstract class Operator {

        public Token token;
        protected int precedence;
        protected int associativity;

        protected Operator() {

        }

        public Operator(int precedence, int associativity) {
            this.precedence = precedence;
            this.associativity = associativity;
        }

        public boolean yieldsTo(Operator other) {
            if (associativity == LEFT_TO_RIGHT) {
                return precedence > other.precedence;
            }
            else {
                return precedence >= other.precedence;
            }
        }

        public abstract Node evaluate();

        @Override
        public String toString() {
            return token.getText();
        }
    }

    private class UnaryOperator extends Operator {

        public UnaryOperator(Token token) {
            this.token = token;
            this.precedence = operatorPrecedence.get(token.getText());
            this.associativity = LEFT_TO_RIGHT;
        }

        @Override
        public Node evaluate() {
            Node result = new Node();
            result.addToken(token);
            result.addChild(operands.pop());
            return result;
        }

    }

    private class BinaryOperator extends Operator {

        public BinaryOperator(Token token) {
            this.token = token;
            this.precedence = operatorPrecedence.get(token.getText().toUpperCase());
            this.associativity = LEFT_TO_RIGHT;
        }

        @Override
        public Node evaluate() {
            Node result = new Node();
            result.addToken(token);
            result.addChild(operands.pop());
            result.addChild(operands.pop());
            return result;
        }

    }

    private class FunctionOperator extends Operator {

        Token name;

        public FunctionOperator(Token name, Token token) {
            this.name = name;
            this.token = token;
            this.precedence = operatorPrecedence.get(token.getText());
            this.associativity = LEFT_TO_RIGHT;
        }

        @Override
        public Node evaluate() {
            Node right = operands.pop();
            Node result = new Node();
            result.addToken(name);
            result.addToken(token);
            result.addChild(right);
            return result;
        }

    }

    private class SequenceOperator extends Operator {

        public SequenceOperator(Token token) {
            this.token = token;
            this.precedence = 98;
            this.associativity = RIGHT_TO_LEFT;
        }

        @Override
        public Node evaluate() {
            Node node0 = operands.pop();
            Node node1 = operands.pop();

            if (",".equals(node0.getToken(0).getText())) {
                node0.addChild(node1);
                return node0;
            }

            Node result = new Node();
            result.addToken(token);
            result.addChild(node1);
            result.addChild(node0);
            return result;
        }

    }

    int state;
    Deque<Node> operands = new ArrayDeque<Node>();
    Deque<Operator> operators = new ArrayDeque<Operator>();

    public final Operator SENTINEL = new Operator(99, RIGHT_TO_LEFT) {

        @Override
        public Node evaluate() {
            throw new RuntimeException("Can not evaluate sentinel.");
        }

        @Override
        public String toString() {
            return "SENTINEL";
        };
    };

    public Spin1ExpressionBuilder() {
        operators.push(SENTINEL);
    }

    public Node getExpression(List<Token> tokens) {
        int state = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            switch (state) {
                case 0:
                    if (token.type == Token.OPERATOR) {
                        addOperator(new UnaryOperator(token));
                        break;
                    }
                    // fall through
                case 1:
                    if (token.type == Token.OPERATOR || operatorPrecedence.containsKey(token.getText().toUpperCase())) {
                        addOperatorToken(token);
                        break;
                    }
                    if (i + 1 < tokens.size()) {
                        Token next = tokens.get(i + 1);
                        if (next.type != Token.EOF && next.type != Token.NL) {
                            if ("(".equals(next.getText())) {
                                addFunctionOperatorToken(token, next);
                                i++;
                                state = 0;
                                break;
                            }
                        }
                    }
                    addValueToken(token);
                    state = 2;
                    break;
                case 2:
                    addOperatorToken(token);
                    state = 1;
                    break;
            }
        }

        return getExpression();
    }

    public void addValueToken(Token token) {
        Node value = new Node();
        value.addToken(token);
        operands.push(value);
    }

    public void addOperatorToken(Token token) {
        Operator operator;
        if (",".equals(token.getText())) {
            operator = new SequenceOperator(token);
        }
        else if ("(".equals(token.getText())) {
            operator = new UnaryOperator(token);
        }
        else if ("[".equals(token.getText())) {
            operator = new UnaryOperator(token);
        }
        else {
            operator = new BinaryOperator(token);
        }
        addOperator(operator);
    }

    public void addOperator(Operator operator) {
        evaluateNotYieldingTo(operator);

        if ("(".equals(operator.token.getText()) || "[".equals(operator.token.getText())) {
            operators.push(operator);
            operators.push(SENTINEL);
        }
        else if (")".equals(operator.token.getText()) || "]".equals(operator.token.getText())) {
            if (operators.pop() != SENTINEL) {
                throw new RuntimeException("Sentinel expected.");
            }
            if (")".equals(operator.token.getText()) && !"(".equals(operators.peek().token.getText())) {
                throw new RuntimeException("Group open expected.");
            }
            if ("]".equals(operator.token.getText()) && !"[".equals(operators.peek().token.getText())) {
                throw new RuntimeException("Index open expected.");
            }
        }
        else {
            operators.push(operator);
        }
    }

    public void addFunctionOperatorToken(Token name, Token token) {
        Operator operator = new FunctionOperator(name, token);
        addOperator(operator);
    }

    private void evaluateNotYieldingTo(Operator operator) {
        while (!operators.peek().yieldsTo(operator)) {
            operands.push(operators.pop().evaluate());
        }
    }

    public Node getExpression() {
        if (operands.isEmpty() || operators.isEmpty()) {
            throw new RuntimeException("Operands / operators is empty: " + this);
        }

        // process remainder
        evaluateNotYieldingTo(SENTINEL);

        if (operators.size() > 1 && operators.peek() == SENTINEL) {
            throw new RuntimeException("Group close expected.");
        }
        if (operands.size() > 1 || operators.size() != 1) {
            throw new RuntimeException("Not all operands / operators were processed: " + this);
        }

        return operands.pop();
    }

    public static void main(String[] args) {
        try {
            print(compile("1 + 2 * 3"), 0);
            //print(compile("(1 + 2) * 3"), 0);
            //print(compile("    a := CNT"), 0);
            //print(compile("        waitcnt(a += 3_000_000)"), 0);
            //print(compile("Player(@Wav,10,11)"), 0);
            //print(compile("gr.line(ship_x - (last_cos ~> 15), ship_y - (last_sin ~> 15) )"), 0);
            //print(compile("mouse.button(THRUST_BUTTON_ID) and (Rand & $01)"), 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Node compile(String text) {
        List<Token> tokens = new ArrayList<Token>();

        Spin1TokenStream stream = new Spin1TokenStream(text);
        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF || token.type == Token.NL) {
                break;
            }
            tokens.add(token);
        }

        Spin1ExpressionBuilder builder = new Spin1ExpressionBuilder();
        return builder.getExpression(tokens);
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
