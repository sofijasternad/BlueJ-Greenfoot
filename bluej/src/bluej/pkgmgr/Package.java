package bluej.pkgmgr;

import bluej.Config;
import bluej.compiler.CompileObserver;
import bluej.utility.SimpleClassLoader;
import bluej.debugger.ObjectBench;
import bluej.debugger.ObjectWrapper;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClassLoader;
import bluej.debugger.CallHistory;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.editor.moe.MoeEditorManager;
import bluej.graph.Graph;
import bluej.graph.Vertex;
import bluej.utility.Debug;
import bluej.utility.ClasspathSearcher;
import bluej.utility.MultiEnumeration;
import bluej.utility.Utility;
import bluej.utility.PackageFileFilter;
import bluej.views.Comment;
import bluej.views.CommentList;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import sun.tools.javac.SourceClass;
import sun.tools.javac.BatchEnvironment;
import sun.tools.java.*;
import sun.tools.javac.BlueJJavacMain;
import sun.tools.javadoc.BlueJDocumentationGenerator;

/**
 ** @version $Id: Package.java 53 1999-04-29 23:43:58Z mik $
 ** @author Michael Cahill
 **
 ** A Java package (collection of Java classes).
 **/
public class Package extends Graph

    implements CompileObserver, MouseListener, MouseMotionListener
{
    static final Color titleCol = Config.getItemColour("colour.text.fg");
    static final Color lightGrey = new Color(224, 224, 224);
    public static String noPackage = Config.getString("pkgmgr.noPackage");
    static final String compiling = Config.getString("pkgmgr.compiling");
    static final String compileDone = Config.getString("pkgmgr.compileDone");
    static final String chooseUsesTo = Config.getString("pkgmgr.chooseUsesTo");
    static final String chooseInhTo = Config.getString("pkgmgr.chooseInhTo");
    public static final String pkgfileName = "bluej.pkg";
    public static final String pkgfileBackup = "bluej.pkh";

    /** ERROR CODES **/
    public static final int NO_ERROR = 0;
    public static final int FILE_NOT_FOUND = 1;
    public static final int ILLEGAL_FORMAT = 2;
    public static final int COPY_ERROR = 3;
    public static final int CLASS_EXISTS = 4;
    public static final int CREATE_ERROR = 5;

    private static final int STARTROWPOS = 20;
    private static final int STARTCOLUMNPOS = 20;
    private static final int DEFAULTTARGETCHARWIDTH = 9;
    private static final int DEFAULTTARGETHEIGHT = 50;
    private static final int TARGETGAP = 20;
    // used to size frame when no existing size information can be found
    private static final int DEFAULTFRAMEHEIGHT = 600;
    private static final int DEFAULTFRAMEWIDTH = 800;
	
    /** Interface to editor **/
    static EditorManager editorManager = new MoeEditorManager(false);
    // static EditorManager editorManager = new RedEditorManager(false);
    // static EditorManager editorManager = new SimpleEditorManager();

    protected String packageName = noPackage;	// name of pkg (eg java.lang)
						//  or string "No Package"
    protected String dirname;			// the directory of this package (may
						//  be relative)
    protected String baseDir;			// the absolute path to the directory
						//  which contains the package directory
    protected String classdir;			// the directory storing the class files
						//  (usually equal to package 
						//  directory). Is added to classpath.
    protected String relclassdir;		// the classdir relative to package
						//  directory

    protected String classpath = null;
    protected Hashtable targets;
    protected Vector usesArrows;
    protected Vector extendsArrows;
    protected Target selected;	// Currently selected target
    protected Target fromChoice;	// Holds the choice of "from" target for a new dependency
    PkgFrame frame;
    Dependency currentArrow;	// used during arrow deletion
	
    private ClasspathSearcher searcher;
    private SimpleClassLoader loader;
    private DebuggerClassLoader debuggerLoader;
    private CallHistory callHistory; 	
    protected boolean showExtends = true;
    protected boolean showUses = true;

    public static final int S_IDLE = 0;
    public static final int S_CHOOSE_USES_FROM = 1;
    public static final int S_CHOOSE_USES_TO = 2;
    public static final int S_CHOOSE_EXT_FROM = 3;
    public static final int S_CHOOSE_EXT_TO = 4;
    public static final int S_DELARROW = 5;

    public static final int HISTORY_LENGTH = 6;

    private int state = S_IDLE;	// What state is this package in? (one of the S_* values)

    /**
     * Create a new package from a given directory in a given frame.
     */
    public Package(String dirname, PkgFrame frame)
    {
	this.dirname = dirname;
	this.frame = frame;
		
	targets = new Hashtable();
	usesArrows = new Vector();
	extendsArrows = new Vector();
	selected = null;
	callHistory = new CallHistory(HISTORY_LENGTH);
    }

    /**
     * Create a package not associated to a frame.
     */
    public Package(String dirname)
    {
	this(dirname, null);
    }
	
    /**
     * Create a new package not associated to a directory or frame.
     */
    public Package()
    {
	this(null, null);
    }
	
    /**
     * Return the path to this package's directory (may be relative).
     */
    public String getDirName() {
	return dirname;
    }
	
    /**
     * Get the unique identifier for this package (it's directory name 
     * at present)
     */
    public String getId()
    {
	return dirname;
    }

    /**
     * Return this package's frame (may be null).
     */
    public PkgFrame getFrame()
    {
	return frame;
    }
	
    /**
     * Return this package's name (eg javax.swing.text or string "No Package").
     */
    public String getName()
    {
	return packageName;
    }

    /**
     * Return the current CLASSPATH.
     */
    public String getClasspath()
    {
	return classpath;
    }

    /**
     * Return the name of the directory used to store the class files (in the
     * current implementation equal to the package directory, but may be different
     * in the future.
     */
    public String getClassDir()
    {
	return (classdir != null) ? classdir : getBaseDir();
    }
		
    public ObjectBench getBench() { 
	ObjectBench bench = null;
	try {
	    bench = ((PkgMgrFrame) frame).objbench;
	} catch (ClassCastException cce) {
	    cce.printStackTrace();
	}
	return bench;
    }


    /**
     * Get the currently selected Target.  Should return null if none are selected.
     *
     * @return the currently selected Target.
     */
    public Target getSelectedTarget() 
    {
	return selected;
    }




    /**
     * For a location of a to-be-generated package file, extract the
     * package name by returning the remainder of the directory name
     * after the matching classpath element has been removed.
     * Assume each package dir will appear in the CLASSPATH somewhere.
     * Note that paths are match case-insensitively.
     * 
     * @return the package name in . delimited format, or null if none can be created
     */
    private static String getPackageName(String packageDir) {
	String classPath = System.getProperty("java.class.path");
	if (classPath == null)
	    return null;
	
	StringTokenizer classPathTokens = new StringTokenizer(classPath, File.pathSeparator);
	String currentClassPathDir = null;
	int longestClassPathMatchLength = 0;
	String longestClassPathMatchPath = "";
	
	// check every class path entry to find the longest one that starts packageDir
	// because we may have two elements in the class path which are different points
	// in the same file system tree branch (e.g., c:\ and c:\java\).  We want the most
	// specific one which matches our package dir.
	while (classPathTokens.hasMoreElements()) {
	    currentClassPathDir = classPathTokens.nextElement().toString().toLowerCase();
	    if (packageDir.toLowerCase().startsWith(currentClassPathDir)) {
		    if (currentClassPathDir.length() > longestClassPathMatchLength) {
			    longestClassPathMatchLength = currentClassPathDir.length(); 
			    longestClassPathMatchPath = currentClassPathDir;
		    }
	    }
	}

	if (longestClassPathMatchLength == 0)
		return null;
	
	// short circuit checking if we have an exact match on a class path
	if (longestClassPathMatchPath.length() == packageDir.length())
	        return "";
	
	String packageNameAsDir = packageDir.substring(longestClassPathMatchPath.length(), packageDir.length());
	// trim leading and/or trailing directory separators
	if (packageNameAsDir.startsWith("/") || packageNameAsDir.startsWith("\\"))
	    packageNameAsDir = packageNameAsDir.substring(1, packageNameAsDir.length());
	if (packageNameAsDir.endsWith("/") || packageNameAsDir.endsWith("\\"))
	    packageNameAsDir = packageNameAsDir.substring(0, packageNameAsDir.length() - 1);
		
	// replace both types of path separator - just in case
	String packageName = packageNameAsDir.replace('/', '.');
	packageName = packageName.replace('\\', '.');
	return packageName;
    }

    /**
     * Create a set of properties for a specified set of classfiles
     * residing in a specified directory.  Invoked when no package
     * file exists in this directory already, or when the files come
     * from an archive library (i.e., ZIP or JAR)
     * 
     * @param classFiles the array of class files to use for the package
     * @param packageDir the directory in which to create the package
     * @param fromArchive true if the package is being created for an archive (i.e., ZIP or JAR)
     * @return the properties describing the new package
     * @exception IOException if the package file could not be saved
     */
    public static Properties createDefaultPackage(String[] classFiles, 
						  String packageDir, 
						  boolean fromArchive) 
	throws IOException 
    {	
	Properties props = new Properties();
	int numberOfTargets = classFiles.length;
	// every file returned by the filter is considered valid, so the array
	// size if the number of targets in the package
	props.put("package.numTargets", "" + numberOfTargets);

	String packageName = getPackageName(packageDir);
	if (packageName == null)
	    packageName = "unknown";
	else if (packageName != "") {
	    // only write the package name if it has a value, 
	    // then append the "." for later when using the
	    // package name as the root for sub package names
	    props.put("package.name", packageName);
	    packageName += ".";
	}
	
		
	// not too sure about this one, let's make it the current directory
	// for now
	// classdir is used to locate the class files for the corresponding java files
	// classdir is added to the classpath for this package
	props.put("package.classdir", ".");

	int nbrColumns = (int) Math.sqrt(new Double("" + numberOfTargets).doubleValue());
	int rowPos = STARTROWPOS;
	int columnPos = STARTCOLUMNPOS;
	// try and layout the targets in a grid, one row at a time
	for (int current = 0; current < classFiles.length; current++) {
	    String currentFile = classFiles[current];
	    if (fromArchive == true) {
		// with archive files, don't check for valid filenames on disk
		if (currentFile.endsWith(".class")) {
		    props.put("target" + (current + 1) + ".type", "ClassTarget");
		    // trim the .class off the filename for class targets	
		    props.put("target" + (current + 1) + ".name", currentFile.substring(0, currentFile.indexOf(".class")));
		} else {
		    props.put("target" + (current + 1) + ".type", "PackageTarget");
		    props.put("target" + (current + 1) + ".name", currentFile);
		    props.put("target" + (current + 1) + ".packageName", packageName + currentFile);
		}
	    } 
	    else {
				// let's assume if it's a directory, it's a package, otherwise it's a class
		if (new File(packageDir + File.separator + currentFile).isDirectory()) {
		    props.put("target" + (current + 1) + ".type", "PackageTarget");
		    props.put("target" + (current + 1) + ".name", currentFile);
		    props.put("target" + (current + 1) + ".packageName", packageName + currentFile);
		} 
		else {
		    props.put("target" + (current + 1) + ".type", "ClassTarget");
		    // trim the .class off the filename for class targets	
		    props.put("target" + (current + 1) + ".name", currentFile.substring(0, currentFile.indexOf(".class")));
		}
	    }
			
	    // make width roughly the length of the name
	    int targetWidth = DEFAULTTARGETCHARWIDTH * props.get("target" + (current + 1) + ".name").toString().length();
	    // add extra width for package targets (default equation leaves them too narrow)
	    targetWidth += props.get("target" + (current + 1) + ".type").toString().equals("PackageTarget") ? 20 : 0;
	    
	    props.put("target" + (current + 1) + ".width", "" + targetWidth);
	    props.put("target" + (current + 1) + ".height", "" + DEFAULTTARGETHEIGHT);
	    props.put("target" + (current + 1) + ".x", "" + rowPos);
	    props.put("target" + (current + 1) + ".y", "" + columnPos);
	    if ((current + 1) % nbrColumns == 0) {
		columnPos += DEFAULTTARGETHEIGHT + TARGETGAP;
		rowPos = STARTROWPOS;
	    } 
	    else
		rowPos += targetWidth + TARGETGAP;
				
	}
	// specify the dimensions large enough to see the entire package
	props.put("package.window.width", "" + DEFAULTFRAMEWIDTH);
	props.put("package.window.height", "" + DEFAULTFRAMEHEIGHT);
	
	if (fromArchive == false) {
	    // throw an exception if we cannot save
	    props.save(new FileOutputStream(packageDir + File.separator + pkgfileName), 
		       fromArchive == true ? "Default layout for archive library" : "Default package layout");
	}
	
	return props;
    }

    /**
     * Load the elements of a package from a specified directory 
     * @param dirname the directory from which to load the properties
     */
    void load(String dirname) {
	this.load(dirname, null, true, false);
    }
    
    /**
     * Load the elements of a package from a specified directory 
     *
     * @param dirname the directory from which to load the properties
     * @param props the already created properties for this package
     * @param readyToPaint true if the UI is in a state suitable for painting right now
     * @param libraryPackage true if this method was called to create a package for the library browser
     */
    public void load(String dirname, Properties props, boolean readyToPaint, boolean libraryPackage)
    {
	// Read the package properties
	String fullpkgfile = dirname + Config.slash + pkgfileName;
	this.dirname = dirname;

	// if we haven't been given properties to use, load them
	if (props == null) {
	    // try to load the package file for this package
	    try {
		FileInputStream input = new FileInputStream(fullpkgfile);

		props = new Properties();
		props.load(input);
	    } catch(IOException e) {
		Utility.reportError(iniLoadError + fullpkgfile + ": " + e);
		// if it's not found, let's create a default one
	    }

	    if (props == null)
		return;
	}
	
	this.packageName = props.getProperty("package.name", noPackage);
	String width_str = props.getProperty("package.window.width", "512");
	String height_str = props.getProperty("package.window.height", "450");
	frame.setSize(Integer.parseInt(width_str), Integer.parseInt(height_str));
		
	// This is to make sure that opening a package into an empty frame
	// works properly

	frame.invalidate();
	frame.validate();

	relclassdir = Config.getPath(props, "package.classdir");
	if(relclassdir != null) {
	    File cd = new File(relclassdir);
	    if(cd.isAbsolute())
		classdir = relclassdir;
	    else
		classdir = dirname + Config.slash + relclassdir;
	}

	setClasspath();

	// read in all the targets contained in this package

	try {
	    int numTargets = Integer.parseInt(props.getProperty("package.numTargets", "0"));
	    int numDependencies = Integer.parseInt(props.getProperty("package.numDependencies", "0"));

	    for(int i = 0; i < numTargets; i++) {
		Target target = null;
		String type = props.getProperty("target" + (i + 1) + ".type");
		
		if("ClassTarget".equals(type)) {
		    target = new ClassTarget(this);
		    // all library classes should be considered to be compiled
		    if (libraryPackage) {
			target.state = Target.S_NORMAL;
			((ClassTarget)target).setLibraryTarget(true);
		    }
		}
		else if("ImportedClassTarget".equals(type))
		    target = new ImportedClassTarget(this);
		else if("PackageTarget".equals(type))
		    target = new PackageTarget(this);

		if(target != null) {
		    target.load(props, "target" + (i + 1));
		    // Debug.message("Load target " + target);
		    addTarget(target);
		}
				// else
		// Debug.message("Failed to load target " + (i + 1));
	    }

	    for(int i = 0; i < numDependencies; i++) {
		Dependency dep = null;
		String type = props.getProperty("dependency" + (i+1) + ".type");

		if("UsesDependency".equals(type))
		    dep = new UsesDependency(this);
		else if("ExtendsDependency".equals(type))
		    dep = new ExtendsDependency(this);
		else if("ImplementsDependency".equals(type))
		    dep = new ImplementsDependency(this);
		
		if(dep != null) {
		    dep.load(props, "dependency" + (i + 1));
		    addDependency(dep, false);
		}
	    }
	    recalcArrows();
	} catch(Exception e) {
	    Utility.reportError(loadError + fullpkgfile + ": " + e);
	    e.printStackTrace();
	    return;
	}

	for(Enumeration e = targets.elements(); e.hasMoreElements(); ) {
	    Target t = (Target)e.nextElement();
	    if((t instanceof ClassTarget)
	       && ((ClassTarget)t).upToDate()) {
		ClassTarget ct = (ClassTarget)t;
		if (readyToPaint)
		    ct.setState(Target.S_NORMAL);
		// XXX: Need to invalidate things dependent on t
	    }
	}
    }

    /**
     * Save this package to disk. The package is saved to the standard
     * package file (bluej.pkg).
     */
    private boolean save(String dirname)
    {
	File dir = new File(dirname);
	if(!dir.exists())
	    if(!dir.mkdir()) {
		Utility.reportError(mkdirError + dirname);
		return false;
	    }

	Properties props = new Properties();
	String fullpkgfile = dirname + Config.slash + pkgfileName;

	File file = new File(fullpkgfile);
	if(file.exists()) {			// make backup of original
	    String backupName = dirname + Config.slash + pkgfileBackup;
	    file.renameTo(new File(backupName));
	}

	if(packageName != noPackage)
	    props.put("package.name", packageName);
	if(frame != null) {
	    Dimension size = frame.getSize();
	    props.put("package.window.width", String.valueOf(size.width));
	    props.put("package.window.height", String.valueOf(size.height));
	}

	// save targets and dependencies in package

	props.put("package.numTargets", String.valueOf(targets.size()));
	props.put("package.numDependencies", 
		  String.valueOf(usesArrows.size() + extendsArrows.size()));
	if(relclassdir != null)
	    Config.putPath(props, "package.classdir", relclassdir);

	Enumeration t_enum = targets.elements();		// targets
	for(int i = 0; t_enum.hasMoreElements(); i++) {
	    Target t = (Target)t_enum.nextElement();
	    t.save(props, "target" + (i + 1));
	}
	for(int i = 0; i < usesArrows.size(); i++) {		// uses arrows
	    Dependency d = (Dependency)usesArrows.elementAt(i);
	    d.save(props, "dependency" + (i + 1));
	}
	for(int i = 0; i < extendsArrows.size(); i++) {		// inherit arrows
	    Dependency d = (Dependency)extendsArrows.elementAt(i);
	    d.save(props, "dependency" + (usesArrows.size() + i + 1));
	}

	try {
	    FileOutputStream output = new FileOutputStream(fullpkgfile);
	    props.save(output, "BlueJ project file");
	} catch(IOException e) {
	    Utility.reportError(pkgSaveError + fullpkgfile + ": " + e);
	    return false;
	}
	
	return true;
    }

    public boolean save()
    {
	return save(dirname);
    }

    /**
     * Save this package under a new name. Returns an error code out of
     * (NO_ERROR, CREATE_ERROR, COPY_ERROR).
     *
     * @return An error code indicating success or failure.
     */
    public int saveAs(String newname)
    {
	if (!save(newname))
	    return CREATE_ERROR;

	boolean okay = true;
	Enumeration t_enum = targets.elements();
	for(int i = 0; t_enum.hasMoreElements(); i++) {
	    Target t = (Target)t_enum.nextElement();
	    okay = okay && t.copyFiles(newname + Config.slash);
	}
	// PENDING: update all package directives in sources

	dirname = newname;
	baseDir = null;		// will be recomputed
	setClasspath();

	if(okay)
	    return NO_ERROR;
	else
	    return COPY_ERROR;
    }

    /**
     * importFile - import a source file into this package as a new
     *  class target. Returns an error code:
     *   NO_ERROR       - everything is fine
     *   FILE_NOT_FOUND - file does not exist
     *   ILLEGAL_FORMAT - the file name does not end in ".java"
     *   CLASS_EXISTS - a class with this name already exists
     *   COPY_ERROR     - could not copy 
     */
    public int importFile(String sourcePath)
    {
	// PENDING: must rewrite or remove package line in class source!

	// check whether specified class exists and is a java file

	File sourceFile = new File(sourcePath);
	if(! sourceFile.exists())
	    return FILE_NOT_FOUND;
	String fileName = sourceFile.getName();

	String className;
	if(fileName.endsWith(".java"))		// it's a Java source file
	    className = fileName.substring(0, fileName.length() - 5);
	else 
	    return ILLEGAL_FORMAT;

	// check whether name is already used
	if(getTarget(className) != null)
	    return CLASS_EXISTS;

	// copy class source into package

	String destPath = dirname + Config.slash + fileName;
	if(!Utility.copyFile(sourcePath, destPath))
	    return COPY_ERROR;

	// create class icon (ClassTarget) for new class

	ClassTarget target = new ClassTarget(this, className);
	addTarget(target);

	return NO_ERROR;
    }
	
    public static boolean importDir(String dirname, String pkgname)
    {
	File dir = new File(dirname);
	if(!dir.isDirectory())
	    return false;
			
	Package newPkg = new Package();

	// create targets for all files in dirname
	newPkg.dirname = dirname;
	newPkg.packageName = pkgname;
		
	int targetnum = 0;
	String[] files = dir.list();
	for(int i = 0; i < files.length; i++) {
	    Target t = null;
	    String filename = files[i];
	    if(filename.endsWith(".java")) {  // it's a Java source file
		String classname = filename.substring(0, filename.length() - 5);
		t = new ClassTarget(newPkg, classname);
	    }
	    else if(pkgname != noPackage) {  // try subdirectories
		File f = new File(dirname, filename);
		String newname = pkgname + "." + filename;
		if(f.isDirectory() && importDir(f.getPath(), newname))
		    t = new PackageTarget(newPkg, filename, newname);
	    }

	    if(t != null) {
		newPkg.addTarget(t);
		t.setPos(20 + 100 * (targetnum % 5), 20 + 80 * (targetnum / 5));
		++targetnum;
	    }
	}
		
	if(targetnum > 0) {
	    newPkg.save(dirname);
	    return true;
	}
	else
	    return false;
    }

    public static boolean importDir(String dirname)
    {
	return importDir(dirname, noPackage);
    }

    /**
     * Add a new class to this package from a library.
     * Invoked by the library browser when the user
     * has selected a class, issued the "use" command
     * and selected the open package to use the class in.
     * 
     * @param packageName the name of the package in java format (eg. java.awt)
     * @param className the name of the class (eg. Frame)
     * @return an error code indicating the status of the insert (eg. NO_ERROR)
     */
    public int insertLibClass(String packageName, String className) {
	Debug.message("Inserting class: " + packageName + "-" + className + " in " + this.getFrame().getTitle());

	if (getFrame() instanceof PkgMgrFrame) {
	    String packagePath;
	    packagePath = ((PkgMgrFrame)getFrame()).getBrowser().getDirectoryForPackage(packageName);
	    Debug.message("Package lives in directory: " + packagePath);

	    // create class icon (ClassTarget) for new class
	    
	    ImportedClassTarget target = new ImportedClassTarget(this, 
								 className,
								 packageName,
								 packagePath);
	    target.setState(Target.S_NORMAL);
	    addTarget(target);
	}
 	return NO_ERROR;
    }
    
    /**
     * Add a new package to this package from a library.
     * Invoked by the library browser when the user
     * has selected a package, issued the "use" command
     * and selected the open package to use the package in.
     * 
     * @param packageName the name of the package in java format (i.e., java.awt)
     * @return an error code indicating the status of the insert (e.g., INSERTOK)
     */
    public int insertLibPackage(String packageName) {
	Debug.message("Inserting package: " + packageName + " in " + this.getFrame().getTitle());

	String packagePath = "";
	if (getFrame() instanceof PkgMgrFrame) {
	    packagePath = ((PkgMgrFrame)getFrame()).getBrowser().getDirectoryForPackage(packageName);
	    Debug.message("Package lives in directory: " + packagePath);
	}

	return NO_ERROR;
    }
    
    private void setClasspath()
    {
	classpath = System.getProperty("java.class.path");
	classpath += Config.colon;
	classpath += getClassDir();
    }

    public Enumeration getVertices()
    {
	return targets.elements();
    }

    public Enumeration getEdges()
    {
	Vector enumerations = new Vector();
		
	if(showUses)
	    enumerations.addElement(usesArrows.elements());
	if(showExtends)
	    enumerations.addElement(extendsArrows.elements());
		
	return new MultiEnumeration(enumerations);
    }

    /**
     * Return the base directory, ie the absolute path to the directory
     * that contains this package (the parent directory).
     */
    public String getBaseDir()
    {
	if(baseDir == null) {
	    if(packageName == noPackage)
		baseDir = dirname;
	    else {
		String dir = new File(dirname).getAbsolutePath();
				
		int next = 0;
		do {
		    dir = new File(dir).getParent();
		    next = packageName.indexOf('.', next + 1);
		} while((next != -1) && (dir != null));

		baseDir = dir;
	    }
	}
	return baseDir;
    }
	
    /** Use Tarjan's algorithm to construct compiler Jobs **/
    protected void search(ClassTarget t, int dfcount, Stack stack)
    {
	if((t.getState() != Target.S_INVALID) || t.isFlagSet(Target.F_QUEUED))
	    return;
		
	t.setFlag(Target.F_QUEUED);
	t.dfn = dfcount;
	t.link = dfcount;
		
	stack.push(t);
		
	Vector enumerations = new Vector();
	enumerations.addElement(usesArrows.elements());
	enumerations.addElement(extendsArrows.elements());
		
	for(Enumeration e = new MultiEnumeration(enumerations); e.hasMoreElements();  )
	    {
		Dependency d = (Dependency)e.nextElement();
		if(!(d.getTo() instanceof ClassTarget))
		    continue;
			
		ClassTarget to = (ClassTarget)d.getTo();
			
		if(to.isFlagSet(Target.F_QUEUED))
		    {
			if((to.dfn < t.dfn) && (stack.search(to) != -1))
			    t.link = Math.min(t.link, to.dfn);
		    }
		else if(to.getState() == Target.S_INVALID)
		    {
			search((ClassTarget)to, dfcount + 1, stack);
			t.link = Math.min(t.link, to.link);
		    }
	    }
			
	if(t.link == t.dfn)
	    {
		Vector v = new Vector();
		ClassTarget x;
			
		do {
		    x = (ClassTarget)stack.pop();
				
		    v.addElement(x.sourceFile());
		} while(x != t);

		String[] files = new String[v.size()];
		v.copyInto(files);
		bluej.compiler.Main.addJob(files, this, classpath, getClassDir());
	    }
    }

    public void compileSet(Vector toCompile)
    {
	for(int i = toCompile.size() - 1; i >= 0; i--)
	    search((ClassTarget)toCompile.elementAt(i), 1, new Stack());
    }

    /**
     ** The standard compile user function
     **/
    public void compile()
    {
	Vector toCompile = new Vector();
		
	for(Enumeration e = targets.elements(); e.hasMoreElements(); )
	    {
		Target target = (Target)e.nextElement();
			
		if(target instanceof ClassTarget)
		    {
			ClassTarget ct = (ClassTarget)target;
			if (ct.editorOpen())
			    ct.getEditor().save();
			if(ct.getState() == Target.S_INVALID)
			    toCompile.addElement(ct);
		    }
	    }
			
	compileSet(toCompile);
    }

    /**
     ** Force compile of all classes. Called by user function "rebuild".
     **/
    public void compileAll()
    {
	Vector v = new Vector();
		
	for(Enumeration e = targets.elements(); e.hasMoreElements(); )
	    {
		Target target = (Target)e.nextElement();
			
		if(target instanceof ClassTarget)
		    {
			ClassTarget ct = (ClassTarget)target;
			if (ct.editorOpen())
			    ct.getEditor().save();
			ct.setState(Target.S_INVALID);
			v.addElement(ct.sourceFile());
		    }
	    }
			
	String[] files = new String[v.size()];
	v.copyInto(files);
	bluej.compiler.Main.addJob(files, this, classpath, classdir);
    }

    public void addTarget(Target t)
    {
	targets.put(t.getName(), t);
    }

    public void removeTarget(Target t)
    {
	targets.remove(t.getName());
    }

    /*
     * Removes a class from the Package
     *
     * @param removableTarget the ClassTarget representing the class to be removed.
     */
    public void removeClass(Target removableTarget)
    {
	if(removableTarget instanceof ClassTarget) 
	    ((ClassTarget)removableTarget).prepareForRemoval();

	removeTarget(removableTarget);
	editor.repaint();
	save();
    }

    public void addDependency(Dependency d, boolean recalc)
    {
	if(d instanceof UsesDependency) {
	    if(usesArrows.contains(d))
		return;
	    else
		usesArrows.addElement(d);
	}
	else {
	    if(extendsArrows.contains(d))
		return;
	    else
		extendsArrows.addElement(d);
	}
		
	Target from = (Target)d.getFrom();
	from.addDependencyOut(d, recalc);
	Target to = (Target)d.getTo();
	to.addDependencyIn(d, recalc);
    }

    public void removeDependency(Dependency d, boolean recalc)
    {
	// Debug.message("Removing dependency " + d);
	if(d instanceof UsesDependency)
	    usesArrows.removeElement(d);
	else
	    extendsArrows.removeElement(d);
	Target from = (Target)d.getFrom();
	from.removeDependencyOut(d, recalc);
	Target to = (Target)d.getTo();
	to.removeDependencyIn(d, recalc);
    }

    public void recalcArrows()
    {
	Enumeration e = getVertices();
	while(e.hasMoreElements()) {
	    Target t = (Target)e.nextElement();
	    
	    t.recalcInUses();
	    t.recalcOutUses();
	}
    }

    public void setActiveVertex(Vertex v)
    {
	if(selected != null)
	    selected.toggleFlag(Target.F_SELECTED);
	selected = (Target)v;
	if(selected != null) {
	    // XXX: currently broken
	    // int index = targets.indexOf(selected);
	    // int last = targets.size() - 1;
	    // Swap selected vertex with top
	    // targets.setElementAt(targets.elementAt(last), index);
	    // targets.setElementAt(selected, last);

	    selected.toggleFlag(Target.F_SELECTED);
	}
    }

    public Target getTarget(String tname)
    {
	Target t = (Target)targets.get(tname);
	return t;
    }

    /**
     * Given a file name, find the target that represents that file.
     *
     * @return The target with the given file name or <null> if not found.
     */
    public ClassTarget getTargetFromFilename(String filename)
    {
	for(Enumeration e = targets.elements(); e.hasMoreElements(); )
	    {
		Target t = (Target)e.nextElement();
		if(!(t instanceof ClassTarget))
		    continue;
				
		ClassTarget ct = (ClassTarget)t;

		if(filename.equals(ct.sourceFile()))
		    return ct;
	    }

	return null;
    }

    public EditableTarget getTargetFromEditor(Editor editor)
    {
	for(Enumeration e = targets.elements(); e.hasMoreElements(); )
	    {
		Target t = (Target)e.nextElement();
		if(!(t instanceof EditableTarget))
		    continue;
				
		EditableTarget et = (EditableTarget)t;

		if(et.usesEditor(editor))
		    return et;
	    }

	return null;
    }

    public boolean toggleShowUses()
    {
	showUses = !showUses;
	return showUses;
    }
	
    public boolean toggleShowExtends()
    {
	showExtends = !showExtends;
	return showExtends;
    }
	
    public void setState(int state)
    {
	// Clean up after current state, if necessary
	switch(this.state) {
	case S_DELARROW:
	    if(currentArrow != null) {
		currentArrow.highlight(frame.editor.getGraphics());
		currentArrow = null;
	    }
	    frame.editor.removeMouseListener(this);
	    frame.editor.removeMouseMotionListener(this);
	    break;
	}
		
	this.state = state;
		
	// Set up new state, if necessary
	switch(this.state) {
	case S_DELARROW:
	    frame.editor.addMouseListener(this);
	    frame.editor.addMouseMotionListener(this);
	    break;
	}
    }

    public int getState()
    {
	return state;
    }
	
    /**
     * Return a path to a file in this package. This is done
     * by concatenating the path to the package and the name
     * of the file.
     *
     * @param basename  The name of the file in the package.
     * @return  The path to the file within the package.
     */
    public String getFileName(String basename)
    {
	return dirname + Config.slash + basename;
    }
	
    public String getClassFileName(String basename)
    {
	String dir = dirname;

	if(classdir != null) {
	    dir = classdir;
	    if(basename.indexOf('.') == -1)
		basename = getQualifiedName(basename);
	}
			
	return dir + Config.slash + basename.replace('.', Config.slash);
    }
	
    /**
     * Return the name of a target qualified by the package name (eg for
     * basename "JFrame" and package "javax.swing" return
     * "javax.swing.JFrame"). If the base name is already qualified, or
     * the package has no name, the basename is returned unchanged.
     */
    public String getQualifiedName(String basename)
    {
	if((packageName == noPackage) || (basename.indexOf('.', 0) != -1))
	    return basename;
	else
	    return packageName + "." + basename;
    }

    /**
     ** Called when in an interesting state (e.g. adding a new dependency)
     ** and a target is selected.
     **/
    void targetSelected(Target t)
    {
	switch(getState()) {
	    case S_CHOOSE_USES_FROM:
		fromChoice = t;
		setState(S_CHOOSE_USES_TO);
		frame.setStatus(chooseUsesTo);
		break;

	    case S_CHOOSE_USES_TO:
		if (t != fromChoice) {
		    setState(S_IDLE);
		    addDependency(new UsesDependency(this, fromChoice, t), true);
		    frame.clearStatus();
		}
		break;

	    case S_CHOOSE_EXT_FROM:
		fromChoice = t;
		setState(S_CHOOSE_EXT_TO);
		frame.setStatus(chooseInhTo);
		break;

	    case S_CHOOSE_EXT_TO:
		if (t != fromChoice) {
		    setState(S_IDLE);
		    if(t instanceof ClassTarget) {
			if(((ClassTarget)t).isInterface())
			    addDependency(new ImplementsDependency(this, fromChoice, t), true);
			else
			    addDependency(new ExtendsDependency(this, fromChoice, t), true);
		    }
		    frame.clearStatus();
		}
		break;

	    default:
		// e.g. deleting arrow - selecting target ignored
		break;
	}
	if (getState() == S_IDLE) 
	    frame.resetDependencyButtons();
    }
	
    /**
     * Report an execption. Usually, we do this through "errorMessage", but
     * if we cannot make sense of the message format, and thus cannot figure
     * out class name and line number, we use this way.
     */
    public void reportException(String text)
    {
	Utility.showMessage(frame,
			    "An exception was thrown:\n" + text);
    }

    /**
     * A thread has hit a breakpoint. Organise display (highlight line
     * in source, pop up exec controls).
     */
    public void hitBreakpoint(String sourcename, int lineNo, 
			      String threadName, boolean firstHit)
    {
	String msg = " ";

	if(firstHit)
	    msg = "Thread \"" + threadName + "\" stopped at breakpoint.";
	showEditorMessage(getFileName(sourcename), lineNo, msg,
			  false, false);
	frame.showHideExecControls(true, firstHit);
	frame.haltExecution();
    }

    /**
     * Display an error message associated with a specific line in a class.
     * This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    private void showEditorMessage(String filename, int lineNo, 
				   String message, boolean invalidate, 
				   boolean beep)
    {
	ClassTarget t = getTargetFromFilename(filename);

	if(t == null)
	    return;

	if(invalidate) {
	    t.setState(Target.S_INVALID);
	    t.unsetFlag(Target.F_QUEUED);
	}

	t.open();
	Editor editor = t.getEditor();
	if(editor!=null)
	    editor.displayMessage(message, lineNo, 0, beep, false);
    }
	
    // ---- bluej.compiler.CompileObserver interface ----

    public void startCompile(String[] sources)
    {
	frame.setStatus(compiling);
	for(int i = 0; i < sources.length; i++)
	    {
		String filename = sources[i];
			
		ClassTarget t = getTargetFromFilename(filename);
		if(t != null)
		    t.setState(ClassTarget.S_COMPILING);
		// else
		// Debug.message(noTarget + filename);
	    }
    }

    /**
     * Display an error message associated with a specific line in a class.
     * This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    public void errorMessage(String filename, int lineNo, String message,
			     boolean invalidate)
    {
	showEditorMessage(filename, lineNo, message, invalidate, true);
    }
	
    public void endCompile(String[] sources, boolean successful)
    {
	for(int i = 0; i < sources.length; i++)
	    {
		String filename = sources[i];
			
		ClassTarget t = getTargetFromFilename(filename);
		t.setState(successful ? Target.S_NORMAL : Target.S_INVALID);
		t.unsetFlag(Target.F_QUEUED);
	    }
	frame.setStatus(compileDone);
	frame.editor.repaint();
    }
	
    // ---- sun.tools.javac.CompileWatcher interface ----

    /**
     * Report an exit of a method through "System.exit()" where we expected
     * a result or an object being created.
     */
    public void reportExit(String exitCode)
    {
	Utility.showMessage(frame,
			    "The method finished through an explicit\n" +
			    "\"exit\" instruction. No result was\n" +
			    "returned. The exit code is " + exitCode + ".");
    }

    public void notifyParsed(ClassDeclaration decl, SourceClass src, 
			     BatchEnvironment env)
    {
	String srcName = src.getName().toString();
	Target srcTarget = getTarget(srcName);
		
	if(srcTarget == null) {
	    // Debug.message("notifyParsed: Failed to get target for " + srcName);
	    return;	// nothing we can do without the source target
	}
		
	BlueJDocumentationGenerator dgen = new BlueJDocumentationGenerator(env);
	if(packageName != noPackage)
	    dgen.addPrefix(packageName + ".");
	CommentList comments = dgen.genComments(src);
		
	String ctxtFilename = getClassFileName(srcTarget.getBaseName()) + ".ctxt";
	try {
	    comments.save(ctxtFilename);
	} catch (IOException ex) {
	    Utility.reportError(docSaveError + ctxtFilename);
	}
    }
	
    public void notifyCompiled(SourceClass src, BatchEnvironment env)
    {
	String srcName = src.getName().toString();
	ClassTarget srcTarget = (ClassTarget)getTarget(srcName);
		
	if(srcTarget == null) {
	    // Debug.message("notifyCompiled: Failed to get target for " + srcName);
	    return;	// nothing we can do without the source target
	}
		
	srcTarget.setModifiers(src.getModifiers());
			
	// XXX: remove existing dependencies???
		
	Hashtable used = new Hashtable();
		
	if(src.getSuperClass() != null)	{	// needed for java.lang.Object
	    String superName = src.getSuperClass().getName().toString();
	    Target superTarget = getTarget(superName);
	    if(superTarget != null) {
		addDependency(new ExtendsDependency(this, srcTarget, 
						    superTarget), true);
		used.put(superName, superName);
	    }
	    // else
	    // Debug.message("Skipping " + srcName + " => " + superName);
	}
		
	ClassDeclaration[] interfaces = src.getInterfaces();
	for(int i = 0; i < interfaces.length; i++) {
	    String intName = interfaces[i].getName().toString();
	    Target intTarget = getTarget(intName);
	    if(intTarget != null) {
		addDependency(new ImplementsDependency(this, srcTarget,
						       intTarget), true);
		used.put(intName, intName);
	    }
	    // else
	    // Debug.message("Skipping " + srcName + " => " + intName);
	}
		
	for(Enumeration e = BlueJJavacMain.getDependencies(src); e.hasMoreElements(); ) {
	    ClassDeclaration to = (ClassDeclaration)e.nextElement();
	    String toName = to.getName().toString();
	    if(used.get(toName) != null) {
		// Debug.message("Skipping " + srcName + " -> " + toName);
		continue;
	    }
	    Target toTarget = getTarget(toName);
	    if(toTarget != null)
		addDependency(new UsesDependency(this, srcTarget, toTarget), true);
	    // else
	    // Debug.message("Can't find used class " + toName);
	}

	if (srcTarget.editorOpen())
	    srcTarget.getEditor().setCompiled(true);
    }
	
    /**
     ** getSearcher - get the ClasspathSearcher for this package
     **/
    public synchronized ClasspathSearcher getSearcher()
    {
	if(searcher == null)
	    searcher = new ClasspathSearcher(getClassDir());
		
	return searcher;
    }
	
    /**
     ** getLocalClassLoader - get the ClassLoader for this package.
     **  The SimpleClassLoader load classes on the local VM.
     **/
    private synchronized SimpleClassLoader getLocalClassLoader()
    {
	if(loader == null)
	    loader = new SimpleClassLoader(getSearcher());
		
	return loader;
    }

    /**
     * removeLocalClassLoader - removes the current classloader, and 
     *  removes references to classes loaded by it (this includes removing
     *  the objects from the object bench).
     *  Should be run whenever a source file changes
     */
    synchronized void removeLocalClassLoader()
    {
	if(loader != null) {
	    // remove objects loaded by this classloader
	    ObjectBench bench = getBench();
	    ObjectWrapper[] wrappers = bench.getWrappers();
	    for(int i = 0; i < wrappers.length; i++) {
		if(wrappers[i].getPackage() == this)
		    bench.remove(wrappers[i], getId());
	    }

	    // XXX: remove views for classes loaded by this classloader

	    loader = null;
	}
    }

    /**
     ** getDebuggerClassLoader - get the DebuggerClassLoader for this
     **  package. The DebuggerClassLoader load classes on the remote VM
     **  (the machine used for user code execution).
     **/
    public synchronized DebuggerClassLoader getRemoteClassLoader()
    {
	if(debuggerLoader == null)
	    debuggerLoader = Debugger.debugger.createClassLoader(getId(), getClassDir());
	return debuggerLoader;
    }
	
    /**
     * removeRemoteClassLoader - removes the remote VM classloader
     *  Should be run whenever a source file changes
     */
    synchronized void removeRemoteClassLoader()
    {
	if(debuggerLoader != null) {
	    Debugger.debugger.removeClassLoader(debuggerLoader);
	    debuggerLoader = null;
	}
    }

    /**
     * loadClass - loads a class using the current classLoader
     * creates a classloader if none currently exists
     */
    public Class loadClass(String className)
    {
	SimpleClassLoader loader = getLocalClassLoader();

	try {
	    return loader.loadClass(className);
	} catch(ClassNotFoundException e) {
	    e.printStackTrace();
	    return null;
	}
    }
	

    /**
     ** closeAllEditors - closes all currently open editors within package
     ** Should be run whenever a package is removed from PkgFrame.
     **/
    public void closeAllEditors()
    {
	for(Enumeration e = targets.elements(); e.hasMoreElements(); )
	    {
		Target t = (Target)e.nextElement();
		if(t instanceof ClassTarget)
		    {
			ClassTarget ct = (ClassTarget)t;
			if(ct.editorOpen())
			    ct.getEditor().close();
		    }
	    }
    }


    /**
     ** get history of invocation calls
     ** @return CallHistory object
     **/
    public CallHistory getCallHistory()
    {
	return callHistory;
    }


	
    // Add a title to printouts
    static final int PRINT_HMARGIN = 16;
    static final int PRINT_VMARGIN = 16;
    static final Font printTitleFont = new Font("SansSerif", Font.PLAIN, 
						Config.printTitleFontsize);
    static final Font printInfoFont = new Font("SansSerif", Font.ITALIC, 
					       Config.printInfoFontsize);

    /**
     * Return the rectangle on the page in which to draw the class diagram.
     * The rectangle is the page minus margins minus space for header and
     * footer text.
     */
    public Rectangle getPrintArea(Dimension pageSize)
    {
	FontMetrics tfm = frame.getFontMetrics(printTitleFont);
	FontMetrics ifm = frame.getFontMetrics(printInfoFont);
		
	return new Rectangle(PRINT_HMARGIN,
			     PRINT_VMARGIN + tfm.getHeight() + 4,
			     pageSize.width - 2 * PRINT_HMARGIN,
			     pageSize.height - 2 * PRINT_VMARGIN - 
			       tfm.getHeight() - ifm.getHeight() - 4 );
    }

    /**
     * Print the page title and other page decorations (frame, footer).
     */
    public void printTitle(Graphics g, Dimension pageSize, int pageNum)
    {
	FontMetrics tfm = frame.getFontMetrics(printTitleFont);
	FontMetrics ifm = frame.getFontMetrics(printInfoFont);
	Rectangle printArea = getPrintArea(pageSize);

	// frame header area
	g.setColor(lightGrey);
	g.fillRect(printArea.x, PRINT_VMARGIN, printArea.width, 
		   printArea.y - PRINT_VMARGIN);

	g.setColor(titleCol);
	g.drawRect(printArea.x, PRINT_VMARGIN, printArea.width, 
		   printArea.y - PRINT_VMARGIN);

	// frame print area
	g.drawRect(printArea.x, printArea.y, printArea.width, 
		   printArea.height);

	// write header
	String title = (packageName == noPackage) ? dirname : packageName;
	g.setFont(printTitleFont);
	Utility.drawCentredText(g, "BlueJ package - " + title,
				printArea.x, PRINT_VMARGIN, 
				printArea.width, tfm.getHeight());

	// write footer
	g.setFont(printInfoFont);
	Utility.drawRightText(g, (new Date()) + ", page " + pageNum,
			      printArea.x, printArea.y + printArea.height,
			      printArea.width, ifm.getHeight());
    }
	
    /**
     ** Called after a change to a Target
     **/
    public void invalidate(Target t)
    {
	if(t instanceof ClassTarget) {
	    ClassTarget ct = (ClassTarget)t;

	    if((loader != null) && loader.hasClass(ct.getName())) {
		removeLocalClassLoader();
		removeRemoteClassLoader();
	    }
	}
    }

    public void compile(ClassTarget ct)
    {
	if (ct.editorOpen())
	    ct.getEditor().save();
	ct.setState(Target.S_INVALID);		// to force compile
	Vector toCompile = new Vector();
	toCompile.addElement(ct);
	compileSet(toCompile);
    }
	
    /**
     ** find an arrow, given a point on the screen
     **/
    Dependency findArrow(int x, int y)
    {
	// FIXME: check if translation necessary (scrolling, etc.)
		
	for(Enumeration e = usesArrows.elements(); e.hasMoreElements(); ) {
	    Dependency d = (Dependency)e.nextElement();
	    if(d.contains(x, y))
		return d;
	}
		
	for(Enumeration e = extendsArrows.elements(); e.hasMoreElements(); ) {
	    Dependency d = (Dependency)e.nextElement();
	    if(d.contains(x, y))
		return d;
	}
		
	return null;
    }
	
    // MouseListener interface

    public void mousePressed(MouseEvent evt)
    {
	switch(state) {
	    case S_DELARROW:
		Dependency selectedArrow = findArrow(evt.getX(), evt.getY());
		if((currentArrow != null) && (currentArrow != selectedArrow))
		    currentArrow.highlight(frame.editor.getGraphics());
		if(selectedArrow != null)
		    {
			removeDependency(selectedArrow, true);
			frame.editor.repaint();
		    }
		currentArrow = null;
		setState(S_IDLE);
		break;
	}
    }
	
    public void mouseReleased(MouseEvent evt) {}
    public void mouseClicked(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    // MouseMotionListener interface

    public void mouseDragged(MouseEvent evt) {}

    public void mouseMoved(MouseEvent evt)
    {
	switch(state) {
	case S_DELARROW:	// currently deleting an arrow
		Dependency selectedArrow = findArrow(evt.getX(), evt.getY());
		if((currentArrow != null) && (currentArrow != selectedArrow))
		    currentArrow.highlight(frame.editor.getGraphics());
		if((selectedArrow != null) && (currentArrow != selectedArrow))
		    selectedArrow.highlight(frame.editor.getGraphics());
		currentArrow = selectedArrow;
		break;
	}
    }
	
    // Internal strings
    static String noTarget = Config.getString("pkgmgr.noTarget");
    static String iniLoadError = Config.getString("pkgmgr.iniLoadError");
    static String loadError = Config.getString("pkgmgr.loadError");
    static String mkdirError = Config.getString("pkgmgr.mkdirError");
    static String pkgSaveError = Config.getString("pkgmgr.pkgSaveError");
    static String docSaveError = Config.getString("pkgmgr.docSaveError");
}
