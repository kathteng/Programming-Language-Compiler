package edu.ufl.cise.plc;

public class Lexer implements ILexer {
    private enum State {START, IDENT, INT_LIT, FLOAT_LIT, STRING_LIT, HAVE_EQ, HAVE_GT, HAVE_LT, HAVE_MINUS, HAVE_PLUS, HAVE_EX};
    State state;
    private char chars[];
    private int pos = 0;

    public Lexer(String input) {
        chars = new char[input.length()];
    }
    @Override
    public IToken next() throws LexicalException {
        int l;
        int c;
        String ss;
        int temppos = pos;
        state = State.START;
        while (true){
            char ch = chars[temppos];
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
                            Token t = new Token(,ss);
                            t.kind = Token.Kind.EQUALS;
                    }
                case HAVE_GT:
                case HAVE_LT:
                case HAVE_MINUS:
                case HAVE_PLUS:
                case HAVE_EX:
            }
            temppos++;
        }
        pos = temppos;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
