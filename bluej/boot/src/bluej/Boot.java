package bluej;

import java.io.File;
import java.net.*;
import java.util.ArrayList;

import sun.misc.*;

/**
 * This class is now the starting point of BlueJ.
 * In order to isolate the libraries used by BlueJ (antlr etc.) from the
 * Extensions, we need to keep those libraries out of the classpath and 
 * start BlueJ with a ClassLoader that understands how to find them.
 * To make sure that classloader is used by by BlueJ we need bluej.Main itself to
 * be loaded by it.
 *
 * So, we need this bootstrap Class, which just sets up the new ClassLoader and 
 * then constructs a bluej.Main object with it.
 *
 * A side effect is that this class also understands where BlueJ is
 * and where the java runtime is.
 *
 * @author	Damiano Bolla
 * @version $Id: Boot.java 2021 2003-06-05 03:16:48Z ajp $
 */
public class Boot
{
    public static int BLUEJ_VERSION_MAJOR = 1;
    public static int BLUEJ_VERSION_MINOR = 3;
    public static int BLUEJ_VERSION_RELEASE = 0;
    public static String BLUEJ_VERSION_SUFFIX = " beta 3";

    public static int BLUEJ_VERSION_NUMBER = BLUEJ_VERSION_MAJOR * 1000 +
                                             BLUEJ_VERSION_MINOR * 100 +
                                             BLUEJ_VERSION_RELEASE;

    public static String BLUEJ_VERSION = BLUEJ_VERSION_MAJOR
                                         + "." + BLUEJ_VERSION_MINOR
                                         + "." + BLUEJ_VERSION_RELEASE
                                         + BLUEJ_VERSION_SUFFIX;

    public static String BLUEJ_VERSION_TITLE = "BlueJ " + BLUEJ_VERSION;

    // A singleton boot object so the rest of BlueJ can pick up args etc.
    private static Boot instance;
    
	// The jar files we expect in the BlueJ lib directory
	private static String jars[] = { "bluejcore.jar", "bluejeditor.jar", "bluejext.jar",
									 "antlr.jar", "junit.jar", "MRJ141Stubs.jar",
									 "MRJToolkitStubs.zip" };

	private static boolean useClassesDir = false;
	
    /**
     * Entry point for booting BlueJ
     * bluej.Boot should be listed in the JAR Manifest as the Main-Class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args)
    {
		if((args.length >= 1) && "-version".equals(args[0])) {
			System.out.println("BlueJ version " + BLUEJ_VERSION
							   + " (Java version "
							   + System.getProperty("java.version")
							   + ")");
			System.out.println("--");

			System.out.println("virtual machine: "
							   + System.getProperty("java.vm.name")
							   + " "
							   + System.getProperty("java.vm.version")
							   + " ("
							   + System.getProperty("java.vm.vendor")
							   + ")");

			System.out.println("running on: "
							   + System.getProperty("os.name")
							   + " "
							   + System.getProperty("os.version")
							   + " ("
							   + System.getProperty("os.arch")
							   + ")");
			System.exit(-1);
		}

		if((args.length >= 1) && "-useclassesdir".equals(args[0])) {
			useClassesDir = true;
		}
		
		SplashWindow splash = new SplashWindow();
    	
        instance = new Boot(args);
        instance.bootBluej();

		splash.remove();
    }


    /**
     * Returns the singleton Boot instance, so the rest of BlueJ can find paths, args, etc.
     *
     * @return    the singleton Boot object instance
     */
    public static Boot get()
    {
        return instance;
    }


    // =================== Real Boot starts here =================================
    private String[] args;      // Command line arguments
    private File javaHomeDir;   // The value returned by System.getProperty
    private File bluejLibDir;   // Calculated below

    private URL[] bootClassPath;
    private ClassLoader bootLoader; // The loader this class is loaded with

    private URL[] runtimeClassPath; // The class path used to run the rest of BlueJ
    private URLClassLoader runtimeLoader;   // The class loader used for the rest of BlueJ


