package edu.kit.kastel.vads.compiler.ir.node;

import java.util.List;
import java.util.ArrayList;

public final class FunctionCallNode extends Node {
    private final String functionName;
    private final List<Node> arguments;

    public FunctionCallNode(Block block, Node sideEffectInput, String functionName, List<Node> arguments) {
        super(block, createInputs(sideEffectInput, arguments));
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
    }

    private static Node[] createInputs(Node sideEffectInput, List<Node> arguments) {
        Node[] inputs = new Node[arguments.size() + 1];
        inputs[0] = sideEffectInput;

        for (int i = 0; i < arguments.size(); i++) {
            inputs[i + 1] = arguments.get(i);
        }

        return inputs;
    }

    public String functionName() {
        return functionName;
    }

    public List<Node> getArguments() {
        return new ArrayList<>(arguments);
    }

    public Node getSideEffectInput() {
        return getInput(0);
    }

    private Node getInput(int i) {
        return this.arguments.get(i);
    }

    public Node getArgument(int index) {
        if (index < 0 || index >= arguments.size()) {
            throw new IndexOutOfBoundsException("Argument index " + index + " out of bounds");
        }
        return getInput(index + 1);
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public boolean isBuiltinFunction() {
        return "print".equals(functionName) || "read".equals(functionName) || "flush".equals(functionName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Call[").append(functionName).append("](");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}