package bluej.pkgmgr.actions;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * "Run all tests" action (test panel). Runs all the unit tests which have
 * been created in this project. Displays the results.
 * 
 * @author Davin McCall
 * @version $Id: RunTestsAction.java 2505 2004-04-21 01:50:28Z davmac $
 */
final public class RunTestsAction extends PkgMgrAction
{
    static private RunTestsAction instance = null;
    
    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public RunTestsAction getInstance()
    {
        if(instance == null)
            instance = new RunTestsAction();
        return instance;
    }
    
    private RunTestsAction()
    {
        super("pkgmgr.test.run");
        putValue(SHORT_DESCRIPTION, Config.getString("tooltip.test"));
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.doTest();
    }
}
