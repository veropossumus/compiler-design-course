package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalNotNode extends Node {
    private final Node operand;

    public LogicalNotNode(Block block, Node operand) {
        super(block);
        this.operand = operand;
    }

    public Node operand() {
        return operand;
    }
}
