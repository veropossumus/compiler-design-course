package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;

import java.util.List;

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
        @SuppressWarnings("unchecked") // TODO
        Namespace<TypeAnalysis.TYPES>[] namespaces = (Namespace<TypeAnalysis.TYPES>[]) new Namespace[MAX_BLOCKS];
        for (int i = 0; i < MAX_BLOCKS; i++) {
            namespaces[i] = new Namespace<>();
        }
        Types types = new Types(namespaces);
        
        // First pass: Collect all function signatures
        collectFunctionSignatures(this.program, types);
        
        this.program.accept(new BreakAnalysis(), new BreakAnalysis.LoopContext());
        this.program.accept(new RecursivePostorderVisitor<>(new LoopAnalysis()), new LoopAnalysis.Loopy());
        this.program.accept(new RecursivePostorderVisitor<>(new TypeAnalysis()), types);

    }
    
    private void collectFunctionSignatures(ProgramTree program, Types types) {
        // Collect function signatures from all functions in the program
        for (FunctionTree function : program.topLevelTrees()) {
            String functionName = function.name().name().asString();
            
            // Collect parameter types
            List<TypeAnalysis.TYPES> paramTypes = function.parameters().stream()
                .map(param -> getTypeFromTypeTree(param.type()))
                .toList();
            
            // Get return type
            TypeAnalysis.TYPES returnType = getTypeFromTypeTree(function.returnType());
            
            // Store function signature
            types.putFunctionSignature(functionName, new FunctionSignature(returnType, paramTypes));
        }
    }
    
    private static TypeAnalysis.TYPES getTypeFromTypeTree(TypeTree typeTree) {
        return switch(typeTree.type()) {
            case BasicType.BOOL -> TypeAnalysis.TYPES.BOOL;
            case BasicType.INT  -> TypeAnalysis.TYPES.INT;
        };
    }

}
