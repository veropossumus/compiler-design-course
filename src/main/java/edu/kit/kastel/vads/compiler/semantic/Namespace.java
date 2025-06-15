package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public class Namespace<T> {

    private final Map<Name, T> content;
    private @Nullable Namespace<T> parent;

    public Namespace() {
        this.content = new HashMap<>();
        this.parent = null;
    }

    private Namespace(@Nullable Namespace<T> parent) {
        this.content = new HashMap<>();
        this.parent = parent;
    }

    public void put(NameTree name, T value, BinaryOperator<T> merger) {
        this.content.merge(name.name(), value, merger);
    }

    public void putInCurrentScope(NameTree name, T value) {
        this.content.put(name.name(), value);
    }

    public @Nullable T get(NameTree name) {
        T value = this.content.get(name.name());
        if (value != null) {
            return value;
        }
        if (this.parent != null) {
            return this.parent.get(name);
        }
        return null;
    }

    public @Nullable T getInCurrentScope(NameTree name) {
        return this.content.get(name.name());
    }

    public Namespace<T> enterScope() {
        return new Namespace<>(this);
    }

    public @Nullable Namespace<T> exitScope() {
        return this.parent;
    }
}