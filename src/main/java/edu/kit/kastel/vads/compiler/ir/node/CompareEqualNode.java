package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareEqualNode extends BinaryOperationNode {
    public CompareEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}