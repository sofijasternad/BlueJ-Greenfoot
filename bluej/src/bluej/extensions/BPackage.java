package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.Target;
import bluej.debugger.ObjectWrapper;



import java.util.List; 
import java.util.ListIterator;
import java.awt.Frame;



/**
 * A wrapper for a single package of a BlueJ project.
 * This represents an open package, and functions relating to that package.
 *
 * @version $Id: BPackage.java 1966 2003-05-21 09:09:15Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public class BPackage
{
    private Identifier packageId;

    /**
     * Constructor for a BPackage.
     */
    BPackage (Identifier aPackageId)
    {
        packageId=aPackageId;
    }


    /**
     * Returns the package's project.
     * It will return null if this is an invalid package.
     */
    public BProject getProject() throws ProjectNotOpenException
    {
        Project bluejProject = packageId.getBluejProject();
        if ( bluejProject == null ) return null;

        return new BProject (new Identifier(bluejProject));
    }



    /**
     * Returns the name of the package. 
     * Returns an empty string if no package name has been set.
     * Returns a null string if it is an invalid package.
     */
    public String getName() throws ProjectNotOpenException
    {
        Package bluejPkg = packageId.getBluejPackage();
        return bluejPkg.getQualifiedName();
    }
    
    /**
     * Returns the package frame.
     * This can be used (e.g.) as the "parent" frame for positioning modal dialogues.
     * Returns null if this is not a valid package.
     */
    public Frame getFrame() throws ProjectNotOpenException
    {
       Package bluejPkg = packageId.getBluejPackage();
       PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluejPkg);
       return pmf;
    }

    
    /**
     * Returns the class with the given name in this package.
     * Returns null if this is an invalid package.
     * Returns null if the class name does not exist.
     * 
     * @param name the simple name of the required class.
     */
    public BClass getBClass (String name) throws ProjectNotOpenException
    {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        Target aTarget = bluejPkg.getTarget (name);

        if ( aTarget == null ) return null;
        if ( !(aTarget instanceof ClassTarget)) return null;

        ClassTarget classTarget = (ClassTarget)aTarget;
        
        return new BClass (new Identifier (bluejPrj,bluejPkg, classTarget.getQualifiedName()));
    }
    
    /**
     * Returns an array containing all the classes in this package.
     * If there are no classes or the package is invalid an empty array will be returned.
     */
    public BClass[] getBClasses() throws ProjectNotOpenException
    {
        Project bluejPrj = packageId.getBluejProject();
        Package bluejPkg = packageId.getBluejPackage();

        String pkgBasename = bluejPkg.getBaseName();
        if ( pkgBasename.length() > 1 ) pkgBasename = pkgBasename+".";
        
        List names = bluejPkg.getAllClassnames();
        
        BClass[] classes = new BClass [names.size()];
        for (ListIterator iter=names.listIterator(); iter.hasNext();) {
            int index=iter.nextIndex();
            String className = pkgBasename+(String)iter.next();
            classes [index] = new BClass (new Identifier (bluejPrj,bluejPkg,className));
        }
        return classes;
    }
    
    /**
     * Returns a wrapper for the object with the given name on BlueJ's object bench.
     * @param name the name of the object as shown on the object bench
     * @return the object, or null if no such object exists.
     */
    public BObject getObject (String instanceName) throws ProjectNotOpenException
    {
        // The usual check to avoid silly stack trace
        if ( instanceName == null ) return null;

        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluejPkg);
        // The above may return null, unfortunately.
        if ( pmf == null ) return null;
        
        ObjectWrapper[] objects = pmf.getObjectBench().getWrappers();
        for (int index=0; index<objects.length; index++) 
            {
            ObjectWrapper wrapper = objects[index];
            if (instanceName.equals(wrapper.getName())) return new BObject (wrapper);
            }
        return null;
    }    

    /**
     * Returns an array of all the Objects on the object bench.
     * The array will be empty if no objects are on the bench.
     */
    public BObject[] getObjects() throws ProjectNotOpenException
    {
        Package bluejPkg = packageId.getBluejPackage();
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluejPkg);
        if ( pmf == null ) return new BObject[0];
   
        ObjectWrapper[] objectWrappers = pmf.getObjectBench().getWrappers();
        BObject[] objects = new BObject [objectWrappers.length];
        for (int index=0; index<objectWrappers.length; index++) {
            ObjectWrapper wrapper = (ObjectWrapper)objectWrappers[index];
            objects[index] = new BObject (wrapper);
        }
        return objects;
    }
    

    /**
     * Compile this package.
     * If forceAll is true it will compile all files otherwise it will compile
     * just the ones that are modified.
     * @param forceAll if <code>true</code> compile all files.
     */
    public void compile (boolean forceAll) throws ProjectNotOpenException
    {
        Package bluejPkg = packageId.getBluejPackage();

        if (bluejPkg == null) return;

        if (forceAll) bluejPkg.rebuild(); 
        else bluejPkg.compile();
    }
    
    /**
     * Reloads the entire package.
     * This is used (e.g.) when a new <code>.java</code> file has been added to the package.
     */
    public void reload() throws ProjectNotOpenException
    {
        Package bluejPkg = packageId.getBluejPackage();
        if (bluejPkg == null) return;
        bluejPkg.reload();
    }

    /**
     * Returns a string representation of the Object
     */
    public String toString () 
      {
      try 
        {
        Package bluejPkg = packageId.getBluejPackage();
        return "BPackage: "+bluejPkg.getQualifiedName();
        }
      catch ( ExtensionException exc )
        {
        return "BPackage: INVALID";  
        }
      }

}
