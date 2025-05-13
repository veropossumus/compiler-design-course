package edu.kit.kastel.vads.compiler.backend.rasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RasmRegisterAllocator implements RegisterAllocator {

    private final Map<Node, Register> registers = new HashMap<>();
    private int nextReg = 0;

    private static final String[] PHYSICAL_REGISTERS = {
            "%rax", "%rbx", "%rcx", "%rdx", "%rsi", "%rdi"
    };

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited);
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node pred : node.predecessors()) {
            if (visited.add(pred)) {
                scan(pred, visited);
            }
        }

        if (needsRegister(node)) {
            if (!registers.containsKey(node)) {
                if (nextReg < PHYSICAL_REGISTERS.length) {
                    registers.put(node, new PhysicalRegister(PHYSICAL_REGISTERS[nextReg++]));
                } else {
                    throw new RuntimeException("Out of physical registers");
                }
            }
        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode
                || node instanceof StartNode
                || node instanceof Block
                || node instanceof ReturnNode);
    }
}



