package edu.kit.kastel.vads.compiler.ir.node;

public final class ParamNode extends Node {
    private final int parameterIndex;

    public ParamNode(Block block, Node startNode, int parameterIndex) {
        super(block, startNode);
        this.parameterIndex = parameterIndex;
    }

    public int parameterIndex() {return parameterIndex;}

    @Override
    public String info() {
        return "Param[" + parameterIndex + "]";
    }
}