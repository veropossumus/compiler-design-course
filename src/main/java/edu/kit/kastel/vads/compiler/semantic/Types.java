package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a function signature for type checking.
 */
record FunctionSignature(TypeAnalysis.TYPES returnType, List<TypeAnalysis.TYPES> parameterTypes) {}

/**
 * Manages the mapping between AST nodes (Tree or NameTree) and their corresponding types.
 * Supports different scopes using an array of Namespace objects.
 */
public class Types {

    private final HashMap<Tree, TypeAnalysis.TYPES> nodeTypeMap = new HashMap<>();
    private final HashMap<String, FunctionSignature> functionSignatures = new HashMap<>();

    private final Namespace<TypeAnalysis.TYPES>[] scopedNamespaces;
    public Types(Namespace<TypeAnalysis.TYPES>[] namespaces) {
        this.scopedNamespaces = namespaces;
    }
    public TypeAnalysis.TYPES get(Tree tree) {
        return nodeTypeMap.get(tree);
    }
    public TypeAnalysis.TYPES get(NameTree name, int scope) {
        // First try the specific scope
        TypeAnalysis.TYPES result = scopedNamespaces[scope].get(name);
        if (result != null) {
            return result;
        }
        
        // If not found and we have scope information, search parent scopes
        // We need access to the scopes to find parent relationships
        // For now, implement a simple fallback that searches from current scope backwards
        // This assumes parent scopes have lower IDs (which is typically true)
        for (int i = scope - 1; i >= 0; i--) {
            result = scopedNamespaces[i].getInCurrentScope(name);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }
    public void put(Tree tree, TypeAnalysis.TYPES type) {
        nodeTypeMap.put(tree, type);
    }
    public void put(NameTree name, TypeAnalysis.TYPES type, int scope) {
        scopedNamespaces[scope].put(name, type, (_, currentName) -> currentName);
    }
    
    public void putByLine(NameTree name, TypeAnalysis.TYPES type, int line) {
        // For now, we'll use a simple approach and put it in the first scope
        // In a more sophisticated implementation, we'd need to map line numbers to scopes
        scopedNamespaces[0].put(name, type, (_, currentName) -> currentName);
    }
    
    // Function signature methods
    public void putFunctionSignature(String functionName, FunctionSignature signature) {
        functionSignatures.put(functionName, signature);
    }
    
    public FunctionSignature getFunctionSignature(String functionName) {
        return functionSignatures.get(functionName);
    }
    
    public boolean hasFunctionSignature(String functionName) {
        return functionSignatures.containsKey(functionName);
    }
}
