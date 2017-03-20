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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Subroutine extends CompilerElement{

    public enum Type {

        constructor, method, function
    }
    private final List<Declaration> declarations = new LinkedList<>();
    private final List<Statement> statements = new LinkedList<>();
    private final Type type;
    private final String returnType;
    private final String name;

    public Subroutine(Integer lineNumber, Integer columnNumber, Type type, String returnType, String name) {
        super(lineNumber, columnNumber);
        this.type = type;
        this.returnType = returnType;
        this.name = name;
    }

    public List<Declaration> getDeclarations() {
        return declarations;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public Type getType() {
        return type;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }
}
