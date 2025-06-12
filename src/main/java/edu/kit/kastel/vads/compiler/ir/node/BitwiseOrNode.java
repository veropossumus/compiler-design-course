package edu.kit.kastel.vads.compiler.ir.node;

public final class BitwiseOrNode extends BinaryOperationNode {
    public BitwiseOrNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}