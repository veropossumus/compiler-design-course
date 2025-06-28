package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    private static class PhysicalRegisterMapper {
        private static final String[] PHYSICAL_REGISTERS = { "%ebx", "%ecx", "%esi", "%edi" };

        public static String map(Register reg) {
            if (reg instanceof VirtualRegister vr) {
                return PHYSICAL_REGISTERS[vr.id() % PHYSICAL_REGISTERS.length];
            }
            return reg.toString();
        }
    }

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();

        builder.append(".section .text\n")
                .append(".global main\n")
                .append(".global _main\n")
                .append("main:\n")
                .append("    call _main\n")
                .append("    movq %rax, %rdi\n")
                .append("    movq $0x3C, %rax\n")
                .append("    syscall\n")
                .append("_main:\n")
                .append("    pushq %rbp\n")
                .append("    movq %rsp, %rbp\n")
                .append("    pushq %rbx\n")
                .append("    subq $32, %rsp\n");

        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            generateForGraph(graph, builder, registers);
        }

        builder.append("    addq $32, %rsp\n")
                .append("    popq %rbx\n")
                .append("    movq %rbp, %rsp\n")
                .append("    popq %rbp\n")
                .append("    ret\n");
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (!visited.contains(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        if (!visited.add(node)) {
            return;
        }

        switch (node) {
            case AddNode add -> binary(builder, registers, add, "addl");
            case SubNode sub -> binary(builder, registers, sub, "subl");
            case MulNode mul -> binary(builder, registers, mul, "imull");
            case DivNode div -> binaryDivMod(builder, registers, div);
            case ModNode mod -> binaryDivMod(builder, registers, mod);
            case ReturnNode ret -> generateReturn(builder, registers, ret);
            case ConstIntNode c -> generateConstant(builder, registers, c);
            case ConstBoolNode c -> generateBoolConstant(builder, registers, c);
            case WhileNode whileNode -> generateWhile(builder, registers, whileNode);
            case BreakNode breakNode -> generateBreak(builder, breakNode);
            case IfNode ifNode -> generateIf(builder, registers, ifNode);
            case ContinueNode continueNode -> generateContinue(builder, continueNode);
            case LogicalNotNode logicalNotNode -> generateLogicalNot(builder, registers, logicalNotNode);

            case BitwiseAndNode bitwiseAnd -> binary(builder, registers, bitwiseAnd, "andl");
            case BitwiseOrNode bitwiseOr -> binary(builder, registers, bitwiseOr, "orl");
            case BitwiseXorNode bitwiseXor -> binary(builder, registers, bitwiseXor, "xorl");
            case ShiftLeftNode shiftLeft -> binary(builder, registers, shiftLeft, "shll");
            case ShiftRightNode shiftRight -> binary(builder, registers, shiftRight, "shrl");

            case CompareGreaterNode cmp -> generateCompare(builder, registers, cmp, "setg");
            case CompareGreaterEqualNode cmp -> generateCompare(builder, registers, cmp, "setge");
            case CompareLessNode cmp -> generateCompare(builder, registers, cmp, "setl");
            case CompareLessEqualNode cmp -> generateCompare(builder, registers, cmp, "setle");
            case CompareEqualNode cmp -> generateCompare(builder, registers, cmp, "sete");
            case CompareNotEqualNode cmp -> generateCompare(builder, registers, cmp, "setne");

            case LogicalAndNode logicalAnd -> generateLogicalAnd(builder, registers, logicalAnd);
            case LogicalOrNode logicalOr -> generateLogicalOr(builder, registers, logicalOr);
            case FunctionCallNode functionCall -> {return;} //TODO
            case ParamNode par -> {return; }

            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _,ProjNode _,StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private void generateCompare(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String setInstr) {

        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);
        String left = PhysicalRegisterMapper.map(registers.get(leftNode));
        String right = PhysicalRegisterMapper.map(registers.get(rightNode));
        String result = PhysicalRegisterMapper.map(registers.get(node));


        builder.append("    cmpl ")
               .append(right)
               .append(", ")
               .append(left)
               .append("\n");
        builder.append("    ")
               .append(setInstr)
               .append(" %al\n");
        builder.append("    movzbl %al, ")
               .append(result)
               .append("\n");
    }

    private static void binary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode) {

        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);
        String left = PhysicalRegisterMapper.map(registers.get(leftNode));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        if (leftNode instanceof ConstIntNode lc && rightNode instanceof ConstIntNode rc) {
            int folded = switch (opcode) {
                case "addl" -> lc.value() + rc.value();
                case "subl" -> lc.value() - rc.value();
                case "imull" -> lc.value() * rc.value();
                case "andl" -> lc.value() & rc.value();
                case "orl"  -> lc.value() | rc.value();
                case "xorl" -> lc.value() ^ rc.value();
                case "shll" -> lc.value() << rc.value();
                case "shrl" -> lc.value() >>> rc.value();
                default -> throw new IllegalStateException("unknown opcode in binary: " + opcode + " for node " + node.getClass().getSimpleName());
            };
            builder.append("    movl $").append(folded).append(", ").append(result).append("\n");
            return;
        }

        if (opcode.equals("subl")) {
            if (leftNode instanceof ConstIntNode leftConst && leftConst.value() == 0) {
                if (rightNode instanceof ConstIntNode constNode) {
                    builder.append("    movl $")
                            .append(-constNode.value())
                            .append(", ")
                            .append(result)
                            .append("\n");
                } else {
                    String right = PhysicalRegisterMapper.map(registers.get(rightNode));
                    if (!result.equals(right)) {
                        builder.append("    movl ")
                                .append(right)
                                .append(", ")
                                .append(result)
                                .append("\n");
                    }
                    builder.append("    negl ")
                            .append(result)
                            .append("\n");
                }
                return;
            }
        }

        if (!result.equals(left)) {
            builder.append("    movl ")
                    .append(left)
                    .append(", ")
                    .append(result)
                    .append("\n");
        }

        String right = PhysicalRegisterMapper.map(registers.get(rightNode));

        switch (opcode) {
            case "addl":
            case "andl":
            case "orl":
            case "xorl":
                if (rightNode instanceof ConstIntNode constNode) {
                    builder.append("    ")
                            .append(opcode)
                            .append(" $")
                            .append(constNode.value())
                            .append(", ")
                            .append(result)
                            .append("\n");
                } else {
                    builder.append("    ")
                            .append(opcode)
                            .append(" ")
                            .append(right)
                            .append(", ")
                            .append(result)
                            .append("\n");
                }
                break;
            case "imull":
                if (rightNode instanceof ConstIntNode constNode) {
                    builder.append("    imull $")
                            .append(constNode.value())
                            .append(", ")
                            .append(result)
                            .append("\n");
                } else {
                    builder.append("    imull ")
                            .append(right)
                            .append(", ")
                            .append(result)
                            .append("\n");
                }
                break;
            case "subl":
                if (rightNode instanceof ConstIntNode constNode) {
                    builder.append("    subl $")
                            .append(constNode.value())
                            .append(", ")
                            .append(result)
                            .append("\n");
                } else {
                    builder.append("    subl ")
                            .append(right)
                            .append(", ")
                            .append(result)
                            .append("\n");
                }
                break;
            case "shll":
            case "shrl":
                if (rightNode instanceof ConstIntNode constNode) {
                    builder.append("    ")
                            .append(opcode)
                            .append(" $")
                            .append(constNode.value())
                            .append(", ")
                            .append(result)
                            .append("\n");
                } else {
                    if (!right.equals("%cl")) {
                        builder.append("    movb ")
                                .append(right)
                                .append(", %cl\n");
                    }
                    builder.append("    ")
                            .append(opcode)
                            .append(" %cl, ")
                            .append(result)
                            .append("\n");
                }
                break;
            default:
                throw new IllegalStateException("unknown opcode in binary: " + opcode + " for node " + node.getClass().getSimpleName());
        }
    }

    private void binaryDivMod(StringBuilder builder, Map<Node, Register> registers,
            BinaryOperationNode node) {
        String left = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String right = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        builder.append("    pushq %rax\n");
        builder.append("    pushq %rdx\n");

        // %eax for division
        builder.append("    movl ")
                .append(left)
                .append(", %eax\n");
        builder.append("    cltd\n");

        builder.append("    idivl ")
                .append(right)
                .append("\n");

        // move back
        if (node instanceof ModNode) {
            builder.append("    movl %edx, ")
                    .append(result)
                    .append("\n");
        } else {
            builder.append("    movl %eax, ")
                    .append(result)
                    .append("\n");
        }

        builder.append("    popq %rdx\n");
        builder.append("    popq %rax\n");
    }

    private void generateReturn(StringBuilder builder, Map<Node, Register> registers, ReturnNode node) {
        String resultRegister = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, ReturnNode.RESULT)));

        builder.append("    movl ")
                .append(resultRegister)
                .append(", %eax\n");
    }

    private void generateConstant(StringBuilder builder, Map<Node, Register> registers, ConstIntNode node) {
        String dest = PhysicalRegisterMapper.map(registers.get(node));

        builder.append("    movl $")
                .append(node.value())
                .append(", ")
                .append(dest)
                .append("\n");
    }

    // private void generateBoolConstant(StringBuilder builder, Map<Node, Register> registers, ConstBoolNode node) {
    //     String dest = PhysicalRegisterMapper.map(registers.get(node));

    //     builder.append("    movl $")
    //             .append(node.value())
    //             .append(", ")
    //             .append(dest)
    //             .append("\n");
    // }

    private void generateBoolConstant(StringBuilder builder, Map<Node, Register> registers, ConstBoolNode node) {
    String dest = PhysicalRegisterMapper.map(registers.get(node));
    int value = node.value() ? 1 : 0;
    builder.append("    movl $")
           .append(value) // tests dont terminate with this for some reason?
           .append(", ")
           .append(dest)
           .append("\n");
}

    private void generateIf(StringBuilder builder, Map<Node, Register> registers, IfNode node) {
        String conditionRegister = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, IfNode.CONDITION)));
        String thenLabel = "if_then_" + node.hashCode();
        String elseLabel = "if_else_" + node.hashCode();
        String endLabel = "if_end_" + node.hashCode();

        builder.append("    cmp $0, ")
                .append(conditionRegister)
                .append("\n")
                .append("    je ")
                .append(elseLabel)
                .append("\n")
                .append(thenLabel)
                .append(":\n");

        Node thenNode = node.thenBranch();
        scan(thenNode, new HashSet<>(), builder, registers);

        if (!endsWithBreak(thenNode)) {
            builder.append("    jmp ")
                    .append(endLabel)
                    .append("\n");
        }

        builder.append(elseLabel).append(":\n");

        Node elseNode = node.elseBranch();
        scan(elseNode, new HashSet<>(), builder, registers);

        builder.append(endLabel).append(":\n");
    }

    private void generateWhile(StringBuilder builder, Map<Node, Register> registers, WhileNode node) {
        String conditionRegister = PhysicalRegisterMapper.map(
                registers.get(predecessorSkipProj(node, WhileNode.CONDITION)));
        String bodyStartLabel = "while_body_" + node.hashCode();
        String endLabel = "while_end_" + node.hashCode();

        builder.append("    cmp $0, ")
                .append(conditionRegister)
                .append("\n")
                .append("    je ")
                .append(endLabel)
                .append("\n")
                .append(bodyStartLabel)
                .append(":\n");

        Node bodyNode = node.body();
        scan(bodyNode, new HashSet<>(), builder, registers);

        if (!endsWithBreak(bodyNode)) {
            builder.append("    jmp ")
                    .append(bodyStartLabel)
                    .append("\n");
        }
        builder.append(endLabel).append(":\n");
    }

    private void generateBreak(StringBuilder builder, BreakNode breakNode) {
        builder.append("    jmp ")
                .append(breakNode.endLabel())
                .append("\n");
    }

    private void generateContinue(StringBuilder builder, ContinueNode continueNode) {
        builder.append("    jmp ")
               .append(continueNode.loopHeadLabel())
               .append("\n");
    }

    private void generateLogicalAnd(
            StringBuilder builder,
            Map<Node, Register> registers,
            LogicalAndNode node) {

        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);
        String left = PhysicalRegisterMapper.map(registers.get(leftNode));
        String right = PhysicalRegisterMapper.map(registers.get(rightNode));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        // left != 0
        builder.append("    cmpl $0, ").append(left).append("\n");
        builder.append("    setne %al\n");
        builder.append("    movzbl %al, %ecx\n");

        // right != 0
        builder.append("    cmpl $0, ").append(right).append("\n");
        builder.append("    setne %al\n");
        builder.append("    movzbl %al, %edx\n");

        builder.append("    andl %edx, %ecx\n");
        builder.append("    movl %ecx, ").append(result).append("\n");
    }

    private void generateLogicalOr(
            StringBuilder builder,
            Map<Node, Register> registers,
            LogicalOrNode node) {

        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);
        String left = PhysicalRegisterMapper.map(registers.get(leftNode));
        String right = PhysicalRegisterMapper.map(registers.get(rightNode));
        String result = PhysicalRegisterMapper.map(registers.get(node));

        builder.append("    cmpl $0, ").append(left).append("\n");
        builder.append("    setne %al\n");
        builder.append("    movzbl %al, %ecx\n");

        builder.append("    cmpl $0, ").append(right).append("\n");
        builder.append("    setne %al\n");
        builder.append("    movzbl %al, %edx\n");

        builder.append("    orl %edx, %ecx\n");
        builder.append("    movl %ecx, ").append(result).append("\n");
    }

    private boolean endsWithBreak(Node node) {
        List<? extends Node> preds = node.predecessors();
        for (int i = preds.size() - 1; i >= 0; --i) {
            Node pred = preds.get(i);
            if (pred instanceof BreakNode)
                return true;
            if (pred instanceof ReturnNode)
                return false;
        }
        return false;
    }

    private void generateLogicalNot(
            StringBuilder builder,
            Map<Node, Register> registers,
            LogicalNotNode node) {

        String dest = PhysicalRegisterMapper.map(registers.get(node));
        String src = PhysicalRegisterMapper.map(registers.get(node.operand()));

        builder.append("    cmpl $0, ").append(src).append("\n");
        builder.append("    sete %al\n");
        builder.append("    movzbl %al, ").append(dest).append("\n");
    }
}
