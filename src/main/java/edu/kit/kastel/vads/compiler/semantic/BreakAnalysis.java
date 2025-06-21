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
        whileTree.condition().accept(this, data);
        whileTree.body().accept(this, data);
        data.loopDepth--;
        return null;
    }

    @Override
    public Unit visit(ForTree forTree, LoopContext data) {
        data.loopDepth++;
        if (forTree.init() != null)
            forTree.init().accept(this, data);
        if (forTree.condition() != null)
            forTree.condition().accept(this, data);
        if (forTree.update() != null)
            forTree.update().accept(this, data);
        forTree.body().accept(this, data);
        data.loopDepth--;
        return null;
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