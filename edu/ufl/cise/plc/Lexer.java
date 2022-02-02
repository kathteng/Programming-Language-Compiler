package edu.ufl.cise.plc;

import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {
    private enum State {
        START, IN_IDENT, IN_COMMENT, IN_STRING, HAVE_ZERO, HAVE_DOT, IN_FLOAT, IN_NUM,
        HAVE_EQ, HAVE_MINUS, HAVE_LT, HAVE_GT, HAVE_EX, HAVE_BSLASH}

    private final Map<String, IToken.Kind> reserved = new HashMap<String, IToken.Kind>();

    private State state;
    private char[] chars;
    private int pos = 0; // position in input aka the index of chars[]
    private int line = 0; // first char of token position
    private int col = 0; // where the token starts

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
        chars = input.toCharArray();
        initMap();
    }
    @Override
    public IToken next() throws LexicalException {
        int tokenLength = 0; // to keep track where next token starts
        String ss = "";
        state = State.START;
        if (chars.length == 0)
            return new Token(IToken.Kind.EOF, 0, 0, "");

        while (pos < chars.length) {
            char ch = chars[pos];
            switch (state){
                case START -> {
                    if (Character.isJavaIdentifierStart(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        pos++;
                        tokenLength++;
                        state = State.IN_IDENT;
                    } else {
                        switch (ch) {
                            case '0' -> {
                                ss = ss.concat(String.valueOf(ch));
                                pos++;
                                tokenLength++;
                                state = State.HAVE_ZERO;
                            }
                            case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                ss = ss.concat(String.valueOf(ch));
                                pos++;
                                tokenLength++;
                                state = State.IN_NUM;
                            }
                            case ' ', '\t' -> {
                                pos++;
                                col++;
                                state = State.START;
                            }
                            case '\r', '\n' -> {
                                pos++;
                                col = 0;
                                line++;
                                state = State.START;
                            }
                            case '"' -> {
                                ss += '"';
                                pos++;
                                tokenLength++;
                                state = State.IN_STRING;
                            }
                            case '#' -> {
                                ss = ss.concat("#");
                                state = State.IN_COMMENT;
                                pos++;
                            }
                            case '=' -> {
                                ss = ss.concat("=");
                                state = State.HAVE_EQ;
                                pos++;
                                tokenLength++;
                            }
                            case '>' -> {
                                ss = ss.concat(">");
                                state = State.HAVE_GT;
                                pos++;
                                tokenLength++;
                            }
                            case '<' -> {
                                ss = ss.concat("<");
                                state = State.HAVE_LT;
                                pos++;
                                tokenLength++;
                            }
                            case '-' -> {
                                ss = ss.concat("-");
                                state = State.HAVE_MINUS;
                                pos++;
                                tokenLength++;
                            }
                            case '!' -> {
                                ss = ss.concat("!");
                                state = State.HAVE_EX;
                                pos++;
                                tokenLength++;
                            }
                            case '|' -> {
                                ss = ss.concat("|");
                                pos++;
                                Token t = new Token(IToken.Kind.OR, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '&' -> {
                                ss = ss.concat("&");
                                pos++;
                                Token t = new Token(IToken.Kind.AND, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '(' -> {
                                ss = ss.concat("(");
                                pos++;
                                Token t = new Token(IToken.Kind.LPAREN, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case ')' -> {
                                ss = ss.concat(")");
                                pos++;
                                Token t = new Token(IToken.Kind.RPAREN, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '[' -> {
                                ss = ss.concat("[");
                                pos++;
                                Token t = new Token(IToken.Kind.LSQUARE, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case ']' -> {
                                ss = ss.concat("]");
                                pos++;
                                Token t = new Token(IToken.Kind.RSQUARE, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '+' -> {
                                ss = ss.concat("+");
                                pos++;
                                Token t = new Token(IToken.Kind.PLUS, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '*' -> {
                                ss = ss.concat("*");
                                pos++;
                                Token t = new Token(IToken.Kind.TIMES, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '/' -> {
                                ss = ss.concat("/");
                                pos++;
                                Token t = new Token(IToken.Kind.DIV, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '%' -> {
                                ss = ss.concat("%");
                                pos++;
                                Token t = new Token(IToken.Kind.MOD, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case ';' -> {
                                ss = ss.concat(";");
                                pos++;
                                Token t = new Token(IToken.Kind.SEMI, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case ',' -> {
                                ss = ss.concat(",");
                                pos++;
                                Token t = new Token(IToken.Kind.COMMA, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            case '^' -> {
                                ss = ss.concat("^");
                                pos++;
                                Token t = new Token(IToken.Kind.RETURN, line, col, ss);
                                col += tokenLength + 1;
                                return t;
                            }
                            default -> throw new LexicalException("Invalid char", line, col);
                        }
                    }
                }
                case IN_IDENT -> {
                    if (Character.isJavaIdentifierPart(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        pos++;
                        tokenLength++;
                    } else {
                        if (reserved.containsKey(ss)) { // if it's a reserved word
                            Token t = new Token(reserved.get(ss), line, col, ss);
                            col += tokenLength;
                            return t;
                        } else {
                            Token t = new Token(IToken.Kind.IDENT, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case IN_COMMENT -> {
                    switch (ch) {
                        case '\r', '\n' -> {
                            pos++;
                            line++;
                            col = 0;
                            tokenLength = 0;
                            state = State.START;
                        }
                        default -> {
                            pos++;
                            col++;
                        }
                    }
                }
                case IN_STRING -> {
                    switch (ch) {
                        case '\\' -> {
                            pos++;
                            tokenLength++;
                            state = State.HAVE_BSLASH;
                        }
                        case '"' -> { // end of string_lit
                            ss += '\"';
                            pos++;
                            Token t = new Token(IToken.Kind.STRING_LIT, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            pos++;
                            tokenLength++;
                            ss += ch;
                        }
                    }
                }
                // still in string_lit, check what's after '\'
                // if valid escape sequence, go back to IN_STRING
                case HAVE_BSLASH -> {
                    switch (ch) {
                        case 'b' -> {
                            ss += '\b';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case 't' -> {
                            ss += '\t';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case 'n' -> {
                            ss += '\n';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case 'f' -> {
                            ss += '\f';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case 'r' -> {
                            ss += '\r';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case '"' -> {
                            ss += '"';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case '\'' -> {
                            ss += '\'';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        case '\\' -> {
                            ss += '\\';
                            pos++;
                            tokenLength++;
                            state = State.IN_STRING;
                        }
                        default -> throw new LexicalException("Unresolved escape sequence");
                    }
                }
                case IN_NUM -> {
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        pos++;
                        tokenLength++;
                    } else {
                        try {
                            Integer.parseInt(ss);
                        } catch (NumberFormatException e) {
                            throw new LexicalException("Integer too large");
                        }
                        Token t = new Token(IToken.Kind.INT_LIT, line, col, ss);
                        col += tokenLength;
                        return t;
                    }
                }
                case IN_FLOAT -> {
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        pos++;
                        tokenLength++;
                    } else {
                        try {
                            Float.parseFloat(ss);
                        } catch (NumberFormatException e) {
                            throw new LexicalException("Float too large");
                        }
                        Token t = new Token(IToken.Kind.FLOAT_LIT, line, col, ss);
                        col += tokenLength;
                        return t;
                    }
                }
                case HAVE_DOT -> {
                    if (Character.isDigit(ch)) {
                        ss = ss.concat(String.valueOf(ch));
                        pos++;
                        tokenLength++;
                        state = State.IN_FLOAT;
                    } else {
                        // throw error (?)
                        throw new LexicalException("Invalid Token", line, col);
                    }
                }
                case HAVE_EQ -> {
                    switch (ch) {
                        case '=' -> {
                            ss = ss.concat("=");
                            pos++;
                            Token t = new Token(IToken.Kind.EQUALS, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.ASSIGN, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case HAVE_GT -> {
                    switch (ch) {
                        case '>' -> {
                            ss = ss.concat(">");
                            pos++;
                            Token t = new Token(IToken.Kind.RANGLE, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            pos++;
                            Token t = new Token(IToken.Kind.GE, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.GT, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case HAVE_LT -> {
                    switch (ch) {
                        case '<' -> {
                            ss = ss.concat("<");
                            pos++;
                            Token t = new Token(IToken.Kind.LANGLE, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            pos++;
                            Token t = new Token(IToken.Kind.LE, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        case '-' -> {
                            ss = ss.concat("-");
                            pos++;
                            Token t = new Token(IToken.Kind.LARROW, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.LT, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case HAVE_MINUS -> {
                    switch (ch) {
                        case '>' -> {
                            ss = ss.concat(">");
                            pos++;
                            Token t = new Token(IToken.Kind.RARROW, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.MINUS, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case HAVE_EX -> {
                    switch (ch) {
                        case '=' -> {
                            ss = ss.concat("=");
                            pos++;
                            Token t = new Token(IToken.Kind.NOT_EQUALS, line, col, ss);
                            col += tokenLength + 1;
                            return t;
                        }
                        default -> {
                            Token t = new Token(IToken.Kind.BANG, line, col, ss);
                            col += tokenLength;
                            return t;
                        }
                    }
                }
                case HAVE_ZERO -> {
                    if (ch == '.') {
                        ss = ss.concat(".");
                        pos++;
                        tokenLength++;
                        state = State.HAVE_DOT;
                    } else {
                        Token t = new Token(IToken.Kind.INT_LIT, line, col, ss);
                        col += tokenLength;
                        return t;
                    }
                }
                default -> throw new LexicalException("Lexer bug");
            }
        }
        return new Token(IToken.Kind.EOF, line, col, "");
    }

    @Override
    public IToken peek() throws LexicalException {
        int pos_ = pos;
        int col_ = col;
        int line_ = line;
        IToken a = next();
        pos = pos_;
        col = col_;
        line = line_;
        return a;
    }
}
