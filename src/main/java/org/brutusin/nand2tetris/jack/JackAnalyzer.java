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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class JackAnalyzer {

    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("File not found!");
            System.exit(1);
        }

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                     compile(file);
                }
            }
        } else if (f.isFile()) {
            compile(f);
        }
    }

    private static void compile(File f) throws CompilerException {
        if (f.getName().endsWith(".jack")) {
            try {
                String className = f.getName().substring(0, f.getName().length() - 5);
                try (FileOutputStream fos = new FileOutputStream(new File(f.getParentFile(), className + ".xml"))) {
                    ClassParser cp = new ClassParser(new Tokenizer(new String(Files.readAllBytes(f.toPath()))));
                    fos.write(cp.getParsedClass().toString().getBytes());
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }
}
