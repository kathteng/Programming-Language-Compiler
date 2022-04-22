package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes.Name;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;

		switch(op) {//AND, OR, PLUS, MINUS, TIMES, DIV, MOD, EQUALS, NOT_EQUALS, LT, LE, GT,GE 
			case AND, OR -> {
				if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN)
					resultType = Type.BOOLEAN;
				else
					check(false, binaryExpr, "incompatible types for operator");
			}
			case EQUALS,NOT_EQUALS -> {
				check(leftType == rightType, binaryExpr, "incompatible types for comparison");
				resultType = Type.BOOLEAN;
			}
			case PLUS, MINUS -> {
				if (leftType == Type.INT && rightType == Type.INT)
					resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT)
					resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.COLOR && rightType == Type.COLOR)
					resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT)
					resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = Type.COLORFLOAT;
				}
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = Type.COLORFLOAT;
				}
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE)
					resultType = Type.IMAGE;
				else 
					check(false, binaryExpr, "incompatible types for operator");
			}
			case TIMES, DIV, MOD -> {
				if (leftType == Type.INT && rightType == Type.INT)
					resultType = Type.INT;
				else if (leftType == Type.FLOAT && rightType == Type.FLOAT)
					resultType = Type.FLOAT;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.FLOAT;
				}
				else if (leftType == Type.COLOR && rightType == Type.COLOR)
					resultType = Type.COLOR;
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLORFLOAT)
					resultType = Type.COLORFLOAT;
				else if (leftType == Type.COLOR && rightType == Type.COLORFLOAT) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = Type.COLORFLOAT;
				}
				else if (leftType == Type.COLORFLOAT && rightType == Type.COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = Type.COLORFLOAT;
				}
				else if (leftType == Type.IMAGE && rightType == Type.IMAGE)
					resultType = Type.IMAGE;
				else if ((leftType == Type.IMAGE && rightType == Type.INT) || (leftType == Type.IMAGE && rightType == Type.FLOAT))
					resultType = Type.IMAGE;
				else if (leftType == Type.INT && rightType == Type.COLOR) {
					binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = Type.COLOR;
				}
				else if (leftType == Type.COLOR && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = Type.COLOR;
				}
				else if ((leftType == Type.FLOAT && rightType == Type.COLOR) || (leftType == Type.COLOR && rightType == Type.FLOAT)) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = Type.COLORFLOAT;
				}
				else
					check(false, binaryExpr, "incompatible types for operator");
			}
			case LT, LE, GT, GE -> {
				if ((leftType == Type.INT && rightType == Type.INT) || (leftType == Type.FLOAT && rightType == Type.FLOAT))
					resultType = Type.BOOLEAN;
				else if (leftType == Type.INT && rightType == Type.FLOAT) {
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = Type.BOOLEAN;
				}
				else if (leftType == Type.FLOAT && rightType == Type.INT) {
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = Type.BOOLEAN;
				}
				else 
					check(false, binaryExpr, "incompatible types for operator");
			}
			default -> {
				throw new Exception("compiler error");
			}
		}
		binaryExpr.setType(resultType);
		return resultType;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();
		Declaration dec = symbolTable.lookup(name);
		check(dec != null, identExpr, "undefined identifier " + name);
		check(dec.isInitialized(), identExpr, "using uninitialized variable");
		identExpr.setDec(dec);  //save declaration--will be useful later. 
		Type type = dec.getType();
		identExpr.setType(type);
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type condType = (Type) conditionalExpr.getCondition().visit(this, arg);
		check(condType == Type.BOOLEAN, conditionalExpr.getCondition(), "condition must be boolean");
		Type trueType = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseType = (Type) conditionalExpr.getFalseCase().visit(this, arg);
		check(trueType == falseType, conditionalExpr, "trueCase must be the same as the type of falseCase");
		conditionalExpr.setType(trueType);
		return trueType;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type widthType = (Type) dimension.getWidth().visit(this, arg);
		check(widthType == Type.INT, dimension.getWidth(), "only ints as dimension components");
		Type heightType = (Type) dimension.getHeight().visit(this, arg);
		check(heightType == Type.INT, dimension.getHeight(), "only ints as dimension components");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		assignmentStatement.setTargetDec(symbolTable.lookup(assignmentStatement.getName()));
		check(assignmentStatement.getTargetDec() != null, assignmentStatement, "target not acquired, abort mission");
		Type targetType = assignmentStatement.getTargetDec().getType();
		assignmentStatement.getTargetDec().setInitialized(true);
		
		if (targetType != Type.IMAGE) {
			check(assignmentStatement.getSelector() == null, assignmentStatement, "listen darling, you're not supposed to select pixels here");
			assignmentStatement.getExpr().visit(this, arg);
			check(assignmentCompatible(assignmentStatement.getTargetDec(), assignmentStatement.getExpr()), assignmentStatement, "honey, we're not compatible I'm sorry :c");
		}
		else {
			if (assignmentStatement.getSelector() == null) {
				assignmentStatement.getExpr().visit(this, arg);
				if (assignmentStatement.getExpr().getType() == Type.INT)
					assignmentStatement.getExpr().setCoerceTo(COLOR);
				else if (assignmentStatement.getExpr().getType() == Type.FLOAT)
					assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);
				else
					check(assignmentStatement.getExpr().getType() == Type.COLOR || assignmentStatement.getExpr().getType() == Type.COLORFLOAT || assignmentStatement.getExpr().getType() == Type.IMAGE, assignmentStatement, "listen you bastard, we aren't compatible alright?");
			}
			else {
				Expr x = assignmentStatement.getSelector().getX();
				Expr y = assignmentStatement.getSelector().getY();
				check(x instanceof IdentExpr && y instanceof IdentExpr, assignmentStatement, "ma'am, this is an IdentExpr");
				
				Token dummy = new Token(Kind.EOF, -1, -1, "");
				VarDeclaration decX = new VarDeclaration(dummy, new NameDef(dummy, "int", x.getText()), new Token(Kind.ASSIGN, -1, -1, ""), x);
				decX.setInitialized(true);
				VarDeclaration decY = new VarDeclaration(dummy, new NameDef(dummy, "int", y.getText()), new Token(Kind.ASSIGN, -1, -1, ""), y);
				decY.setInitialized(true);
				Boolean insertedX = symbolTable.insert(x.getText(), decX);
				Boolean insertedY = symbolTable.insert(y.getText(), decY);
				check(insertedX && insertedY, assignmentStatement, "we stan uniqueness in this household");
				x.setType(INT);
				y.setType(INT);
				assignmentStatement.getExpr().visit(this, arg);
				switch (assignmentStatement.getExpr().getType()) {
					case COLOR, COLORFLOAT, FLOAT, INT -> {
						assignmentStatement.getExpr().setCoerceTo(COLOR);
					}
					default -> {
						throw new TypeCheckException("excuse you", assignmentStatement.getSourceLoc());
					}
				}
				symbolTable.entries.remove(x.getText());
				symbolTable.entries.remove(y.getText());
			}
		}
		return null;
	}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		readStatement.getSource().visit(this, arg);
		readStatement.setTargetDec(symbolTable.lookup(readStatement.getName()));
		Type targetType = readStatement.getTargetDec().getType();
		readStatement.getTargetDec().setInitialized(true);
		check(readStatement.getSelector() == null, readStatement, "hey remember that sanders changed her mind about the pixels? Yeah");
		check(readStatement.getSource().getType() == Type.CONSOLE || readStatement.getSource().getType() == Type.STRING, readStatement, "read is so picky. Jeez");
		if (readStatement.getSource().getType() == Type.CONSOLE)
			readStatement.getSource().setCoerceTo(targetType);
		return null;
	}

	private boolean assignmentCompatible(Declaration target, Expr rhs) {
		Type targetType = target.getType();
		Type rhsType = rhs.getType();
		if (targetType == rhsType)
			return true;
		else if ((targetType == Type.INT && rhsType == Type.FLOAT) || (targetType == Type.FLOAT && rhsType == Type.INT)
		|| (targetType == Type.COLOR && rhsType == Type.INT) || (targetType == Type.INT && rhsType == Type.COLOR)) {
			rhs.setCoerceTo(targetType);
			return true;
		}
		else if (targetType == Type.IMAGE) {
			if (rhsType == Type.INT)
				rhs.setCoerceTo(COLOR);
			else if (rhsType == Type.FLOAT)
				rhs.setCoerceTo(COLORFLOAT);
			else if (rhsType == COLOR || rhsType == COLORFLOAT)
				return true;
			return true;
		}
		return false;
	}
	
	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		declaration.getNameDef().visit(this, arg);
		
		Kind op = null;
		if (declaration.getOp() != null) {
			op = declaration.getOp().getKind();
			declaration.getExpr().visit(this, arg);
		}

		if (declaration.getNameDef().getType() == Type.IMAGE) {
			if (declaration.getNameDef() instanceof NameDefWithDim)
				check(declaration.getNameDef().getDim() != null, declaration, "no dimension? :(");
			else
				check(declaration.getExpr().getType() == Type.IMAGE || declaration.getExpr().getType() == Type.STRING, declaration, "reminder that image is close minded");
		}			

		if (op == Kind.ASSIGN) {
			check(assignmentCompatible(declaration.getNameDef(), declaration.getExpr()), declaration, "honey, we're not compatible I'm sorry :(");
			declaration.getNameDef().setInitialized(true);
		}
		else if (op == Kind.LARROW) {
			check(declaration.getExpr().getType() == Type.CONSOLE || (declaration.getExpr().getType() == Type.STRING), declaration, "honey, I can't read you I'm sorry");
			if (declaration.getExpr().getType() == Type.CONSOLE)
				declaration.getExpr().setCoerceTo(declaration.getNameDef().getType());
			declaration.getNameDef().setInitialized(true);
		}		
		return null;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		
		//Save root of AST so return type can be accessed in return statements
		root = program;
		
		symbolTable.insert(program.getName(), (Declaration) arg);
		
		List<NameDef> params = program.getParams();
		for (NameDef node : params) {
			node.setInitialized(true);
			node.visit(this, arg);
		}

		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		Boolean inserted = symbolTable.insert(nameDef.getName(), nameDef);
		check(inserted, nameDef, "hey babe, this name already exists");
		return nameDef.getType();
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		Boolean inserted = symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim);
		check(inserted, nameDefWithDim, "hey babe, this name already exists");
		nameDefWithDim.getDim().visit(this, arg);
		return nameDefWithDim.getType();
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
