/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2016  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.text.BadLocationException;

import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

/**
 * Run a whole directory of sample source files through our parser.
 *
 * @author  Andrew Patterson
 */
public class BasicParseTest extends junit.framework.TestCase
{
    /**
     * Get a data or result file from our hidden stash..
     * NOTE: the stash of data files is in the ast/data directory.
     */
    private File getFile(String name)
    {
        URL url = getClass().getResource("/bluej/parser/ast/data/" + name);
        
        if (url == null || url.getFile().equals(""))
            return null;
        else
            return new File(url.getFile());
    }
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
    }

    /**
     * Find a target method/class in the comments and return its index (or -1 if not found).
     */
    private int findTarget(Properties comments, String target)
    {
        for (int commentNum = 0; ; commentNum++) {
            String comment = comments.getProperty("comment" + commentNum + ".target");
            if (comment == null) {
                return -1;
            }
            if (comment.equals(target)) {
                return commentNum;
            }
        }
    }
    
    /**
     * Lots of sample files, none of which should cause exceptions
     * in our parser.
     * 
     * @throws Exception
     */
    public void testNoParseExceptionsOnStandardCode()
        throws Exception
    {
        // this file came from some guys web page.. it just includes lots of
        // Java constructs
        assertNotNull(InfoParser.parse(getFile("java_basic.dat")));

        // these files came from the test suite accompanying antlr
        assertNotNull(InfoParser.parse(getFile("A.dat")));
        assertNotNull(InfoParser.parse(getFile("B.dat")));
        assertNotNull(InfoParser.parse(getFile("C.dat")));
        assertNotNull(InfoParser.parse(getFile("D.dat")));
        assertNotNull(InfoParser.parse(getFile("E.dat")));
        
        // these files were added later
        assertNotNull(InfoParser.parse(getFile("F.dat")));
        assertNotNull(InfoParser.parse(getFile("G.dat")));
    }
    
    public void testNoParseExceptionsOnGenerics()
        throws Exception
    {
        // Parse generics
        assertNotNull(InfoParser.parse(getFile("15_generic.dat")));
    }
    
    public void testCode()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );

        StringReader sr = new StringReader(
                "class A {\n" +
                "  Class<int[]> cc = int[].class;" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, ter, "testpkg");
        assertNotNull(info);
    }
    
    public void testValidClassInfo()
        throws Exception
    {
        List<String> references = new ArrayList<String>();
        references.add("Insets");
        references.add("Color");
        references.add("Rectangle");
        references.add("Graphics");
        references.add("Graphics2D");
        references.add("Runnable");
        references.add("Exception");
        references.add("Dummy1");
        references.add("Dummy2");
        
        File file = getFile("AffinedTransformer.dat");
        ClassInfo info = InfoParser.parse(file, new ClassLoaderResolver(this.getClass().getClassLoader()));

        assertEquals("AffinedTransformer",info.getName());
        assertEquals("javax.swing.JFrame",info.getSuperclass());
        assertEquals("bluej.parser.ast.data",info.getPackage());

        //assertEquals(7, info.getUsed().size());
        
        // Check package selections
        Selection testSel = info.getPackageNameSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(9, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(30, testSel.getEndColumn());
        
        testSel = info.getPackageSemiSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(30, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(31, testSel.getEndColumn());
        
        testSel = info.getPackageStatementSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(1, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(8, testSel.getEndColumn());

        // No type parameters
        List<String> l = info.getTypeParameterTexts();
        if (l != null)
            assertEquals(0, l.size());
//        testSel = info.getTypeParametersSelection();
//        assertNull(testSel);

        // Check that comment is created with parameter names
        Properties comments = info.getComments();
        
        String wantedComment = "void resizeToInternalSize(int, int)";
        int wci = findTarget(comments, wantedComment);
        assertTrue(wci != -1);
        String paramNames = comments.getProperty("comment" + wci + ".params");
        assertEquals("internalWidth internalHeight", paramNames);
    }
    
    public void testValidClassInfo2() throws Exception
    {
        StringReader sr = new StringReader(
                "class A implements Runnable, Iterable {\n" +
                "  void someMethod() {\n" +
                "    I i = new I();\n" +
                "  }\n" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr,
                new ClassLoaderResolver(this.getClass().getClassLoader()), null);
        List<String> implemented = info.getImplements();
        assertNotNull(implemented);
        assertEquals(2, implemented.size());
        assertTrue(implemented.contains("java.lang.Runnable"));
        assertTrue(implemented.contains("java.lang.Iterable"));
    }
    
    public void testValidClassInfo3() throws Exception
    {
        StringReader sr = new StringReader(
                "interface A extends Runnable, Iterable {\n" +
                "}\n");
        ClassInfo info = InfoParser.parse(sr,
                new ClassLoaderResolver(this.getClass().getClassLoader()), null);
        List<String> implemented = info.getImplements();
        assertNotNull(implemented);
        assertEquals(2, implemented.size());
        assertTrue(implemented.contains("java.lang.Runnable"));
        assertTrue(implemented.contains("java.lang.Iterable"));
    }

    /**
     * Test recognition of interfaces
     */
    public void testValidClassInfo4() throws Exception
    {
        StringReader sr = new StringReader(
                "interface A {}"
        );
        ClassInfo info = InfoParser.parse(sr, null, null);
        assertTrue(info.isInterface());
    }

    /**
     * Test recognition of enumerations
     */
    public void testValidClassInfo5() throws Exception
    {
        StringReader sr = new StringReader(
                "enum A { monday { public int getAnInt() { return 3;} }, tuesday() {}, wednesday }"
        );
        ClassInfo info = InfoParser.parse(sr, null, null);
        assertNotNull(info);
        assertTrue(info.isEnum());
    }
    
    public void testMultiDimensionalArrayParam() throws Exception
    {
        File file = getFile("I.dat");
        ClassInfo info = InfoParser.parse(file);
        
        // Check that comment is created with parameter names
        Properties comments = info.getComments();
        
        String wantedComment = "void method(int[][])";
        int commentNum = findTarget(comments, wantedComment);
        assertTrue(commentNum != -1);
        String paramNames = comments.getProperty("comment" + commentNum + ".params");
        assertEquals(paramNames, "args");
    }
    
    public void testCommentExtraction() throws Exception
    {
        String aSrc = "class A {\n"
            + "  void method1(int [] a) { }\n"
            + "  void method2(int a[]) { }\n"
            + "  void method3(String [] a) { }\n"
            + "}\n";
        
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        Properties comments = info.getComments();
        assertTrue(findTarget(comments, "void method1(int[])") != -1);
        assertTrue(findTarget(comments, "void method2(int[])") != -1);
        assertTrue(findTarget(comments, "void method3(java.lang.String[])") != -1);
    }

    public void testCommentExtraction2() throws Exception
    {
        String aSrc = "class A<T> {\n"
            + "  void method1(T [] a) { }\n"
            + "  <U> void method2(U a[]) { }\n"
            + "}\n";
        
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        Properties comments = info.getComments();
        assertTrue(findTarget(comments, "void method1(java.lang.Object[])") != -1);
        assertTrue(findTarget(comments, "void method2(java.lang.Object[])") != -1);
    }

    public void testCommentExtraction3() throws Exception
    {
        String aSrc = "import java.util.*;\n" 
                + "class A {\n"
                + "  void method1(List<List<Integer>> a) { }\n"
                + "}\n";
            
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        Properties comments = info.getComments();
        assertTrue(findTarget(comments, "void method1(java.util.List)") != -1);
    }
    
    public void testCommentExtraction4() throws Exception
    {
        String aSrc = "class A<T> {\n"
                + "  void method1(A<? extends T> a) { }\n"
                + "  void method2(A<? super T> a) { }\n"
                + "}\n";
            
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        Properties comments = info.getComments();
        assertTrue(findTarget(comments, "void method1(A)") != -1);
        assertTrue(findTarget(comments, "void method2(A)") != -1);
    }
    
    public void testMultipleInterfaceExtends() throws Exception
    {
        String aSrc = "interface A extends B, C { }";
        
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), null, null);
        assertNotNull(info);
    }
    
    public void testClassTpars() throws Exception
    {
        String aSrc = "class B {\n"
                + "  <T> void method1(A<? extends T> a) { }\n"
                + "}\n";
            
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        assertTrue(info.getTypeParameterTexts().isEmpty());
        
        aSrc = "class B<U extends Runnable> { }";
        info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        assertTrue(info.getTypeParameterTexts().size() == 1);
        assertEquals("U", info.getTypeParameterTexts().get(0));
    }
    
    private ParsedCUNode cuForSource(String sourceCode, EntityResolver resolver)
    {
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
        document.enableParser(true);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }
    
    public void testInterfaceSelections()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("interface I {}", pkgr));
        //ter.addCompilationUnit("", cuForSource("interface II extends I {}", pkgr));
        ter.addCompilationUnit("", cuForSource("interface J {}", pkgr));
        //ter.addCompilationUnit("", cuForSource("interface JJ extends I, J {}", pkgr));
    }
    
    public void testDependencyAnalysis()
        throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class J<T> {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class K {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class L {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class M {}", pkgr));
        
        FileInputStream fis = new FileInputStream(getFile("H.dat"));
        ClassInfo info = InfoParser.parse(new InputStreamReader(fis), pkgr, "");
        
        List<String> used = info.getUsed();
        assertTrue(used.contains("I")); 
        assertTrue(used.contains("J")); 
        assertTrue(used.contains("K")); 
        assertTrue(used.contains("L")); 
        assertTrue(used.contains("M")); 
    }
    
    /**
     * Test dependency analysis works correctly in the presence of inner classes.
     * In this example, the "I" in the method body refers to the inner class "I" and
     * should not generate an external reference.
     */
    public void testDependencyAnalysis2() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        ter.addCompilationUnit("", cuForSource("class I {}", ter));
        StringReader sr = new StringReader(
                "class A {\n" +
                "  void someMethod() {\n" +
                "    I i = new I();\n" +
                "  }\n" +
                "  class I { }\n" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, null, null);
        List<String> used = info.getUsed();
        
        assertFalse(used.contains("I"));
    }
    
    /**
     * Test loop iterator variable declaration dependency
     */
    public void testDependencyAnalysis3() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class JJ { public static I someMethod() { return null; } }", pkgr));
        
        StringReader sr = new StringReader(
                        "class A {\n" +
                        "  void someMethod() {\n" +
                        "    for(I ii = JJ.someMethod(); ;) ;\n" +
                        "  }\n" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        List<String> used = info.getUsed();
        
        assertTrue(used.contains("I"));
    }
    
    /**
     * Test reference to class via static method call
     */
    public void testDependencyAnalysis4() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I {}", pkgr));
        ter.addCompilationUnit("", cuForSource("class JJ { public static I someMethod() { return null; } }", pkgr));
        
        StringReader sr = new StringReader(
                        "class A {\n" +
                        "  void someMethod() {\n" +
                        "    for(I ii = JJ.someMethod(); ;) ;\n" +
                        "  }\n" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        List<String> used = info.getUsed();
        
        assertTrue(used.contains("JJ"));
    }
    
    /**
     * Test that type parameters are recognized and that they shadow classes with the same name
     */
    public void testDependencyAnalysis5() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        ter.addCompilationUnit("", cuForSource("class T {}", ter));
        
        StringReader sr = new StringReader(
                        "class A<T> {\n" +
                        "  public T someVar;" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, ter, "");
        List<String> used = info.getUsed();
        
        assertFalse(used.contains("T"));
    }

    /**
     * Test dependency analysis within a named package
     */
    public void testDependencyAnalysis6() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "testpkg");
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class N {}", pkgr));
        
        StringReader sr = new StringReader(
                        "package testpkg;" +
                        "class A {\n" +
                        "  public N someVar;" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "testpkg");
        List<String> used = info.getUsed();
        
        assertTrue(used.contains("N"));
    }

    /**
     * Test dependency analysis handles qualified names
     */
    public void testDependencyAnalysis7() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "testpkg");
        PackageResolver pkgmr = new PackageResolver(ter, "otherpkg");
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class N {}", pkgr));
        ter.addCompilationUnit("otherpkg", cuForSource("package otherpkg; class M {}", pkgmr));
        
        StringReader sr = new StringReader(
                        "package testpkg;" +
                        "class A {\n" +
                        "  public testpkg.N someVar;" +
                        "  public otherpkg.M otherVar;" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "testpkg");
        List<String> used = info.getUsed();
        
        assertTrue(used.contains("N"));
        assertFalse(used.contains("M"));
    }
    
    /**
     * Test that an imported class shadows another class in the same package.
     */
    public void testDependencyAnalysis8() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        ter.addCompilationUnit("testpkg", cuForSource("class N {}", ter));
        ter.addCompilationUnit("otherpkg", cuForSource("class N {}", ter));
        
        StringReader sr = new StringReader(
                        "package testpkg;" +
                        "import otherpkg.N;" +
                        "class A {\n" +
                        "  public N someVar;" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, ter, "testpkg");
        List<String> used = info.getUsed();
        
        assertFalse(used.contains("N"));
    }

    /**
     * Test reference to class via static value reference
     */
    public void testDependencyAnalysis9() throws Exception
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I { public static int xyz = 3; }", pkgr));
        
        StringReader sr = new StringReader(
                        "class A {\n" +
                        "  int n = I.xyz;" +
                        "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        List<String> used = info.getUsed();
        
        assertTrue(used.contains("I"));
    }
    
    /**
     * Test that a type argument generates a dependency.
     */
    public void testDependencyAnalysis10()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I { }", pkgr));

        StringReader sr = new StringReader(
                "import java.util.List;" +
                "class A {\n" +
                "  List<I> list;" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        List<String> used = info.getUsed();

        assertTrue(used.contains("I"));
    }

    /**
     * Test that a type parameter bound generates a dependency.
     */
    public void testDependencyAnalysis11()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");
        ter.addCompilationUnit("", cuForSource("class I { }", pkgr));

        StringReader sr = new StringReader(
                "class A<T extends I> {\n" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        List<String> used = info.getUsed();

        assertTrue(used.contains("I"));
    }
    
    public void testDependencyAnalysis12()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "testpkg");
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class I { }", pkgr));
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class J { }", pkgr));

        StringReader sr = new StringReader(
                "package testpkg;" +
                "class A {\n" +
                "  Class<?> cc = I.class;" +
                "  Class<?> cc2 = testpkg.J.class;" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "testpkg");
        List<String> used = info.getUsed();

        assertTrue(used.contains("I"));
        assertTrue(used.contains("J"));
    }

    public void testDependencyAnalysis13()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "testpkg");
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class I { }", pkgr));
        ter.addCompilationUnit("testpkg", cuForSource("package testpkg; class J { }", pkgr));

        StringReader sr = new StringReader(
                "package testpkg;" +
                "class A {\n" +
                "  Class<? extends I> cc;" +
                "  Class<? super J> cc2;" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "testpkg");
        List<String> used = info.getUsed();

        assertTrue(used.contains("I"));
        assertTrue(used.contains("J"));
    }
    
    public void testClassModifiers()
    {
        InitConfig.init();
        TestEntityResolver ter = new TestEntityResolver(
                new ClassLoaderResolver(this.getClass().getClassLoader())
                );
        PackageResolver pkgr = new PackageResolver(ter, "");

        StringReader sr = new StringReader(
                "abstract class A {\n" +
                "}\n"
        );
        ClassInfo info = InfoParser.parse(sr, pkgr, "");
        assertTrue(info.isAbstract());
        assertFalse(info.isInterface());
        assertFalse(info.isEnum());
    }
        
    public void testAnnotation1()
    {
        String aSrc = "public @interface UnderConstruction {\n" +
            "    public enum Priority { LOW, MEDIUM, HIGH }\n" +
            "    String owner() default \"Aqifg\";\n" +
            "    public abstract String lastChanged() default \"08/07/2011\";\n" +
            "    int someInteger() default 123;\n" +
            "    java.lang.String someString() default \"I am a String\";\n" +
            "    String ff = \"another string\";\n" +
            "}\n";
            
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        assertNotNull(info);
        assertFalse(info.hadParseError());
    }
    
    /**
     * Parse some code with an error in it (regression test).
     */
    public void testParseBroken()
    {
        String aSrc = "class A {\n" +
                "  <T> fff(List<\n" +
                "  void xyz(int n) { }\n" +
                "}\n";
        ClassInfo info = InfoParser.parse(new StringReader(aSrc), new ClassLoaderResolver(getClass().getClassLoader()), null);
        assertNotNull(info);
        assertTrue(info.hadParseError());
    }
}
