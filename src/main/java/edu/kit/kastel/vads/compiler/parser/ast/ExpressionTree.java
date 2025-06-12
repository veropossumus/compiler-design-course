package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ExpressionTree extends Tree
        permits TernaryOperationTree, BinaryOperationTree, IdentExpressionTree, LiteralTree, NegateTree {
}
