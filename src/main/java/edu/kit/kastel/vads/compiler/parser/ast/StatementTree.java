package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface StatementTree extends Tree permits AssignmentTree, BlockTree, BreakTree, ContinueTree, DeclarationTree, ForTree, FunctionCallTree, IfTree, ReturnTree, WhileTree {
}
