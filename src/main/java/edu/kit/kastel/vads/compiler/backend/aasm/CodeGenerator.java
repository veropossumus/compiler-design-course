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

    private static class PhysicalRegisterMapper {
        private static final String[] PHYSICAL_REGISTERS = { "%eax", "%ebx", "%ecx", "%edx", "%esi", "%edi" };

        public static String map(Register reg) {
            if (reg instanceof VirtualRegister vr) {
                return PHYSICAL_REGISTERS[vr.id() % PHYSICAL_REGISTERS.length];
            }
            return reg.toString();
        }
    }

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();

        builder.append(".global main\n")
               .append(".global _main\n")
               .append(".text\n")
               .append("main:\n")
               .append("    call _main\n")
               .append("    movq %rax, %rdi\n")
               .append("    movq $0x3C, %rax\n")
               .append("    syscall\n")
               .append("_main:\n")
               .append("    pushq %rbp\n")
               .append("    movq %rsp, %rbp\n")
               .append("    subq $16, %rsp\n");

        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            generateForGraph(graph, builder, registers);
        }

        builder.append("    movq %rbp, %rsp\n")
               .append("    popq %rbp\n")
               .append("    ret\n");

        builder.append("_divzero:\n")
               .append("    movq $1, %rdi\n")
               .append("    movq $60, %rax\n")
               .append("    syscall\n");
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
            case AddNode add -> binary(builder, registers, add, "addl");
            case SubNode sub -> binary(builder, registers, sub, "subl");
            case MulNode mul -> binary(builder, registers, mul, "imull");

            case DivNode div -> binaryDivMod(builder, registers, div);
            case ModNode mod -> binaryDivMod(builder, registers, mod);

            case ReturnNode ret -> generateReturn(builder, registers, ret);
            case ConstIntNode c -> generateConstant(builder, registers, c);

            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _,ProjNode _,StartNode _ -> {
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
            String opcode) {

        String left = PhysicalRegisterMapper.map(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String right = PhysicalRegisterMapper.map(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        builder.append("    movl ")
                .append(left)
                .append(", ")
                .append(result)
                .append("\n")
                .append("    ")
                .append(opcode)
                .append(" ")
                .append(right)
                .append(", ")
                .append(result)
                .append("\n");
    }

    private void binaryDivMod(StringBuilder builder, Map<Node, Register> registers,
            BinaryOperationNode node) {
        String left = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String right = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        builder.append("    cmpl $0, ")
                .append(right)
                .append("\n")
                .append("    je _divzero\n");

        if (!left.equals("%eax")) {
            builder.append("    movl ")
                    .append(left)
                    .append(", %eax\n");
        }

        builder.append("    cltd\n")
                .append("    idivl ")
                .append(right)
                .append("\n");

        if (node instanceof ModNode) {
            builder.append("    movl %edx, ")
                    .append(result)
                    .append("\n");
        } else {
            builder.append("    movl %eax, ")
                    .append(result)
                    .append("\n");
        }
    }

    private void generateReturn(StringBuilder builder, Map<Node, Register> registers, ReturnNode node) {
        String resultRegister = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, ReturnNode.RESULT)));

        if (!resultRegister.equals("%eax")) {
            builder.append("    movl ")
                    .append(resultRegister)
                    .append(", %eax\n");
        }
    }

    private void generateConstant(StringBuilder builder, Map<Node, Register> registers, ConstIntNode node) {
        String dest = PhysicalRegisterMapper.map(registers.get(node));
        builder.append("    movl $")
                .append(node.value())
                .append(", ")
                .append(dest)
                .append("\n");
    }

}
