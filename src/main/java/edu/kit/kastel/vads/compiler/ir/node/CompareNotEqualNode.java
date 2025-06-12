package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareNotEqualNode extends BinaryOperationNode {
    public CompareNotEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}