package edu.ufl.cise.plc;

public class Lexer implements ILexer {
    private enum State {START, IDENT, INT_LIT, FLOAT_LIT, STRING_LIT, HAVE_EQ, HAVE_GT, HAVE_LT, HAVE_MINUS, HAVE_EX, HAVE_ZERO};
    State state;
    private char chars[];
    private int line = 0;
    private int col = 0;

    public Lexer(String input) {
        chars = new char[input.length()];
    }
    @Override
    public IToken next() throws LexicalException {
        String ss = "";
        int tokencol = col;
        state = State.START;
        while (true){
            char ch = chars[col];
            switch (state){
                case START:
                    switch(ch){
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
                    }
                case IDENT:
                case INT_LIT:
                case FLOAT_LIT:
                case STRING_LIT:
                case HAVE_EQ:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.EQUALS;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.ASSIGN;
                            return t;
                        }
                    }
                case HAVE_GT:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.RANGLE;
                            col++;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.GE;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.GT;
                            return t;
                        }
                    }
                case HAVE_LT:
                    switch(ch){
                        case '<' -> {
                            ss = ss.concat("<");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.LANGLE;
                            col++;
                            return t;
                        }
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.LE;
                            col++;
                            return t;
                        }
                        case '-' -> {
                            ss = ss.concat("-");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.LARROW;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.LT;
                            return t;
                        }
                    }
                case HAVE_MINUS:
                    switch(ch){
                        case '>' -> {
                            ss = ss.concat(">");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.RARROW;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.MINUS;
                            return t;
                        }
                    }
                case HAVE_EX:
                    switch(ch){
                        case '=' -> {
                            ss = ss.concat("=");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.NOT_EQUALS;
                            col++;
                            return t;
                        }
                        default -> {
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.BANG;
                            return t;
                        }
                    }
                case HAVE_ZERO:
            }
        }
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
