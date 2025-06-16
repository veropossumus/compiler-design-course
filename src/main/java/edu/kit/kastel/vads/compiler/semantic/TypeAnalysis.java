package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

import java.util.HashMap;
import java.util.List;

public class TypeAnalysis implements NoOpVisitor<Types> {

    enum TYPES{
        BOOL, VALID, INT
    }

    private TYPES getType(TypeTree typeTree) {
        Type name = typeTree.type();
        return switch(typeTree.type()) {
            case BasicType.BOOL -> TYPES.BOOL;
            case BasicType.INT  -> TYPES.INT;
        };
    }

    @Override
    public Unit visit(BinaryBoolOperationTree binaryBoolOperationTree, Types data){
        TYPES lhs = data.get(binaryBoolOperationTree.lhs());
        TYPES rhs = data.get(binaryBoolOperationTree.rhs());

        if(lhs != TYPES.INT) throwError(binaryBoolOperationTree, lhs, TYPES.INT);
        if(rhs != TYPES.INT) throwError(binaryBoolOperationTree, rhs, TYPES.INT);

        data.put(binaryBoolOperationTree, TYPES.BOOL);
        return NoOpVisitor.super.visit(binaryBoolOperationTree, data);
    }

    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, Types data){
        TYPES lhs = data.get(binaryOperationTree.lhs());
        TYPES rhs = data.get(binaryOperationTree.rhs());
        String operator = binaryOperationTree.operatorType().toString();

        switch (operator) {
            case "=" -> {
                if (lhs == null || rhs == null || lhs != rhs) {
                    throwError(binaryOperationTree, rhs, lhs);
                }
                data.put(binaryOperationTree, TYPES.VALID);
            }
            case ">", "<", ">=", "<=", "==", "!="-> {
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.BOOL);
            }
            case "+", "-", "*", "/", "%", "&", "|", "<<", ">>" -> {
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.INT);
            }
            case "+=", "-=", "*=", "/=", "%=", "^=", "&=", "|=", ">>=", "<<=" -> { //They shouldn't be here at all actually but at the assignment tree??
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.INT);
            }
            default -> {
                throwError(binaryOperationTree, lhs, TYPES.INT);
            }
        }

        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }


    @Override
    public Unit visit(BlockTree blockTree, Types data){
        List<StatementTree> blockStatements = blockTree.statements();
        for(StatementTree statement : blockStatements){
            if(data.get(statement) != TYPES.VALID){
                throwError(blockTree, data.get(statement), TYPES.VALID);
            }
        }

        data.put(blockTree, TYPES.VALID);
        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(AssignmentTree assignmentTree, Types data) {
        TYPES lhs;
        switch(assignmentTree.lValue()) {
            case LValueIdentTree identTree ->
                    lhs = data.get(identTree.name(), assignmentTree.block());
        }
        TYPES rhs = data.get(assignmentTree.expression());
        if(lhs != rhs) throwError(assignmentTree, rhs, lhs);
        if (!assignmentTree.operator().type().toString().equals("=") && !lhs.equals(TYPES.INT)){
            throwError(assignmentTree, lhs, TYPES.INT);

        }
        data.put(assignmentTree, TYPES.VALID);
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Types data) {
        TYPES declaredType = getType(declarationTree.type());

        if (declarationTree.initializer() != null) {
            TYPES exprType = data.get(declarationTree.initializer());
            System.out.println("Debug: Initializer type for " + declarationTree.name() + " is: " + exprType); // Debug

            if (exprType == null) {
                System.out.println("Debug: Initializer type is null, skipping type check for " + declarationTree.name());
            } else if (exprType != declaredType) {
                throwError(declarationTree, exprType, declaredType);
            }
        }

        data.put(declarationTree.name(), declaredType, declarationTree.block());
        data.put(declarationTree, TYPES.VALID);
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(WhileTree whileLoopTree, Types data) {
        if (data.get(whileLoopTree.condition()) != TYPES.BOOL) {
            throwError(whileLoopTree, data.get(whileLoopTree.condition()), TYPES.BOOL);
        }
        if (data.get(whileLoopTree.body()) != TYPES.VALID) {
            throwError(whileLoopTree, data.get(whileLoopTree.body()), TYPES.VALID);
        }

        data.put(whileLoopTree, TYPES.VALID);
        return NoOpVisitor.super.visit(whileLoopTree, data);
    }

    @Override
    public Unit visit(ForTree forLoopTree, Types data) {
        if (data.get(forLoopTree.init()) != TYPES.VALID) {
            throwError(forLoopTree.init(), data.get(forLoopTree.init()), TYPES.VALID);
        }

        TYPES conditionType = data.get(forLoopTree.condition());
        System.out.println("For loop condition type: " + conditionType + " at span: " + forLoopTree.condition().span());

        if (conditionType != TYPES.BOOL) {
            throwError(forLoopTree.condition(), conditionType, TYPES.BOOL);
        }
        if (data.get(forLoopTree.body()) != TYPES.VALID) {
            throwError(forLoopTree.body(), data.get(forLoopTree.body()), TYPES.VALID);
        }

        data.put(forLoopTree, TYPES.VALID);
        return NoOpVisitor.super.visit(forLoopTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, Types data) {
        TYPES exprType = data.get(returnTree.expression());
        if (exprType != TYPES.INT && exprType != TYPES.BOOL) {
            throwError(returnTree, exprType, TYPES.INT);
        }
        data.put(returnTree, TYPES.VALID);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(NegateTree negateTree, Types data) {
        TYPES exprType = data.get(negateTree.expression());

        if (exprType != TYPES.INT) {
            throwError(negateTree, exprType, TYPES.INT);
        }
        data.put(negateTree, TYPES.INT);

        return NoOpVisitor.super.visit(negateTree, data);
    }


    @Override
    public Unit visit(IfTree conditionalTree, Types data) {
        if (data.get(conditionalTree.condition()) != TYPES.BOOL) {
            throwError(conditionalTree, data.get(conditionalTree.condition()), TYPES.BOOL);
        }

        TYPES thenType = data.get(conditionalTree.thenBranch());

        if (conditionalTree.elseBranch() != null) {
            TYPES elseType = data.get(conditionalTree.elseBranch());
            if (thenType != elseType) {
                throwError(conditionalTree, elseType, thenType);
            }
            data.put(conditionalTree, thenType);
        } else {
            data.put(conditionalTree, TYPES.VALID);
        }

        return NoOpVisitor.super.visit(conditionalTree, data);
    }

    @Override
    public Unit visit(LiteralTree literalTree, Types data) {
        data.put(literalTree, TYPES.INT);
        return NoOpVisitor.super.visit(literalTree, data);
    }

    @Override
    public Unit visit(BoolLiteralTree boolLiteralTree, Types data) {
        data.put(boolLiteralTree, TYPES.BOOL);
        return NoOpVisitor.super.visit(boolLiteralTree, data);
    }

    @Override
    public Unit visit(IdentExpressionTree identExpr, Types data) {
        TYPES type = null;
        int currentScope = identExpr.block();

        while (currentScope >= 0 && type == null) {
            type = data.get(identExpr.name(), currentScope);
            currentScope--;
        }

        System.out.println("Looking up variable: " + identExpr.name() + ", found type: " + type);
        data.put(identExpr, type);
        return NoOpVisitor.super.visit(identExpr, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, Types data) {

        if (data.get(functionTree.body()) != TYPES.VALID) {
            throwError(functionTree, data.get(functionTree.body()), TYPES.VALID);
        }

        data.put(functionTree, TYPES.VALID);
        return NoOpVisitor.super.visit(functionTree, data);
    }

    private static void throwError(Tree tree, TYPES actualType, TYPES expectedType){
        throw new SemanticException("Invalid type at " + tree.span() + ": found " + actualType + ", expected " + expectedType);
    }

}