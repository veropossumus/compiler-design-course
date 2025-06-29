package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

/// Checks that functions return.
/// Currently only works for straight-line code.
class ReturnAnalysis implements NoOpVisitor<ReturnAnalysis.ReturnState> {

    static class ReturnState {
        boolean returns = false;
    }

    @Override
    public Unit visit(ReturnTree returnTree, ReturnState data) {
        data.returns = true;
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, ReturnState data) {
        if (!hasReturn(functionTree.body())) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        return NoOpVisitor.super.visit(functionTree, data);
    }

    private boolean hasReturn(StatementTree statement) {
        ReturnState state = new ReturnState();
        statement.accept(this, state);
        return state.returns;
    }


    @Override
    public Unit visit(IfTree ifTree, ReturnState data) {
        boolean originalReturns = data.returns;

        data.returns = false;
        ifTree.thenBranch().accept(this, data);
        boolean thenReturns = data.returns;

        boolean elseReturns = false;
        if (ifTree.elseBranch() != null) {
            data.returns = false;
            ifTree.elseBranch().accept(this, data);
            elseReturns = data.returns;

            data.returns = originalReturns || (thenReturns && elseReturns);
        } else {
            data.returns = originalReturns;
        }
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, ReturnState data) {
        for (StatementTree statement : blockTree.statements()) {
            statement.accept(this, data);

            if (data.returns) {
                break;
            }
        }

        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(FunctionCallTree functionCallTree, ReturnState data) {
        // Function calls don't affect return analysis
        return NoOpVisitor.super.visit(functionCallTree, data);
    }

    @Override
    public Unit visit(ParameterTree parameterTree, ReturnState data) {
        // Parameters don't affect return analysis
        return Unit.INSTANCE;
    }
}
