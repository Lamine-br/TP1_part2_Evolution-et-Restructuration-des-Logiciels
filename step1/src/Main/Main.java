package Main;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class Main {

    public static final String projectPath = "E:\\PDC\\Visitor Pattern";
    public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

    public static void main(String[] args) throws Exception {
        // List of Java files
        List<File> javaFiles = listJavaFiles(new File(projectPath));

        // Counters for class declarations, lines of code, methods, attributes, and packages
        int classCount = 0;
        int lineCount = 0;
        int methodCount = 0;
        int attributeCount = 0; // New counter for attributes
        Set<String> packageSet = new HashSet<>(); // To store unique packages
        List<MethodLineInfo> methodLineInfoList = new ArrayList<>(); // For storing method line info

        // Store class method and attribute counts for analysis
        List<ClassMethodAttributeInfo> classInfoList = new ArrayList<>();

        // Parse each Java file and count class declarations, lines of code, methods, attributes, and packages
        for (File javaFile : javaFiles) {
            String sourceCode = new String(Files.readAllBytes(javaFile.toPath()));
            ClassInfo classInfo = countClassesAndAttributes(sourceCode); // Get class and attribute counts
            classCount += classInfo.getClassCount();
            attributeCount += classInfo.getAttributeCount(); // Add attributes
            lineCount += countLinesOfCode(sourceCode);
            int methodsInFile = countMethods(sourceCode, methodLineInfoList); // Count methods and save line info
            methodCount += methodsInFile;
            packageSet.add(getPackageName(sourceCode)); // Add package name to the set
            
            // Add class info for analysis
            classInfoList.add(new ClassMethodAttributeInfo(javaFile.getName().replace(".java", ""), methodsInFile, classInfo.getAttributeCount()));
        }

        // Calculate averages
        double averageMethodsPerClass = classCount == 0 ? 0 : (double) methodCount / classCount;
        double averageAttributesPerClass = classCount == 0 ? 0 : (double) attributeCount / classCount; // Average attributes
        double averageLinesPerMethod = methodCount == 0 ? 0 : (double) lineCount / methodCount;

        // Output results in the specified order
        System.out.println("1. Nombre de classes de l’application: " + classCount);
        System.out.println("2. Nombre de lignes de code de l’application: " + lineCount);
        System.out.println("3. Nombre total de méthodes de l’application: " + methodCount);
        System.out.println("4. Nombre total de packages de l’application: " + packageSet.size());
        System.out.printf("5. Nombre moyen de méthodes par classe: %.2f%n", averageMethodsPerClass);
        System.out.printf("6. Nombre moyen de lignes de code par méthode: %.2f%n", averageLinesPerMethod);
        System.out.printf("7. Nombre moyen d’attributs par classe: %.2f%n", averageAttributesPerClass);

        // Find and print top 10% methods with the greatest number of lines of code
        findTop10PercentMethods(methodLineInfoList);

        // Find and print classes that possess more than X methods
        int X = 5; // Replace this with the desired threshold
        findClassesWithMoreThanXMethods(classInfoList, X);

        // Find and print top 10% classes with the greatest number of methods
        findTop10PercentClassesWithMostMethods(classInfoList);

        // Find and print top 10% classes with the greatest number of attributes
        findTop10PercentClassesWithMostAttributes(classInfoList);

        // Find and print classes that belong to both previous categories
        findClassesInBothCategories(classInfoList);

        // Find maximum number of parameters across all methods
        int maxParameters = findMaxParameters(javaFiles);
        System.out.println("13. Le nombre maximal de paramètres par rapport à toutes les méthodes de l’application: " + maxParameters);
    }

    // Recursively lists all Java files in the source directory
    private static List<File> listJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                javaFiles.addAll(listJavaFiles(file));
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
        return javaFiles;
    }

    // Counts classes and attributes in the provided source code
    private static ClassInfo countClassesAndAttributes(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS4); // Update to the latest JLS version
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        // Set environment for the parser
        String[] classpath = { jrePath }; // Adjust the classpath as needed
        String[] sources = { projectPath };
        parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);

        parser.setSource(sourceCode.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null); // create and parse

        ClassCounterVisitor visitor = new ClassCounterVisitor();
        cu.accept(visitor);

        return visitor.getClassInfo(); // Return class information
    }

    // Counts the lines of code in the provided source code
    private static int countLinesOfCode(String sourceCode) {
        // Split the source code into lines and count them
        String[] lines = sourceCode.split("\r\n|\r|\n");
        return lines.length;
    }

    // Counts the methods in the provided source code and collects line info
    private static int countMethods(String sourceCode, List<MethodLineInfo> methodLineInfoList) {
        ASTParser parser = ASTParser.newParser(AST.JLS4); // Update to the latest JLS version
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        // Set environment for the parser
        String[] classpath = { jrePath }; // Adjust the classpath as needed
        String[] sources = { projectPath };
        parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);

        parser.setSource(sourceCode.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null); // create and parse

        MethodLineCounterVisitor visitor = new MethodLineCounterVisitor(methodLineInfoList);
        cu.accept(visitor);

        return visitor.getMethodCount();
    }

    // Extracts the package name from the source code
    private static String getPackageName(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(sourceCode.toCharArray());

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        PackageDeclaration packageDeclaration = cu.getPackage();
        
        if (packageDeclaration != null) {
            return packageDeclaration.getName().getFullyQualifiedName();
        }
        return "default"; // Return a default package name if none is declared
    }

    // ASTVisitor to count class declarations and attributes
    private static class ClassCounterVisitor extends org.eclipse.jdt.core.dom.ASTVisitor {
        private int classCount = 0;
        private int attributeCount = 0; // Counter for attributes

        @Override
        public boolean visit(TypeDeclaration node) {
            classCount++;
            // Count attributes in the class
            attributeCount += node.getFields().length; // Increment by the number of fields
            return super.visit(node);
        }

        public ClassInfo getClassInfo() {
            return new ClassInfo(classCount, attributeCount); // Return class and attribute counts
        }
    }

    // Class to store information about classes and attributes
    private static class ClassInfo {
        private final int classCount;
        private final int attributeCount;

        public ClassInfo(int classCount, int attributeCount) {
            this.classCount = classCount;
            this.attributeCount = attributeCount;
        }

        public int getClassCount() {
            return classCount;
        }

        public int getAttributeCount() {
            return attributeCount;
        }
    }

    // Class to store method and attribute information for each class
    private static class ClassMethodAttributeInfo {
        private final String className;
        private final int methodCount;
        private final int attributeCount;

        public ClassMethodAttributeInfo(String className, int methodCount, int attributeCount) {
            this.className = className;
            this.methodCount = methodCount;
            this.attributeCount = attributeCount;
        }

        public String getClassName() {
            return className;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public int getAttributeCount() {
            return attributeCount;
        }
    }

    // ASTVisitor to count methods and their line information
    private static class MethodLineCounterVisitor extends org.eclipse.jdt.core.dom.ASTVisitor {
        private int methodCount = 0;
        private final List<MethodLineInfo> methodLineInfoList;

        public MethodLineCounterVisitor(List<MethodLineInfo> methodLineInfoList) {
            this.methodLineInfoList = methodLineInfoList;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            methodCount++;
            // Count lines of code in the method
            int linesOfCode = countLinesOfMethod(node);
            methodLineInfoList.add(new MethodLineInfo(node.getName().getFullyQualifiedName(), linesOfCode, node.parameters().size()));
            return super.visit(node);
        }

        public int getMethodCount() {
            return methodCount;
        }

        private int countLinesOfMethod(MethodDeclaration node) {
            // A simple approximation of method length based on its starting and ending line numbers
            return node.getStartPosition() + node.getLength(); // You may want to refine this
        }
    }

    // Class to store method line info
    private static class MethodLineInfo {
        private final String methodName;
        private final int linesOfCode;
        private final int parameterCount;

        public MethodLineInfo(String methodName, int linesOfCode, int parameterCount) {
            this.methodName = methodName;
            this.linesOfCode = linesOfCode;
            this.parameterCount = parameterCount;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLinesOfCode() {
            return linesOfCode;
        }

        public int getParameterCount() {
            return parameterCount;
        }
    }

    // Find and print classes that possess more than X methods
    private static void findClassesWithMoreThanXMethods(List<ClassMethodAttributeInfo> classInfoList, int X) {
        List<ClassMethodAttributeInfo> filteredClasses = classInfoList.stream()
            .filter(classInfo -> classInfo.getMethodCount() > X)
            .collect(Collectors.toList());

        System.out.println("\nClasses with more than " + X + " methods:");
        for (ClassMethodAttributeInfo classInfo : filteredClasses) {
            System.out.printf("Class: %s, Methods: %d, Attributes: %d%n", classInfo.getClassName(), classInfo.getMethodCount(), classInfo.getAttributeCount());
        }
    }

    // Find and print top 10% methods with the greatest number of lines of code
    private static void findTop10PercentMethods(List<MethodLineInfo> methodLineInfoList) {
        if (methodLineInfoList.isEmpty()) {
            System.out.println("\nNo methods found.");
            return;
        }

        // Sort by lines of code in descending order
        methodLineInfoList.sort((m1, m2) -> Integer.compare(m2.getLinesOfCode(), m1.getLinesOfCode()));
        
        int topTenPercentCount = (int) Math.ceil(methodLineInfoList.size() * 0.10);
        List<MethodLineInfo> topMethods = methodLineInfoList.subList(0, Math.min(topTenPercentCount, methodLineInfoList.size()));

        System.out.println("\nTop 10% methods with the greatest number of lines of code:");
        for (MethodLineInfo methodInfo : topMethods) {
            System.out.printf("Method: %s, Lines of Code: %d, Parameters: %d%n", 
                methodInfo.getMethodName(), methodInfo.getLinesOfCode(), methodInfo.getParameterCount());
        }
    }

    // Find the maximum number of parameters across all methods
    private static int findMaxParameters(List<File> javaFiles) throws Exception {
        int maxParameters = 0;
        for (File javaFile : javaFiles) {
            String sourceCode = new String(Files.readAllBytes(javaFile.toPath()));
            ASTParser parser = ASTParser.newParser(AST.JLS4);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(sourceCode.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            
            MethodParameterVisitor visitor = new MethodParameterVisitor();
            cu.accept(visitor);
            maxParameters = Math.max(maxParameters, visitor.getMaxParameters());
        }
        return maxParameters;
    }

    // Find and print top 10% classes with the greatest number of methods
    private static void findTop10PercentClassesWithMostMethods(List<ClassMethodAttributeInfo> classInfoList) {
        if (classInfoList.isEmpty()) {
            System.out.println("\nNo classes found.");
            return;
        }

        // Sort by method count in descending order
        classInfoList.sort((c1, c2) -> Integer.compare(c2.getMethodCount(), c1.getMethodCount()));

        int topTenPercentCount = (int) Math.ceil(classInfoList.size() * 0.10);
        List<ClassMethodAttributeInfo> topClasses = classInfoList.subList(0, Math.min(topTenPercentCount, classInfoList.size()));

        System.out.println("\nTop 10% classes with the greatest number of methods:");
        for (ClassMethodAttributeInfo classInfo : topClasses) {
            System.out.printf("Class: %s, Methods: %d, Attributes: %d%n", classInfo.getClassName(), classInfo.getMethodCount(), classInfo.getAttributeCount());
        }
    }

    // Find and print top 10% classes with the greatest number of attributes
    private static void findTop10PercentClassesWithMostAttributes(List<ClassMethodAttributeInfo> classInfoList) {
        if (classInfoList.isEmpty()) {
            System.out.println("\nNo classes found.");
            return;
        }

        // Sort by attribute count in descending order
        classInfoList.sort((c1, c2) -> Integer.compare(c2.getAttributeCount(), c1.getAttributeCount()));

        int topTenPercentCount = (int) Math.ceil(classInfoList.size() * 0.10);
        List<ClassMethodAttributeInfo> topClasses = classInfoList.subList(0, Math.min(topTenPercentCount, classInfoList.size()));

        System.out.println("\nTop 10% classes with the greatest number of attributes:");
        for (ClassMethodAttributeInfo classInfo : topClasses) {
            System.out.printf("Class: %s, Methods: %d, Attributes: %d%n", classInfo.getClassName(), classInfo.getMethodCount(), classInfo.getAttributeCount());
        }
    }

    // Find and print classes that belong to both previous categories
    private static void findClassesInBothCategories(List<ClassMethodAttributeInfo> classInfoList) {
        // Get top 10% classes by method count
        List<String> topMethodClasses = classInfoList.stream()
            .sorted((c1, c2) -> Integer.compare(c2.getMethodCount(), c1.getMethodCount()))
            .limit((long) Math.ceil(classInfoList.size() * 0.10))
            .map(ClassMethodAttributeInfo::getClassName)
            .collect(Collectors.toList());

        // Get top 10% classes by attribute count
        List<String> topAttributeClasses = classInfoList.stream()
            .sorted((c1, c2) -> Integer.compare(c2.getAttributeCount(), c1.getAttributeCount()))
            .limit((long) Math.ceil(classInfoList.size() * 0.10))
            .map(ClassMethodAttributeInfo::getClassName)
            .collect(Collectors.toList());

        // Find intersection
        topMethodClasses.retainAll(topAttributeClasses);

        System.out.println("\nClasses that belong to both top categories:");
        for (String className : topMethodClasses) {
            System.out.println("Class: " + className);
        }
    }

    // ASTVisitor to find the maximum number of parameters in methods
    private static class MethodParameterVisitor extends org.eclipse.jdt.core.dom.ASTVisitor {
        private int maxParameters = 0;

        @Override
        public boolean visit(MethodDeclaration node) {
            maxParameters = Math.max(maxParameters, node.parameters().size());
            return super.visit(node);
        }

        public int getMaxParameters() {
            return maxParameters;
        }
    }
}
