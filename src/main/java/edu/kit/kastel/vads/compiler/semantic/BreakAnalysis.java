package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class BreakAnalysis implements NoOpVisitor<BreakAnalysis.LoopContext> {

    static class LoopContext {
        boolean inLoop = false;
    }

    @Override
    public Unit visit(BreakTree breakTree, LoopContext data) {
        if (!data.inLoop) {
            throw new SemanticException("break not inside a loop");
        }
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, LoopContext data) {
        if (!data.inLoop) {
            throw new SemanticException("continue not inside a loop");
        }
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(WhileTree whileTree, LoopContext data) {
        boolean originalInLoop = data.inLoop;
        data.inLoop = true;

        Unit result = NoOpVisitor.super.visit(whileTree, data);

        data.inLoop = originalInLoop;
        return result;
    }

    @Override
    public Unit visit(ForTree forTree, LoopContext data) {
        boolean originalInLoop = data.inLoop;
        data.inLoop = true;

        Unit result = NoOpVisitor.super.visit(forTree, data);

        data.inLoop = originalInLoop;
        return result;
    }
}