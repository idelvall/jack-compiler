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
package org.brutusin.nand2tetris.jack;

import java.util.HashMap;
import java.util.Map;
import org.brutusin.nand2tetris.jack.model.Declaration;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SymbolTable {

    private final Map<String, SymEntry> entryMap = new HashMap<>();
    private final Map<Declaration.Scope, Integer> counters = new HashMap<>();

    public void add(Declaration declaration) throws AlreadyRegisteredException {
        if (entryMap.containsKey(declaration.getName())) {
            throw new AlreadyRegisteredException(declaration.getName());
        }
        Integer index = counters.get(declaration.getScope());
        if (index == null) {
            index = 0;
        } else {
            index++;
        }
        counters.put(declaration.getScope(), index);
        SymEntry se = new SymEntry();
        se.declaration = declaration;
        se.index = index;
        entryMap.put(declaration.getName(), se);
    }

    public SymEntry getEntry(String name) {
        return entryMap.get(name);
    }

    public int count(Declaration.Scope scope) {
        Integer ret = counters.get(scope);
        if (ret == null) {
            return 0;
        }
        return ret + 1;
    }

    public static class AlreadyRegisteredException extends Exception {

        public AlreadyRegisteredException(String message) {
            super(message);
        }
    }

    public static class SymEntry {

        private Declaration declaration;
        private int index;

        public Declaration getDeclaration() {
            return declaration;
        }

        public int getIndex() {
            return index;
        }
    }
}
