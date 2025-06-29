package edu.kit.kastel.vads.compiler.ir.node;

public final class ParameterNode extends Node {
    private final String parameterName;
    private final int parameterIndex;
    
    public ParameterNode(Block block, String parameterName, int parameterIndex) {
        super(block);
        this.parameterName = parameterName;
        this.parameterIndex = parameterIndex;
    }
    
    public String parameterName() {
        return parameterName;
    }
    
    public int parameterIndex() {
        return parameterIndex;
    }
}
