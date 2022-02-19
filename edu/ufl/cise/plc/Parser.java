package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {
    final String input;
    private final ILexer lexer;
    private IToken t;

    public Parser(String input) {
        this.input = input;
        lexer = CompilerComponentFactory.getLexer(input);
    }

    @Override
    public ASTNode parse() throws PLCException {
        t = lexer.next();

        return expr();
    }

    private Expr expr() throws PLCException {
        if (t.getKind() == IToken.Kind.EOF)
            return null;

        if (t.getKind() == IToken.Kind.KW_IF)
            return conditional();
        else
            return logicalOr();
    }
    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else'  Expr 'fi'
    private ConditionalExpr conditional() throws PLCException {
        t = lexer.next();
        if (t.getKind() == IToken.Kind.LPAREN) {
            t = lexer.next();
            //lexer.next(); // t = '('
            Expr condition = expr();
            //t = lexer.next();
            if (t.getKind() != IToken.Kind.RPAREN)
                throw new SyntaxException("No right paren for condition dumbass");
            t = lexer.next();
                //lexer.next(); // t = ')'
            Expr trueCase = expr();
            //t = lexer.next();
            if (t.getKind() != IToken.Kind.KW_ELSE)
                throw new SyntaxException("No else dumbass");
            t = lexer.next();
                //lexer.next();
            Expr falseCase = expr();
            //t = lexer.next();
            if (t.getKind() != IToken.Kind.KW_FI)
                throw new SyntaxException("No fi dumbass");
                         
            //lexer.next();
            ConditionalExpr c = new ConditionalExpr(t, condition, trueCase, falseCase);
            t = lexer.next();
            return c;
        }
        else
            throw new SyntaxException("No left paren for condition dumbass");
    }

    private Expr logicalOr() throws PLCException {
        Expr a = logicalAnd();
        Expr b = null;
        IToken op = null;
        IToken start = t;
        while (t.getKind() == IToken.Kind.OR) {
            op = t;
            t = lexer.next();
            b = logicalAnd();
        }
        if (op == null)
            return a;
        return new BinaryExpr(start, a, op, b);
    }

    private Expr logicalAnd() throws PLCException {
        Expr a = comparison();
        Expr b = null;
        IToken op = null;
        while (t.getKind() == IToken.Kind.AND) {
            op = t;
            t = lexer.next();
            b = comparison();
        }
        if (op == null)
            return a;
        return new BinaryExpr(t, a, op, b);
    }

    private Expr comparison() throws PLCException {
        Expr a = additive();
        Expr b = null;
        IToken op = null;
        while (t.getKind() == Kind.LE||t.getKind() == Kind.LT||t.getKind() == Kind.EQUALS||t.getKind() == Kind.GE||t.getKind() == Kind.GT||t.getKind() == Kind.NOT_EQUALS) {
                    op = t;
                    t = lexer.next();
                    b = additive();
            
        }
        if (op == null)
            return a;
        return new BinaryExpr(t, a, op, b);
    }

    private Expr additive() throws PLCException {
        Expr a = multipl();
        Expr b = null;
        IToken op = null;
        while (t.getKind() == IToken.Kind.PLUS || t.getKind() == IToken.Kind.MINUS) {
            op = t;
            t = lexer.next();
            b = multipl();
        }
        if (op == null)
            return a;
        return new BinaryExpr(t, a, op, b);
    }

    private Expr multipl() throws PLCException {
        Expr a = unary();
        Expr b = null;
        IToken op = null;
        while (t.getKind() == IToken.Kind.TIMES || t.getKind() == IToken.Kind.DIV || t.getKind() == IToken.Kind.MOD) {
            op = t;
            t = lexer.next();
            b = unary();
        }
        if (op == null)
            return a;
        return new BinaryExpr(t, a, op, b);
    }

    // UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr  |  UnaryExprPostfix
    private Expr unary() throws PLCException {
        IToken op = null;
        Expr a = null;
        while (t.getKind() == IToken.Kind.BANG || t.getKind() == IToken.Kind.MINUS || t.getKind() == IToken.Kind.COLOR_OP || t.getKind() == IToken.Kind.IMAGE_OP) {
            op = t;
            t = lexer.next();
            a = unary();
        }
        if (op == null) {
            return unaryPostfix();
        }
        return new UnaryExpr(t, op, a);
    }

    // UnaryExprPostfix::= PrimaryExpr PixelSelector?
    // PixelSelector::= '[' Expr ',' Expr ']'
    private Expr unaryPostfix() throws PLCException {
        //t = lexer.next();
        Expr a = primary();
        t = lexer.next();
        if (t.getKind() == IToken.Kind.EOF)
            return a;
        PixelSelector b = null;
        //while (t.getKind() != IToken.Kind.EOF) {
            if (t.getKind() == IToken.Kind.LSQUARE) {
                b = pixel();
                t = lexer.next();
            }
        //}
        if (b == null)
            return a;
        else
            return new UnaryExprPostfix(t, a, b);
    }

    private Expr primary() throws PLCException {
        switch (t.getKind()) {
            case BOOLEAN_LIT -> {
                return new BooleanLitExpr(t);
            }
            case IDENT -> {
                return new IdentExpr(t);
            }
            case INT_LIT -> {
                return new IntLitExpr(t);
            }
            case FLOAT_LIT -> {
                return new FloatLitExpr(t);
            }
            case STRING_LIT -> {
                return new StringLitExpr(t);
            }
            case LPAREN -> {
                t = lexer.next();
                Expr a = expr();
                if (t.getKind() == IToken.Kind.RPAREN)
                    return a;
                else
                    throw new SyntaxException("No right paren for expr dumbass");
            }
            default -> {
                throw new SyntaxException("it's none of the above. fucktard");
            }
        }
    }

    private PixelSelector pixel() throws PLCException {
            t = lexer.next();
            Expr a = expr(); // expr x
            if (t.getKind() != IToken.Kind.COMMA)
                throw new SyntaxException("Bad PixelSelector. Bad.");
            else {
                t = lexer.next();
                Expr b = expr(); // expr y
                if (t.getKind() != IToken.Kind.RSQUARE)
                    throw new SyntaxException("Very Bad PixelSelector. Very Bad.");
                else {
                    return new PixelSelector(t, a, b);
                }
            }
    }
}
