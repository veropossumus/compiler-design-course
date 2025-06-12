package edu.kit.kastel.vads.compiler.ir.node;

public final class IfNode extends Node {
    public static final int CONDITION = 0;
    public static final int THEN_BRANCH = 1;
    public static final int ELSE_BRANCH = 2;

    public IfNode(Block block, Node condition, Node thenBranch, Node elseBranch) {
        super(block, condition, thenBranch, elseBranch);
    }

    public Node condition() {
        return predecessor(CONDITION);
    }

    public Node thenBranch() {
        return predecessor(THEN_BRANCH);
    }

    public Node elseBranch() {
        return predecessor(ELSE_BRANCH);
    }
}