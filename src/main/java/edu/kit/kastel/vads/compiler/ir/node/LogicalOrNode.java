package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalOrNode extends BinaryOperationNode {
    public LogicalOrNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}