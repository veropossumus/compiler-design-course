package edu.kit.kastel.vads.compiler.backend.rasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    private static final String INDENT = "    ";

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            RasmRegisterAllocator allocator = new RasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            String functionName = graph.name().equals("main") ? "_main" : graph.name();
            builder.append(".globl ").append(functionName).append("\n");
            builder.append(functionName).append(":\n");

            generateForGraph(graph, builder, registers);
        }
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case ConstIntNode c -> {
                Register r = registers.get(c);
                builder.append(INDENT).append("mov $")
                        .append(c.value()).append(", ").append(r).append("\n");
            }

            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> binary(builder, registers, mul, "imul");

            case DivNode div -> {
                Register lhs = registers.get(predecessorSkipProj(div, BinaryOperationNode.LEFT));
                Register rhs = registers.get(predecessorSkipProj(div, BinaryOperationNode.RIGHT));
                Register result = registers.get(div);

                if (!lhs.toString().equals("%rax")) {
                    builder.append(INDENT).append("mov ").append(lhs).append(", %rax\n");
                }

                builder.append(INDENT).append("cqto\n");
                builder.append(INDENT).append("idiv ").append(rhs).append("\n");

                if (!result.toString().equals("%rax")) {
                    builder.append(INDENT).append("mov %rax, ").append(result).append("\n");
                }
            }

            case ModNode mod -> {
                Register lhs = registers.get(predecessorSkipProj(mod, BinaryOperationNode.LEFT));
                Register rhs = registers.get(predecessorSkipProj(mod, BinaryOperationNode.RIGHT));
                Register result = registers.get(mod);

                if (!lhs.toString().equals("%rax")) {
                    builder.append(INDENT).append("mov ").append(lhs).append(", %rax\n");
                }

                builder.append(INDENT).append("cqto\n");
                builder.append(INDENT).append("idiv ").append(rhs).append("\n");

                if (!result.toString().equals("%rdx")) {
                    builder.append(INDENT).append("mov %rdx, ").append(result).append("\n");
                }
            }

            case ReturnNode r -> {
                Register val = registers.get(predecessorSkipProj(r, ReturnNode.RESULT));
                if (!val.toString().equals("%rax")) {
                    builder.append(INDENT).append("mov ").append(val).append(", %rax\n");
                }
                builder.append(INDENT).append("ret\n");
            }

            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }

            default -> throw new UnsupportedOperationException("Error something went wrong");
        }
        builder.append("\n");
    }

    private static void binary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        Register left = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register right = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        Register dest = registers.get(node);

        if (!dest.equals(left)) {
            builder.append(INDENT).append("mov ").append(left).append(", ").append(dest).append("\n");
        }

        builder.append(INDENT).append(opcode).append(" ").append(right).append(", ").append(dest).append("\n");
    }
}
