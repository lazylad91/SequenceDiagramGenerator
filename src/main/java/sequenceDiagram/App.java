package sequenceDiagram;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import net.sourceforge.plantuml.SourceStringReader;

/**
 * Draw Sequence Diagram
 *
 */
public class App {

	public static HashMap<CompilationUnit, String> cuMap = new HashMap<CompilationUnit, String>();
	public static HashMap<String, ArrayList<MethodDeclaration>> classMethodMap = new HashMap<String, ArrayList<MethodDeclaration>>();
	public static HashMap<String, String> methodClassNameMap = new HashMap<String, String>();
	public static HashMap<String, ArrayList<MethodCallExpr>> MethodCallsMap = new HashMap<String, ArrayList<MethodCallExpr>>();
    public static String code ="@startuml \n autonumber";
    public static final String PARTICIPANT = "participant ";
    public static final String ARROW="->";
    public static final String ACTIVATE="ACTIVATE";
    public static final String DEACTIVATE ="DEACTIVATE";
    public static void main(String[] args) throws ParseException, IOException {

		final File folder = new File(args[0]);
		for (final File f : folder.listFiles()) {
			FileInputStream in = new FileInputStream(f);
			CompilationUnit cu;
			try {
				cu = JavaParser.parse(in);
				cuMap.put(cu, "");
			} finally {
				in.close();
			}

		}
		String functionName = args[1];
		populate(classMethodMap, cuMap, MethodCallsMap);
		// Adding first element in code
		code=code+"\n"+PARTICIPANT+"user";
		code=code+"\n"+"user"+ARROW+methodClassNameMap.get(functionName)+" : "+functionName ;
		code = code+"\n"+ACTIVATE+" "+ methodClassNameMap.get(functionName);
		findFunctionInCu(functionName, cuMap);
		drawSequenceDiagram(functionName);
		code=code+"\n";
		code=code+"\n"+DEACTIVATE + " "+ methodClassNameMap.get(functionName);
		//ending Uml
		code=code+"\n"+"@enduml";
		generateUML(code,args[2]);
		System.out.println(code);
	}

	/*
	 * It will be recursively called to draw the sequence diagram
	 */
	private static void drawSequenceDiagram(String functionName) {
		for (MethodCallExpr methodCallExpr : MethodCallsMap.get(functionName)) {
			if(methodCallExpr instanceof MethodCallExpr){
			System.out.println(methodCallExpr);
			String methodName = methodCallExpr.getName();
			String objectName = methodCallExpr.getScope().toString();
			if (null != methodClassNameMap.get(methodName)) {
				code = code + "\n" + methodClassNameMap.get(functionName)+ARROW+methodClassNameMap.get(methodName)+":"+ methodCallExpr.toString();
				code=code+ "\n" +ACTIVATE+" "+methodClassNameMap.get(methodName);
					drawSequenceDiagram(methodName);
					code=code+ "\n" +DEACTIVATE+" "+methodClassNameMap.get(methodName);
			}
		}
		}
			
	}

	/*
	 * This method will add all methoddeclaration list in map with class name as
	 * key
	 */
	private static void populate(HashMap<String, ArrayList<MethodDeclaration>> classMethodMap,
			HashMap<CompilationUnit, String> cuMap, HashMap<String, ArrayList<MethodCallExpr>> methodCallsMap) {
		for (CompilationUnit cu : cuMap.keySet()) {
			ArrayList<MethodDeclaration> methodDeclartionList = new ArrayList<MethodDeclaration>();
			String className = "";
			List<TypeDeclaration> typeDeclarationList = cu.getTypes();
			for (Node node : typeDeclarationList) {
				ClassOrInterfaceDeclaration ClassDeclaration = (ClassOrInterfaceDeclaration) node;
				className = ClassDeclaration.getName();
				for (BodyDeclaration t : ((TypeDeclaration) ClassDeclaration).getMembers()) {
					if (t instanceof MethodDeclaration) {
						methodCallsMap.put(((MethodDeclaration) (t)).getName(),
								getMethodBodyList(((MethodDeclaration) (t))));
						methodDeclartionList.add(((MethodDeclaration) (t)));
						methodClassNameMap.put(((MethodDeclaration) (t)).getName(), className);
					}
					if (t instanceof FieldDeclaration) {
						String classNameofInstance = ((FieldDeclaration) (t)).getType().toString();
						String nameOfObject = ((FieldDeclaration) (t)).getVariables().get(0).toString();

					}
				}
			}
			classMethodMap.put(className, methodDeclartionList);
		}
	}

	private static ArrayList<MethodCallExpr> getMethodBodyList(MethodDeclaration methodDeclaration) {
		ArrayList<MethodCallExpr> methodCallList = new ArrayList<MethodCallExpr>();
		for (Object blockstmt : methodDeclaration.getChildrenNodes()) {
			if (blockstmt instanceof BlockStmt) {
				for (Object exprstmt : ((Node) blockstmt).getChildrenNodes()) {
					if (exprstmt instanceof ExpressionStmt) {
						if (((ExpressionStmt) (exprstmt)).getExpression() instanceof MethodCallExpr) {
							methodCallList.add((MethodCallExpr) (((ExpressionStmt) (exprstmt)).getExpression()));
						}
					}
				}
			}
		}
		return methodCallList;
	}

	/*
	 * This will find the function name is in which Compilation Unit
	 */
	private static void findFunctionInCu(String functionName, HashMap<CompilationUnit, String> cuMap) {

		for (CompilationUnit cu : cuMap.keySet()) {
			List<TypeDeclaration> typeDeclarationList = cu.getTypes();
			for (Node node : typeDeclarationList) {
				ClassOrInterfaceDeclaration ClassDeclaration = (ClassOrInterfaceDeclaration) node;
				for (BodyDeclaration t : ((TypeDeclaration) ClassDeclaration).getMembers()) {
					if (t instanceof MethodDeclaration) {
						if (functionName.equals(((MethodDeclaration) t).getName())) {
							cuMap.put(cu, "main");

						}
					}
				}
			}
		}
	}

	private static String generateUML(String source,String filename) throws IOException {
		String outPutFile = filename+".png";
		System.out.println(outPutFile);
		OutputStream outputPng = new FileOutputStream(outPutFile);

		SourceStringReader reader = new SourceStringReader(source);
		String desc = reader.generateImage(outputPng);
		return desc;

	}
}
