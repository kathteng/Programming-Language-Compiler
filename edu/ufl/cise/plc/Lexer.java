package edu.ufl.cise.plc;

public class Lexer implements ILexer {
    private String str;

    public Lexer(String input) {
        this.str = input;
    }
    @Override
    public IToken next() throws LexicalException {
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
