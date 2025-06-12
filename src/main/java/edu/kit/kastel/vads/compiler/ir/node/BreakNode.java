package edu.kit.kastel.vads.compiler.ir.node;

public final class BreakNode extends Node {
    private final String endLabel;

    public BreakNode(Block block, String endLabel) {
        super(block);
        this.endLabel = endLabel;
    }

    public String endLabel() {
        return endLabel;
    }
}
