package bluej.pkgmgr;

import java.util.jar.*;
import java.util.zip.*;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.io.*;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * Component to manage storing projects to jar file format.
 *
 * @author  Michael Kolling
 * @version $Id: ExportManager.java 3480 2005-07-27 18:47:08Z damiano $
 */
final class ExportManager
{
    private static final String specifyJar = Config.getString("pkgmgr.export.specifyJar");
    private static final String createJarText = Config.getString("pkgmgr.export.createJarText");
    
    private static final String sourceSuffix = ".java";
    private static final String contextSuffix = ".ctxt";
    private static final String packageFilePrefix = "bluej.pk";
    private static final String packageFileBackup = "bluej.pkh";

    private PkgMgrFrame frame;

    public ExportManager(PkgMgrFrame frame)
    {
        this.frame = frame;
    }

    /**
     * Envoke the "create jar" user function. This starts by displaying the
     * export dialog, then it reads the options and performs the export to jar.
     */
    public void export()
    {
        ExportDialog dialog = new ExportDialog(frame);
        boolean okay = dialog.display();

        if(!okay)
            return;

        String fileName = FileUtility.getFileName(frame, specifyJar, createJarText, 
                                                 false, null, false);
        if(fileName == null)
            return;

        String sourceDir = frame.getProject().getProjectDir().getPath();

        createJar(fileName, sourceDir, dialog.getMainClass(), dialog.getSelectedLibs(),
                  dialog.includeSource(), dialog.includePkgFiles());
    }

    /**
     * Export this project to a jar file.
     */
    private void createJar(String fileName, String sourceDir, String mainClass,
                           List userLibs, boolean includeSource, boolean includePkgFiles)
    {
        // Construct classpath with used library jars
        
        String classpath = "";

        // add jar files from +libs to classpath
        // TODO: This logic is not correct, all libraries are in a single place now, no need to look in multiple places.
        File[] projectLibs = frame.getProject().getClassLoader().getClassPathAsFiles();
        for(int i=0; i < projectLibs.length; i++) {
            classpath += " " + projectLibs[i].getName();
        }
        
        // add jar files from userlibs to classpath
        for(Iterator it = userLibs.iterator(); it.hasNext(); ) {
            classpath += " " + ((File)it.next()).getName();
        }
        
        File jarFile = null;
        File parent = null;
        
        if(classpath.length() == 0) {
            // if we don't have library jars, just create a single jar file
            if(!fileName.endsWith(".jar"))
                fileName = fileName + ".jar";

            jarFile = new File(fileName);
            
            if(jarFile.exists()) {
                if (DialogManager.askQuestion(frame, "error-jar-exists") != 0)
                    return;
            }
        }
        else {
            // if we have library jars, create a directory with the new jar file
            // and all library jar files in it
            if(fileName.endsWith(".jar"))
                fileName = fileName.substring(0, fileName.length() - 4);
            parent = new File(fileName);

            if(parent.exists()) {
                if (DialogManager.askQuestion(frame, "error-jar-exists") != 0)
                    return;
            }
            parent.mkdir();
            jarFile = new File(parent, parent.getName() + ".jar");
        }
        
        OutputStream oStream = null;
        JarOutputStream jStream = null;

        try {
            // create manifest
            Manifest manifest = new Manifest();
            Attributes attr = manifest.getMainAttributes();
            attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attr.put(Attributes.Name.MAIN_CLASS, mainClass);
            attr.put(Attributes.Name.CLASS_PATH, classpath);

            // create jar file
            oStream = new FileOutputStream(jarFile);
            jStream = new JarOutputStream(oStream, manifest);

            writeDirToJar(new File(sourceDir), "", jStream, includeSource,
                            includePkgFiles,
                            jarFile.getCanonicalFile());
            if(parent != null) {
                copyLibsToJar(Arrays.asList(projectLibs), parent);
                copyLibsToJar(userLibs, parent);
            }
            
            frame.setStatus(Config.getString("pkgmgr.exported.jar"));
        }
        catch(IOException exc) {
            DialogManager.showError(frame, "error-writing-jar");
            Debug.reportError("problen writing jar file: " + exc);
        } finally {
            try {
                if(jStream != null)
                    jStream.close();
            } catch (IOException e) {}
        }
    }

    /**
     * Write the contents of a directory to a jar stream. Recursively called
     * for subdirectories.
     * outputFile should be the canonical file representation of the Jar file
     * we are creating (to prevent including itself in the Jar file)
     */
    private void writeDirToJar(File sourceDir, String pathPrefix,
                               JarOutputStream jStream, boolean includeSource, boolean includePkg, File outputFile)
        throws IOException
    {
        File[] dir = sourceDir.listFiles();
        for(int i = 0; i < dir.length; i++) {
            if(dir[i].isDirectory()) {
                if(!skipDir(dir[i], includePkg)) {
                    writeDirToJar(dir[i], pathPrefix + dir[i].getName() + "/",
                                  jStream, includeSource, includePkg, outputFile);
                }
            }
            else {
                // check against a list of file we don't want to export and also
                // check that we don't try to export the jar file we are writing
                // (hangs the machine)
                if(!skipFile(dir[i].getName(), !includeSource, !includePkg) &&
                    !outputFile.equals(dir[i].getCanonicalFile())) {
                        writeJarEntry(dir[i], jStream, pathPrefix + dir[i].getName());
                }
            }
        }
    }

    /**
     * Copy all files specified in the given list to the new jar directory.
     */
    private void copyLibsToJar(List userLibs, File destDir)
    {
        for(Iterator it = userLibs.iterator(); it.hasNext(); ) {
            File lib = (File)it.next();
            FileUtility.copyFile(lib, new File(destDir, lib.getName()));
        }
    }

    /** array of directory names not to be included in jar file **/
    private static final String[] skipDirs = { "CVS" };

    /**
     * Test whether a given directory should be skipped (not included) in
     * export.
     */
    private boolean skipDir(File dir, boolean includePkg)
    {
        if (dir.getName().equals(Project.projectLibDirName))
            return ! includePkg;
        
        for(int i = 0; i < skipDirs.length; i++) {
            if(dir.getName().equals(skipDirs[i]))
                return true;
        }
        return false;
    }

    /**
     * Checks whether a file should be skipped during a copy operation.
     * BlueJ specific files (bluej.pkg and *.ctxt) and - optionally - Java
     * source files are skipped.
     */
    private boolean skipFile(String fileName, boolean skipSource, boolean skipPkg)
    {
        if(fileName.equals(packageFileBackup))
            return true;
        
        if(fileName.endsWith(sourceSuffix))
            return skipSource;

        if(fileName.startsWith(packageFilePrefix) || fileName.endsWith(contextSuffix))
            return skipPkg;

        return false;
    }

    /**
     * Write a jar file entry to the jar output stream.
     * Note: entryName should always be a path with / seperators
     *       (NOT the platform dependant File.seperator)
     */
    private void writeJarEntry(File file, JarOutputStream jStream,
                                  String entryName)
        throws IOException
    {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            jStream.putNextEntry(new ZipEntry(entryName));
            FileUtility.copyStream(in, jStream);
        }
        catch(ZipException exc) {
            Debug.message("warning: " + exc);
        }
        finally {
            if(in != null)
                in.close();
        }
    }
}
