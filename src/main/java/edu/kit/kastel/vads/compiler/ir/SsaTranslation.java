package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
    private final FunctionTree function;
    private final GraphConstructor constructor;
    private final Deque<String> loopEndLabels = new ArrayDeque<>();
    private final Deque<String> loopHeadLabels = new ArrayDeque<>();

    public SsaTranslation(FunctionTree function, Optimizer optimizer) {
        this.function = function;
        this.constructor = new GraphConstructor(optimizer, function.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.function.accept(visitor, this);
        return this.constructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.constructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.constructor.readVariable(variable, block);
    }

    private Block currentBlock() {
        return this.constructor.currentBlock();
    }

    Deque<String> loopEndLabels() {
        return loopEndLabels;
    }

    Deque<String> loopHeadLabels() {
        return loopHeadLabels;
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.getDebugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.constructor::newSub;
                case ASSIGN_PLUS -> data.constructor::newAdd;
                case ASSIGN_MUL -> data.constructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case ASSIGN_BITWISE_AND -> data.constructor::newBitwiseAnd;
                case ASSIGN_BITWISE_XOR -> data.constructor::newBitwiseXor;
                case ASSIGN_BITWISE_OR -> data.constructor::newBitwiseOr;
                case ASSIGN_SHIFT_LEFT -> data.constructor::newShiftLeft;
                case ASSIGN_SHIFT_RIGHT -> data.constructor::newShiftRight; 
                case ASSIGN -> null;
                default ->
                    throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
            pushSpan(binaryOperationTree);
            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MINUS -> data.constructor.newSub(lhs, rhs);
                case PLUS -> data.constructor.newAdd(lhs, rhs);
                case MUL -> data.constructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case COMPARE_GREATER -> data.constructor.newCompareGreater(lhs, rhs);
                case COMPARE_GREATER_EQUAL -> data.constructor.newCompareGreaterEqual(lhs, rhs);
                case COMPARE_LESS -> data.constructor.newCompareLess(lhs, rhs);
                case COMPARE_LESS_EQUAL -> data.constructor.newCompareLessEqual(lhs, rhs);
                case COMPARE_EQUAL -> data.constructor.newCompareEqual(lhs, rhs);
                case COMPARE_NOT_EQUAL -> data.constructor.newCompareNotEqual(lhs, rhs);
                case BITWISE_AND -> data.constructor.newBitwiseAnd(lhs, rhs);
                case BITWISE_XOR -> data.constructor.newBitwiseXor(lhs, rhs);
                case BITWISE_OR -> data.constructor.newBitwiseOr(lhs, rhs);
                case SHIFT_LEFT -> data.constructor.newShiftLeft(lhs, rhs);
                case SHIFT_RIGHT -> data.constructor.newShiftRight(lhs, rhs);
                case AND -> data.constructor.newLogicalAnd(lhs, rhs);
                case OR -> data.constructor.newLogicalOr(lhs, rhs);
                default ->
                    throw new IllegalArgumentException(
                            "not a binary expression operator " + binaryOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);

            Node blockNode = new Block(data.constructor.graph());

            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                if (statement instanceof BreakTree || statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return Optional.of(blockNode);
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            if (declarationTree.initializer() != null) {
                Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);
            Node start = data.constructor.newStart();
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(LiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newSub(data.constructor.newConstInt(0), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.constructor.newReturn(node);
            data.constructor.graph().endBlock().addPredecessor(ret);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
            pushSpan(whileTree);
            Node condition = whileTree.condition().accept(this, data).orElseThrow();
            String endLabel = "while_end_" + whileTree.hashCode();
            data.loopEndLabels().push(endLabel);
            data.loopHeadLabels().push("while_head_" + whileTree.hashCode());
            whileTree.body().accept(this, data);
            data.loopEndLabels().pop();
            data.loopHeadLabels().pop();
            Node loop = data.constructor.newLoop(condition, data.currentBlock());
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(loop));

            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            pushSpan(breakTree);
            String endLabel = data.loopEndLabels().peek();
            if (endLabel == null) {
                throw new IllegalStateException("break statement not inside a loop");
            }
            Node breakNode = data.constructor.newBreak(endLabel);
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(breakNode));
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            pushSpan(continueTree);
            String loopHeadLabel = data.loopHeadLabels().peek();
            if (loopHeadLabel == null) {
                throw new IllegalStateException("continue statement not inside a loop");
            }
            Node continueNode = data.constructor.newContinue(loopHeadLabel);
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(continueNode));
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
            pushSpan(forTree);

            forTree.init().accept(this, data);

            String endLabel = "for_end_" + forTree.hashCode();
            String headLabel = "for_head_" + forTree.hashCode();
            data.loopEndLabels().push(endLabel);
            data.loopHeadLabels().push(headLabel);

            Node condition = null;
            condition = forTree.condition().accept(this, data).orElseThrow();
            forTree.body().accept(this, data);
            forTree.update().accept(this, data);

            data.loopEndLabels().pop();
            data.loopHeadLabels().pop();

            Node loop = data.constructor.newLoop(condition, new Block(data.constructor.graph()));
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(loop));

            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);
            Node condition = ifTree.condition().accept(this, data).orElseThrow();
            Node thenBlock = ifTree.thenBranch().accept(this, data).orElseThrow();

            Node elseBlock;
            if (ifTree.elseBranch() != null) {
                elseBlock = ifTree.elseBranch().accept(this, data).orElseThrow();
            } else {
                elseBlock = data.constructor.currentBlock(); // Empty block
            }

            Node ifNode = data.constructor.newIf(condition, thenBlock, elseBlock);
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(ifNode));
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TernaryOperationTree ternaryOperationTree, SsaTranslation data) {
            pushSpan(ternaryOperationTree);
            Node condition = ternaryOperationTree.condition().accept(this, data).orElseThrow();
            Node trueExpr = ternaryOperationTree.trueExpression().accept(this, data).orElseThrow();
            Node falseExpr = ternaryOperationTree.falseExpression().accept(this, data).orElseThrow();
            Node ternaryNode = data.constructor.newIf(condition, trueExpr, falseExpr);
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(ternaryNode));
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.constructor.newSideEffectProj(divMod);
            data.constructor.writeCurrentSideEffect(projSideEffect);
            return data.constructor.newResultProj(divMod);
        }
    }

}
