package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.util.FunctionSignature;

import java.util.HashMap;
import java.util.Map;

public class ProgramIr {
    private final Map<String, IrGraph> functions = new HashMap<>();
    private final Map<String, edu.kit.kastel.vads.compiler.ir.util.FunctionSignature> functionSignatures = new HashMap<>();

    public void addFunction(String name, IrGraph graph, edu.kit.kastel.vads.compiler.ir.util.FunctionSignature signature) {
        this.functions.put(name, graph);
        this.functionSignatures.put(name, signature);
    }

    public IrGraph getFunction(String name) {
        return this.functions.get(name);
    }

    public edu.kit.kastel.vads.compiler.ir.util.FunctionSignature functionSignature(String name) {
        return this.functionSignatures.get(name);
    }

    public Map<String, IrGraph> getAllFunctions() {
        return this.functions;
    }

    public Map<String, FunctionSignature> getAllSignatures() {
        return this.functionSignatures;
    }
}