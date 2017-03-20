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
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.brutusin.nand2tetris.jack.model.Declaration;
import org.brutusin.nand2tetris.jack.model.DoStatement;
import org.brutusin.nand2tetris.jack.model.Expression;
import org.brutusin.nand2tetris.jack.model.IfStatement;
import org.brutusin.nand2tetris.jack.model.JackClass;
import org.brutusin.nand2tetris.jack.model.LetStatement;
import org.brutusin.nand2tetris.jack.model.ReturnStatement;
import org.brutusin.nand2tetris.jack.model.Statement;
import org.brutusin.nand2tetris.jack.model.Subroutine;
import org.brutusin.nand2tetris.jack.model.SubroutineCall;
import org.brutusin.nand2tetris.jack.model.Term;
import org.brutusin.nand2tetris.jack.model.WhileStatement;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ClassWriter {

    private final JackClass clazz;
    private final StringBuilder code;
    private final SymbolTable classSymTable = new SymbolTable();

    private int labelCounter;

    public ClassWriter(JackClass clazz) throws CompilerException {
        this.clazz = clazz;
        this.code = new StringBuilder();
        this.processClass(clazz);
    }

    public void writeCode(OutputStream os) {
        try {
            os.write(code.toString().getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void processClass(JackClass clazz) throws CompilerException {
        List<Declaration> declarations = clazz.getDeclarations();
        for (Declaration declaration : declarations) {
            try {
                classSymTable.add(declaration);
            } catch (SymbolTable.AlreadyRegisteredException ex) {
                throw new CompilerException("Invalid declaration. Identifier '" + declaration.getName() + "' is already in use", declaration.getLineNumber(), declaration.getColumnNumber());
            }
        }
        List<Subroutine> subroutines = clazz.getSubroutines();
        for (Subroutine subroutine : subroutines) {
            processSubroutine(subroutine);
        }
    }

    private void processSubroutine(Subroutine subroutine) throws CompilerException {
        SymbolTable st = new SymbolTable();
        if (subroutine.getType() == Subroutine.Type.method) {
            try {
                st.add(new Declaration(subroutine.getLineNumber(), subroutine.getColumnNumber(), clazz.getName(), "this", Declaration.Scope.argument));
            } catch (SymbolTable.AlreadyRegisteredException ex) {
                throw new AssertionError();
            }
        }
        List<Declaration> declarations = subroutine.getDeclarations();
        int varCount = 0;
        for (Declaration declaration : declarations) {
            if (declaration.getScope() == Declaration.Scope.local) {
                varCount++;
            }
            try {
                st.add(declaration);
            } catch (SymbolTable.AlreadyRegisteredException ex) {
                throw new CompilerException("Invalid parameter name. Identifier '" + declaration.getName() + "' is already in use", declaration.getLineNumber(), declaration.getColumnNumber());
            }
        }
        code.append("function").append(" ").append(clazz.getName()).append(".").append(subroutine.getName()).append(" ").append(varCount);
        code.append("\n");
        if (subroutine.getType() == Subroutine.Type.constructor) {
            code.append("push constant ").append(classSymTable.count(Declaration.Scope.field));
            code.append("\n");
            code.append("call Memory.alloc 1");
            code.append("\n");
            code.append("pop pointer 0");
            code.append("\n");
        } else if (subroutine.getType() == Subroutine.Type.method) {
            code.append("push argument 0");
            code.append("\n");
            code.append("pop pointer 0");
            code.append("\n");
        }
        processStatements(subroutine.getStatements(), st);
    }

    private void processStatements(List<Statement> statements, SymbolTable st) throws CompilerException {
        for (Statement statement : statements) {
            if (statement instanceof DoStatement) {
                processDoStatement((DoStatement) statement, st);
            } else if (statement instanceof ReturnStatement) {
                processReturnStatement((ReturnStatement) statement, st);
            } else if (statement instanceof LetStatement) {
                processLetStatement((LetStatement) statement, st);
            } else if (statement instanceof IfStatement) {
                processIfStatement((IfStatement) statement, st);
            } else if (statement instanceof WhileStatement) {
                processWhileStatement((WhileStatement) statement, st);
            }
        }
    }

    private void processReturnStatement(ReturnStatement statement, SymbolTable st) throws CompilerException {
        Expression exp = statement.getExpression();
        if (exp == null) {
            code.append("push constant 0");
            code.append("\n");
        } else {
            processExpression(exp, st);
        }
        code.append("return");
        code.append("\n");
    }

    private void processDoStatement(DoStatement statement, SymbolTable st) throws CompilerException {
        processSubroutineCall(statement.getAction(), st);
        code.append("pop temp 0");
        code.append("\n");
    }

    private void processSubroutineCall(SubroutineCall call, SymbolTable st) throws CompilerException {
        List<Expression> arguments = call.getArguments();
        String target = call.getTarget();
        int offset = 0;
        if (call.getTarget() == null) { // method invocation in same object
            target = clazz.getName();
            code.append("push pointer 0");
            code.append("\n");
            offset = 1;
        } else {
            SymbolTable.SymEntry entry = st.getEntry(target);
            if (entry == null) {
                entry = classSymTable.getEntry(target);
            }
            if (entry != null) { // method invocation to other object
                target = entry.getDeclaration().getType();
                code.append("push ").append(entry.getDeclaration().getScope()).append(" ").append(entry.getIndex());
                code.append("\n");
                offset = 1;
            }
        }
        for (Expression argument : arguments) {
            processExpression(argument, st);
        }
        //TODO validate num arguments
        code.append("call").append(" ").append(target).append(".").append(call.getName()).append(" ").append((arguments.size() + offset));
        code.append("\n");
    }

    private void processLetStatement(LetStatement statement, SymbolTable st) throws CompilerException {
        processExpression(statement.getExpression(), st);
        String varName = statement.getTarget().getVarName();
        SymbolTable.SymEntry entry = st.getEntry(varName);
        if (entry == null) {
            entry = classSymTable.getEntry(varName);
        }
        if (entry == null) {
            throw new CompilerException("Variable not declared ' " + varName + "'", statement.getTarget().getLineNumber(), statement.getTarget().getColumnNumber());
        }
        Term.Reference target = statement.getTarget();
        if (target instanceof Term.ArrayReference) {
            Term.ArrayReference arrayRef = (Term.ArrayReference) target;
            code.append("push ").append(entry.getDeclaration().getScope()).append(" ").append(entry.getIndex());
            code.append("\n");
            Expression indexExp = arrayRef.getIndex();
            processExpression(indexExp, st);
            code.append("add");
            code.append("\n");
            code.append("pop pointer 1");
            code.append("\n");
            code.append("pop that 0");
            code.append("\n");
        } else {
            code.append("pop ").append(entry.getDeclaration().getScope()).append(" ").append(entry.getIndex());
            code.append("\n");
        }
    }

    private void processIfStatement(IfStatement statement, SymbolTable st) throws CompilerException {
        int labelId = labelCounter++;
        processExpression(statement.getCondition(), st);
        code.append("if-goto ").append("IF_").append(labelId);
        code.append("\n");
        processStatements(statement.getElseStatements(), st);
        code.append("goto ").append("ENDIF_").append(labelId);
        code.append("\n");
        code.append("label ").append("IF_").append(labelId);
        code.append("\n");
        processStatements(statement.getIfStatements(), st);
        code.append("label ").append("ENDIF_").append(labelId);
        code.append("\n");
    }

    private void processWhileStatement(WhileStatement statement, SymbolTable st) throws CompilerException {
        int labelId = labelCounter++;
        code.append("label ").append("WHILE_").append(labelId);
        code.append("\n");
        processExpression(statement.getCondition(), st);
        code.append("not");
        code.append("\n");
        code.append("if-goto ").append("END_WHILE_").append(labelId);
        code.append("\n");
        processStatements(statement.getStatements(), st);
        code.append("goto ").append("WHILE_").append(labelId);
        code.append("\n");
        code.append("label ").append("END_WHILE_").append(labelId);
        code.append("\n");
    }

    private void processExpression(Expression exp, SymbolTable st) throws CompilerException {
        Iterator<Term> terms = exp.getTerms().iterator();
        Iterator<Character> operators = exp.getOperators().iterator();
        Term term = terms.next();
        processTerm(term, st);
        while (terms.hasNext()) {
            term = terms.next();
            processTerm(term, st);
            processOperator(operators.next());
        }
    }

    private void processOperator(char operator) throws CompilerException {
        if (operator == '+') {
            code.append("add");
        } else if (operator == '-') {
            code.append("sub");
        } else if (operator == '*') {
            code.append("call Math.multiply 2");
        } else if (operator == '/') {
            code.append("call Math.divide 2");
        } else if (operator == '&') {
            code.append("and");
        } else if (operator == '|') {
            code.append("or");
        } else if (operator == '<') {
            code.append("lt");
        } else if (operator == '>') {
            code.append("gt");
        } else if (operator == '=') {
            code.append("eq");
        } else {
            throw new AssertionError();
        }
        code.append("\n");
    }

    private void processTerm(Term term, SymbolTable st) throws CompilerException {
        if (term instanceof Term.Constant) {
            Term.Constant constant = (Term.Constant) term;
            if (constant.getType() == Term.Constant.Type.integer) {
                code.append("push constant ").append(constant.getValue());
                code.append("\n");
            } else if (constant.getType() == Term.Constant.Type.keyword) {
                if (constant.getValue().equals("null")) {
                    code.append("push constant 0");
                    code.append("\n");
                } else if (constant.getValue().equals("false")) {
                    code.append("push constant 0");
                    code.append("\n");
                } else if (constant.getValue().equals("true")) {
                    code.append("push constant 1");
                    code.append("\n");
                    code.append("neg");
                    code.append("\n");
                } else if (constant.getValue().equals("this")) {
                    code.append("push pointer 0");
                    code.append("\n");
                }
            } else if (constant.getType() == Term.Constant.Type.string) {
                int length = constant.getValue().length();
                code.append("push constant ").append(length);
                code.append("\n");
                code.append("call String.new 1");
                code.append("\n");
                for (int i = 0; i < length; i++) {
                    code.append("push constant ").append(Integer.valueOf(constant.getValue().charAt(i)));
                    code.append("\n");
                    code.append("call String.appendChar 2");
                    code.append("\n");
                }
            }
        } else if (term instanceof Expression) {
            processExpression((Expression) term, st);
        } else if (term instanceof Term.UnaryTerm) {
            Term.UnaryTerm unaryTerm = (Term.UnaryTerm) term;
            Character operator = unaryTerm.getOperator();
            processTerm(unaryTerm.getTerm(), st);
            if (operator == '~') {
                code.append("not");
            } else if (operator == '-') {
                code.append("neg");
            }
            code.append("\n");
        } else if (term instanceof Term.Reference) {
            Term.Reference ref = (Term.Reference) term;
            String varName = ref.getVarName();
            SymbolTable.SymEntry entry = st.getEntry(varName);
            if (entry == null) {
                entry = classSymTable.getEntry(varName);
            }
            if (entry == null) {
                throw new CompilerException("Variable not declared ' " + varName + "'", ref.getLineNumber(), ref.getColumnNumber());
            }
            if (ref instanceof Term.ArrayReference) {
                Term.ArrayReference arrayRef = (Term.ArrayReference) ref;
                code.append("push ").append(entry.getDeclaration().getScope()).append(" ").append(entry.getIndex());
                code.append("\n");
                Expression indexExp = arrayRef.getIndex();
                processExpression(indexExp, st);
                code.append("add");
                code.append("\n");
                code.append("pop pointer 1");
                code.append("\n");
                code.append("push that 0");
                code.append("\n");
            } else {
                code.append("push ").append(entry.getDeclaration().getScope()).append(" ").append(entry.getIndex());
                code.append("\n");
            }
        } else if (term instanceof SubroutineCall) {
            processSubroutineCall((SubroutineCall) term, st);
        }
    }

    public static void main(String[] args) throws Exception {
        String code = "class Main {\n"
                + "   function void main() {\n"
                + "     var Array a; \n"
                + "     var int length;\n"
                + "     var int i, sum;\n"
                + "\n"
                + "     let length = Keyboard.readInt(\"How many numbers? \");\n"
                + "     let a = Array.new(length); // constructs the array\n"
                + "     \n"
                + "     let i = 0;\n"
                + "     while (i < length) {\n"
                + "        let a[i] = Keyboard.readInt(\"Enter a number: \");\n"
                + "        let sum = sum + a[i];\n"
                + "        let i = i + 1;\n"
                + "     }\n"
                + "     \n"
                + "     do Output.printString(\"The average is \");\n"
                + "     do Output.printInt(sum / length);\n"
                + "     return;\n"
                + "   }\n"
                + "}";

        ClassParser cp = new ClassParser(new Tokenizer(code));
        ClassWriter cw = new ClassWriter(cp.getParsedClass());
        cw.writeCode(System.out);
    }
}
