package edu.kit.kastel.vads.compiler.ir.node;

public final class ContinueNode extends Node {
    private final String loopHeadLabel;
    public ContinueNode(Block block, String loopHeadLabel) {
        super(block);
        this.loopHeadLabel = loopHeadLabel;
    }
    public String loopHeadLabel() { return loopHeadLabel; }
}
