package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ExpressionTree extends Tree
        permits BinaryBoolOperationTree, BinaryOperationTree, BoolLiteralTree, IdentExpressionTree, LiteralTree, NegateTree, NotTree, TernaryOperationTree {
}
