package edu.ufl.cise.plc;

public class Token implements IToken {

    public Kind kind;
    SourceLocation s;
    String str;
    
    public Token(int l, int c, String ss){
        s = new SourceLocation(l,c);
        str = ss;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return str;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return s;
    }

    @Override
    public int getIntValue() {
        return Integer.parseInt(str);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(str);
    }

    @Override
    public boolean getBooleanValue() {
        if (str.equals("true"))
            return true;
        else
            return false;
    }

    @Override
    public String getStringValue() {
        return str;
    }
}
