package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.List;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

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
        return program();
    }

    private Program program() throws PLCException {
        if (t.getKind() == Kind.TYPE || t.getKind() == Kind.KW_VOID) {
            // convert to Type
            Type returnType = Types.Type.toType(t.getText());
            IToken firstToken = t;

            t = lexer.next();
            if (t.getKind() != Kind.IDENT)
                throw new SyntaxException("you dumb bitch");
            
            String name = t.getText();
            t = lexer.next();
            if (t.getKind() != Kind.LPAREN)
                throw new SyntaxException("you dumb bitch pt 2");
            
            t = lexer.next();
            if (t.getKind() == Kind.EOF)
                throw new SyntaxException("you dumb bitch pt 3");
            
            List<NameDef> params = new ArrayList<>();
            List<ASTNode> decsNStatements = new ArrayList<>();
            if (t.getKind() != Kind.RPAREN) {
                params.add(nameDef());
                t = lexer.next();
                while (t.getKind() == Kind.COMMA) {
                    t = lexer.next();
                    if (t.getKind() == Kind.EOF || t.getKind() == Kind.RPAREN)
                        throw new SyntaxException("comma cant end it all my dude");
                    
                    params.add(nameDef());
                    t = lexer.next();
                }
            }
            //else {
                t = lexer.next();
                while (t.getKind() != Kind.EOF) {
                    if (t.getKind() == Kind.TYPE) {
                        decsNStatements.add(declare());
                    }
                    else
                        decsNStatements.add(statement());
                    
                    if (t.getKind() != Kind.SEMI)
                        throw new SyntaxException("no semicolon dumbass");
                    t = lexer.next();
                }
            //}
            return new Program(firstToken, returnType, name, params, decsNStatements);
        }
        else
            throw new SyntaxException("this can't work out man i'm sowwy");
    }

    private Declaration declare() throws PLCException {
        IToken firstToken = t;
        NameDef nameDef = nameDef();
        IToken op = null;
        Expr a = null;
        t = lexer.next();
        if (t.getKind() == Kind.ASSIGN || t.getKind() == Kind.LARROW) {
            op = t;
            t = lexer.next();
            a = expr();
        }
        return new VarDeclaration(firstToken, nameDef, op, a);
    }

    private NameDef nameDef() throws PLCException {
        if (t.getKind() == Kind.TYPE) {
            IToken type = t;
            IToken name;
            t = lexer.next();
            if (t.getKind() == Kind.IDENT) {
                name = t;
                return new NameDef(type, type, name);
            }
            if (t.getKind() == Kind.LSQUARE) {
                IToken firstToken = t;
                t = lexer.next();
                if (t.getKind() == Kind.EOF)
                    throw new SyntaxException("there's nothing here dude");
                Expr width = expr();
                if (t.getKind() != IToken.Kind.COMMA)
                    throw new SyntaxException("Bad Dimension. Bad.");
                else {
                    t = lexer.next();
                    Expr height = expr();
                    if (t.getKind() != IToken.Kind.RSQUARE)
                        throw new SyntaxException("Very Bad Dimension. Very Bad.");
                    
                    Dimension dim = new Dimension(firstToken, width, height);
                    t = lexer.next();
                    if (t.getKind() == Kind.IDENT)
                        return new NameDefWithDim(type, type, t, dim);
                    else
                        throw new SyntaxException("ya can't do that");
                }
            }
            else
                throw new SyntaxException("ayo the thing after ain't right dumbass");
        }
        return null;
    }

    private Statement statement() throws PLCException {
        IToken firstToken = t;
        Expr e;
        if (t.getKind() == Kind.IDENT) {
            e = new IdentExpr(t);
            t = lexer.next();
            PixelSelector p = null;
            if (t.getKind() == Kind.LSQUARE) {
                p = pixel();
                t = lexer.next();
            }
            
            //t = lexer.next();
            if (t.getKind() == Kind.ASSIGN) {
                t = lexer.next();
                Expr expr = expr();
                return new AssignmentStatement(firstToken, e.getText(), p, expr);
            }
            else if (t.getKind() == Kind.LARROW) {
                t = lexer.next();
                Expr expr = expr();
                return new ReadStatement(firstToken, e.getText(), p, expr);
            }
        }
        else if (t.getKind() == Kind.RETURN) {
            t = lexer.next();
            e = expr();
            return new ReturnStatement(firstToken, e);
        }
        else if (t.getKind() == Kind.KW_WRITE) {
            t = lexer.next();
            e = expr();
            
            if (t.getKind() == Kind.RARROW)
                t = lexer.next();
            else
                throw new SyntaxException("no right arrow dumbass");
            Expr dest = expr();
            return new WriteStatement(firstToken, e, dest);
        }
        return null;
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
        IToken firstToken = t;
        t = lexer.next();
        if (t.getKind() == IToken.Kind.LPAREN) {
            t = lexer.next();
            Expr condition = expr();
            if (t.getKind() != IToken.Kind.RPAREN)
                throw new SyntaxException("No right paren for condition dumbass");
            t = lexer.next();
            Expr trueCase = expr();
            if (t.getKind() != IToken.Kind.KW_ELSE)
                throw new SyntaxException("No else dumbass");
            t = lexer.next();
            Expr falseCase = expr();
            if (t.getKind() != IToken.Kind.KW_FI)
                throw new SyntaxException("No fi dumbass");
                         
            ConditionalExpr c = new ConditionalExpr(firstToken, condition, trueCase, falseCase);
            t = lexer.next();
            return c;
        }
        else
            throw new SyntaxException("No left paren for condition dumbass");
    }

    private Expr logicalOr() throws PLCException {
        IToken firstToken = t;
        Expr a = logicalAnd();
        Expr b = null;
        IToken op = null;
        BinaryExpr binary = null;
        if (t.getKind() == IToken.Kind.OR) {
            op = t;
            t = lexer.next();
            b = logicalAnd();
            binary = new BinaryExpr(firstToken, a, op, b);
        }
        while (t.getKind() == IToken.Kind.OR) {
            op = t;
            t = lexer.next();
            b = logicalAnd();
            binary = new BinaryExpr(firstToken, binary, op, b);
        }
        if (op == null)
            return a;
        return binary;
    }

    private Expr logicalAnd() throws PLCException {
        IToken firstToken = t;
        Expr a = comparison();
        Expr b = null;
        IToken op = null;
        BinaryExpr binary = null;
        if (t.getKind() == IToken.Kind.AND) {
            op = t;
            t = lexer.next();
            b = comparison();
            binary = new BinaryExpr(firstToken, a, op, b);
        }
        while (t.getKind() == IToken.Kind.AND) {
            op = t;
            t = lexer.next();
            b = comparison();
            binary = new BinaryExpr(firstToken, binary, op, b);
        }
        if (op == null)
            return a;
        return binary;
    }

    private Expr comparison() throws PLCException {
        IToken firstToken = t;
        Expr a = additive();
        Expr b = null;
        IToken op = null;
        BinaryExpr binary = null;
        if (t.getKind() == Kind.LE||t.getKind() == Kind.LT||t.getKind() == Kind.EQUALS||t.getKind() == Kind.GE||t.getKind() == Kind.GT||t.getKind() == Kind.NOT_EQUALS) {
            op = t;
            t = lexer.next();
            b = additive();
            binary = new BinaryExpr(firstToken, a, op, b);
        }
        while (t.getKind() == Kind.LE||t.getKind() == Kind.LT||t.getKind() == Kind.EQUALS||t.getKind() == Kind.GE||t.getKind() == Kind.GT||t.getKind() == Kind.NOT_EQUALS) {
            op = t;
            t = lexer.next();
            b = additive();
            binary = new BinaryExpr(firstToken, binary, op, b);
        }
        if (op == null)
            return a;
        return binary;
    }

    private Expr additive() throws PLCException {
        IToken firstToken = t;
        Expr a = multipl();
        Expr b = null;
        IToken op = null;
        BinaryExpr binary = null;
        if (t.getKind() == IToken.Kind.PLUS || t.getKind() == IToken.Kind.MINUS) {
            op = t;
            t = lexer.next();
            b = multipl();
            binary = new BinaryExpr(firstToken, a, op, b);
        }
        while (t.getKind() == IToken.Kind.PLUS || t.getKind() == IToken.Kind.MINUS) {
            op = t;
            t = lexer.next();
            b = multipl();
            binary = new BinaryExpr(firstToken, binary, op, b);
        }
        if (op == null)
            return a;
        return binary;
    }

    private Expr multipl() throws PLCException {
        IToken firstToken = t;
        Expr a = unary();
        Expr b = null;
        IToken op = null;
        BinaryExpr binary = null;
        if (t.getKind() == IToken.Kind.TIMES || t.getKind() == IToken.Kind.DIV || t.getKind() == IToken.Kind.MOD) {
            op = t;
            t = lexer.next();
            b = unary();
            binary = new BinaryExpr(firstToken, a, op, b);
        }
        while (t.getKind() == IToken.Kind.TIMES || t.getKind() == IToken.Kind.DIV || t.getKind() == IToken.Kind.MOD) {
            op = t;
            t = lexer.next();
            b = unary();
            binary = new BinaryExpr(firstToken, binary, op, b);
        }
        if (op == null)
            return a;
        return binary;
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
        return new UnaryExpr(op, op, a);
    }

    // UnaryExprPostfix::= PrimaryExpr PixelSelector?
    // PixelSelector::= '[' Expr ',' Expr ']'
    private Expr unaryPostfix() throws PLCException {
        IToken firstToken = t;
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
            return new UnaryExprPostfix(firstToken, a, b);
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
            case COLOR_CONST -> {
                return new ColorConstExpr(t);
            }
            case LANGLE -> {
                IToken firstToken = t;
                t = lexer.next();
                Expr a, b, c;
                if (t.getKind() != Kind.EOF)
                    a = expr();
                else
                    throw new SyntaxException("bruh why");

                if (t.getKind() == Kind.COMMA) {
                    t = lexer.next();
                    b = expr();
                }
                else
                    throw new SyntaxException("bruh why x2");
                
                if (t.getKind() == Kind.COMMA) {
                    t = lexer.next();
                    c = expr();
                }
                else
                    throw new SyntaxException("bruh why x3");
                
                if (t.getKind() == Kind.RANGLE)
                    return new ColorExpr(firstToken, a, b, c);
                else
                    throw new SyntaxException("ya forgot rangle dumbass");
            }
            case KW_CONSOLE -> {
                return new ConsoleExpr(t);
            }
            default -> {
                throw new SyntaxException("it's none of the above lil dumdum");
            }
        }
    }

    private PixelSelector pixel() throws PLCException {
        IToken firstToken = t;
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
                return new PixelSelector(firstToken, a, b);
            }
        }
    }
}
