package edu.kit.kastel.vads.compiler.ir.node;

public final class WhileNode extends Node {
    public static final int CONDITION = 0;
    public static final int BODY = 1;

    public WhileNode(Block block, Node condition, Node body) {
        super(block, condition, body);
    }

    public Node condition() {
        return predecessor(CONDITION);
    }

    public Node body() {
        return predecessor(BODY);
    }
}
