package edu.kit.kastel.vads.compiler.ir.node;

import java.util.List;

public final class CallNode extends Node {
    public static final int SIDE_EFFECT = 0;
    
    private final String functionName;
    
    public CallNode(Block block, Node sideEffect, String functionName, List<Node> arguments) {
        super(block, buildPredecessors(sideEffect, arguments));
        this.functionName = functionName;
    }
    
    private static Node[] buildPredecessors(Node sideEffect, List<Node> arguments) {
        Node[] predecessors = new Node[1 + arguments.size()];
        predecessors[0] = sideEffect;
        for (int i = 0; i < arguments.size(); i++) {
            predecessors[i + 1] = arguments.get(i);
        }
        return predecessors;
    }
    
    public String functionName() {
        return functionName;
    }
    
    public Node sideEffect() {
        return predecessors().get(SIDE_EFFECT);
    }
    
    public List<Node> arguments() {
        return List.copyOf(predecessors().subList(1, predecessors().size()));
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}
