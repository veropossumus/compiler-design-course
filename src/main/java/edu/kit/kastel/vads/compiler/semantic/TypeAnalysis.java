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
    public Unit visit(BinaryOperationTree binaryOperationTree, Types data){
        TYPES type1 = data.get(binaryOperationTree.lhs());
        TYPES type2 = data.get(binaryOperationTree.rhs());

        if(type1 != TYPES.INT) throwError(binaryOperationTree, type1, TYPES.INT);
        if(type2 != TYPES.INT) throwError(binaryOperationTree, type2, TYPES.INT);

        data.put(binaryOperationTree, TYPES.INT);
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
        TYPES lhs = data.get(assignmentTree.lValue());
        TYPES rhs = data.get(assignmentTree.expression());

        if (lhs != rhs) {
            throwError(assignmentTree, rhs, lhs);
        }

        data.put(assignmentTree, TYPES.VALID);
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Types data) {
        TYPES t = getType(declarationTree.type());

        //TODO i gave up for now
        data.put(declarationTree, TYPES.VALID);
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, Types data) {
        if (data.get(ifTree.condition()) != TYPES.BOOL) {
            throwError(ifTree, data.get(ifTree.condition()), TYPES.BOOL);
        }

        if (data.get(ifTree.thenBranch()) != TYPES.VALID || data.get(ifTree.elseBranch()) != TYPES.VALID) {
            throwError(ifTree, TYPES.VALID, TYPES.VALID); // Could refine but keep generic here
        }

        data.put(ifTree, TYPES.VALID);
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, Types data) {
        if (data.get(whileTree.condition()) != TYPES.BOOL) {
            throwError(whileTree, data.get(whileTree.condition()), TYPES.BOOL);
        }
        if (data.get(whileTree.body()) != TYPES.VALID) {
            throwError(whileTree, data.get(whileTree.body()), TYPES.VALID);
        }

        data.put(whileTree, TYPES.VALID);
        return NoOpVisitor.super.visit(whileTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, Types data) {
        if (data.get(forTree.init()) != TYPES.VALID) throwError(forTree.init(), data.get(forTree.init()), TYPES.VALID);
        if (data.get(forTree.condition()) != TYPES.BOOL) throwError(forTree.condition(), data.get(forTree.condition()), TYPES.BOOL);
        if (data.get(forTree.body()) != TYPES.VALID) throwError(forTree.body(), data.get(forTree.body()), TYPES.VALID);

        data.put(forTree, TYPES.VALID);
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, Types data) {
        TYPES exprType = data.get(returnTree.expression());
        if (exprType != TYPES.INT) {
            throwError(returnTree, exprType, TYPES.INT);
        }
        data.put(returnTree, TYPES.VALID);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(BreakTree breakTree, Types data) {
        data.put(breakTree, TYPES.VALID);
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, Types data) {
        data.put(continueTree, TYPES.VALID);
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(NegateTree negateTree, Types data) {
        if (data.get(negateTree.expression()) != TYPES.INT) {
            throwError(negateTree, data.get(negateTree.expression()), TYPES.INT);
        }
        data.put(negateTree, TYPES.INT);
        return NoOpVisitor.super.visit(negateTree, data);
    }

    @Override
    public Unit visit(TernaryOperationTree ternaryTree, Types data) {
        if (data.get(ternaryTree.condition()) != TYPES.BOOL) {
            throwError(ternaryTree, data.get(ternaryTree.condition()), TYPES.BOOL);
        }

        TYPES thenType = data.get(ternaryTree.trueExpression());
        TYPES elseType = data.get(ternaryTree.falseExpression());

        if (thenType != elseType) {
            throwError(ternaryTree, thenType, elseType);
        }

        data.put(ternaryTree, thenType);
        return NoOpVisitor.super.visit(ternaryTree, data);
    }

    @Override
    public Unit visit(LiteralTree literalTree, Types data) {
        if (literalTree.value().equals("true") || literalTree.value().equals("false")) {
            data.put(literalTree, TYPES.BOOL);
        } else {
            data.put(literalTree, TYPES.INT);
        }
        return NoOpVisitor.super.visit(literalTree, data);
    }

    @Override
    public Unit visit(IdentExpressionTree identExpr, Types data) {
        TYPES type = data.get(identExpr.name());
        data.put(identExpr, type);
        return NoOpVisitor.super.visit(identExpr, data);
    }

    @Override
    public Unit visit(ProgramTree programTree, Types data) {
        return NoOpVisitor.super.visit(programTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, Types data) {
        return NoOpVisitor.super.visit(functionTree, data);
    }

    private static void throwError(Tree tree, TYPES actualType, TYPES expectedType){
        throw new SemanticException("Invalid type at " + tree.span() + ": found " + actualType + ", expected " + expectedType);
    }

}
