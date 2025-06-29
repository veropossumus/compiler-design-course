package edu.kit.kastel.vads.compiler.semantic;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import java.util.List;
public class TypeAnalysis implements NoOpVisitor<Types> {
    enum TYPES{
        BOOL, VALID, INT
    }
    private TYPES getType(TypeTree typeTree) {
        return switch(typeTree.type()) {
            case BasicType.BOOL -> TYPES.BOOL;
            case BasicType.INT  -> TYPES.INT;
        };
    }
    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, Types data){
        TYPES lhs = data.get(binaryOperationTree.lhs());
        TYPES rhs = data.get(binaryOperationTree.rhs());
        String operator = binaryOperationTree.operatorType().toString();
        switch (operator) {
            case "+", "-", "*", "/", "%", "<<", ">>" -> {
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.INT);
            }
            case "<", "<=", ">", ">=" -> {
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.BOOL);
            }
            case "==", "!=" -> {
                if (lhs != rhs || (lhs != TYPES.INT && lhs != TYPES.BOOL)) {
                    throwError(binaryOperationTree, lhs, rhs);
                }
                data.put(binaryOperationTree, TYPES.BOOL);
            }
            case "&&", "||" -> {
                if (lhs != TYPES.BOOL || rhs != TYPES.BOOL) {
                    throwError(binaryOperationTree, lhs, TYPES.BOOL);
                }
                data.put(binaryOperationTree, TYPES.BOOL);
            }
            case "&", "|", "^" -> {
                if (lhs != TYPES.INT || rhs != TYPES.INT) {
                    throwError(binaryOperationTree, lhs, TYPES.INT);
                }
                data.put(binaryOperationTree, TYPES.INT);
            }
            default -> {
                throwError(binaryOperationTree, lhs, TYPES.INT);
            }
        }
        return NoOpVisitor.super.visit(binaryOperationTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, Types data){
        List<StatementTree> blockStatements = blockTree.statements();
        for(StatementTree statement : blockStatements){
            if (statement instanceof BreakTree || statement instanceof ContinueTree) {
                continue;
            }
            if(data.get(statement) != TYPES.VALID){
                throwError(blockTree, data.get(statement), TYPES.VALID);
            }
        }
        data.put(blockTree, TYPES.VALID);
        return NoOpVisitor.super.visit(blockTree, data);
    }
    @Override
    public Unit visit(AssignmentTree assignmentTree, Types data) {
        TYPES lhs;
        switch(assignmentTree.lValue()) {
            case LValueIdentTree identTree ->
                    lhs = data.get(identTree.name(), assignmentTree.block());
        }
        TYPES rhs = data.get(assignmentTree.expression());
        if(lhs != rhs) throwError(assignmentTree, rhs, lhs);
        if (!assignmentTree.operator().type().toString().equals("=") && !lhs.equals(TYPES.INT)){
            throwError(assignmentTree, lhs, TYPES.INT);
        }
        data.put(assignmentTree, TYPES.VALID);
        return NoOpVisitor.super.visit(assignmentTree, data);
    }
    @Override
    public Unit visit(DeclarationTree declarationTree, Types data) {
        TYPES declaredType = getType(declarationTree.type());
        if (declarationTree.initializer() != null) {
            TYPES exprType = data.get(declarationTree.initializer());
            if (exprType != declaredType) {
                System.out.println("Declaring variable: " + declarationTree.name() + " with type: " + declaredType); // Debug
                throwError(declarationTree, exprType, declaredType);
            }
        }
        data.put(declarationTree.name(), declaredType, declarationTree.block());
        data.put(declarationTree, TYPES.VALID);
        return NoOpVisitor.super.visit(declarationTree, data);
    }
    @Override
    public Unit visit(WhileTree whileLoopTree, Types data) {
        if (data.get(whileLoopTree.condition()) != TYPES.BOOL) {
            throwError(whileLoopTree, data.get(whileLoopTree.condition()), TYPES.BOOL);
        }
        if (data.get(whileLoopTree.body()) != TYPES.VALID) {
            throwError(whileLoopTree, data.get(whileLoopTree.body()), TYPES.VALID);
        }
        data.put(whileLoopTree, TYPES.VALID);
        return NoOpVisitor.super.visit(whileLoopTree, data);
    }
    @Override
    public Unit visit(ForTree forLoopTree, Types data) {
        if (forLoopTree.init() != null && data.get(forLoopTree.init()) != TYPES.VALID) {
            throwError(forLoopTree.init(), data.get(forLoopTree.init()), TYPES.VALID);
        }
        if (forLoopTree.condition() != null) {
            TYPES conditionType = data.get(forLoopTree.condition());
            if (conditionType != TYPES.BOOL) {
                throwError(forLoopTree.condition(), conditionType, TYPES.BOOL);
            }
        }
        if (forLoopTree.update() != null && data.get(forLoopTree.update()) != TYPES.VALID) {
            throwError(forLoopTree.update(), data.get(forLoopTree.update()), TYPES.VALID);
        }

        if (data.get(forLoopTree.body()) != TYPES.VALID) {
            throwError(forLoopTree.body(), data.get(forLoopTree.body()), TYPES.VALID);
        }
        data.put(forLoopTree, TYPES.VALID);
        return NoOpVisitor.super.visit(forLoopTree, data);
    }
    @Override
    public Unit visit(ReturnTree returnTree, Types data) {
        TYPES exprType = data.get(returnTree.expression());
        if (exprType != TYPES.INT) {
            throwError(returnTree, exprType, TYPES.INT);
        }
        data.put(returnTree, TYPES.VALID);
        return NoOpVisitor.super.visit(returnTree, data);
    }
    @Override
    public Unit visit(NegateTree negateTree, Types data) {
        TYPES exprType = data.get(negateTree.expression());
        if (exprType != TYPES.INT) {
            throwError(negateTree, exprType, TYPES.INT);
        }
        data.put(negateTree, TYPES.INT);
        return NoOpVisitor.super.visit(negateTree, data);
    }
    
    @Override
    public Unit visit(LogicalNotTree logicalNotTree, Types data) {
        TYPES exprType = data.get(logicalNotTree.expression());
        if (exprType != TYPES.BOOL) {
            throwError(logicalNotTree, exprType, TYPES.BOOL);
        }
        data.put(logicalNotTree, TYPES.BOOL);
        return NoOpVisitor.super.visit(logicalNotTree, data);
    }
    @Override
    public Unit visit(IfTree conditionalTree, Types data) {
        if (data.get(conditionalTree.condition()) != TYPES.BOOL) {
            throwError(conditionalTree, data.get(conditionalTree.condition()), TYPES.BOOL);
        }
        TYPES thenType = data.get(conditionalTree.thenBranch());
        if (conditionalTree.elseBranch() != null) {
            TYPES elseType = data.get(conditionalTree.elseBranch());
            if (thenType != elseType) {
                throwError(conditionalTree, elseType, thenType);
            }
            data.put(conditionalTree, thenType);
        } else {
            data.put(conditionalTree, TYPES.VALID);
        }
        return NoOpVisitor.super.visit(conditionalTree, data);
    }
    @Override
    public Unit visit(LiteralTree literalTree, Types data) {
        data.put(literalTree, TYPES.INT);
        return NoOpVisitor.super.visit(literalTree, data);
    }
    @Override
    public Unit visit(BoolLiteralTree boolLiteralTree, Types data) {
        data.put(boolLiteralTree, TYPES.BOOL);
        return NoOpVisitor.super.visit(boolLiteralTree, data);
    }
    @Override
    public Unit visit(IdentExpressionTree identExpr, Types data) {
        TYPES type = null;
        int currentScope = identExpr.block();
        while (currentScope >= 0 && type == null) {
            type = data.get(identExpr.name(), currentScope);
            currentScope--;
        }
        if (type == null) {
            throw new SemanticException("Variable " + identExpr.name() + " is not declared in any accessible scope (at " + identExpr.span() + ")");
        }
        data.put(identExpr, type);
        return NoOpVisitor.super.visit(identExpr, data);
    }
    @Override
    public Unit visit(FunctionTree functionTree, Types data) {
        // Function signatures are already collected in SemanticAnalysis
        // Just type-check the function body
        if (data.get(functionTree.body()) != TYPES.VALID) {
            throwError(functionTree, data.get(functionTree.body()), TYPES.VALID);
        }
        data.put(functionTree, TYPES.VALID);
        return NoOpVisitor.super.visit(functionTree, data);
    }
    @Override
    public Unit visit(TernaryOperationTree ternaryTree, Types data) {
        TYPES condType = data.get(ternaryTree.condition());
        if (condType != TYPES.BOOL) {
            throwError(ternaryTree, condType, TYPES.BOOL);
        }
        TYPES trueType = data.get(ternaryTree.trueExpression());
        TYPES falseType = data.get(ternaryTree.falseExpression());
        if (trueType != falseType) {
            throwError(ternaryTree, falseType, trueType);
        }
        data.put(ternaryTree, trueType);
        return NoOpVisitor.super.visit(ternaryTree, data);
    }
    @Override
    public Unit visit(FunctionCallTree functionCallTree, Types data) {
        String functionName = functionCallTree.functionName().name().asString();
        
        // Handle built-in functions
        switch (functionName) {
            case "print" -> {
                if (functionCallTree.arguments().size() != 1) {
                    throw new SemanticException("print function expects exactly 1 argument, got " + functionCallTree.arguments().size());
                }
                TYPES argType = data.get(functionCallTree.arguments().get(0));
                if (argType != TYPES.INT) {
                    throw new SemanticException("print function expects int argument, got " + argType);
                }
                data.put(functionCallTree, TYPES.INT); // print returns int
            }
            case "read" -> {
                if (!functionCallTree.arguments().isEmpty()) {
                    throw new SemanticException("read function expects no arguments, got " + functionCallTree.arguments().size());
                }
                data.put(functionCallTree, TYPES.INT); // read returns int
            }
            case "flush" -> {
                if (!functionCallTree.arguments().isEmpty()) {
                    throw new SemanticException("flush function expects no arguments, got " + functionCallTree.arguments().size());
                }
                data.put(functionCallTree, TYPES.INT); // flush returns int
            }
            default -> {
                // Handle user-defined functions
                if (data.hasFunctionSignature(functionName)) {
                    FunctionSignature signature = data.getFunctionSignature(functionName);
                    
                    // Check argument count
                    if (functionCallTree.arguments().size() != signature.parameterTypes().size()) {
                        throw new SemanticException("Function " + functionName + " expects " + 
                            signature.parameterTypes().size() + " arguments, got " + 
                            functionCallTree.arguments().size());
                    }
                    
                    // Check argument types
                    for (int i = 0; i < functionCallTree.arguments().size(); i++) {
                        TYPES argType = data.get(functionCallTree.arguments().get(i));
                        TYPES expectedType = signature.parameterTypes().get(i);
                        if (argType != expectedType) {
                            throw new SemanticException("Function " + functionName + " argument " + i + 
                                " expects " + expectedType + ", got " + argType);
                        }
                    }
                    
                    // Set return type
                    data.put(functionCallTree, signature.returnType());
                } else {
                    throw new SemanticException("Unknown function: " + functionName);
                }
            }
        }
        
        return NoOpVisitor.super.visit(functionCallTree, data);
    }

    @Override
    public Unit visit(ParameterTree parameterTree, Types data) {
        // Parameters should be treated like variable declarations in function scope
        TYPES paramType = getType(parameterTree.type());
        data.putByLine(parameterTree.name(), paramType, parameterTree.name().span().start().line());
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(FunctionCallStatementTree functionCallStatementTree, Types data) {
        // The function call itself will be type-checked by visit(FunctionCallTree)
        // The statement as a whole is always valid
        data.put(functionCallStatementTree, TYPES.VALID);
        return NoOpVisitor.super.visit(functionCallStatementTree, data);
    }

    private static void throwError(Tree tree, TYPES actualType, TYPES expectedType){
        throw new SemanticException("Invalid type at " + tree.span() + ": found " + actualType + ", expected " + expectedType);
    }
}
