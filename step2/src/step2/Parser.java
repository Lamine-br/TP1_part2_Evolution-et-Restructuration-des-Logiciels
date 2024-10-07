package step2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class Parser {
	
	public static final String projectPath = "E:\\PDC\\Visitor Pattern";
	public static final String projectSourcePath = projectPath + "\\src";
	public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

	public static void main(String[] args) throws IOException {

		// read java files
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);

		Map<String, List<String>> callGraph = new HashMap<>(); // Call graph storage

		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
			CompilationUnit parse = parse(content.toCharArray());

			// Create an instance of CallGraphVisitor to build the graph
			CallGraphVisitor visitor = new CallGraphVisitor(callGraph);
			parse.accept(visitor);
		}

		// Print the call graph
		System.out.println("Call Graph:");
		for (Map.Entry<String, List<String>> entry : callGraph.entrySet()) {
			System.out.println(entry.getKey() + " calls: " + entry.getValue());
		}
	}

	// read all java files from specific folder
	public static ArrayList<File> listJavaFilesForFolder(final File folder) {
		ArrayList<File> javaFiles = new ArrayList<File>();
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				javaFiles.addAll(listJavaFilesForFolder(fileEntry));
			} else if (fileEntry.getName().endsWith(".java")) {
				javaFiles.add(fileEntry);
			}
		}
		return javaFiles;
	}

	// create AST
	private static CompilationUnit parse(char[] classSource) {
		ASTParser parser = ASTParser.newParser(AST.JLS4); // java +1.6
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
 
		parser.setBindingsRecovery(true);
 
		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
 
		parser.setUnitName("");
 
		String[] sources = { projectSourcePath }; 
		String[] classpath = {jrePath};
 
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8"}, true);
		parser.setSource(classSource);
		
		return (CompilationUnit) parser.createAST(null); // create and parse
	}

	// Visitor for building the call graph
	private static class CallGraphVisitor extends ASTVisitor {
		private Map<String, List<String>> callGraph;

		public CallGraphVisitor(Map<String, List<String>> callGraph) {
			this.callGraph = callGraph;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			String methodName = node.getName().getFullyQualifiedName();
			callGraph.putIfAbsent(methodName, new ArrayList<>()); // Initialize the method entry in the graph
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodInvocation node) {
			// Get the method name being invoked
			String invokedMethodName = node.getName().getFullyQualifiedName();

			// Find the nearest parent method declaration
			MethodDeclaration parentMethod = getParentMethodDeclaration(node);
			if (parentMethod != null) {
				String callingMethodName = parentMethod.getName().getFullyQualifiedName();

				// Update the call graph
				callGraph.get(callingMethodName).add(invokedMethodName);
			}

			return super.visit(node);
		}

		// Helper method to find the nearest MethodDeclaration
		private MethodDeclaration getParentMethodDeclaration(ASTNode node) {
			ASTNode current = node.getParent();
			while (current != null && !(current instanceof MethodDeclaration)) {
				current = current.getParent();
			}
			return (MethodDeclaration) current; // This will return null if not found
		}
	}
}
