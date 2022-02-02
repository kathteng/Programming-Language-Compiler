package edu.ufl.cise.plc;

public class Token implements IToken {

    private final Kind kind;
    private SourceLocation s;
    private String str;
    
    public Token(Kind k, int l, int c, String ss){
        kind = k;
        s = new SourceLocation(l,c);
        str = ss;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        StringBuilder strText = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (i == 0 || i == str.length() - 1) {
                if (str.charAt(i) == '\"') {
                    strText.append('"');
                    continue;
                }
            }
            switch (str.charAt(i)) {
                case '\b' -> {
                    strText.append("\\b");
                }
                case '\t' -> {
                    strText.append("\\t");
                }
                case '\n' -> {
                    strText.append("\\n");
                }
                case '\f' -> {
                    strText.append("\\f");
                }
                case '\r' -> {
                    strText.append("\\r");
                }
                case '\"' -> {
                    strText.append("\\\"");
                }
                case '\'' -> {
                    strText.append("\\'");
                }
                case '\\' -> {
                    strText.append("\\\\");
                }
                default -> strText.append(str.charAt(i));
            }
        }
        return strText.toString();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return s;
    }

    @Override
    public int getIntValue() { return Integer.parseInt(str);}

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
        StringBuilder strVal = new StringBuilder(str);
        strVal.deleteCharAt(0);
        strVal.deleteCharAt(strVal.length() - 1);
        return strVal.toString();
    }
}
