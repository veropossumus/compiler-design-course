package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ExpressionTree extends Tree
        permits BoolLiteralTree, IdentExpressionTree, LiteralTree, LogicalNotTree, BinaryOperationTree, NegateTree, TernaryOperationTree {
}
