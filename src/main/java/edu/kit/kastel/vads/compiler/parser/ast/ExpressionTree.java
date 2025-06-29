package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ExpressionTree extends Tree
        permits LogicalNotTree, BinaryOperationTree, BoolLiteralTree, FunctionCallTree, IdentExpressionTree, LiteralTree, NegateTree, TernaryOperationTree {
}
