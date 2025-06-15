package edu.kit.kastel.vads.compiler.parser.symbol;

public final class IdentName implements Name {

    private final String identifier;

    public IdentName(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String asString() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentName other)) return false;
        return identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier;
    }
}
