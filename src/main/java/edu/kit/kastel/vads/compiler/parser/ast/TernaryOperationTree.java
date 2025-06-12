package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record TernaryOperationTree(ExpressionTree condition, ExpressionTree trueExpression,
        ExpressionTree falseExpression) implements ExpressionTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(condition.span().start(), falseExpression.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}