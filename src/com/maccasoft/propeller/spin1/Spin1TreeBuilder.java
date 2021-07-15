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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maccasoft.propeller.model.Token;

public class Spin1TreeBuilder {

    static int highestPrecedence = 1;
    static Map<String, Integer> precedence = new HashMap<String, Integer>();
    static {
        //precedence.put("(", highestPrecedence);
        //precedence.put(")", highestPrecedence);
        //highestPrecedence++;

        //precedence.put("[", highestPrecedence);
        //precedence.put("]", highestPrecedence);
        //highestPrecedence++;

        //precedence.put("\\", highestPrecedence);
        //highestPrecedence++;

        precedence.put(">>", highestPrecedence);
        precedence.put("<<", highestPrecedence);
        precedence.put("~>", highestPrecedence);
        precedence.put("->", highestPrecedence);
        precedence.put("<-", highestPrecedence);
        precedence.put("><", highestPrecedence);
        //precedence.put("ZEROX", 3);
        //precedence.put("SIGNX", 3);
        highestPrecedence++;

        precedence.put("&", highestPrecedence);
        highestPrecedence++;
        precedence.put("^", highestPrecedence);
        precedence.put("|", highestPrecedence);
        highestPrecedence++;

        precedence.put("*", highestPrecedence);
        precedence.put("**", highestPrecedence);
        precedence.put("/", highestPrecedence);
        //precedence.put("+/", 7);
        precedence.put("//", highestPrecedence);
        //precedence.put("+//", 7);
        //precedence.put("SCA", 7);
        //precedence.put("SCAS", 7);
        //precedence.put("FRAC", 7);
        highestPrecedence++;

        precedence.put("+", highestPrecedence);
        precedence.put("-", highestPrecedence);
        highestPrecedence++;

        precedence.put("#>", highestPrecedence);
        precedence.put("<#", highestPrecedence);
        highestPrecedence++;

        precedence.put("<", highestPrecedence);
        //precedence.put("+<", 11);
        precedence.put("=<", highestPrecedence);
        //precedence.put("+<=", 11);
        precedence.put("==", highestPrecedence);
        precedence.put("<>", highestPrecedence);
        precedence.put("=>", highestPrecedence);
        //precedence.put("+>=", 11);
        precedence.put(">", highestPrecedence);
        //precedence.put("+>", 11);
        //precedence.put("<=>", 11);
        highestPrecedence++;

        //precedence.put("&&", 12);
        precedence.put("AND", highestPrecedence);
        //precedence.put("^^", 12);
        //precedence.put("XOR", 12);
        //precedence.put("||", 12);
        highestPrecedence++;
        precedence.put("OR", highestPrecedence);
        highestPrecedence++;

        precedence.put("..", highestPrecedence);
        highestPrecedence++;

        //precedence.put(",", highestPrecedence);
        precedence.put(":", highestPrecedence);
        precedence.put("?", highestPrecedence);
        highestPrecedence++;

        precedence.put(":=", highestPrecedence);

        precedence.put(">>=", highestPrecedence);
        precedence.put("<<=", highestPrecedence);
        precedence.put("~>=", highestPrecedence);
        precedence.put("->=", highestPrecedence);
        precedence.put("<-=", highestPrecedence);
        precedence.put("><=", highestPrecedence);
        //precedence.put("ZEROX=", highestPrecedence);
        //precedence.put("SIGNX=", highestPrecedence);

        precedence.put("&=", highestPrecedence);
        precedence.put("^=", highestPrecedence);
        precedence.put("|=", highestPrecedence);

        precedence.put("*=", highestPrecedence);
        precedence.put("**=", highestPrecedence);
        precedence.put("/=", highestPrecedence);
        //precedence.put("+/=", highestPrecedence);
        precedence.put("//=", highestPrecedence);
        //precedence.put("+//=", highestPrecedence);
        //precedence.put("SCA=", highestPrecedence);
        //precedence.put("SCAS=", highestPrecedence);
        //precedence.put("FRAC=", highestPrecedence);

        precedence.put("+=", highestPrecedence);
        precedence.put("-=", highestPrecedence);

        precedence.put("#>=", highestPrecedence);
        precedence.put("<#=", highestPrecedence);

        precedence.put("<=", highestPrecedence);
        //precedence.put("+<=", highestPrecedence);
        precedence.put("=<=", highestPrecedence);
        //precedence.put("+<==", highestPrecedence);
        precedence.put("===", highestPrecedence);
        precedence.put("<>=", highestPrecedence);
        precedence.put("=>=", highestPrecedence);
        //precedence.put("+>==", highestPrecedence);
        precedence.put(">=", highestPrecedence);
        //precedence.put("+>=", highestPrecedence);
        //precedence.put("<=>=", highestPrecedence);

        //precedence.put("&&=", highestPrecedence);
        precedence.put("AND=", highestPrecedence);
        //precedence.put("^^=", highestPrecedence);
        //precedence.put("XOR=", highestPrecedence);
        //precedence.put("||=", highestPrecedence);
        precedence.put("OR=", highestPrecedence);
    }

