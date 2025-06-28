package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionAnalysis implements NoOpVisitor<BreakAnalysis.LoopContext>{


    static class FunctionTable {
    Map<String, List<TypeAnalysis.TYPES>> table = new HashMap<>();
    }

}
