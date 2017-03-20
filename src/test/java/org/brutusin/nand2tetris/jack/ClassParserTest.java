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

import java.io.IOException;
import org.brutusin.commons.utils.Miscellaneous;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ClassParserTest {

    public ClassParserTest() {
    }

    @Test
    public void test1() throws IOException, CompilerException {
        test("ArrayTest/Main.jack", "ArrayTest/Main.xml");
    }

    @Test
    public void test2() throws IOException, CompilerException {
        test("ExpressionLessSquare/Main.jack", "ExpressionLessSquare/Main.xml");
    }

    @Test
    public void test3() throws IOException, CompilerException {
        test("ExpressionLessSquare/Square.jack", "ExpressionLessSquare/Square.xml");
    }

    @Test
    public void test4() throws IOException, CompilerException {
        test("ExpressionLessSquare/SquareGame.jack", "ExpressionLessSquare/SquareGame.xml");
    }

    @Test
    public void test5() throws IOException, CompilerException {
        test("Square/Main.jack", "Square/Main.xml");
    }

    @Test
    public void test6() throws IOException, CompilerException {
        test("Square/Square.jack", "Square/Square.xml");
    }

    @Test
    public void test7() throws IOException, CompilerException {
        test("Square/SquareGame.jack", "Square/SquareGame.xml");
    }

    public void test(String codeResName, String xmlResName) throws IOException, CompilerException {
        try {
            String code = Miscellaneous.toString(getClass().getClassLoader().getResourceAsStream(codeResName), "UTF-8");
            String xml = Miscellaneous.toString(getClass().getClassLoader().getResourceAsStream(xmlResName), "UTF-8");
            Tokenizer tokenizer = new Tokenizer(code);
            ClassParser cp = new ClassParser(tokenizer);
            System.out.println(cp.getParsedClass().toString().replaceAll("\\s*", ""));
            System.out.println(xml.replaceAll("\\s*", ""));
            assertEquals(cp.getParsedClass().toString().replaceAll("\\s*", ""), xml.replaceAll("\\s*", ""));
        } catch (CompilerException pe) {
            System.err.println(pe.getMessage() + " at (" + pe.getLineNumber() + ", " + pe.getColNumber() + ")");
            throw pe;
        }
    }

}
