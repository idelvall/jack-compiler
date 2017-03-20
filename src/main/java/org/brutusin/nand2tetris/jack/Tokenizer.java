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


import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Tokenizer implements Iterable<Tokenizer.Token> {

    private final static BigInteger MAX_INT_LITERAL = new BigInteger("32767");
    private final static Set<String> KEYWORDS;
    private final static Set<Character> SYMBOLS;

    private final List<Token> tokens;

    static {
        KEYWORDS = new HashSet<>();
        KEYWORDS.add("class");
        KEYWORDS.add("constructor");
        KEYWORDS.add("function");
        KEYWORDS.add("method");
        KEYWORDS.add("field");
        KEYWORDS.add("static");
        KEYWORDS.add("var");
        KEYWORDS.add("int");
        KEYWORDS.add("char");
        KEYWORDS.add("boolean");
        KEYWORDS.add("void");
        KEYWORDS.add("true");
        KEYWORDS.add("false");
        KEYWORDS.add("null");
        KEYWORDS.add("this");
        KEYWORDS.add("let");
        KEYWORDS.add("do");
        KEYWORDS.add("if");
        KEYWORDS.add("else");
        KEYWORDS.add("while");
        KEYWORDS.add("return");

        SYMBOLS = new HashSet<>();
        SYMBOLS.add('{');
        SYMBOLS.add('}');
        SYMBOLS.add('(');
        SYMBOLS.add(')');
        SYMBOLS.add('[');
        SYMBOLS.add(']');
        SYMBOLS.add('.');
        SYMBOLS.add(',');
        SYMBOLS.add(';');
        SYMBOLS.add('+');
        SYMBOLS.add('-');
        SYMBOLS.add('*');
        SYMBOLS.add('/');
        SYMBOLS.add('&');
        SYMBOLS.add('~');
        SYMBOLS.add('|');
        SYMBOLS.add('<');
        SYMBOLS.add('>');
        SYMBOLS.add('=');
    }

    public Tokenizer(String code) throws CompilerException {
        this.tokens = parse(code);
    }

    private static List<Token> parse(String code) throws CompilerException {
        LinkedList<Token> ret = new LinkedList<>();
        boolean escaping = false;
        boolean inString = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        int start = 0;
        int lineNumber = 1;
        int i = 0;
        int lineStartIndex = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (inString) {
                if (c == '\n') {
                    throw new CompilerException("End of line found inside string literal", lineNumber, start - lineStartIndex + 1);
                }
                if (escaping) {
                    escaping = false;
                } else {
                    if (c == '\\') {
                        escaping = true;
                    } else if (c == '\"') {
                        inString = false;
                        ret.add(new Token(code.substring(start, i), Token.Type.stringConstant, lineNumber, start - lineStartIndex + 1));
                        start = i + 1;
                    }
                }
            } else {
                if (inSingleLineComment) {
                    if (c == '\n') {
                        start = i + 1;
                        lineNumber++;
                        lineStartIndex = i + 1;
                        inSingleLineComment = false;
                    }
                } else if (inMultiLineComment) {
                    if (c == '\n') {
                        lineNumber++;
                        lineStartIndex = i + 1;
                    } else if (c == '*' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                        start = i + 2;
                        i++;
                        inMultiLineComment = false;
                    }
                } else if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                    if (i != start) {
                        ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
                    }
                    inSingleLineComment = true;
                } else if (c == '/' && i + 2 < code.length() && code.charAt(i + 1) == '*' && code.charAt(i + 2) == '*') {
                    if (i != start) {
                        ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
                    }
                    inMultiLineComment = true;
                } else if (c == '\"') {
                    if (i != start) {
                        ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
                    }
                    start = i + 1;
                    inString = true;
                } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    if (i != start) {
                        ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
                    }
                    start = i + 1;
                    if (c == '\n') {
                        lineNumber++;
                        lineStartIndex = i + 1;
                    }
                } else if (SYMBOLS.contains(c)) {
                    if (i != start) {
                        ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
                    }
                    ret.add(new Token(code.substring(i, i + 1), Token.Type.symbol, lineNumber, start - lineStartIndex + 1));
                    start = i + 1;
                }
            }
            i++;
        }
        if (inString) {
            throw new CompilerException("Non terminated string literal", lineNumber, start - lineStartIndex + 1);
        }
        if (i != start) {
            ret.add(createTokenFrom(code.substring(start, i), lineNumber, start - lineStartIndex + 1));
        }
        return ret;
    }

    private static Token createTokenFrom(String s, int line, int column) throws CompilerException {
        if (s.charAt(0) > 47 && s.charAt(0) < 58) { // starts with number
            try {
                BigInteger v = new BigInteger(s);
                if (MAX_INT_LITERAL.compareTo(v) < 0) {
                    throw new CompilerException("Integer literal cannot exceed " + MAX_INT_LITERAL, line, column);
                }
                return new Token(s, Token.Type.integerConstant, line, column);
            } catch (NumberFormatException nfe) {
                throw new CompilerException("Invalid token found: " + s, line, column);
            }
        } else if (KEYWORDS.contains(s)) {
            return new Token(s, Token.Type.keyword, line, column);
        } else {
            return new Token(s, Token.Type.identifier, line, column);
        }
    }

    @Override
    public final String toString() {
        return toXml();
    }

    public String toXml() {
        StringBuilder sb = new StringBuilder("<tokens>");
        for (Token token : this) {
            sb.append("\n");
            sb.append(token);
        }
        sb.append("\n");
        sb.append("</tokens>");
        return sb.toString();
    }

    @Override
    public Iterator<Token> iterator() {
        return tokenIterator();
    }

    public TokenizerIterator tokenIterator() {
        return new TokenizerIterator();
    }

    public static void main(String[] args) {
        try {
            Tokenizer tokenizer = new Tokenizer("class {a=1}");
            System.out.println(tokenizer);
        } catch (CompilerException pe) {
            System.err.println(pe.getMessage() + " at line " + pe.getLineNumber() + ", column " + pe.getColNumber());
        }
    }

    public class TokenizerIterator implements Iterator<Token> {

        private Iterator<Token> it = tokens.iterator();
        private Token next; // Used to store queried but not consumed token

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            return it.hasNext();
        }

        @Override
        public Token next() {
            return poll();
        }

        public Token poll() {
            if (next != null) {
                Token ret = next;
                next = null;
                return ret;
            } else {
                return it.next();
            }
        }

        public Token peek() {
            if (next == null) {
                next = it.next();
            }
            return next;
        }
    }

    public static class Token {

        public enum Type {

            keyword, symbol, identifier, integerConstant, stringConstant;
        }

        private final String value;
        private final Type type;
        private final int columNumber;
        private final int lineNumber;

        private Token(String value, Type type, int lineNumber, int columNumber) {
            this.value = value;
            this.type = type;
            this.lineNumber = lineNumber;
            this.columNumber = columNumber;
        }

        public Type getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getColumNumber() {
            return columNumber;
        }

        @Override
        public final String toString() {
            return toXml();
        }

        public String toXml() {
            return "<" + type + ">" + escapeXML(value) + "</" + type + ">";
        }

        private static String escapeXML(String value) {
            return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
