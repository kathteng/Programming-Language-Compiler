package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {
    String input;
    ILexer lexer;
    Expr dummy;

    public Parser(String input) {
        this.input = input;
        lexer = CompilerComponentFactory.getLexer(input);
        IToken dummyT = new Token(IToken.Kind.EOF, -1, -1, "dummy");
        dummy = new BooleanLitExpr(dummyT);
    }

    @Override
    public ASTNode parse() throws PLCException {
        IToken t = lexer.next();

        // TODO: figure out how to do this recursively (pass test8)
        // go through each token in input, then work our way up the parse tree
        while (t.getKind() != IToken.Kind.EOF) {
            switch (t.getKind()) {
                case BOOLEAN_LIT -> {
                    BooleanLitExpr b = new BooleanLitExpr(t);
                    IToken peek = lexer.peek();
                    switch (peek.getKind()) {
                        // is a BinaryExpr
                        case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                            return binary(t, b);
                        }
                        // is a UnaryExprPostfix
                        case LSQUARE -> {
                            dummy = unaryPostfix(t, b);
                        }
                        default -> {
                            return b;
                        }
                    }
                }
                case STRING_LIT -> {
                    StringLitExpr b = new StringLitExpr(t);
                    IToken peek = lexer.peek();
                    switch (peek.getKind()) {
                        case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                            return binary(t, b);
                        }
                        case LSQUARE -> {
                            dummy = unaryPostfix(t, b);
                        }
                        default -> {
                            return b;
                        }
                    }
                }
                case INT_LIT -> {
                    IntLitExpr b = new IntLitExpr(t);
                    IToken peek = lexer.peek();
                    switch (peek.getKind()) {
                        case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                            return binary(t, b);
                        }
                        case LSQUARE -> {
                            return unaryPostfix(t, b);
                        }
                        default -> {
                            return b;
                        }
                    }
                }
                case FLOAT_LIT -> {
                    FloatLitExpr b = new FloatLitExpr(t);
                    IToken peek = lexer.peek();
                    switch (peek.getKind()) {
                        case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                            return binary(t, b);
                        }
                        case LSQUARE -> {
                            return unaryPostfix(t, b);
                        }
                        default -> {
                            return b;
                        }
                    }
                }
                case IDENT -> {
                    IdentExpr b = new IdentExpr(t);
                    IToken peek = lexer.peek();
                    switch (peek.getKind()) {
                        case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                            return binary(t, b);
                        }
                        case LSQUARE -> {
                            dummy = unaryPostfix(t, b);
                        }
                        default -> {
                            return b;
                        }
                    }
                }
                // is a ConditionalExpr
                case KW_IF -> {
                    return conditional(t);
                }
                // is a UnaryExpr
                case BANG, MINUS, COLOR_OP, IMAGE_OP -> {
                    return unary(t);
                }
            }
            switch (lexer.peek().getKind()) {
                case PLUS, MINUS, TIMES, DIV, MOD, AND, OR, LE, GE, GT, LT, EQUALS, NOT_EQUALS -> {
                    dummy = binary(t, dummy);
                }
            }
            t = lexer.next();
        }
        return dummy;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    private ConditionalExpr conditional(IToken t) throws PLCException {
        // t = 'if'
        IToken x = lexer.next(); // check if next token is '('
        if (x.getKind() == IToken.Kind.LPAREN) {
            Expr condition = (Expr) parse();
            if (lexer.next().getKind() != IToken.Kind.RPAREN)
                throw new SyntaxException("No right paren dumbass");
            Expr trueCase = (Expr) parse();
            if (lexer.next().getKind() != IToken.Kind.KW_ELSE)
                throw new SyntaxException("No else dumbass");
            Expr falseCase = (Expr) parse();
            if (lexer.next().getKind() != IToken.Kind.KW_FI)
                throw new SyntaxException("No fi dumbass");
            return new ConditionalExpr(t, condition, trueCase, falseCase);
        }
        else
            throw new SyntaxException("Bad Conditional. Bad.");
    }
    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    private UnaryExpr unary(IToken t) throws PLCException {
        IToken next = lexer.peek();
        if (next.getKind() == IToken.Kind.EOF)
            throw new SyntaxException("Bad Unary. Bad.");
        else {
            Expr b = (Expr) parse();
            return new UnaryExpr(t, t, b);
        }
    }
    // BinaryExpr = +, -, *, /, %, |, &, >, <, <=, >=, ==, !=
    private BinaryExpr binary(IToken t, Expr e) throws PLCException {
        IToken op = lexer.next();
        IToken next = lexer.peek();
        if (next.getKind() == IToken.Kind.EOF)
            throw new SyntaxException("Bad BinaryExpr. Bad.");
        else {
            Expr a = (Expr) parse();
            return new BinaryExpr(t, e, op, a);
        }
    }
    // UnaryExprPostfix::= PrimaryExpr PixelSelector?
    // PixelSelector::= '[' Expr ',' Expr ']'
    private UnaryExprPostfix unaryPostfix(IToken t, Expr e) throws PLCException {
        // t = '['
        IToken next = lexer.next();
        if (next.getKind() == IToken.Kind.EOF)
            throw new SyntaxException("Bad unaryExprPostfix. Bad.");
        else {
            Expr a = (Expr) parse(); // expr x
            if (lexer.next().getKind() != IToken.Kind.COMMA)
                throw new SyntaxException("Bad PixelSelector. Bad.");
            else {
                Expr b = (Expr) parse(); // expr y
                if (lexer.peek().getKind() != IToken.Kind.RSQUARE)
                    throw new SyntaxException("Very Bad PixelSelector. Very Bad.");
                else {
                    PixelSelector p = new PixelSelector(next, a, b);
                    return new UnaryExprPostfix(t, e, p);
                }
            }
        }
    }
}
