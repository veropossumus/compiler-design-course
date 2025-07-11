package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.*;

import java.util.List;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree(List<StatementTree> statements, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case FunctionTree(var returnType, var name, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("()");
                space();
                printTree(body);
            }
            case NameTree(var name, _) -> print(name.asString());
            case ProgramTree(var topLevelTrees, var scope) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case LiteralTree(var value, _, _) -> this.builder.append(value);
            case BoolLiteralTree(var value, _) -> this.builder.append(value);
            case NegateTree(var expression, _) -> {
                print("-(");
                printTree(expression);
                print(")");
            }
            case LogicalNotTree(var expression, _) -> {
                print("!(");
                printTree(expression);
                print(")");
            }
            case AssignmentTree(var lValue, var op, var expression, _) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer, _) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case IdentExpressionTree(var name, var block) -> printTree(name);
            case WhileTree(var condition, var body, _) -> {
                print("while (");
                printTree(condition);
                print(") ");
                printTree(body);
            }
            case BreakTree(_, _) -> {
                print("break");
                semicolon();
            }
            case IfTree(var condition, var thenBranch, var elseBranch, _) -> {
                print("if (");
                printTree(condition);
                print(") ");
                printTree(thenBranch);
                if (elseBranch != null) {
                    print(" else ");
                    printTree(elseBranch);
                }
            }

            case ForTree(var init, var condition, var update, var body, _) -> {
                print("for (");
                if (init != null) {
                    printTree(init);
                }
                print("; ");
                if (condition != null) {
                    printTree(condition);
                }
                print("; ");
                if (update != null) {
                    printTree(update);
                }
                print(") ");
                printTree(body);
            }

            case ContinueTree(_, _) -> {
                print("continue");
                semicolon();
            }

            case TernaryOperationTree(var condition, var thenBranch, var elseBranch) -> {
                print("(");
                printTree(condition);
                print(") ? ");
                printTree(thenBranch);
                print(" : ");
                printTree(elseBranch);
            }

        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
