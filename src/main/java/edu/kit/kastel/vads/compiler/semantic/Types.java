package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import java.util.HashMap;

/**
 * Manages the mapping between AST nodes (Tree or NameTree) and their corresponding types.
 * Supports different scopes using an array of Namespace objects.
 */
public class Types {

    private final HashMap<Tree, TypeAnalysis.TYPES> nodeTypeMap = new HashMap<>();

    private final Namespace<TypeAnalysis.TYPES>[] scopedNamespaces;
    public Types(Namespace<TypeAnalysis.TYPES>[] namespaces) {
        this.scopedNamespaces = namespaces;
    }
    public TypeAnalysis.TYPES get(Tree tree) {
        return nodeTypeMap.get(tree);
    }
    public TypeAnalysis.TYPES get(NameTree name, int scope) {
        return scopedNamespaces[scope].get(name);
    }
    public void put(Tree tree, TypeAnalysis.TYPES type) {
        nodeTypeMap.put(tree, type);
    }
    public void put(NameTree name, TypeAnalysis.TYPES type, int scope) {
        scopedNamespaces[scope].put(name, type, (oldName, currentName) -> currentName);
    }
}
