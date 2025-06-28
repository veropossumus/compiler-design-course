package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.*;

public interface Visitor<T, R> {

    R visit(AssignmentTree assignmentTree, T data);

    R visit(BinaryOperationTree binaryOperationTree, T data);

    R visit(BlockTree blockTree, T data);

    R visit(DeclarationTree declarationTree, T data);

    R visit(FunctionTree functionTree, T data);

    R visit(FunctionCallTree functionCallTree, T data);

    R visit(IdentExpressionTree identExpressionTree, T data);

    R visit(LiteralTree literalTree, T data);

    R visit(LValueIdentTree lValueIdentTree, T data);

    R visit(NameTree nameTree, T data);

    R visit(NegateTree negateTree, T data);

    R visit(ProgramTree programTree, T data);

    R visit(ReturnTree returnTree, T data);

    R visit(TypeTree typeTree, T data);

    R visit(WhileTree whileTree, T data);

    R visit(BreakTree breakTree, T data);

    R visit(IfTree ifTree, T data);

    R visit(ForTree forTree, T data);

    R visit(ContinueTree continueTree, T data);

    R visit(TernaryOperationTree ternaryOperationTree, T data);

    R visit(BoolLiteralTree boolLiteralTree, T data);

    R visit(LogicalNotTree logicalNotTree, T data);
}