    int index;
    List<Token> tokens = new ArrayList<Token>();

    public void addToken(Token token) {
        tokens.add(token);
    }

    public Spin1StatementNode getRoot() {
        Spin1StatementNode left = parseLevel(highestPrecedence);

        Token token = peek();
        if (token == null) {
            return left;
        }

        Integer p = precedence.get(token.getText().toUpperCase());
        if (p != null && p.intValue() == highestPrecedence) {
            Spin1StatementNode node = new Spin1StatementNode(next());
            node.addChild(left);
            node.addChild(parseLevel(highestPrecedence));
            return node;
        }

        throw new RuntimeException("unexpected " + token.getText());
    }

    Spin1StatementNode parseLevel(int level) {
        Spin1StatementNode left = level == 0 ? parseAtom() : parseLevel(level - 1);

        Token token = peek();
        if (token == null) {
            return left;
        }

        Integer p = precedence.get(token.getText().toUpperCase());
        if (p != null && p.intValue() == level) {
            Spin1StatementNode node = new Spin1StatementNode(next());
            node.addChild(left);
            node.addChild(level == 0 ? parseAtom() : parseLevel(level));
            return node;
        }

        return left;
    }

    Spin1StatementNode parseAtom() {
        Token token = peek();

        if ("+".equals(token.getText()) || "-".equals(token.getText()) || "?".equals(token.getText()) || "++".equals(token.getText()) || "--".equals(token.getText()) || "~".equals(token.getText())
            || "~~".equals(token.getText()) || "\\".equals(token.getText()) || "||".equals(token.getText())) {
            Spin1StatementNode node = new Spin1StatementNode(next());
            node.addChild(parseAtom());
            return node;
        }

        if ("(".equals(token.getText())) {
            next();
            Spin1StatementNode node = parseLevel(highestPrecedence);
            token = next();
            if (token == null) {
                throw new RuntimeException("expecting closing parenthesis");
            }
            if (!")".equals(token.getText())) {
                throw new RuntimeException("expecting closing parenthesis, got " + token.getText());
            }
            return node;
        }

        if ("[".equals(token.getText())) {
            next();
            Spin1StatementNode node = parseLevel(highestPrecedence);
            token = next();
            if (token == null) {
                throw new RuntimeException("expecting closing parenthesis");
            }
            if (!"]".equals(token.getText())) {
                throw new RuntimeException("expecting closing parenthesis, got " + token.getText());
            }
            return node;
        }

        if (token.type == 0) {
            Spin1StatementNode node = new Spin1StatementNode(next());
            if ((token = peek()) != null) {
                if ("(".equals(token.getText())) {
                    next();
                    if (peek() != null && ")".equals(peek().getText())) {
                        next();
                        return node;
                    }
                    for (;;) {
                        Spin1StatementNode child = parseLevel(highestPrecedence);
                        if (node.getChildCount() == 1 && ":".equals(node.getChild(0).getText())) {
                            node.getChild(0).addChild(child);
                        }
                        else {
                            node.addChild(child);
                        }
                        token = next();
                        if (token == null) {
                            throw new RuntimeException("expecting closing parenthesis");
                        }
                        if (")".equals(token.getText())) {
                            return node;
                        }
                        if (!",".equals(token.getText()) && !":".equals(token.getText())) {
                            throw new RuntimeException("expecting closing parenthesis, got " + token.getText());
                        }
                    }
                }
                if ("[".equals(token.getText())) {
                    next();
                    node.addChild(parseLevel(highestPrecedence));
                    token = next();
                    if (token == null) {
                        throw new RuntimeException("expecting closing parenthesis");
                    }
                    if (!"]".equals(token.getText())) {
                        throw new RuntimeException("expecting closing parenthesis, got " + token.getText());
                    }

                    token = peek();
                    if (token == null) {
                        return node;
                    }
                    if ("[".equals(token.getText())) {
                        next();
                        node.addChild(parseLevel(highestPrecedence));
                        token = next();
                        if (token == null) {
                            throw new RuntimeException("expecting closing parenthesis");
                        }
                        if (!"]".equals(token.getText())) {
                            throw new RuntimeException("expecting closing parenthesis, got " + token.getText());
                        }
                    }
                }
                if ("?".equals(token.getText()) || "++".equals(token.getText()) || "--".equals(token.getText()) || "~".equals(token.getText()) || "~~".equals(token.getText())) {
                    node.addChild(new Spin1StatementNode(next()));
                }
            }
            return node;
        }

        if (token.type != Token.OPERATOR) {
            return new Spin1StatementNode(next());
        }

        throw new RuntimeException("unexpected " + token.getText());
    }

