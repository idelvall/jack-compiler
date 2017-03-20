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
package org.brutusin.nand2tetris.jack.model;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Declaration extends CompilerElement {

    public enum Scope {

        statiz("static"), field("this"), local, argument;

        private String str;

        Scope(String str) {
            this.str = str;
        }

        Scope() {
            this.str = name();
        }

        @Override
        public String toString() {
            return str;
        }
    }

    private final String type;
    private final String name;
    private final Scope scope;

    public Declaration(Integer lineNumber, Integer columnNumber, String type, String name, Scope scope) {
        super(lineNumber, columnNumber);
        this.type = type;
        this.name = name;
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Scope getScope() {
        return scope;
    }
}
