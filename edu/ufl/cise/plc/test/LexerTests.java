package edu.ufl.cise.plc.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plc.CompilerComponentFactory;
import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.LexicalException;

import java.util.Arrays;


public class LexerTests {

	ILexer getLexer(String input){
		 return CompilerComponentFactory.getLexer(input);
	}
	
	//makes it easy to turn output on and off (and less typing than System.out.println)
	static final boolean VERBOSE = true;
	void show(Object obj) {
		if(VERBOSE) {
			System.out.println(obj);
		}
	}
	
	//check that this token has the expected kind
	void checkToken(IToken t, Kind expectedKind) {
		assertEquals(expectedKind, t.getKind());
	}
		
	//check that the token has the expected kind and position
	void checkToken(IToken t, Kind expectedKind, int expectedLine, int expectedColumn){
		assertEquals(expectedKind, t.getKind());
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}
	
	//check that this token is an IDENT and has the expected name
	void checkIdent(IToken t, String expectedName){
		assertEquals(Kind.IDENT, t.getKind());
		assertEquals(expectedName, t.getText());
	}
	
	//check that this token is an IDENT, has the expected name, and has the expected position
	void checkIdent(IToken t, String expectedName, int expectedLine, int expectedColumn){
		checkIdent(t,expectedName);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}
	
	//check that this token is an INT_LIT with expected int value
	void checkInt(IToken t, int expectedValue) {
		assertEquals(Kind.INT_LIT, t.getKind());
		assertEquals(expectedValue, t.getIntValue());	
	}
	
	//check that this token  is an INT_LIT with expected int value and position
	void checkInt(IToken t, int expectedValue, int expectedLine, int expectedColumn) {
		checkInt(t,expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());		
	}

	//check that this token is an FLOAT_LIT with expected float value
	void checkFloat(IToken t, float expectedValue) {
		assertEquals(Kind.FLOAT_LIT, t.getKind());
		assertEquals(expectedValue, t.getFloatValue());
	}

