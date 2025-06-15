package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.parser.Scope;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());

        int MAX_BLOCKS = this.program.scopes().size();
        @SuppressWarnings("unchecked") //TODO
        Namespace<TypeAnalysis.TYPES>[] namespaces = (Namespace<TypeAnalysis.TYPES>[]) new Namespace[MAX_BLOCKS];
        for (int i = 0; i < MAX_BLOCKS; i++) {
            namespaces[i] = new Namespace<>();
        }
        Types types = new Types(namespaces);
        this.program.accept(new RecursivePostorderVisitor<>(new TypeAnalysis()), types);
        this.program.accept(new RecursivePostorderVisitor<>(new LoopAnalysis()), new LoopAnalysis.Loopy());

    }

}