    /**
     * Retuns the args list passed to the starting program.
     *
     * @return    The args value
     */
    public String[] getArgs()
    {
        return args;
    }


    /**
     * Returns the home directory of the java we have been started with
     *
     * @return    The javaHome value
     */
    public File getJavaHome()
    {
        return javaHomeDir;
    }


    /**
     * Returns the BlueJ library directory.
     *
     * @return    The bluejLibDir value
     */
    public File getBluejLibDir()
    {
        return bluejLibDir;
    }


    /**
     * Returns the runtime classpath. The one used to run BlueJ.
     *
     * @return    The runtimeClassPath value
     */
    public URL[] getRuntimeClassPath()
    {
        return runtimeClassPath;
    }


    /**
     * Returns the runtime class path as a String.
     * Can be used to start another JVM which will then see the same
     * libraries as BlueJ itself.
     *
     * @return    The runtimeClassPathString value
     */
    public String getRuntimeClassPathString()
    {
        String pathSep = System.getProperty("path.separator");
        StringBuffer result = new StringBuffer(300);

        for (int index = 0; index < runtimeClassPath.length; index++) {
            String filename = runtimeClassPath[index].getFile();
            result.append(filename);
            result.append(pathSep);
        }

        return result.toString();
    }


    /**
     * Constructor for the singleton Boot object, invoked from main()
     *
     * @param  someArgs  the arguments with which main() was invoked
     */
    private Boot(String[] someArgs)
    {
        args = someArgs;
    }


