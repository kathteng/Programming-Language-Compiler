package edu.ufl.cise.plc;

import java.util.List;

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
        StringBuilder sb = (StringBuilder) arg;
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ColorTuple;"))
            imports.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
        if (!imports.toString().contains("java.awt.Color;"))
            imports.append("import java.awt.Color;\n");
        sb.append("ColorTuple.unpack(Color." + colorConstExpr.getText() + ".getRGB())");
        return sb.toString();
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ConsoleIO;"))
            imports.append("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
        StringBuilder sb = (StringBuilder) arg;
        sb.append("(" + consoleExpr.getCoerceTo().toString().toLowerCase() + ") ");
        sb.append("ConsoleIO.readValueFromConsole(\"" + consoleExpr.getCoerceTo().toString() + "\", \"Enter ");
        sb.append(consoleExpr.getCoerceTo().toString().toLowerCase() + ":\")");
        return sb.toString();
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ColorTuple;"))
            imports.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
        sb.append("new ColorTuple(");
        colorExpr.getRed().visit(this, sb);
        sb.append(", ");
        colorExpr.getGreen().visit(this, sb);
        sb.append(", ");
        colorExpr.getBlue().visit(this, sb);
        sb.append(")");
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
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        if (left.getType() == Type.IMAGE && right.getType() == Type.IMAGE) {
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ImageOps;"))
                imports.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
            sb.append("(ImageOps.binaryImageImageOp(ImageOps.OP." + binaryExpr.getOp().getStringValue() + ", " + left.getText() + ", " + right.getText() + "));\n");
        }
        else if (left.getType() == Type.COLOR && right.getType() == Type.COLOR) {
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ImageOps;"))
                imports.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
            sb.append("(ImageOps.binaryTupleOp(ImageOps.OP." + binaryExpr.getOp().getStringValue() + ", " + left.getText() + ", " + right.getText() + "));\n");
        }
        else if (left.getType() == Type.IMAGE && right.getType() == Type.COLOR) {
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ImageOps;"))
                imports.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ColorTuple;"))
                imports.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
            sb.append("(ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOp().getStringValue() + ", " + left.getText() + ", ColorTuple.makePackedColor(ColorTuple.getRed(" + right.getText() + "), ColorTuple.getGreen(" + right.getText() + "), ColorTuple.getBlue(" + right.getText() + ")));\n");
        }
        else if (left.getType() == Type.IMAGE && right.getType() == Type.INT) {
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ImageOps;"))
                imports.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
            sb.append("(ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOp().getStringValue() + ", " + left.getText() + ", " + right.getText() + "));\n");
        }
        else {
            if (binaryExpr.getCoerceTo() != null && binaryExpr.getCoerceTo() != binaryExpr.getType()) {
                if (binaryExpr.getCoerceTo() == Type.STRING)
                    sb.append("(String)");
                else
                    sb.append("(" + binaryExpr.getCoerceTo().toString().toLowerCase() + ")");
            }
            sb.append("(");
            if (left.getType() == Type.STRING && right.getType() == Type.STRING) {
                if (binaryExpr.getOp().getKind() == Kind.EQUALS) {
                    left.visit(this, sb);
                    sb.append(".equals(");
                }
                else if (binaryExpr.getOp().getKind() == Kind.NOT_EQUALS) {
                    sb.append("!");
                    left.visit(this, sb);
                    sb.append(".equals(");
                }
                right.visit(this, sb);
                sb.append(")");
            }
            else {
                left.visit(this, sb);
                sb.append(" " + binaryExpr.getOp().getText() + " ");
                right.visit(this, sb);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {
            if (identExpr.getCoerceTo() == Type.STRING)
                sb.append("(String)");
            else
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
        StringBuilder sb = (StringBuilder) arg;
        dimension.getWidth().visit(this, sb);
        sb.append(", ");
        dimension.getHeight().visit(this, sb);
        return sb.toString();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        return sb.toString();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        Expr expr = assignmentStatement.getExpr();
        if (assignmentStatement.getTargetDec().getType() == Type.IMAGE && expr.getCoerceTo() == Type.COLOR) {
            if (assignmentStatement.getSelector() != null) {
                if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ImageOps;"))
                    imports.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
                String x = assignmentStatement.getSelector().getX().getText();
                String y = assignmentStatement.getSelector().getY().getText();
                sb.append("for (int " + x + " = 0; " + x + " < " + assignmentStatement.getName() + ".getWidth(); " + x + "++) {\n");
                sb.append("\tfor (int " + y + " = 0; " + y + " < " + assignmentStatement.getName() + ".getHeight(); " + y + "++) {\n");
                sb.append("\t\tImageOps.setColor(" + assignmentStatement.getName() + ", " + x + ", " + y + ", ");
                expr.visit(this, sb);
                sb.append(");\n\t}\n}");
            }
        }
        else {
            sb.append(assignmentStatement.getName() + " = ");
            expr.visit(this, sb);
            sb.append(";\n");
        }
        return sb.toString();
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ConsoleIO;"))
            imports.append("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
        if (writeStatement.getSource().getType() == Type.IMAGE) {
            if (writeStatement.getDest().getType() == Type.CONSOLE)
                sb.append("ConsoleIO.displayImageOnScreen(" + writeStatement.getSource().getText() + ");\n");
            else if (writeStatement.getDest().getType() == Type.IMAGE)
                sb.append("FileURLIO.writeImage(" + writeStatement.getSource().getText() + ", " + writeStatement.getDest().getText() + ");\n");
            else
                sb.append("FileURLIO.writeValue(" + writeStatement.getSource().getText() + ", " + writeStatement.getDest().getText() + ");\n");
        }
        else {
            sb.append("ConsoleIO.console.println(");
            writeStatement.getSource().visit(this, sb);
            sb.append(");\n");
        }
        return sb.toString();
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (readStatement.getTargetDec().getType() == Type.IMAGE && !imports.toString().contains("edu.ufl.cise.plc.runtime.FileURLIO;"))
            imports.append("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
        if (readStatement.getTargetDec().getType() == Type.IMAGE) {
            if (!imports.toString().contains("java.awt.image.BufferedImage;"))
                imports.append("import java.awt.image.BufferedImage;\n");
            sb.append("BufferedImage " + readStatement.getName() + " = " + "FileURLIO.readImage(");
            readStatement.getSource().visit(this, sb);
        }
        else
            sb.append(readStatement.getName() + " = ");
        readStatement.getSource().visit(this, sb);
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        StringBuilder sb = new StringBuilder("");
        if (pkgName.length() != 0) {
            sb.append("package ");
            sb.append(pkgName + ";\n");
        }
        sb.append("public class " + program.getName() + "{\n\tpublic static ");
        if (program.getReturnType() == Type.STRING)
            sb.append("String apply(");
        else if (program.getReturnType() == Type.IMAGE)
            sb.append("BufferedImage apply(");
        else
            sb.append(program.getReturnType().toString().toLowerCase() + " apply(");
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
        // imports
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
        StringBuilder sb = (StringBuilder) arg;
        if (!imports.toString().contains("java.awt.image.BufferedImage;"))
            imports.append("import java.awt.image.BufferedImage;\n");
        sb.append("BufferedImage " + nameDefWithDim.getName() + " = new BufferedImage(");
        nameDefWithDim.getDim().visit(this, sb);
        sb.append(", BufferedImage.TYPE_INT_RGB)");
        return sb.toString();
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
        if (nameDef.getType() == Type.IMAGE) {
            if (nameDef instanceof NameDefWithDim && declaration.getOp() == null)
                nameDef.visit(this, sb);
            else {
                if (!imports.toString().contains("java.awt.image.BufferedImage;"))
                    imports.append("import java.awt.image.BufferedImage;\n");
                sb.append("BufferedImage " + nameDef.getName());
            }
        }
        else if (nameDef.getType() == Type.COLOR) {
            if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ColorTuple;"))
                imports.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
        }
        else
            nameDef.visit(this, sb);
        
        if (declaration.getOp() != null) {
            if (nameDef.getType() == Type.IMAGE) {
                if (!imports.toString().contains("edu.ufl.cise.plc.runtime.FileURLIO;"))
                    imports.append("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
                sb.append(" = FileURLIO.readImage(");
                declaration.getExpr().visit(this, sb);
                if (nameDef instanceof NameDefWithDim) {
                    NameDefWithDim nd = (NameDefWithDim)nameDef;
                    sb.append(", ");
                    nd.getDim().visit(this, sb);
                }
                sb.append(");\nFileURLIO.closeFiles();\n");
            }
            else {
                sb.append(" " + declaration.getOp().getText() + " ");
                Expr expr = declaration.getExpr();
                if (expr.getCoerceTo() != null && expr.getCoerceTo() != expr.getType())
                    sb.append("(" + expr.getCoerceTo().toString().toLowerCase() + ")");
                expr.visit(this, sb);
            }
        }
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        StringBuilder sb = (StringBuilder) arg;
        if (!imports.toString().contains("edu.ufl.cise.plc.runtime.ColorTuple;"))
            imports.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
        sb.append("ColorTuple.unpack(" + unaryExprPostfix.getText() + ".getRGB(" + unaryExprPostfix.getSelector().visit(this, sb) + "))");
        return sb.toString();
    }
    
}
