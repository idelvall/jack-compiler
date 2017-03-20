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
public class Expression extends Term {

    private final List<Term> terms = new LinkedList<>();
    private final List<Character> operators = new LinkedList<>();

    public Expression(Integer lineNumber, Integer columnNumber) {
        super(lineNumber, columnNumber);
    }

    public List<Term> getTerms() {
        return terms;
    }

    public List<Character> getOperators() {
        return operators;
    }
}
