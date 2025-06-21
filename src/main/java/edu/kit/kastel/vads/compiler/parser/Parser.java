package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Parser {
    private final TokenSource tokenSource;
    private final List<Scope> scopes = new ArrayList<>();
    private final Stack<Scope> currentBlock = new Stack();
    private final Stack<Integer> currentLoopBlock = new Stack(); //keep track of loop
    private int loopCounter = 0;

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()), this.scopes);
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        if (!programTree.topLevelTrees().stream()
                .anyMatch(function -> function.name().name().asString().equals("main"))) {
            throw new ParseException("no main function");
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        enterNewScope();
        BlockTree body = parseBlock();
        leaveCurrentBlock();

        return new FunctionTree(
                new TypeTree(BasicType.INT, returnType.span()),
                name(identifier),
                body);
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() {
        StatementTree statement;
        if (this.tokenSource.peek().isKeyword(KeywordType.INT) || this.tokenSource.peek().isKeyword(KeywordType.BOOL)) {
            statement = parseDeclaration();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.RETURN)) {
            statement = parseReturn();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.WHILE)) {
            statement = parseWhile();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.BREAK)) {
            statement = parseBreak();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.IF)) {
            statement = parseIf();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.CONTINUE)) {
            statement = parseContinue();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.FOR)) {
            statement = parseFor();
        } else if (this.tokenSource.peek().isSeparator(SeparatorType.BRACE_OPEN)) {
                statement = parseBlock();
        } else {
            statement = parseSimple();
        }

        if (!(statement instanceof BlockTree || statement instanceof WhileTree || statement instanceof ForTree
                || statement instanceof IfTree)) {
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        }
        return statement;
    }

    private StatementTree parseDeclaration() {
        Keyword typeToken;
        BasicType type;
        if (this.tokenSource.peek().isKeyword(KeywordType.INT)) {
            typeToken = this.tokenSource.expectKeyword(KeywordType.INT);
            type = BasicType.INT;
        } else if (this.tokenSource.peek().isKeyword(KeywordType.BOOL)) {
            typeToken = this.tokenSource.expectKeyword(KeywordType.BOOL);
            type = BasicType.BOOL;
        } else {
            throw new ParseException("expected type (int or bool) but got " + this.tokenSource.peek());
        }
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }
        return new DeclarationTree(new TypeTree(type, typeToken.span()), name(ident), expr, this.currentBlock.peek().getId());
    }

    private StatementTree parseSimple() {
        LValueTree lValue = parseLValue();
        Operator assignmentOperator = parseAssignmentOperator();
        ExpressionTree expression = parseExpression();
        return new AssignmentTree(lValue, assignmentOperator, expression, this.currentBlock.peek().getId());
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS,
                     ASSIGN_BITWISE_AND, ASSIGN_BITWISE_OR, ASSIGN_BITWISE_XOR, ASSIGN_SHIFT_LEFT, ASSIGN_SHIFT_RIGHT -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    private StatementTree parseIf() {
        Keyword ifKeyword = this.tokenSource.expectKeyword(KeywordType.IF);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        enterNewScope();
        StatementTree thenStmt = parseStatement();
        leaveCurrentBlock();

        StatementTree elseStmt = null;
        if (this.tokenSource.peek().isKeyword(KeywordType.ELSE)) {
            this.tokenSource.expectKeyword(KeywordType.ELSE);
            enterNewScope();
            elseStmt = parseStatement();
            leaveCurrentBlock();
        }

        return new IfTree(condition, thenStmt, elseStmt, ifKeyword.span());
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    private void enterNewScope() {
        Scope scope;
        if(this.currentBlock.empty()) scope = new Scope(this.scopes.size());
        else scope = new Scope(this.scopes.size(), this.currentBlock.peek());
        this.scopes.add(scope);
        this.currentBlock.push(scope);
    }

    private void enterNewLoopBlock() {
        this.currentLoopBlock.push(this.loopCounter++);
    }

    private void leaveCurrentBlock() {
        this.currentBlock.pop();
    }

    private void leaveCurrentLoopBlock() {
        this.currentLoopBlock.pop();
    }

    private int getCurrentLoopBlock() {
        if(!this.currentLoopBlock.empty()) return this.currentLoopBlock.peek();
        else return -1;
    }

    private static final class OpInfo {
        final int precedence;
        final boolean rightAssoc;

        OpInfo(int precedence, boolean rightAssoc) {
            this.precedence = precedence;
            this.rightAssoc = rightAssoc;
        }
    }

    private static final Map<OperatorType, OpInfo> OP_INFO = Map.ofEntries(
            Map.entry(OperatorType.ASSIGN, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_PLUS, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_MINUS, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_MUL, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_DIV, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_MOD, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_BITWISE_AND, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_BITWISE_OR, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_BITWISE_XOR, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_SHIFT_LEFT, new OpInfo(1, true)),
            Map.entry(OperatorType.ASSIGN_SHIFT_RIGHT, new OpInfo(1, true)),

            Map.entry(OperatorType.QUESTION, new OpInfo(2, true)),
            Map.entry(OperatorType.OR, new OpInfo(3, false)),
            Map.entry(OperatorType.AND, new OpInfo(4, false)),
            Map.entry(OperatorType.BITWISE_OR, new OpInfo(5, false)),
            Map.entry(OperatorType.BITWISE_XOR, new OpInfo(6, false)),
            Map.entry(OperatorType.BITWISE_AND, new OpInfo(7, false)),
            Map.entry(OperatorType.COMPARE_EQUAL, new OpInfo(8, false)),
            Map.entry(OperatorType.COMPARE_NOT_EQUAL, new OpInfo(8, false)),
            Map.entry(OperatorType.COMPARE_LESS, new OpInfo(9, false)),
            Map.entry(OperatorType.COMPARE_LESS_EQUAL, new OpInfo(9, false)),
            Map.entry(OperatorType.COMPARE_GREATER, new OpInfo(9, false)),
            Map.entry(OperatorType.COMPARE_GREATER_EQUAL, new OpInfo(9, false)),
            Map.entry(OperatorType.SHIFT_LEFT, new OpInfo(10, false)),
            Map.entry(OperatorType.SHIFT_RIGHT, new OpInfo(10, false)),
            Map.entry(OperatorType.PLUS, new OpInfo(11, false)),
            Map.entry(OperatorType.MINUS, new OpInfo(11, false)),
            Map.entry(OperatorType.MUL, new OpInfo(12, false)),
            Map.entry(OperatorType.DIV, new OpInfo(12, false)),
            Map.entry(OperatorType.MOD, new OpInfo(12, false)));

    private ExpressionTree parseExpression() {
        return parseExprPrec(0);
    }

    private ExpressionTree parseExprPrec(int minPrec) {
        ExpressionTree lhs = parseFactor();

        while (true) {
            Token next = this.tokenSource.peek();
            if (!(next instanceof Operator op))
                break;
            OpInfo info = OP_INFO.get(op.type());
            if (info == null)
                break;

            int prec = info.precedence;
            boolean right = info.rightAssoc;

            if (prec < minPrec)
                break;

            this.tokenSource.consume();

            if (op.type() == OperatorType.QUESTION) {
                ExpressionTree thenExpr = parseExprPrec(0);
                this.tokenSource.expectOperator(OperatorType.COLON);
                ExpressionTree elseExpr = parseExprPrec(prec);
                lhs = new TernaryOperationTree(lhs, thenExpr, elseExpr);
                continue;
            }

            int nextMinPrec = right ? prec : prec + 1;
            ExpressionTree rhs = parseExprPrec(nextMinPrec);

            if (op.type() == OperatorType.AND) {
                lhs = new TernaryOperationTree(lhs, rhs, new BoolLiteralTree(false, op.span()));
                continue;
            }
            if (op.type() == OperatorType.OR) {
                lhs = new TernaryOperationTree(lhs, new BoolLiteralTree(true, op.span()), rhs);
                continue;
            }

            lhs = new BinaryOperationTree(lhs, rhs, op.type());
        }

        return lhs;
    }

    private boolean isAssignmentOperator(OperatorType opType) {
        return switch (opType) {
            case ASSIGN,           // =
                 ASSIGN_PLUS,      // +=
                 ASSIGN_MINUS,     // -=
                 ASSIGN_MUL,  // *=
                 ASSIGN_DIV ,    // /=
                 ASSIGN_MOD,    // %=
                 ASSIGN_SHIFT_LEFT,    // <<=
                 ASSIGN_SHIFT_RIGHT,
                 ASSIGN_BITWISE_AND,
                 ASSIGN_BITWISE_XOR,
                 ASSIGN_BITWISE_OR
                    -> true;
            default -> false;
        };
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident), this.currentBlock.peek().getId());
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Keyword(var type, var span) when type == KeywordType.TRUE -> {
                this.tokenSource.consume();
                yield new BoolLiteralTree(true, span);
            }
            case Keyword(var type, var span) when type == KeywordType.FALSE -> {
                this.tokenSource.consume();
                yield new BoolLiteralTree(false, span);
            }
            case Operator(var type, _) when type == OperatorType.NOT -> {
                Span span = this.tokenSource.consume().span();
                yield new NotTree(parseFactor(), span);
            }

            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }

    private StatementTree parseFor() {
        this.tokenSource.expectKeyword(KeywordType.FOR);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);

        enterNewScope();
        enterNewLoopBlock();

        int currentLoopId = getCurrentLoopBlock();

        StatementTree init = null;
        if (!this.tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            if (this.tokenSource.peek().isKeyword(KeywordType.INT)
                    || this.tokenSource.peek().isKeyword(KeywordType.BOOL)) {
                init = parseDeclaration();
            } else {
                init = parseSimple();
            }
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        ExpressionTree condition = null;
        if (!this.tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            condition = parseExpression();
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        ExpressionTree update = null;
        if (!this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            update = parseExpression();
        }
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        StatementTree body = parseOptionalBlockOrStatement();

        leaveCurrentLoopBlock();
        leaveCurrentBlock();

        return new ForTree(init, condition, update, body, currentLoopId);
    }

    private StatementTree parseContinue() {
        Keyword continueKeyword = this.tokenSource.expectKeyword(KeywordType.CONTINUE);
        int loopBlock = getCurrentLoopBlock();
        if (loopBlock == -1) {
            //throw new SemanticException("continue statement not in loop");
        }
        return new ContinueTree(continueKeyword.span(), loopBlock);
    }

    private StatementTree parseBreak() {
        Keyword breakKeyword = this.tokenSource.expectKeyword(KeywordType.BREAK);
        int loopBlock = getCurrentLoopBlock();
        if (loopBlock == -1) {
            //throw new SemanticException("break statement not in loop");
        }
        return new BreakTree(breakKeyword.span(), loopBlock);
    }

    private StatementTree parseWhile() {
        this.tokenSource.expectKeyword(KeywordType.WHILE);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        enterNewLoopBlock();
        enterNewScope();

        int currentLoopId = getCurrentLoopBlock();

        StatementTree body = parseOptionalBlockOrStatement();
        leaveCurrentBlock();
        leaveCurrentLoopBlock();

        return new WhileTree(condition, body, currentLoopId);
    }


    private StatementTree parseOptionalBlockOrStatement() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.BRACE_OPEN)) {
            enterNewScope();
            StatementTree block = parseBlock();
            leaveCurrentBlock();
            return block;
        } else {
            return parseStatement();
        }
    }

}