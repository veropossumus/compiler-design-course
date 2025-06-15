package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/// Checks that variables are
/// - declared before assignment
/// - not declared twice
/// - not initialized twice
/// - assigned before referenced
class VariableStatusAnalysis implements NoOpVisitor<Namespace<VariableStatusAnalysis.VariableStatus>> {

    @Override
    public Unit visit(AssignmentTree assignmentTree, Namespace<VariableStatus> data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) -> {
                VariableStatus status = data.get(name);
                if (assignmentTree.operator().type() == Operator.OperatorType.ASSIGN) {
                    checkDeclared(name, status);
                } else {
                    checkInitialized(name, status);
                }
                if (status != VariableStatus.INITIALIZED) {
                    updateStatus(data, VariableStatus.INITIALIZED, name);
                }
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    private static void checkDeclared(NameTree name, @Nullable VariableStatus status) {
        if (status == null) {
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
    }

    private static void checkInitialized(NameTree name, @Nullable VariableStatus status) {
        if (status == null || status == VariableStatus.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use");
        }
    }

    private static void checkUndeclared(NameTree name, @Nullable VariableStatus status) {
        if (status != null) {
            throw new SemanticException("Variable " + name + " is already declared");
        }
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Namespace<VariableStatus> data) {
        checkUndeclared(declarationTree.name(), data.getInCurrentScope(declarationTree.name()));
        VariableStatus status = declarationTree.initializer() == null
                ? VariableStatus.DECLARED
                : VariableStatus.INITIALIZED;
        data.putInCurrentScope(declarationTree.name(), status);

        // Visit the initializer if it exists (important for expressions like `int x = y + 1`)
        if (declarationTree.initializer() != null) {
            declarationTree.initializer().accept(this, data);
        }

        return Unit.INSTANCE;
    }

    private static void updateStatus(Namespace<VariableStatus> data, VariableStatus status, NameTree name) {
        data.put(name, status, (existing, replacement) -> {
            if (existing.ordinal() >= replacement.ordinal()) {
                throw new SemanticException("variable is already " + existing + ". Cannot be " + replacement + " here.");
            }
            return replacement;
        });
    }

    @Override
    public Unit visit(IdentExpressionTree identExpressionTree, Namespace<VariableStatus> data) {
        VariableStatus status = data.get(identExpressionTree.name());
        checkInitialized(identExpressionTree.name(), status);
        return NoOpVisitor.super.visit(identExpressionTree, data);
    }

    enum VariableStatus {
        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public Unit visit(ForTree forLoopTree, Namespace<VariableStatus> scope) {
        Namespace<VariableStatus> loopScope = scope.enterScope();

        if (forLoopTree.init() != null) {
            forLoopTree.init().accept(this, loopScope);
        }

        if (forLoopTree.condition() != null) {
            forLoopTree.condition().accept(this, loopScope);
        }

        if (forLoopTree.body() != null) {
            forLoopTree.body().accept(this, loopScope);
        }

        if (forLoopTree.update() != null) {
            forLoopTree.update().accept(this, loopScope);
        }

        Namespace<VariableStatus> parentScope = loopScope.exitScope();

        return Unit.INSTANCE;
    }



    @Override
    public Unit visit(BlockTree blockTree, Namespace<VariableStatus> data) {
        Namespace<VariableStatus> newScope = data.enterScope();

        for (var statement : blockTree.statements()) {
            statement.accept(this, newScope);
        }

        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(WhileTree whileTree, Namespace<VariableStatus> data) {
        Namespace<VariableStatus> newScope = data.enterScope();

        whileTree.condition().accept(this, newScope);

        whileTree.body().accept(this, newScope);

        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(IfTree ifTree, Namespace<VariableStatus> data) {
        ifTree.condition().accept(this, data);

        Namespace<VariableStatus> thenScope = data.enterScope();
        ifTree.thenBranch().accept(this, thenScope);

        if (ifTree.elseBranch() != null) {
            Namespace<VariableStatus> elseScope = data.enterScope();
            ifTree.elseBranch().accept(this, elseScope);
        }

        return Unit.INSTANCE;
    }
}