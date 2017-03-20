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
 * See the License for the specific keyword governing permissions and
 * limitations under the License.
 */
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.brutusin.nand2tetris.jack.model.Declaration;
import org.brutusin.nand2tetris.jack.model.DoStatement;
import org.brutusin.nand2tetris.jack.model.Expression;
import org.brutusin.nand2tetris.jack.model.IfStatement;
import org.brutusin.nand2tetris.jack.model.JackClass;
import org.brutusin.nand2tetris.jack.model.LetStatement;
import org.brutusin.nand2tetris.jack.model.ReturnStatement;
import org.brutusin.nand2tetris.jack.model.Statement;
import org.brutusin.nand2tetris.jack.model.Subroutine;
import org.brutusin.nand2tetris.jack.model.SubroutineCall;
import org.brutusin.nand2tetris.jack.model.Term;
import org.brutusin.nand2tetris.jack.model.WhileStatement;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ClassParser {
    
    private final JackClass parsedClass;
    
    public ClassParser(Tokenizer tokenizer) throws CompilerException {
        this.parsedClass = parseClass(tokenizer.tokenIterator());
    }
    
    public JackClass getParsedClass() {
        return parsedClass;
    }
    
    private static JackClass parseClass(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "class");
        token = nextTokens.poll(); // class name
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (!startsWithUppercase(token.getValue())) {
            throw new CompilerException("Invalid class identifier " + token.getValue() + ". Class identifiers must start with an uppercase letter", token.getLineNumber(), token.getColumNumber());
        }
        JackClass ret = new JackClass(token.getLineNumber(), token.getColumNumber(), token.getValue());
        
        token = nextTokens.poll();  // left {
        assertToken(token, "{");
        
        while (true) {
            List<Declaration> varDec = parseClassVarDec(nextTokens);
            if (varDec == null) {
                break;
            }
            ret.getDeclarations().addAll(varDec);
        }
        while (true) {
            Subroutine subroutine = parseSubrutineDec(nextTokens);
            if (subroutine == null) {
                break;
            }
            ret.getSubroutines().add(subroutine);
        }
        token = nextTokens.poll(); // right }
        assertToken(token, "}");
        return ret;
    }
    
    private static List<Declaration> parseClassVarDec(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        return parseVarDec(nextTokens, true);
    }
    
    private static List<Declaration> parseSubroutineVarDec(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        return parseVarDec(nextTokens, false);
    }
    
    private static List<Declaration> parseVarDec(Tokenizer.TokenizerIterator nextTokens, boolean classLevel) throws CompilerException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() != Tokenizer.Token.Type.keyword) {
            return null;
        }
        Declaration.Scope scope;
        if (classLevel) {
            if (token.getValue().equals("static")) {
                scope = Declaration.Scope.statiz;
            } else if (token.getValue().equals("field")) {
                scope = Declaration.Scope.field;
            } else {
                return null;
            }
        } else {
            if (token.getValue().equals("var")) {
                scope = Declaration.Scope.local;
            } else {
                return null;
            }
        }
        token = nextTokens.poll(); // consume it
        List<Declaration> ret = new LinkedList<>();
        token = nextTokens.poll();  // type
        assertTypeToken(token);
        String type = token.getValue();
        int i = 0;
        while (true) {
            token = nextTokens.poll();
            if (token.getType() == Tokenizer.Token.Type.symbol && token.getValue().equals(";")) {
                break;
            } else if (i > 0) {
                assertToken(token, ",");
                token = nextTokens.poll();
            }
            assertToken(token, Tokenizer.Token.Type.identifier);
            ret.add(new Declaration(token.getLineNumber(), token.getColumNumber(), type, token.getValue(), scope));
            i++;
        }
        return ret;
    }
    
    private static Subroutine parseSubrutineDec(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() != Tokenizer.Token.Type.keyword) {
            return null;
        }
        Subroutine.Type type;
        if (token.getValue().equals("constructor")) {
            type = Subroutine.Type.constructor;
        } else if (token.getValue().equals("function")) {
            type = Subroutine.Type.function;
        } else if (token.getValue().equals("method")) {
            type = Subroutine.Type.method;
        } else {
            return null;
        }
        token = nextTokens.poll(); // consume it
        token = nextTokens.poll();  // return type
        assertReturnTypeToken(token);
        String returnType = token.getValue();
        token = nextTokens.poll(); // routine name
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (startsWithUppercase(token.getValue())) {
            throw new CompilerException("Invalid subroutine name " + token.getValue() + ". Subroutine names must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
        }
        String name = token.getValue();
        Subroutine ret = new Subroutine(token.getLineNumber(), token.getColumNumber(), type, returnType, name);
        token = nextTokens.poll();
        assertToken(token, "(");
        ret.getDeclarations().addAll(parseParamList(nextTokens));
        token = nextTokens.poll();
        assertToken(token, ")");
        token = nextTokens.poll();  // left {
        assertToken(token, "{");
        while (true) {
            List<Declaration> varDesc = parseSubroutineVarDec(nextTokens);
            if (varDesc == null) {
                break;
            }
            ret.getDeclarations().addAll(varDesc);
        }
        ret.getStatements().addAll(parseStatements(nextTokens));
        
        token = nextTokens.poll(); // right }
        assertToken(token, "}");
        
        return ret;
    }
    
    private static List<Declaration> parseParamList(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        
        List<Declaration> ret = new LinkedList<>();
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
                }
                token = nextTokens.poll();
                assertTypeToken(token);
                String type = token.getValue();
                token = nextTokens.poll();
                assertToken(token, Tokenizer.Token.Type.identifier);
                String name = token.getValue();
                ret.add(new Declaration(token.getLineNumber(), token.getColumNumber(), type, name, Declaration.Scope.argument));
            }
            i++;
        }
        return ret;
    }
    
    private static List<Statement> parseStatements(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        List<Statement> ret = new LinkedList<>();
        while (true) {
            Tokenizer.Token token = nextTokens.peek();
            if (token.getType() == Tokenizer.Token.Type.keyword) {
                if (token.getValue().equals("let")) {
                    ret.add(parseLetStatement(nextTokens));
                } else if (token.getValue().equals("if")) {
                    ret.add(parseIfStatement(nextTokens));
                } else if (token.getValue().equals("while")) {
                    ret.add(parseWhileStatement(nextTokens));
                } else if (token.getValue().equals("do")) {
                    ret.add(parseDoStatement(nextTokens));
                } else if (token.getValue().equals("return")) {
                    ret.add(parseReturnStatement(nextTokens));
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return ret;
    }
    
    private static Term parseTerm(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.peek();
        if (token.getType() == Tokenizer.Token.Type.integerConstant) {
            nextTokens.poll();
            return new Term.Constant(token.getLineNumber(), token.getColumNumber(), token.getValue(), Term.Constant.Type.integer);
        } else if (token.getType() == Tokenizer.Token.Type.stringConstant) {
            nextTokens.poll();
            return new Term.Constant(token.getLineNumber(), token.getColumNumber(), token.getValue().substring(1, token.getValue().length() - 1), Term.Constant.Type.string);
        } else if (token.getType() == Tokenizer.Token.Type.keyword) {
            if (token.getValue().equals("true") || token.getValue().equals("false") || token.getValue().equals("null") || token.getValue().equals("this")) {
                nextTokens.poll();
                return new Term.Constant(token.getLineNumber(), token.getColumNumber(), token.getValue(), Term.Constant.Type.keyword);
            }
        } else if (token.getType() == Tokenizer.Token.Type.symbol) {
            if (token.getValue().equals("-") || token.getValue().equals("~")) {
                nextTokens.poll();
                List<ParserNode> children = new LinkedList<>();
                children.add(new SimpleNode(token));
                Term term = parseTerm(nextTokens);
                if (term == null) {
                    throw new CompilerException("Unary operator requires a term", token.getLineNumber(), token.getColumNumber());
                }
                return new Term.UnaryTerm(token.getLineNumber(), token.getColumNumber(), token.getValue().charAt(0), term);
            } else if (token.getValue().equals("(")) {
                nextTokens.poll();
                Expression expression = parseExpression(nextTokens);
                if (expression == null) {
                    throw new CompilerException("Expected expression after (", token.getLineNumber(), token.getColumNumber());
                }
                token = nextTokens.poll();
                assertToken(token, ")");
                return expression;
            }
        } else if (token.getType() == Tokenizer.Token.Type.identifier) {
            nextTokens.poll();
            String name = token.getValue();
            Tokenizer.Token nextToken = nextTokens.peek();
            if (nextToken.getType() == Tokenizer.Token.Type.symbol) {
                if (nextToken.getValue().equals(".")) {
                    nextTokens.poll();
                    token = nextTokens.poll();
                    assertToken(token, Tokenizer.Token.Type.identifier);
                    if (startsWithUppercase(token.getValue())) {
                        throw new CompilerException("Subroutine name must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
                    }
                    String subroutine = token.getValue();
                    token = nextTokens.poll();
                    assertToken(token, "(");
                    SubroutineCall ret = new SubroutineCall(token.getLineNumber(), token.getColumNumber(), name, subroutine);
                    ret.getArguments().addAll(parseExpressionList(nextTokens));
                    token = nextTokens.poll();
                    assertToken(token, ")");
                    return ret;
                } else if (nextToken.getValue().equals("(")) {
                    nextTokens.poll();
                    SubroutineCall ret = new SubroutineCall(token.getLineNumber(), token.getColumNumber(), null, name);
                    ret.getArguments().addAll(parseExpressionList(nextTokens));
                    token = nextTokens.poll();
                    assertToken(token, ")");
                    return ret;
                } else if (nextToken.getValue().equals("[")) {
                    nextTokens.poll();
                    Expression exp = parseExpression(nextTokens);
                    if (exp == null) {
                        throw new CompilerException("Expression expected after [", nextToken.getLineNumber(), nextToken.getColumNumber());
                    }
                    token = nextTokens.poll();
                    assertToken(token, "]");
                    return new Term.ArrayReference(token.getLineNumber(), token.getColumNumber(), name, exp);
                }
            }
            return new Term.Reference(token.getLineNumber(), token.getColumNumber(), name);
        }
        return null;
    }
    
    private static Expression parseExpression(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        
        Term term = parseTerm(nextTokens);
        if (term == null) {
            return null;
        }
        Expression exp = new Expression(term.getLineNumber(), term.getColumnNumber());
        exp.getTerms().add(term);
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
                exp.getOperators().add(nextToken.getValue().charAt(0));
                term = parseTerm(nextTokens);
                if (term == null) {
                    throw new CompilerException("Expression expected after '" + nextToken.getValue() + "'", nextToken.getLineNumber(), nextToken.getColumNumber());
                }
                exp.getTerms().add(term);
            } else {
                break;
            }
        }
        return exp;
    }
    
    private static LetStatement parseLetStatement(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "let");
        token = nextTokens.poll(); // local name
        assertToken(token, Tokenizer.Token.Type.identifier);
        Term.Reference target;
        String varName = token.getValue();
        token = nextTokens.peek();
        if (token.getValue().equals("[")) {
            assertToken(token, Tokenizer.Token.Type.symbol);
            token = nextTokens.poll();
            Expression expression = parseExpression(nextTokens);
            if (expression == null) {
                throw new CompilerException("Empty expression found for array index", token.getLineNumber(), token.getColumNumber());
            }
            token = nextTokens.poll();
            assertToken(token, "]");
            target = new Term.ArrayReference(token.getLineNumber(), token.getColumNumber(), varName, expression);
        } else {
            target = new Term.Reference(token.getLineNumber(), token.getColumNumber(), varName);
        }
        token = nextTokens.poll();
        assertToken(token, "=");
        
        Expression expression = parseExpression(nextTokens);
        if (expression == null) {
            throw new CompilerException("Empty expression found after equals", token.getLineNumber(), token.getColumNumber());
        }
        token = nextTokens.poll();
        assertToken(token, ";");
        return new LetStatement(token.getLineNumber(), token.getColumNumber(), target, expression);
    }
    
    private static IfStatement parseIfStatement(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "if");
        
        token = nextTokens.poll();
        assertToken(token, "(");
        
        Expression expression = parseExpression(nextTokens);
        if (expression == null) {
            throw new CompilerException("Empty expression found for if condition", token.getLineNumber(), token.getColumNumber());
        }
        IfStatement ret = new IfStatement(token.getLineNumber(), token.getColumNumber(), expression);
        
        token = nextTokens.poll();
        assertToken(token, ")");
        
        token = nextTokens.poll();
        assertToken(token, "{");
        
        ret.getIfStatements().addAll(parseStatements(nextTokens));
        
        token = nextTokens.poll();
        assertToken(token, "}");
        
        token = nextTokens.peek();
        if (token.getValue().equals("else")) {
            assertToken(token, Tokenizer.Token.Type.keyword);
            token = nextTokens.poll();
            token = nextTokens.poll();
            assertToken(token, "{");
            ret.getElseStatements().addAll(parseStatements(nextTokens));
            token = nextTokens.poll();
            assertToken(token, "}");
        }
        
        return ret;
    }
    
    private static WhileStatement parseWhileStatement(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "while");
        
        token = nextTokens.poll();
        assertToken(token, "(");
        
        Expression expression = parseExpression(nextTokens);
        if (expression == null) {
            throw new CompilerException("Empty expression found for while condition", token.getLineNumber(), token.getColumNumber());
        }
        WhileStatement ret = new WhileStatement(token.getLineNumber(), token.getColumNumber(), expression);
        
        token = nextTokens.poll();
        assertToken(token, ")");
        
        token = nextTokens.poll();
        assertToken(token, "{");
        
        ret.getStatements().addAll(parseStatements(nextTokens));
        
        token = nextTokens.poll();
        assertToken(token, "}");
        
        return ret;
    }
    
    private static ReturnStatement parseReturnStatement(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "return");
        
        ReturnStatement ret = new ReturnStatement(token.getLineNumber(), token.getColumNumber(), parseExpression(nextTokens));
        
        token = nextTokens.poll();
        assertToken(token, ";");
        
        return ret;
    }
    
    private static DoStatement parseDoStatement(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        Tokenizer.Token token = nextTokens.poll();
        assertToken(token, "do");
        token = nextTokens.poll();
        Tokenizer.Token nextToken = nextTokens.peek();
        String target;
        if (nextToken.getType() == Tokenizer.Token.Type.symbol && nextToken.getValue().equals(".")) {
            nextTokens.poll();
            target = token.getValue();
            token = nextTokens.poll();
        } else {
            target = null;
        }
        assertToken(token, Tokenizer.Token.Type.identifier);
        if (startsWithUppercase(token.getValue())) {
            throw new CompilerException("Subroutine name must start with a lowercase letter", token.getLineNumber(), token.getColumNumber());
        }
        String name = token.getValue();
        token = nextTokens.poll();
        assertToken(token, "(");
        
        SubroutineCall action = new SubroutineCall(token.getLineNumber(), token.getColumNumber(), target, name);
        action.getArguments().addAll(parseExpressionList(nextTokens));
        
        token = nextTokens.poll();
        assertToken(token, ")");
        
        token = nextTokens.poll();
        assertToken(token, ";");
        
        return new DoStatement(token.getLineNumber(), token.getColumNumber(), action);
    }
    
    private static List<Expression> parseExpressionList(Tokenizer.TokenizerIterator nextTokens) throws CompilerException {
        int i = 0;
        Tokenizer.Token token = null;
        List<Expression> ret = new LinkedList<>();
        while (true) {
            if (i > 0) {
                token = nextTokens.peek();
                if (token.getType() == Tokenizer.Token.Type.symbol && token.getValue().equals(",")) {
                    nextTokens.poll();
                } else {
                    break;
                }
            }
            Expression expression = parseExpression(nextTokens);
            if (expression == null) {
                if (i == 0) {
                    break;
                } else {
                    throw new CompilerException("Missing expression after ','", token.getLineNumber(), token.getColumNumber());
                }
            }
            ret.add(expression);
            i++;
        }
        return ret;
    }
    
    private static boolean startsWithUppercase(String s) {
        return s.charAt(0) > 64 && s.charAt(0) < 91;
    }
    
    private static void assertTypeToken(Tokenizer.Token token) throws CompilerException {
        assertTypeToken(token, false);
    }
    
    private static void assertReturnTypeToken(Tokenizer.Token token) throws CompilerException {
        assertTypeToken(token, true);
    }
    
    private static void assertTypeToken(Tokenizer.Token token, boolean includeVoid) throws CompilerException {
        if (token.getType() == Tokenizer.Token.Type.keyword) {
            if (!token.getValue().equals("int") && !token.getValue().equals("char") && !token.getValue().equals("boolean") && !(includeVoid && token.getValue().equals("void"))) {
                throw new CompilerException("Invalid keyword " + token.getValue() + " found", token.getLineNumber(), token.getColumNumber());
            }
        } else if (token.getType() == Tokenizer.Token.Type.identifier) {
            if (!startsWithUppercase(token.getValue())) {
                throw new CompilerException("Invalid type token " + token.getValue() + " found. A type was expected", token.getLineNumber(), token.getColumNumber());
            }
        } else {
            throw new CompilerException("Invalid token " + token.getValue() + " found. A type was expected", token.getLineNumber(), token.getColumNumber());
        }
    }
    
    private static void assertToken(Tokenizer.Token token, Tokenizer.Token.Type type) throws CompilerException {
        if (token.getType() != type) {
            throw new CompilerException("Expected token of type " + type + " but found '" + token.getValue() + "'", token.getLineNumber(), token.getColumNumber());
        }
    }
    
    private static void assertToken(Tokenizer.Token token, String value) throws CompilerException {
        if (token.getType() == Tokenizer.Token.Type.stringConstant) {
            throw new CompilerException("A string literal was not expected at position", token.getLineNumber(), token.getColumNumber());
        }
        if (value != null && !value.equals(token.getValue())) {
            throw new CompilerException("Expected token " + value + " but found '" + token.getValue() + "'", token.getLineNumber(), token.getColumNumber());
        }
    }
    
    public static void main(String[] args) throws CompilerException {
        String code = "class Main {\n"
                + "   function void main() {\n"
                + "     var Array a; \n"
                + "     var int length;\n"
                + "     var int i, sum;\n"
                + "\n"
                + "     let length = Keyboard.readInt(\"How many numbers? \");\n"
                + "     let a = Array.new(length); // constructs the array\n"
                + "     \n"
                + "     let i = 0;\n"
                + "     while (i < length) {\n"
                + "        let a[i] = Keyboard.readInt(\"Enter a number: \");\n"
                + "        let sum = sum + a[i];\n"
                + "        let i = i + 1;\n"
                + "     }\n"
                + "     \n"
                + "     do Output.printString(\"The average is \");\n"
                + "     do Output.printInt(sum / length);\n"
                + "     return;\n"
                + "   }\n"
                + "}";
        
        ClassParser cp = new ClassParser(new Tokenizer(code));
        System.out.println(cp.getParsedClass());
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
