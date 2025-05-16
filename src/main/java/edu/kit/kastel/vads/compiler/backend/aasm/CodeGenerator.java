package edu.kit.kastel.vads.compiler.backend.aasm;

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

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();

        builder.append(".global _main\n")
              .append(".text\n")
              .append("main:\n")
              .append("call _main\n")
              .append("movq %rax, %rdi\n")
              .append("movq $0x3C, %rax\n")
              .append("syscall\n")
              .append("_main:\n");

        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            builder.append("function ")
                .append(graph.name())
                .append(" {\n");
            generateForGraph(graph, builder, registers);
            builder.append("}");
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
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> binary(builder, registers, mul, "mul");
            case DivNode div -> {
                Register right = registers.get(predecessorSkipProj(div, BinaryOperationNode.RIGHT));
                builder.append("cmpl $0, ").append(right).append("\n")
                      .append("e _divzero\n");
                builder.append("movl ").append(registers.get(predecessorSkipProj(div, BinaryOperationNode.LEFT)))
                      .append(", %eax\n")
                      .append("cltd\n")
                      .append("idivl ").append(right).append("\n")
                      .append("movl %eax, ").append(registers.get(div)).append("\n");
            }
            case ModNode mod -> {
                Register right = registers.get(predecessorSkipProj(mod, BinaryOperationNode.RIGHT));
                builder.append("cmpl $0, ").append(right).append("\n")
                      .append("je _divzero\n");
                builder.append("movl ").append(registers.get(predecessorSkipProj(mod, BinaryOperationNode.LEFT)))
                      .append(", %eax\n")
                      .append("cltd\n")
                      .append("idivl ").append(right).append("\n")
                      .append("movl %edx, ").append(registers.get(mod)).append("\n");
            }
            // case DivNode div -> binary(builder, registers, div, "div");
            // case ModNode mod -> binary(builder, registers, mod, "mod");

            case ReturnNode r -> builder.repeat(" ", 2).append("ret ")
                .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)));
            case ConstIntNode c -> builder.repeat(" ", 2)
                .append(registers.get(c))
                .append(" = const ")
                .append(c.value());
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private static void binary(
        StringBuilder builder,
        Map<Node, Register> registers,
        BinaryOperationNode node,
        String opcode
    ) {
        builder.repeat(" ", 2).append(registers.get(node))
            .append(" = ")
            .append(opcode)
            .append(" ")
            .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
            .append(" ")
            .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
    }
}
