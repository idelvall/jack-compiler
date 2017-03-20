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
public class SubroutineCall extends Term {

    private final String target;
    private final String name;
    private final List<Expression> arguments = new LinkedList<>();

    public SubroutineCall(Integer lineNumber, Integer columnNumber, String target, String name) {
        super(lineNumber, columnNumber);
        this.target = target;
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public List<Expression> getArguments() {
        return arguments;
    }
}
