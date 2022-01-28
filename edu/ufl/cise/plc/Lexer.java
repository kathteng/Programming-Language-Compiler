package edu.ufl.cise.plc;

public class Lexer implements ILexer {
    private enum State {START, IDENT, INT_LIT, FLOAT_LIT, STRING_LIT, HAVE_EQ, HAVE_GT, HAVE_LT, HAVE_MINUS, HAVE_PLUS, HAVE_EX};
    State state;
    private char chars[];
    private int line = 0;
    private int col = 0;

    public Lexer(String input) {
        chars = new char[input.length()];
    }
    @Override
    public IToken next() throws LexicalException {
        String ss;
        int tokencol = col;
        state = State.START;
        while (true){
            char ch = chars[col];
            switch (state){
                case START:
                    switch(ch){
                        
                    }
                case IDENT:
                case INT_LIT:
                case FLOAT_LIT:
                case STRING_LIT:
                case HAVE_EQ:
                    switch(ch){
                        case '=':
                            ss = ss.concat("=");
                            Token t = new Token(line,tokencol,ss);
                            t.kind = Token.Kind.EQUALS;
                            return t;
                    }
                case HAVE_GT:
                case HAVE_LT:
                case HAVE_MINUS:
                case HAVE_PLUS:
                case HAVE_EX:
            }
            pos++;
        }
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