	// check that this token  is an FLOAT_LIT with expected float value and position
	void checkFloat(IToken t, float expectedValue, int expectedLine, int expectedColumn) {
		checkFloat(t,expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}

	void checkBool(IToken t, Boolean expectedValue) {
		assertEquals(Kind.BOOLEAN_LIT, t.getKind());
		assertEquals(expectedValue, t.getBooleanValue());
	}

	void checkBool(IToken t, Boolean expectedValue, int expectedLine, int expectedColumn) {
		checkBool(t,expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}

	//check that this token is the EOF token
	void checkEOF(IToken t) {
		checkToken(t, Kind.EOF);
	}
	
	
	//The lexer should add an EOF token to the end.
	@Test
	void testEmpty() throws LexicalException {
		String input = "";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}
	
	//A couple of single character tokens
	@Test
	void testSingleChar0() throws LexicalException {
		String input = """
				+ 
				- 	 
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.PLUS, 0,0);
		checkToken(lexer.next(), Kind.MINUS, 1,0);
		checkEOF(lexer.next());
	}
	
	//comments should be skipped
	@Test
	void testComment0() throws LexicalException {
		//Note that the quotes around "This is a string" are passed to the lexer.  
		String input = """
				"This is a string"
				#this is a comment
				*
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0,0);
		checkToken(lexer.next(), Kind.TIMES, 2,0);
		checkEOF(lexer.next());
	}
	
	//Example for testing input with an illegal character 
	@Test
	void testError0() throws LexicalException {
		String input = """
				abc
				@
				""";
		show(input);
		ILexer lexer = getLexer(input);
		//this check should succeed
		checkIdent(lexer.next(), "abc");
		//this is expected to throw an exception since @ is not a legal 
		//character unless it is part of a string or comment

		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
			assertEquals(Kind.ERROR, token.getKind());
		});
	}
	
	//Several identifiers to test positions
	@Test
	public void testIdent0() throws LexicalException {
		String input = """
				abc
				  def
				     ghi

				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.peek(), "abc", 0,0);
		checkIdent(lexer.next(), "abc", 0,0);
		checkIdent(lexer.peek(), "def", 1,2);
		checkIdent(lexer.next(), "def", 1,2);
		checkIdent(lexer.next(), "ghi", 2,5);
		checkEOF(lexer.next());
	}
	
	
	@Test
	public void testEquals0() throws LexicalException {
		String input = """
				= == ===
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(),Kind.ASSIGN,0,0);
		checkToken(lexer.next(),Kind.EQUALS,0,2);
		checkToken(lexer.next(),Kind.EQUALS,0,5);
		checkToken(lexer.next(),Kind.ASSIGN,0,7);
		checkEOF(lexer.next());
	}
	
	@Test
	public void testIdenInt() throws LexicalException {
		String input = """
				a123 456b
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 0,0);
		checkInt(lexer.next(), 456, 0,5);
		checkIdent(lexer.next(), "b",0,8);
		checkEOF(lexer.next());
		}
	
	
	//example showing how to handle number that are too big.
	@Test
	public void testIntTooBig() throws LexicalException {
		String input = """
				42
				99999999999999999999999999999999999999999999999999999999999999999999999
				""";
		ILexer lexer = getLexer(input);
		checkInt(lexer.next(),42);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();			
		});
	}

	String getASCII(String s) {
		int[] ascii = new int[s.length()];
		for (int i = 0; i != s.length(); i++) {
			ascii[i] = s.charAt(i);
		}
		return Arrays.toString(ascii);
	}
	@Test
	public void testEscapeSequences0() throws LexicalException {
		String input = "\"\\b \\t \\n \\f \\r \"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = "\b \t \n \f \r ";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\"\\b \\t \\n \\f \\r \"";
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
	}

	@Test
	public void testEscapeSequences1() throws LexicalException {
		String input = "   \" ...  \\\"  \\\'  \\\\  \"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = " ...  \"  \'  \\  ";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\" ...  \\\"  \\\'  \\\\  \""; //almost the same as input, but white space is omitted
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
	}

	@Test
	void testComment1() throws LexicalException {
		//Note that the quotes around "This is a string" are passed to the lexer.  
		String input = """
				"This is a string"
				#this is a comment
				*
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.peek(), Kind.STRING_LIT, 0,0);
		checkToken(lexer.next(), Kind.STRING_LIT, 0,0);
		checkToken(lexer.peek(), Kind.TIMES, 2,0);
		checkToken(lexer.next(), Kind.TIMES, 2,0);
		checkEOF(lexer.next());
	}

	@Test
	public void testReserve() throws LexicalException {
		String input = """
				a123 int
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 0,0);
		checkToken(lexer.next(), Kind.TYPE, 0,5);
		checkEOF(lexer.next());
	}

	@Test
	public void testError1() throws LexicalException {
		String input = """
				0.12.2
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkFloat(lexer.next(), 0.12f,0,0);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	public void testError2() throws LexicalException {
		String input = """
				1.
				""";
		show(input);
		ILexer lexer = getLexer(input);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	public void checkBool() throws LexicalException {
		String input = """
				true
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkBool(lexer.next(), true,0,0);
	}

	void checkToken1(IToken t, Kind expectedKind, int expectedLine, int expectedColumn, String expectedText){
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedText, t.getText());
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}


	@Test
	void testReservedWords() throws LexicalException {
		String input = """
			string CYAN
			int
			float
			boolean
			color
			image
			void
			getWidth
			getHeight
			getRed
			getGreen
			getBlue
			BLACK
			BLUE
			CYAN
			DARK_GRAY
			GRAY
			GREEN
			LIGHT_GRAY
			MAGENTA
			ORANGE
			PINK
			RED
			WHITE
			YELLOW
			true
			false
			if
			else
			fi
			write
			console	 
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken1(lexer.next(), Kind.TYPE,		    0, 0, "string");
		checkToken1(lexer.next(), Kind.COLOR_CONST,    0, 7, "CYAN");
		checkToken1(lexer.next(), Kind.TYPE,		    1, 0, "int");
		checkToken1(lexer.next(), Kind.TYPE,		    2, 0, "float");
		checkToken1(lexer.next(), Kind.TYPE,		    3, 0, "boolean");
		checkToken1(lexer.next(), Kind.TYPE,		    4, 0, "color");
		checkToken1(lexer.next(), Kind.TYPE,		    5, 0, "image");
		checkToken1(lexer.next(), Kind.KW_VOID,	    6, 0, "void");
		checkToken1(lexer.next(), Kind.IMAGE_OP,	    7, 0, "getWidth");
		checkToken1(lexer.next(), Kind.IMAGE_OP,	    8, 0, "getHeight");
		checkToken1(lexer.next(), Kind.COLOR_OP,	    9, 0, "getRed");
		checkToken1(lexer.next(), Kind.COLOR_OP,	    10, 0, "getGreen");
		checkToken1(lexer.next(), Kind.COLOR_OP,	    11, 0, "getBlue");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	12, 0, "BLACK");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	13, 0, "BLUE");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	14, 0, "CYAN");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	15, 0, "DARK_GRAY");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	16, 0, "GRAY");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	17, 0, "GREEN");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	18, 0, "LIGHT_GRAY");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	19, 0, "MAGENTA");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	20, 0, "ORANGE");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	21, 0, "PINK");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	22, 0, "RED");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	23, 0, "WHITE");
		checkToken1(lexer.next(), Kind.COLOR_CONST,	24, 0, "YELLOW");
		checkToken1(lexer.next(), Kind.BOOLEAN_LIT,	25, 0, "true");
		checkToken1(lexer.next(), Kind.BOOLEAN_LIT,	26, 0, "false");
		checkToken1(lexer.next(), Kind.KW_IF,        27, 0, "if");
		checkToken1(lexer.next(), Kind.KW_ELSE,	    28, 0, "else");
		checkToken1(lexer.next(), Kind.KW_FI,	    29, 0, "fi");
		checkToken1(lexer.next(), Kind.KW_WRITE,	    30, 0, "write");
		checkToken1(lexer.next(), Kind.KW_CONSOLE,	31, 0, "console");
		checkEOF(lexer.next());
	}

	@Test
	void testIntFloatError() throws LexicalException {
		String input = """
			0.32
			00.15
			10.030.32
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkFloat(lexer.next(), (float) 0.32,	0, 0);
		checkInt(lexer.next(), 0, 			1, 0);
		checkFloat(lexer.next(), (float) 0.15,	1, 1);
		checkFloat(lexer.next(), (float) 10.030,	2, 0);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	public void testStringErrorEOF() throws LexicalException {
		String input = """
                "good"
                "test
   
                """;
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	void testError3() throws LexicalException {
		String input = """
			abc
			00.4
			123
			_Name1
			_1@
			""";
		show(input);
		ILexer lexer = getLexer(input);
		//these checks should succeed
		checkIdent(lexer.next(), "abc");
		checkInt(lexer.next(), 0, 1,0);
		checkToken(lexer.peek(), Kind.FLOAT_LIT, 1, 1);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 1);
		checkToken(lexer.next(), Kind.INT_LIT, 2,0);
		checkIdent(lexer.next(), "_Name1", 3, 0);
		checkIdent(lexer.next(), "_1", 4, 0);
		//this is expected to throw an exception since @ is not a legal
		//character unless it is part of a string or comment
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	void testMany() throws LexicalException {
		String input = """
			[
			int a = 28.3 * 55.597;
			string _b1 = "testing \\nstring";
			boolean c$5 = true;
			]
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.LSQUARE, 0,0);
		checkToken1(lexer.next(), Kind.TYPE, 1,0, "int");
		checkToken1(lexer.next(), Kind.IDENT, 1,4, "a");
		checkToken(lexer.next(), Kind.ASSIGN, 1,6);
		checkToken1(lexer.next(), Kind.FLOAT_LIT, 1, 8, "28.3");
		checkToken(lexer.next(), Kind.TIMES, 1,13);
		checkToken1(lexer.next(), Kind.FLOAT_LIT, 1, 15, "55.597");
		checkToken(lexer.next(), Kind.SEMI, 1,21);
		checkToken1(lexer.next(), Kind.TYPE, 2,0, "string");
		checkToken1(lexer.next(), Kind.IDENT, 2,7, "_b1");
		checkToken(lexer.next(), Kind.ASSIGN, 2,11);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = "testing \nstring";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\"testing \\nstring\"";
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
		checkToken(lexer.next(), Kind.SEMI, 2,31);
		checkToken1(lexer.next(), Kind.TYPE, 3,0, "boolean");
		checkToken1(lexer.next(), Kind.IDENT, 3,8, "c$5");
		checkToken(lexer.next(), Kind.ASSIGN, 3,12);
		checkToken1(lexer.next(), Kind.BOOLEAN_LIT, 3,14, "true");
		checkToken(lexer.next(), Kind.SEMI, 3,18);
		checkToken(lexer.next(), Kind.RSQUARE, 4,0);
		checkEOF(lexer.next());
	}

	@Test
	public void testBang() throws LexicalException{
		String input = """
        !=
        !!
        !=!
        !!=>>>=<-<<<
        """;
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.NOT_EQUALS, 0, 0);
		checkToken(lexer.next(), Kind.BANG, 1, 0);
		checkToken(lexer.next(), Kind.BANG, 1, 1);
		checkToken(lexer.next(), Kind.NOT_EQUALS, 2, 0);
		checkToken(lexer.next(), Kind.BANG, 2, 2);
		checkToken(lexer.next(), Kind.BANG, 3, 0 );
		checkToken(lexer.next(), Kind.NOT_EQUALS, 3, 1);
		checkToken(lexer.next(), Kind.RANGLE, 3, 3);
		checkToken(lexer.next(), Kind.GE, 3, 5);
		checkToken(lexer.next(), Kind.LARROW, 3,7);
		checkToken(lexer.next(), Kind.LANGLE, 3, 9);
		checkToken(lexer.next(), Kind.LT, 3, 11);
		checkEOF(lexer.next());
	}

	@Test
	public void testCodeExample() throws LexicalException{
		String input = """
  		string a = "hello\\nworld";
  		int size = 11;
  		string b = "";
  		boolean display = true;
  		
	 	for (int i = size - 1;i >= 0; i++) [
	 		b = b + a[i];
	 	]

  		if (display == true)
  		print(b);
	  	""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken1(lexer.next(), Kind.TYPE, 0, 1, "string");
		checkToken1(lexer.next(), Kind.IDENT, 0, 8, "a");
		checkToken1(lexer.next(), Kind.ASSIGN, 0, 10, "=");
		checkToken1(lexer.next(), Kind.STRING_LIT, 0, 12, "\"hello\\nworld\"");
		checkToken(lexer.next(), Kind.SEMI, 0, 26);

		checkToken1(lexer.next(), Kind.TYPE, 1, 0, "int");
		checkToken1(lexer.next(), Kind.IDENT, 1, 4, "size");
		checkToken1(lexer.next(), Kind.ASSIGN, 1, 9, "=");
		checkInt(lexer.next(), 11, 1, 11);
		checkToken(lexer.next(), Kind.SEMI, 1, 13);

		checkToken(lexer.next(), Kind.TYPE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.STRING_LIT);
		checkToken(lexer.next(), Kind.SEMI);

		checkToken1(lexer.next(), Kind.TYPE, 3, 0, "boolean");
		checkToken1(lexer.next(), Kind.IDENT, 3, 8, "display");
		checkToken1(lexer.next(), Kind.ASSIGN, 3, 16, "=");
		checkToken1(lexer.next(), Kind.BOOLEAN_LIT, 3, 18, "true");
		checkToken(lexer.next(), Kind.SEMI, 3, 22);

		checkToken1(lexer.next(), Kind.IDENT, 5, 0, "for");
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.TYPE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.MINUS);
		checkInt(lexer.next(), 1);
		checkToken(lexer.next(), Kind.SEMI);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.GE);
		checkInt(lexer.next(), 0);
		checkToken(lexer.next(), Kind.SEMI);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.LSQUARE);

		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.LSQUARE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.RSQUARE);
		checkToken(lexer.next(), Kind.SEMI);

		checkToken(lexer.next(), Kind.RSQUARE);

		checkToken(lexer.next(), Kind.KW_IF);
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.EQUALS);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.SEMI);

		checkEOF(lexer.next());
	}

	@Test
	public void testEOF1() throws LexicalException {
		String input = "#ThisEndsInEOF";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}

	@Test
	public void testCommentEOF2() throws LexicalException {
		String input = "#";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}

	@Test
	void testOnlyEmptyStringLit() throws LexicalException {
		String input = "\"\"";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken1(lexer.next(), Kind.STRING_LIT, 0, 0, "\"\"");
	}
}
