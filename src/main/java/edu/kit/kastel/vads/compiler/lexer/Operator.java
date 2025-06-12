package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {

        NOT("!"),
        BITWISE_NOT("~"),
        MINUS("-"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        PLUS("+"),
        ASSIGN_PLUS("+="),
        ASSIGN_MINUS("-="),

        SHIFT_LEFT("<<"),
        ASSIGN_SHIFT_LEFT("<<="),
        SHIFT_RIGHT(">>"),
        ASSIGN_SHIFT_RIGHT(">>="),

        COMPARE_LESS("<"),
        COMPARE_LESS_EQUAL("<="),
        COMPARE_GREATER(">"),
        COMPARE_GREATER_EQUAL(">="),

        COMPARE_EQUAL("=="),
        COMPARE_NOT_EQUAL("!="),

        BITWISE_AND("&"),
        ASSIGN_BITWISE_AND("&="),
        BITWISE_XOR("^"),
        ASSIGN_BITWISE_XOR("^="),
        BITWISE_OR("|"),
        ASSIGN_BITWISE_OR("|="),

        AND("&&"),
        OR("||"),

        QUESTION("?"),
        COLON(":"),

        ASSIGN("="),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        ASSIGN_MOD("%="),

        ;

        private final String value;

        OperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
