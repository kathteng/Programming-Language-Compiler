package edu.ufl.cise.plc;

import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {
    private enum State {
        START, IN_IDENT, HAVE_ZERO, HAVE_DOT, IN_FLOAT, IN_NUM, HAVE_EQ, HAVE_MINUS, HAVE_LT, HAVE_GT, HAVE_EX};

    private Map<String, IToken.Kind> reserved = new HashMap<String, IToken.Kind>();

    State state;
    private char chars[];
    private int line = 0;
    private int col = 0;

    private void initMap() {
        reserved.put("string", IToken.Kind.TYPE);
        reserved.put("int", IToken.Kind.TYPE);
        reserved.put("float", IToken.Kind.TYPE);
        reserved.put("boolean", IToken.Kind.TYPE);
        reserved.put("color", IToken.Kind.TYPE);
        reserved.put("image", IToken.Kind.TYPE);

        reserved.put("getWidth", IToken.Kind.IMAGE_OP);
        reserved.put("getHeight", IToken.Kind.IMAGE_OP);

        reserved.put("getRed", IToken.Kind.COLOR_OP);
        reserved.put("getGreen", IToken.Kind.COLOR_OP);
        reserved.put("getBlue", IToken.Kind.COLOR_OP);

        reserved.put("BLACK", IToken.Kind.COLOR_CONST);
        reserved.put("BLUE", IToken.Kind.COLOR_CONST);
        reserved.put("CYAN", IToken.Kind.COLOR_CONST);
        reserved.put("DARK_GRAY", IToken.Kind.COLOR_CONST);
        reserved.put("GRAY", IToken.Kind.COLOR_CONST);
        reserved.put("GREEN", IToken.Kind.COLOR_CONST);
        reserved.put("LIGHT_GRAY", IToken.Kind.COLOR_CONST);
        reserved.put("MAGENTA", IToken.Kind.COLOR_CONST);
        reserved.put("ORANGE", IToken.Kind.COLOR_CONST);
        reserved.put("PINK", IToken.Kind.COLOR_CONST);
        reserved.put("RED", IToken.Kind.COLOR_CONST);
        reserved.put("WHITE", IToken.Kind.COLOR_CONST);
        reserved.put("YELLOW", IToken.Kind.COLOR_CONST);

        reserved.put("true", IToken.Kind.BOOLEAN_LIT);
        reserved.put("false", IToken.Kind.BOOLEAN_LIT);

        reserved.put("if", IToken.Kind.KW_IF);
        reserved.put("else", IToken.Kind.KW_ELSE);
        reserved.put("fi", IToken.Kind.KW_FI);
        reserved.put("write", IToken.Kind.KW_WRITE);
        reserved.put("console", IToken.Kind.KW_CONSOLE);
        reserved.put("void", IToken.Kind.KW_VOID);
    }
    public Lexer(String input) {
        chars = new char[input.length()];
        initMap();
    }
    @Override
    public IToken next() throws LexicalException {
        String ss = "";
        state = State.START;
        while (true){
            char ch = chars[col];
            switch (state){
                case START:
                    if (Character.isJavaIdentifierStart(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                        state = State.IN_IDENT;
                    }
                    else {
                        switch(ch){
                            case ' ', '\r', '\t', '\n' -> {
                                col++;
                                state = State.START;
                            }
                            case '=' -> {
                                ss = ss.concat("=");
                                state = State.HAVE_EQ;
                                col++;
                            }
                            case '>' -> {
                                ss = ss.concat(">");
                                state = State.HAVE_GT;
                                col++;
                            }
                            case '<' -> {
                                ss = ss.concat("<");
                                state = State.HAVE_LT;
                                col++;
                            }
                            case '-' -> {
                                ss = ss.concat("-");
                                state = State.HAVE_MINUS;
                                col++;
                            }
                            case '!' -> {
                                ss = ss.concat("!");
                                state = State.HAVE_EX;
                                col++;
                            }
                            case '|' -> {
                                ss = ss.concat("|");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.OR;
                                col++;
                            }
                            case '&' -> {
                                ss = ss.concat("&");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.AND;
                                col++;
                            }
                            case '(' -> {
                                ss = ss.concat("(");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.LPAREN;
                                col++;
                            }
                            case ')' -> {
                                ss = ss.concat(")");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.RPAREN;
                                col++;
                            }
                            case '[' -> {
                                ss = ss.concat("[");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.LSQUARE;
                                col++;
                            }
                            case ']' -> {
                                ss = ss.concat("]");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.RSQUARE;
                                col++;
                            }
                            case '+' -> {
                                ss = ss.concat("+");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.PLUS;
                                col++;
                            }
                            case '*' -> {
                                ss = ss.concat("*");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.TIMES;
                                col++;
                            }
                            case '/' -> {
                                ss = ss.concat("/");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.DIV;
                                col++;
                            }
                            case '%' -> {
                                ss = ss.concat("%");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.MOD;
                                col++;
                            }
                            case ';' -> {
                                ss = ss.concat(";");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.SEMI;
                                col++;
                            }
                            case ',' -> {
                                ss = ss.concat(",");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.COMMA;
                                col++;
                            }
                            case '^' -> {
                                ss = ss.concat("^");
                                Token t = new Token(line,col,ss);
                                t.kind = IToken.Kind.RETURN;
                                col++;
                            }
                        }
                    }
                case IN_IDENT:
                    if (Character.isJavaIdentifierPart(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        Token t = new Token(line,col,ss);
                        //TODO: check if in reserved
                        col++;
                        return t;
                    }
                case IN_NUM:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        Token t = new Token(line,col,ss);
                        t.kind = IToken.Kind.INT_LIT;
                        col++;
                        return t;
                    }
                case IN_FLOAT:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        Token t = new Token(line,col,ss);
                        t.kind = IToken.Kind.FLOAT_LIT;
                        col++;
                        return t;
                    }
                case HAVE_DOT:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                        state = State.IN_FLOAT;
                    }
                    else {
                        // throw error (?)
                    }
                case HAVE_EQ:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.EQUALS;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.ASSIGN;
                            return t;
                        }
                    }
                case HAVE_GT:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.RANGLE;
                            col++;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.GE;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.GT;
                            return t;
                        }
                    }
                case HAVE_LT:
                    switch(ch){
                        case '<' -> {
                            ss = ss.concat("<");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.LANGLE;
                            col++;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.LE;
                            col++;
                            return t;
                        }
                        case '-' -> {
                            ss = ss.concat("-");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.LARROW;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.LT;
                            return t;
                        }
                    }
                case HAVE_MINUS:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.RARROW;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.MINUS;
                            return t;
                        }
                    }
                case HAVE_EX:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.NOT_EQUALS;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,col,ss);
                            t.kind = Token.Kind.BANG;
                            return t;
                        }
                    }
                case HAVE_ZERO:
                    if (ch == '.') {
                        ss = ss.concat(".");
                        col++;
                        state = State.HAVE_DOT;
                    }
                    else {
                        Token t = new Token(line,col,ss);
                        t.kind = IToken.Kind.INT_LIT;
                        return t;
                    }
            }
        }
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
