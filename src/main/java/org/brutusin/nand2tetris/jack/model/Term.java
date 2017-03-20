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
package org.brutusin.nand2tetris.jack.model;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class Term extends CompilerElement {

    public Term(Integer lineNumber, Integer columnNumber) {
        super(lineNumber, columnNumber);
    }

    public static class Constant extends Term {

        public enum Type {

            integer, string, keyword;
        }

        private final String value;
        private final Type type;

        public Constant(Integer lineNumber, Integer columnNumber, String value, Type type) {
            super(lineNumber, columnNumber);
            this.value = value;
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public Type getType() {
            return type;
        }        
    }

    public static class Reference extends Term {

        private final String varName;

        public Reference(Integer lineNumber, Integer columnNumber, String varName) {
            super(lineNumber, columnNumber);
            this.varName = varName;
        }

        public String getVarName() {
            return varName;
        }
    }

    public static class UnaryTerm extends Term {

        private final Character operator;
        private final Term term;

        public UnaryTerm(Integer lineNumber, Integer columnNumber, Character operator, Term term) {
            super(lineNumber, columnNumber);
            this.operator = operator;
            this.term = term;
        }

        public Term getTerm() {
            return term;
        }

        public Character getOperator() {
            return operator;
        }
    }

    public static class ArrayReference extends Reference {

        private final Expression index;

        public ArrayReference(Integer lineNumber, Integer columnNumber, String varName, Expression index) {
            super(lineNumber, columnNumber, varName);
            this.index = index;
        }

        public Expression getIndex() {
            return index;
        }
    }
}
