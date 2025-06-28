package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ExpressionTree extends Tree
        permits BinaryOperationTree, BoolLiteralTree, FunctionCallTree, IdentExpressionTree, LiteralTree, LogicalNotTree, NegateTree, TernaryOperationTree {
}
