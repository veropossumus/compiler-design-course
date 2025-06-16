package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class BreakAnalysis implements NoOpVisitor<BreakAnalysis.LoopContext> {

    static class LoopContext {
        int loopDepth = 0;
    }

    @Override
    public Unit visit(WhileTree whileTree, LoopContext data) {
       data.loopDepth++;
        return NoOpVisitor.super.visit(whileTree, data);
    }

    @Override
    public Unit visit(ForTree forTree, LoopContext data) {
        data.loopDepth++;

        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, LoopContext data) {
        if (continueTree.loopId() == -1) {
            throw new SemanticException("continue not inside a loop");
        }
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(BreakTree breakTree, LoopContext data) {
        if (breakTree.loopId() == -1) {
            throw new SemanticException("break not inside a loop");
        }
        return NoOpVisitor.super.visit(breakTree, data);
    }

}