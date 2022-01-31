package edu.ufl.cise.plc;

import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {
    private enum State {
        START, IN_IDENT, IN_COMMENT, HAVE_ZERO, HAVE_DOT, IN_FLOAT, IN_NUM, HAVE_EQ, HAVE_MINUS, HAVE_LT, HAVE_GT, HAVE_EX};

    private final Map<String, IToken.Kind> reserved = new HashMap<String, IToken.Kind>();

    private State state;
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
        while (col < chars.length) {
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
                            case '#' -> {
                                ss = ss.concat("#");
                                state = State.IN_COMMENT;
                                col++;
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
                                col++;
                                return new Token(IToken.Kind.OR, line,col,ss);
                            }
                            case '&' -> {
                                ss = ss.concat("&");
                                col++;
                                return new Token(IToken.Kind.AND, line,col,ss);
                            }
                            case '(' -> {
                                ss = ss.concat("(");
                                col++;
                                return new Token(IToken.Kind.LPAREN, line,col,ss);
                            }
                            case ')' -> {
                                ss = ss.concat(")");
                                col++;
                                return new Token(IToken.Kind.RPAREN, line,col,ss);
                            }
                            case '[' -> {
                                ss = ss.concat("[");
                                col++;
                                return new Token(IToken.Kind.LSQUARE, line,col,ss);
                            }
                            case ']' -> {
                                ss = ss.concat("]");
                                col++;
                                return new Token(IToken.Kind.RSQUARE, line,col,ss);
                            }
                            case '+' -> {
                                ss = ss.concat("+");
                                col++;
                                return new Token(IToken.Kind.PLUS, line,col,ss);
                            }
                            case '*' -> {
                                ss = ss.concat("*");
                                col++;
                                return new Token(IToken.Kind.TIMES, line,col,ss);
                            }
                            case '/' -> {
                                ss = ss.concat("/");
                                col++;
                                return new Token(IToken.Kind.DIV, line,col,ss);
                            }
                            case '%' -> {
                                ss = ss.concat("%");
                                col++;
                                return new Token(IToken.Kind.MOD, line,col,ss);
                            }
                            case ';' -> {
                                ss = ss.concat(";");
                                col++;
                                return new Token(IToken.Kind.SEMI, line,col,ss);
                            }
                            case ',' -> {
                                ss = ss.concat(",");
                                col++;
                                return new Token(IToken.Kind.COMMA, line,col,ss);
                            }
                            case '^' -> {
                                ss = ss.concat("^");
                                col++;
                                return new Token(IToken.Kind.RETURN, line,col,ss);
                            }
                        }
                    }
                case IN_IDENT:
                    if (Character.isJavaIdentifierPart(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        col++;
                        if (reserved.containsKey(ss))
                            return new Token(reserved.get(ss), line, col, ss);
                        else
                            return new Token(IToken.Kind.IDENT, line, col, ss);
                    }
                case IN_COMMENT:
                    switch (ch) {
                        case '\r', '\n' -> {
                            col++;
                            state = State.START;
                        }
                        default -> col++;
                    }
                case IN_NUM:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        col++;
                        return new Token(IToken.Kind.INT_LIT, line,col,ss);
                    }
                case IN_FLOAT:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                    }
                    else {
                        col++;
                        return new Token(IToken.Kind.FLOAT_LIT, line,col,ss);
                    }
                case HAVE_DOT:
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        col++;
                        state = State.IN_FLOAT;
                    }
                    else {
                        // throw error (?)
                        throw new LexicalException("");
                    }
                case HAVE_EQ:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            col++;
                            return new Token(IToken.Kind.EQUALS, line,col,ss);
                        }
                        default -> {
                            col++;
                            return new Token(IToken.Kind.ASSIGN, line,col,ss);
                        }
                    }
                case HAVE_GT:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            col++;
                            return new Token(IToken.Kind.RANGLE, line,col,ss);
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            col++;
                            return new Token(IToken.Kind.GE, line,col,ss);
                        }
                        default -> {
                            col++;
                            return new Token(IToken.Kind.GT, line,col,ss);
                        }
                    }
                case HAVE_LT:
                    switch(ch){
                        case '<' -> {
                            ss = ss.concat("<");
                            col++;
                            return new Token(IToken.Kind.LANGLE, line,col,ss);
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            col++;
                            return new Token(IToken.Kind.LE, line,col,ss);
                        }
                        case '-' -> {
                            ss = ss.concat("-");
                            col++;
                            return new Token(IToken.Kind.LARROW, line,col,ss);
                        }
                        default -> {
                            col++;
                            return new Token(IToken.Kind.LT, line,col,ss);
                        }
                    }
                case HAVE_MINUS:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            col++;
                            return new Token(IToken.Kind.RARROW, line,col,ss);
                        }
                        default -> {
                            col++;
                            return new Token(IToken.Kind.MINUS, line,col,ss);
                        }
                    }
                case HAVE_EX:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            col++;
                            return new Token(IToken.Kind.NOT_EQUALS, line,col,ss);
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.BANG, line,col,ss);
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
                        col++;
                        return new Token(IToken.Kind.INT_LIT, line,col,ss);
                    }
            }
        }
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
