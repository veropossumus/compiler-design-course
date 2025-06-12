package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareGreaterEqualNode extends BinaryOperationNode {
    public CompareGreaterEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}