    Token peek() {
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        return null;
    }

    Token next() {
        if (index < tokens.size()) {
            return tokens.get(index++);
        }
        return null;
    }

    public static void main(String[] args) {
        String text;

        text = "1 + 2 + 3 + 4";
        System.out.println(text);
        System.out.println(parse(text));

        text = "chr := -15 + --chr & %11011111 + 39*(chr > 56)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "function(1 + 2 * 3, 4, (5 + 6) * 7)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "a := function1(1, 2) + function2(3) * function3(4, 5, 6)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "a := b[c]";
        System.out.println(text);
        System.out.println(parse(text));

        text = "a := b[c][0]";
        System.out.println(text);
        System.out.println(parse(text));

        text = "a := b[0+1*2] + e[4 + c * d] * long[@a][1]";
        System.out.println(text);
        System.out.println(parse(text));

        text = "a[1]~~";
        System.out.println(text);
        System.out.println(parse(text));

        text = "~~a[1]";
        System.out.println(text);
        System.out.println(parse(text));

        text = "    a := lookup(b : 10, 20..30, 40)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "((chr := byte[stringptr][index++]) == 0)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "z_pad or (div == 1)";
        System.out.println(text);
        System.out.println(parse(text));

        text = "r := a(1 + 2) * b++ - b(c += 6) * -c(8 + 9)";
        System.out.println(text);
        System.out.println(parse(text));
    }

    static String parse(String text) {
        Spin1TreeBuilder builder = new Spin1TreeBuilder();

        Spin1TokenStream stream = new Spin1TokenStream(text);
        while (true) {
            Token token = stream.nextToken();
            if (token.type == Token.EOF) {
                break;
            }
            builder.tokens.add(token);
        }

        Spin1StatementNode root = builder.getRoot();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        print(new PrintStream(os), root, 0);
        return os.toString();
    }

    static void print(PrintStream out, Spin1StatementNode node, int indent) {
        if (indent != 0) {
            for (int i = 1; i < indent; i++) {
                out.print("     ");
            }
            out.print(" +-- ");
        }

        out.print("[" + node.getText().replaceAll("\n", "\\\\n") + "]");
        out.println();

        for (Spin1StatementNode child : node.getChilds()) {
            print(out, child, indent + 1);
        }
    }

}
