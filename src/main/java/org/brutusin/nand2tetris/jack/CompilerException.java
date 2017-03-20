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


/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
 public class CompilerException extends Exception {

    private final Integer lineNumber;
    private final Integer colNumber;

    public CompilerException(String message) {
        this(message, null, null);
    }

    public CompilerException(String message, Integer lineNumber, Integer colNumber) {
        super(message);
        this.lineNumber = lineNumber;
        this.colNumber = colNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getColNumber() {
        return colNumber;
    }
}
