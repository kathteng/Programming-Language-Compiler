package edu.ufl.cise.plc;

import java.util.List;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
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
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;
import edu.ufl.cise.plc.ast.Types.Type;

public class CodeGenVisitor implements ASTVisitor {
    String pkgName;
    StringBuilder imports;

    public CodeGenVisitor (String packageName) {
        pkgName = packageName;
        imports = new StringBuilder();
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(booleanLitExpr.getValue());
        return sb.toString();
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("\"\"\"\n" + stringLitExpr.getValue() + "\"\"\"");
        return sb.toString();
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Type.INT)
            sb.append("(" + intLitExpr.getCoerceTo().toString().toLowerCase() + ")");
        sb.append(intLitExpr.getValue());
        return sb.toString();
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Type.FLOAT)
            sb.append("(" + floatLitExpr.getCoerceTo().toString().toLowerCase() + ")");
        sb.append(floatLitExpr.getValue() + "f");
        return sb.toString();
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ConsoleIO;"))
            imports.append("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
        StringBuilder sb = (StringBuilder) arg;
        sb.append("(" + consoleExpr.getCoerceTo().toString().toLowerCase() + ") ");
        sb.append("ConsoleIO.readValueFromConsole(\"" + consoleExpr.getCoerceTo().toString() + "\", \"Enter ");
        sb.append(consoleExpr.getCoerceTo().toString().toLowerCase() + ":\");\n");
        return sb.toString();
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        // TODO Auto-generated method stub
        StringBuilder sb = (StringBuilder) arg;
        return sb.toString();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("(" + unaryExpression.getOp().getText());
        unaryExpression.getExpr().visit(this, sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (binaryExpr.getCoerceTo() != null && binaryExpr.getCoerceTo() != binaryExpr.getType()) {
            if (binaryExpr.getCoerceTo() == Type.BOOLEAN)
                sb.append("(Boolean)");
            else if (binaryExpr.getCoerceTo() == Type.STRING)
                sb.append("(String)");
            sb.append("(" + binaryExpr.getCoerceTo().toString().toLowerCase() + ")");
        }
        sb.append("(");
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        left.visit(this, sb);
        sb.append(" " + binaryExpr.getOp().getText() + " ");
        right.visit(this, sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {
            if (identExpr.getCoerceTo() == Type.BOOLEAN)
                sb.append("(Boolean)");
            else if (identExpr.getCoerceTo() == Type.STRING)
                sb.append("(String)");
            sb.append("(" + identExpr.getCoerceTo().toString().toLowerCase() + ")");
        }
        sb.append(identExpr.getText());
        return sb.toString();
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("((");
        conditionalExpr.getCondition().visit(this, sb);
        sb.append(") ? ");
        conditionalExpr.getTrueCase().visit(this, sb);
        sb.append(" : ");
        conditionalExpr.getFalseCase().visit(this, sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Expr expr = assignmentStatement.getExpr();
        sb.append(assignmentStatement.getName() + " = ");
        expr.visit(this, sb);
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("ConsoleIO.console.println(");
        writeStatement.getSource().visit(this, sb);
        sb.append(");\n");
        return sb.toString();
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(readStatement.getName() + " = ");
        readStatement.getSource().visit(this, sb);
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        StringBuilder sb = new StringBuilder("");
        sb.append("package ");
        sb.append(pkgName + ";");
        sb.append("\n");
        // TODO: import
        sb.append("public class " + program.getName() + "{\n");
        if (program.getReturnType() == Type.STRING)
            sb.append("\t" + "public static String apply(");
        else
            sb.append("\t" + "public static " + program.getReturnType().toString().toLowerCase() + " apply(");
        List<NameDef> params = program.getParams();
        for (NameDef node : params) {
            node.visit(this, sb);
            sb.append(",");
        }
        if (params.size() != 0)
            sb.deleteCharAt(sb.length() - 1);
        sb.append(") {\n");
        sb.append("\t\t");
        List<ASTNode> decsAndStatements = program.getDecsAndStatements();
        for (ASTNode node : decsAndStatements)
            node.visit(this, sb);
        
        sb.append("\t}\n}\n");
        int index = sb.indexOf("public class");
        sb.insert(index - 1, imports);
        return sb.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        String type = nameDef.getType().toString();
        if (nameDef.getType() == Type.STRING)
            sb.append("String " + nameDef.getName());
        else if (nameDef.getType() == Type.BOOLEAN)
            sb.append("Boolean " + nameDef.getName());
        else
            sb.append(type.toLowerCase() + " " + nameDef.getName());
        return sb.toString();
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("return ");
        Expr expr = returnStatement.getExpr();
        expr.visit(this, sb);
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        NameDef nameDef = declaration.getNameDef();
        nameDef.visit(this, sb);
        if (declaration.getOp() != null) {
            sb.append(" " + declaration.getOp().getText() + " ");
            Expr expr = declaration.getExpr();
            if (expr.getCoerceTo() != null && expr.getCoerceTo() != expr.getType())
                sb.append("(" + expr.getCoerceTo().toString().toLowerCase() + ")");
            expr.visit(this, sb);
        }
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }
    
}
