package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record ForTree(
    StatementTree init,
    ExpressionTree condition,
    ExpressionTree update,
    StatementTree body,
    int loopId
) implements StatementTree {
    @Override
    public Span span() {
        return new Span.SimpleSpan(init.span().start(), body.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