    /**
     * Calculate the various path values, create a new classloader and
     * construct a bluej.Main. This needs to be outside the constructor to
     * ensure that the singleton instance is valid by the time
     * bluej.Main is run.
     */
    private void bootBluej()
    {
        // Remember the boot class path list.
        URLClassPath aPath = Launcher.getBootstrapClassPath();
        bootClassPath = aPath.getURLs();

        // Retrieve the current classLoader, this is the boot loader.
        bootLoader = getClass().getClassLoader();

        // Get the home directory of the Java implementation we're being run by
        javaHomeDir = new File(System.getProperty("java.home"));

        // Now work out what the BlueJ lib directory is.
        bluejLibDir = calculateBluejLibDir();

        try {
            // Find all the "hidden" libraries needed by BlueJ
            runtimeClassPath = getKnownJars();
            // Construct a new class loader which knows about them
            runtimeLoader = new URLClassLoader(runtimeClassPath, bootLoader);

            // Use the new class loader to find and construct a
            // bluej.Main object. This starts BlueJ "proper".
            Class theMainClass = Class.forName("bluej.Main", true, runtimeLoader);
            Object theMain = theMainClass.newInstance();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Calculate the bluejLibDir value by doing some reasoning on a resource 
     * we know we have: the .class file for the Boot class.
     * For example:
     * bootUrl=jar:file:/C:/home/bluej/bluej/lib/bluej.jar!/bluej/Boot.class
     * bootFullName=file:/C:/home/bluej/bluej/lib/bluej.jar!/bluej/Boot.class
     * bootName=file:/C:/home/bluej/bluej/lib/bluej.jar
     * finalName=/C:/home/bluej/bluej/lib/bluej.jar
     * Parent=C:\home\bluej\bluej\lib
     *
     * @return    the path of the BlueJ lib directory
     */
    private File calculateBluejLibDir()
    {
        URL bootUrl = getClass().getResource("Boot.class");
//    System.out.println ("bootUrl="+bootUrl);

        String bootFullName = bootUrl.getFile();
//   System.out.println ("bootFullName="+bootFullName);

        int classIndex = bootFullName.indexOf("!");
        String bootName = bootFullName.substring(0, classIndex);
//    System.out.println ("bootName="+bootName);

        if (!bootName.startsWith("file:"))
            throw new NullPointerException("bootName does not start with file:");

        // Get rid of the initial "file:" string
        String finalName = bootName.substring(5);
//    System.out.println ("finalName="+finalName);

        File finalFile = new File(finalName);
//    System.out.println ("finalFile="+finalFile);

        File bluejDir = finalFile.getParentFile();
//    System.out.println ("bluejDir="+bluejDir);

        return bluejDir;
    }

	/**
	 * Returns an array of URLs for all the required BlueJ jars
	 *
	 * @return                            URLs of the required JAR files
	 * @exception  MalformedURLException  for any problems with the URLs
	 */
	private URL[] getKnownJars() throws MalformedURLException
	{
		// by default, we require all our known jars to be present
		int startJar = 0;
		ArrayList urlList = new ArrayList();

		// a hack to let BlueJ run from within Eclipse.
		// If specified on command line, lets add a ../classes
		// directory to the classpath (where Eclipse stores the
		// .class files)
		if (useClassesDir) {
			File classesDir = new File(bluejLibDir.getParentFile(), "classes");
			
			if (classesDir.isDirectory()) {
				urlList.add(classesDir.toURI().toURL());
				// skip over requiring bluejcore.jar, bluejeditor.jar etc.
				startJar = 3;
			}
		}

		for (int i=startJar; i < jars.length; i++) {
			File toAdd;
		
			toAdd = new File(bluejLibDir, jars[i]);

			if (!toAdd.canRead())
				throw new IllegalStateException("required jar is missing or unreadable: " + toAdd);

			urlList.add(toAdd.toURI().toURL());
		}
		
		// We also need to add tools.jar
		urlList.add(getToolsURL());

		return (URL[]) urlList.toArray(new URL[0]);
	}
	
    /**
     * Returns an array of URLs for all the JAR files located in the lib/ext directory
     *
     * @return                            URLs of the discovered JAR files
     * @exception  MalformedURLException  for any problems with the URLs
     */
    private URL[] getLibraryItems() throws MalformedURLException
    {
        File extDir = new File(bluejLibDir, "ext");

        File[] files = extDir.listFiles();
        if (files == null) {
            // There are no files (at all) here
            throw new NullPointerException("Boot.getLibraryItems: BlueJ libraries (lib/ext) not found");
        }

        ArrayList urlList = new ArrayList();
        for (int index = 0; index < files.length; index++) {
            File thisFile = files[index];

            // Skip nested directories
            if (thisFile.isDirectory())
                continue;

            // Skip files that do not end in .jar or .zip
            if (!hasValidExtension(thisFile))
                continue;

            // This one looks good, add it to the list.
            urlList.add(thisFile.toURL());
        }

        // We also need to add tools.jar
        urlList.add(getToolsURL());

        return (URL[]) urlList.toArray(new URL[0]);
    }



    /**
     * Try to decide if this filename has the right extension to be a
     * library
     *
     * @param  aFile  the File to be checked
     * @return        true if the File could be library
     */
    private boolean hasValidExtension(File aFile)
    {
        if (aFile == null)
            return false;

        // If it ends in jar it is good.
        if (aFile.getName().endsWith(".jar"))
            return true;

        // if it ends in zip also
        if (aFile.getName().endsWith(".zip"))
            return true;

        return false;
    }


    /**
     * Get the URL of the  current tools.jar file
     * Looks for lib/tools.jar in the current javaHome
     * and in the parent of it.
     *
     * @return                            The URL of the tools.jar file for the current Java implementation
     * @exception  MalformedURLException  for any problems with the URL
     */
    private URL getToolsURL() throws MalformedURLException
    {
        File toolsFile = new File(javaHomeDir, "lib/tools.jar");
        if (toolsFile.canRead())
            return toolsFile.toURL();

        File parentDir = javaHomeDir.getParentFile();
        toolsFile = new File(parentDir, "lib/tools.jar");
        if (toolsFile.canRead())
            return toolsFile.toURL();

        throw new NullPointerException("Boot.getToolsUrl: Cannot find tools.jar. javaHome=" + javaHomeDir);
    }

}
