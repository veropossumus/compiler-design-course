package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionCallTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ParameterTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class LoopAnalysis implements NoOpVisitor<LoopAnalysis.Loopy> {

    public static class Loopy {
        boolean error = false;
        Span position;
    }

    @Override
    public Unit visit(ForTree forTree, Loopy data) {
        if(forTree.body() instanceof DeclarationTree) {
            data.error = true;
            data.position = forTree.span();
        }
        return NoOpVisitor.super.visit(forTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, Loopy data) {
        if(data.error) {
            throw new SemanticException("Function " + functionTree.name() + " is doing something wrong");
        }
        return NoOpVisitor.super.visit(functionTree, data);
    }

    @Override
    public Unit visit(FunctionCallTree functionCallTree, Loopy data) {
        // Function calls don't affect loop analysis
        return NoOpVisitor.super.visit(functionCallTree, data);
    }

    @Override
    public Unit visit(ParameterTree parameterTree, Loopy data) {
        // Parameters don't affect loop analysis
        return Unit.INSTANCE;
    }

}