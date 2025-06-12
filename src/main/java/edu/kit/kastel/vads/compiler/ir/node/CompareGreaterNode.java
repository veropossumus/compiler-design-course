package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareGreaterNode extends BinaryOperationNode {
    public CompareGreaterNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}