package edu.kit.kastel.vads.compiler.ir.node;

public final class CompareLessNode extends BinaryOperationNode {
    public CompareLessNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}