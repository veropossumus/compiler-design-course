package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record BreakTree(Span span, int loopId) implements StatementTree {
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    @Override
    public Span span() {
        return span;
    }
}