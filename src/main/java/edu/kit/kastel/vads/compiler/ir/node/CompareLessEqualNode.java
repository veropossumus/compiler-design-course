package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareLessEqualNode extends BinaryOperationNode {
    public CompareLessEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}