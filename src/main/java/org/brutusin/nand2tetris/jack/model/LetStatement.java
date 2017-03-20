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
public class LetStatement extends CompilerElement implements Statement {

    private final Term.Reference target;
    private final Expression expression;

    public LetStatement(Integer lineNumber, Integer columnNumber, Term.Reference target, Expression expression) {
        super(lineNumber, columnNumber);
        this.target = target;
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public Term.Reference getTarget() {
        return target;
    }
}
