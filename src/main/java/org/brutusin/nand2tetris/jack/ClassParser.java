package org.brutusin.nand2tetris.jack;

/*
 * Copyright 2017 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ClassParser {

    private final CompositeNode classNode;

    public ClassParser(Tokenizer tokenizer) throws ParseException {
        this.classNode = parseClass(tokenizer.tokenIterator());
    }

    public ParserNode getClassNode() {
        return classNode;
    }

    private static CompositeNode parseClass(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "class");
        children.add(new SimpleNode(token));

        token = nextTokens.poll(); // class name
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (!startsWithUppercase(token.getValue())) {
            throw new ParseException("Invalid class identifier " + token.getValue() + ". Class identifiers must start with an uppercase letter", token.getLineNumber(), token.getColumNumber());
        }
        children.add(new SimpleNode(token));

        token = nextTokens.poll();  // left {
        assertToken(token, "{");
        children.add(new SimpleNode(token));

        while (true) {
            CompositeNode classVarDec = parseClassVarDec(nextTokens);
            if (classVarDec == null) {
                break;
            }
            children.add(classVarDec);
        }
        while (true) {
            CompositeNode parseSubrutineDec = parseSubrutineDec(nextTokens);
            if (parseSubrutineDec == null) {
                break;
            }
            children.add(parseSubrutineDec);
        }

        token = nextTokens.poll(); // right }
        assertToken(token, "}");
        children.add(new SimpleNode(token));

        return new CompositeNode("class", children);
    }

    private static CompositeNode parseClassVarDec(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        return parseVarDec(nextTokens, true);
    }

    private static CompositeNode parseSubroutineVarDec(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        return parseVarDec(nextTokens, false);
    }

    private static CompositeNode parseVarDec(Tokenizer.TokenizerIterator nextTokens, boolean classLevel) throws ParseException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() != Tokenizer.Token.Type.keyword) {
            return null;
        }
        String nodeName;
        if (classLevel) {
            nodeName = "classVarDec";
            if (!token.getValue().equals("static") && !token.getValue().equals("field")) {
                return null;
            }
        } else {
            nodeName = "varDec";
            if (!token.getValue().equals("var")) {
                return null;
            }
        }
        token = nextTokens.poll(); // consume it
        List<ParserNode> children = new LinkedList<>();
        children.add(new SimpleNode(token));

        token = nextTokens.poll();  // type
        assertTypeToken(token);
        children.add(new SimpleNode(token));

        token = nextTokens.poll();  // identifier
        assertToken(token, Tokenizer.Token.Type.identifier);
        children.add(new SimpleNode(token));
        while (true) {
            token = nextTokens.poll();
            if (token.getType() == Tokenizer.Token.Type.symbol && token.getValue().equals(";")) {
                children.add(new SimpleNode(token));
                break;
            } else {
                assertToken(token, ",");
                children.add(new SimpleNode(token));
                token = nextTokens.poll();
                assertToken(token, Tokenizer.Token.Type.identifier);
                children.add(new SimpleNode(token));
            }
        }
        return new CompositeNode(nodeName, children);
    }

    private static CompositeNode parseSubrutineDec(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() != Tokenizer.Token.Type.keyword) {
            return null;
        }
        if (!token.getValue().equals("constructor") && !token.getValue().equals("function") && !token.getValue().equals("method")) {
            return null;
        }
        token = nextTokens.poll(); // consume it
        List<ParserNode> children = new LinkedList<>();
        children.add(new SimpleNode(token));

        token = nextTokens.poll();  // return type
        assertReturnTypeToken(token);
        children.add(new SimpleNode(token));

        token = nextTokens.poll(); // routine name
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (startsWithUppercase(token.getValue())) {
            throw new ParseException("Invalid subroutine name " + token.getValue() + ". Subroutine names must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
        }
        children.add(new SimpleNode(token));
        token = nextTokens.poll();
        assertToken(token, "(");
        children.add(new SimpleNode(token));
        children.add(parseParamList(nextTokens));
        token = nextTokens.poll();
        assertToken(token, ")");
        children.add(new SimpleNode(token));
        children.add(parseSubroutineBody(nextTokens));

        return new CompositeNode("subroutineDec", children);
    }

    private static CompositeNode parseParamList(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();

        Tokenizer.Token token;
        int i = 0;
        while (true) {
            token = nextTokens.peek();
            if (token.getType() == Tokenizer.Token.Type.symbol && token.getValue().equals(")")) {
                break;
            } else {
                if (i > 0) {
                    token = nextTokens.poll();
                    assertToken(token, ",");
                    children.add(new SimpleNode(token));
                }
                token = nextTokens.poll();
                assertTypeToken(token);
                children.add(new SimpleNode(token));
                token = nextTokens.poll();
                assertToken(token, Tokenizer.Token.Type.identifier);
                children.add(new SimpleNode(token));
            }
            i++;
        }
        return new CompositeNode("parameterList", children);
    }

    private static CompositeNode parseStatements(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        while (true) {
            Tokenizer.Token token = nextTokens.peek();
            if (token.getType() == Tokenizer.Token.Type.keyword) {
                if (token.getValue().equals("let")) {
                    children.add(parseLetStatement(nextTokens));
                } else if (token.getValue().equals("if")) {
                    children.add(parseIfStatement(nextTokens));
                } else if (token.getValue().equals("while")) {
                    children.add(parseWhileStatement(nextTokens));
                } else if (token.getValue().equals("do")) {
                    children.add(parseDoStatement(nextTokens));
                } else if (token.getValue().equals("return")) {
                    children.add(parseReturnStatement(nextTokens));
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return new CompositeNode("statements", children);
    }

    private static CompositeNode parseTerm(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() == Tokenizer.Token.Type.integerConstant || token.getType() == Tokenizer.Token.Type.stringConstant) {
            nextTokens.poll();
            List<ParserNode> children = new LinkedList<>();
            children.add(new SimpleNode(token));
            return new CompositeNode("term", children);
        } else if (token.getType() == Tokenizer.Token.Type.keyword) {
            if (token.getValue().equals("true") || token.getValue().equals("false") || token.getValue().equals("null") || token.getValue().equals("this")) {
                nextTokens.poll();
                List<ParserNode> children = new LinkedList<>();
                children.add(new SimpleNode(token));
                return new CompositeNode("term", children);
            }
        } else if (token.getType() == Tokenizer.Token.Type.symbol) {
            if (token.getValue().equals("-") || token.getValue().equals("~")) {
                nextTokens.poll();
                List<ParserNode> children = new LinkedList<>();
                children.add(new SimpleNode(token));
                CompositeNode nextTermNode = parseTerm(nextTokens);
                if (nextTermNode == null) {
                    throw new ParseException("Unary operator requires a term", token.getLineNumber(), token.getColumNumber());
                }
                children.add(nextTermNode);
                return new CompositeNode("term", children);
            } else if (token.getValue().equals("(")) {
                nextTokens.poll();
                List<ParserNode> children = new LinkedList<>();
                children.add(new SimpleNode(token));
                CompositeNode expNode = parseExpression(nextTokens);
                if (expNode == null) {
                    throw new ParseException("Expected expression after (", token.getLineNumber(), token.getColumNumber());
                }
                children.add(expNode);
                token = nextTokens.poll();
                assertToken(token, ")");
                children.add(new SimpleNode(token));
                return new CompositeNode("term", children);
            }
        } else if (token.getType() == Tokenizer.Token.Type.identifier) {
            List<ParserNode> children = new LinkedList<>();
            nextTokens.poll();
            children.add(new SimpleNode(token));
            Tokenizer.Token nextToken = nextTokens.peek();
            if (nextToken.getType() == Tokenizer.Token.Type.symbol) {
                if (nextToken.getValue().equals(".")) {
                    nextTokens.poll();
                    children.add(new SimpleNode(nextToken));
                    token = nextTokens.poll();
                    assertToken(token, Tokenizer.Token.Type.identifier);
                    if (startsWithUppercase(token.getValue())) {
                        throw new ParseException("Subroutine name must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
                    }
                    children.add(new SimpleNode(token));
                    token = nextTokens.poll();
                    assertToken(token, "(");
                    children.add(new SimpleNode(token));

                    children.add(parseExpressionList(nextTokens));

                    token = nextTokens.poll();
                    assertToken(token, ")");
                    children.add(new SimpleNode(token));
                } else if (nextToken.getValue().equals("(")) {
                    nextTokens.poll();
                    children.add(new SimpleNode(nextToken));
                    children.add(parseExpressionList(nextTokens));
                    token = nextTokens.poll();
                    assertToken(token, ")");
                    children.add(new SimpleNode(token));
                } else if (nextToken.getValue().equals("[")) {
                    nextTokens.poll();
                    children.add(new SimpleNode(nextToken));
                    CompositeNode expNode = parseExpression(nextTokens);
                    if (expNode == null) {
                        throw new ParseException("Expression expected after [", nextToken.getLineNumber(), nextToken.getColumNumber());
                    }
                    children.add(expNode);
                    token = nextTokens.poll();
                    assertToken(token, "]");
                    children.add(new SimpleNode(token));
                }
            }
            return new CompositeNode("term", children);
        }
        return null;
    }

    private static CompositeNode parseExpression(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        CompositeNode termNode = parseTerm(nextTokens);
        if (termNode == null) {
            return null;
        }
        List<ParserNode> children = new LinkedList<>();
        children.add(termNode);
        while (true) {
            Tokenizer.Token nextToken = nextTokens.peek();
            if (nextToken.getType() == Tokenizer.Token.Type.symbol && (nextToken.getValue().equals("+")
                    || nextToken.getValue().equals("-")
                    || nextToken.getValue().equals("*")
                    || nextToken.getValue().equals("/")
                    || nextToken.getValue().equals("&")
                    || nextToken.getValue().equals("|")
                    || nextToken.getValue().equals("<")
                    || nextToken.getValue().equals(">")
                    || nextToken.getValue().equals("="))) {

                nextTokens.poll();
                children.add(new SimpleNode(nextToken));
                termNode = parseTerm(nextTokens);
                if (termNode == null) {
                    throw new ParseException("Expression expected after '" + nextToken.getValue() + "'", nextToken.getLineNumber(), nextToken.getColumNumber());
                }
                children.add(termNode);
            } else {
                break;
            }
        }

        return new CompositeNode("expression", children);
    }

    private static CompositeNode parseLetStatement(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "let");
        children.add(new SimpleNode(token));
        token = nextTokens.poll(); // var name
        assertToken(token, Tokenizer.Token.Type.identifier);
        children.add(new SimpleNode(token));
        token = nextTokens.peek();
        if (token.getValue().equals("[")) {
            assertToken(token, Tokenizer.Token.Type.symbol);
            token = nextTokens.poll();
            children.add(new SimpleNode(token));
            CompositeNode expNode = parseExpression(nextTokens);
            if (expNode == null) {
                throw new ParseException("Empty expression found for array index", token.getLineNumber(), token.getColumNumber());
            }
            children.add(expNode);
            token = nextTokens.poll();
            assertToken(token, "]");
            children.add(new SimpleNode(token));
        }
        token = nextTokens.poll();
        assertToken(token, "=");
        children.add(new SimpleNode(token));

        CompositeNode expNode = parseExpression(nextTokens);
        if (expNode == null) {
            throw new ParseException("Empty expression found after equals", token.getLineNumber(), token.getColumNumber());
        }
        children.add(expNode);

        token = nextTokens.poll();
        assertToken(token, ";");
        children.add(new SimpleNode(token));

        return new CompositeNode("letStatement", children);
    }

    private static CompositeNode parseIfStatement(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "if");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, "(");
        children.add(new SimpleNode(token));

        CompositeNode expNode = parseExpression(nextTokens);
        if (expNode == null) {
            throw new ParseException("Empty expression found for if condition", token.getLineNumber(), token.getColumNumber());
        }
        children.add(expNode);

        token = nextTokens.poll();
        assertToken(token, ")");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, "{");
        children.add(new SimpleNode(token));

        children.add(parseStatements(nextTokens));

        token = nextTokens.poll();
        assertToken(token, "}");
        children.add(new SimpleNode(token));

        token = nextTokens.peek();
        if (token.getValue().equals("else")) {
            assertToken(token, Tokenizer.Token.Type.keyword);
            token = nextTokens.poll();
            children.add(new SimpleNode(token));

            token = nextTokens.poll();
            assertToken(token, "{");
            children.add(new SimpleNode(token));

            children.add(parseStatements(nextTokens));

            token = nextTokens.poll();
            assertToken(token, "}");
            children.add(new SimpleNode(token));
        }

        return new CompositeNode("ifStatement", children);
    }

    private static CompositeNode parseWhileStatement(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "while");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, "(");
        children.add(new SimpleNode(token));

        CompositeNode expNode = parseExpression(nextTokens);
        if (expNode == null) {
            throw new ParseException("Empty expression found for while condition", token.getLineNumber(), token.getColumNumber());
        }
        children.add(expNode);

        token = nextTokens.poll();
        assertToken(token, ")");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, "{");
        children.add(new SimpleNode(token));

        children.add(parseStatements(nextTokens));

        token = nextTokens.poll();
        assertToken(token, "}");
        children.add(new SimpleNode(token));

        return new CompositeNode("whileStatement", children);
    }

    private static CompositeNode parseReturnStatement(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "return");
        children.add(new SimpleNode(token));

        CompositeNode expNode = parseExpression(nextTokens);
        if (expNode != null) {
            children.add(expNode);
        }

        token = nextTokens.poll();
        assertToken(token, ";");
        children.add(new SimpleNode(token));

        return new CompositeNode("returnStatement", children);
    }

    private static CompositeNode parseDoStatement(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "do");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        Tokenizer.Token nextToken = nextTokens.peek();
        if (nextToken.getType() == Tokenizer.Token.Type.symbol && nextToken.getValue().equals(".")) {
            nextTokens.poll();
            children.add(new SimpleNode(token));
            children.add(new SimpleNode(nextToken));
            token = nextTokens.poll();
        }
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (startsWithUppercase(token.getValue())) {
            throw new ParseException("Subroutine name must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
        }
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, "(");
        children.add(new SimpleNode(token));

        children.add(parseExpressionList(nextTokens));

        token = nextTokens.poll();
        assertToken(token, ")");
        children.add(new SimpleNode(token));

        token = nextTokens.poll();
        assertToken(token, ";");
        children.add(new SimpleNode(token));

        return new CompositeNode("doStatement", children);
    }

    private static CompositeNode parseExpressionList(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();
        int i = 0;
        Tokenizer.Token token = null;
        while (true) {
            if (i > 0) {
                token = nextTokens.peek();
                if (token.getType() == Tokenizer.Token.Type.symbol && token.getValue().equals(",")) {
                    nextTokens.poll();
                    children.add(new SimpleNode(token));
                } else {
                    break;
                }
            }
            CompositeNode expNode = parseExpression(nextTokens);
            if (expNode == null) {
                if (i == 0) {
                    break;
                } else {
                    throw new ParseException("Missing expression after ','", token.getLineNumber(), token.getColumNumber());
                }
            }
            children.add(expNode);
            i++;
        }
        return new CompositeNode("expressionList", children);
    }

    private static CompositeNode parseSubroutineBody(Tokenizer.TokenizerIterator nextTokens) throws ParseException {
        List<ParserNode> children = new LinkedList<>();

        Tokenizer.Token token = nextTokens.poll();  // left {
        assertToken(token, "{");
        children.add(new SimpleNode(token));

        while (true) {
            CompositeNode varDevNode = parseSubroutineVarDec(nextTokens);
            if (varDevNode == null) {
                break;
            }
            children.add(varDevNode);
        }

        children.add(parseStatements(nextTokens));

        token = nextTokens.poll(); // right }
        assertToken(token, "}");
        children.add(new SimpleNode(token));

        return new CompositeNode("subroutineBody", children);
    }

    private static boolean startsWithUppercase(String s) {
        return s.charAt(0) > 64 && s.charAt(0) < 91;
    }

    private static void assertTypeToken(Tokenizer.Token token) throws ParseException {
        assertTypeToken(token, false);
    }

    private static void assertReturnTypeToken(Tokenizer.Token token) throws ParseException {
        assertTypeToken(token, true);
    }

    private static void assertTypeToken(Tokenizer.Token token, boolean includeVoid) throws ParseException {
        if (token.getType() == Tokenizer.Token.Type.keyword) {
            if (!token.getValue().equals("int") && !token.getValue().equals("char") && !token.getValue().equals("boolean") && !(includeVoid && token.getValue().equals("void"))) {
                throw new ParseException("Invalid keyword " + token.getValue() + " found", token.getLineNumber(), token.getColumNumber());
            }
        } else if (token.getType() == Tokenizer.Token.Type.identifier) {
            if (!startsWithUppercase(token.getValue())) {
                throw new ParseException("Invalid type token " + token.getValue() + " found. A type was expected", token.getLineNumber(), token.getColumNumber());
            }
        } else {
            throw new ParseException("Invalid token " + token.getValue() + " found. A type was expected", token.getLineNumber(), token.getColumNumber());
        }
    }

    private static void assertToken(Tokenizer.Token token, Tokenizer.Token.Type type) throws ParseException {
        if (token.getType() != type) {
            throw new ParseException("Expected token of type " + type + " but found '" + token.getValue() + "'", token.getLineNumber(), token.getColumNumber());
        }
    }

    private static void assertToken(Tokenizer.Token token, String value) throws ParseException {
        if (token.getType() == Tokenizer.Token.Type.stringConstant) {
            throw new ParseException("A string literal was not expected at position", token.getLineNumber(), token.getColumNumber());
        }
        if (value != null && !value.equals(token.getValue())) {
            throw new ParseException("Expected token " + value + " but found '" + token.getValue() + "'", token.getLineNumber(), token.getColumNumber());
        }
    }

    public static void main(String[] args) throws ParseException {
        String code = "class SquareGame {\n"
                + "   field Square square1, square2; // the square of this game\n"
                + "   field int direction; // the square's current direction: \n"
                + "                        // 0=none, 1=up, 2=down, 3=left, 4=right\n"
                + "}";

        ClassParser cp = new ClassParser(new Tokenizer(code));
        System.out.println(cp.getClassNode());
    }

    public abstract static class ParserNode {

        private final String name;

        public ParserNode(String name) {
            this.name = name;
        }

        public final String getName() {
            return name;
        }

        public abstract Integer getColumNumber();

        public abstract Integer getLineNumber();

        @Override
        public final String toString() {
            return toXml(0);
        }

        public abstract String toXml(int ident);
    }

    public static class CompositeNode extends ParserNode {

        private final List<ParserNode> children;

        public CompositeNode(String name, List<ParserNode> children) {
            super(name);
            if (children == null) {
                throw new IllegalArgumentException("children is required");
            }
            this.children = children;
        }

        public Iterator<ParserNode> getChildren() {
            return children.iterator();
        }

        @Override
        public Integer getLineNumber() {
            if (children.isEmpty()) {
                return null;
            } else {
                return children.get(0).getLineNumber();
            }
        }

        @Override
        public Integer getColumNumber() {
            if (children.isEmpty()) {
                return null;
            } else {
                return children.get(0).getColumNumber();
            }
        }

        @Override
        public String toXml(int ident) {
            StringBuilder identation = new StringBuilder();
            for (int i = 0; i < ident; i++) {
                identation.append("\t");
            }
            StringBuilder sb = new StringBuilder(identation).append("<").append(getName()).append(">");
            for (ParserNode child : children) {
                sb.append("\n");
                sb.append(child.toXml(ident + 1));
            }
            sb.append("\n");
            sb.append(identation).append("</").append(getName()).append(">");
            return sb.toString();
        }
    }

    public static class SimpleNode extends ParserNode {

        private final Tokenizer.Token token;

        public SimpleNode(Tokenizer.Token token) {
            super(token.getType().toString());
            this.token = token;
        }

        @Override
        public Integer getLineNumber() {
            return token.getLineNumber();
        }

        @Override
        public Integer getColumNumber() {
            return token.getColumNumber();
        }

        @Override
        public String toXml(int ident) {
            StringBuilder identation = new StringBuilder();
            for (int i = 0; i < ident; i++) {
                identation.append("\t");
            }
            return identation + token.toXml();
        }

        public String getValue() {
            return token.getValue();
        }
    }
}
