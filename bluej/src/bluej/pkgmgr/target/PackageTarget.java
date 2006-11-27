package bluej.pkgmgr.target;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Properties;

import javax.swing.*;

import bluej.Config;
import bluej.graph.Moveable;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;

/**
 * A sub package (or parent package)
 * 
 * @author Michael Cahill
 * @version $Id: PackageTarget.java 4708 2006-11-27 00:47:57Z bquig $
 */
public class PackageTarget extends Target
    implements Moveable
{
    static final int MIN_WIDTH = 60;
    static final int MIN_HEIGHT = 40;

    private static final int TAB_HEIGHT = 12;

    static String openStr = Config.getString("pkgmgr.packagemenu.open");
    static String removeStr = Config.getString("pkgmgr.packagemenu.remove");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(3);

    private int ghostX;
    private int ghostY;
    private int ghostWidth;
    private int ghostHeight;
    private boolean isDragging;
    private boolean isMoveable = true;

    public PackageTarget(Package pkg, String baseName)
    {
        super(pkg, baseName);

        setSize(calculateWidth(baseName), DEF_HEIGHT + TAB_HEIGHT);
    }

    /**
     * Return the target's base name (ie the name without the package name). eg.
     * Target
     */
    public String getBaseName()
    {
        return getIdentifierName();
    }

    /**
     * Return the target's name, including the package name. eg. bluej.pkgmgr
     */
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "PackageTarget");
    }

    /**
     * Deletes applicable files (directory and ALL contentes) prior to this
     * PackageTarget being removed from a Package.
     */
    public void deleteFiles()
    {
        FileUtility.deleteDir(new File(getPackage().getPath(), getBaseName()));
    }

    /**
     * Copy all the files belonging to this target to a new location.
     * For package targets, this has not yet been implemented.
     *
     * @arg directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
        //XXX not working
        return true;
    }

    /**
     * Called when a package icon in a GraphEditor is double clicked. Creates a
     * new PkgFrame when a package is drilled down on.
     */
    public void doubleClick(MouseEvent evt)
    {
        getPackage().getEditor().raiseOpenPackageEvent(this, getPackage().getQualifiedName(getBaseName()));
    }

    /**
     * Disply the context menu.
     */
    public void popupMenu(int x, int y)
    {
        JPopupMenu menu = createMenu();
        if (menu != null)
            menu.show(getPackage().getEditor(), x, y);
    }

    /**
     * Construct a popup menu which displays all our parent packages.
     */
    private JPopupMenu createMenu()
    {
        JPopupMenu menu = new JPopupMenu(getBaseName());

        Action openAction = new OpenAction(openStr, this, getPackage().getQualifiedName(getBaseName()));
        addMenuItem(menu, openAction);
        
        Action removeAction = new RemoveAction(removeStr, this);
        addMenuItem(menu, removeAction);

        return menu;
    }

    private void addMenuItem(JPopupMenu menu, Action action)
    {
        JMenuItem item = menu.add(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
    }

    private class OpenAction extends AbstractAction
    {
        private Target t;
        private String pkgName;

        public OpenAction(String menu, Target t, String pkgName)
        {
            super(menu);
            this.t = t;
            this.pkgName = pkgName;
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseOpenPackageEvent(t, pkgName);
        }
    }

    private class RemoveAction extends AbstractAction
    {
        private Target t;

        public RemoveAction(String menu, Target t)
        {
            super(menu);
            this.t = t;
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().getEditor().raiseRemoveTargetEvent(t);
        }
    }

    public void remove()
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
        if (pmf.askRemovePackage(this)) {
            deleteFiles();
            getPackage().getProject().removePackage(getQualifiedName());
            getPackage().removeTarget(this);
        }
    }

    /**
     * Removes the package associated with this target. No question asked, it
     * would be nice if it was something like public void remove (boolean
     * askConfirm); D.
     */
    public void removeImmediate()
    {
        deleteFiles();
        getPackage().removeTarget(this);
        getPackage().getProject().removePackage(getQualifiedName());
    }

    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
        setGhostSize(0, 0);
    }

    public void setPos(int x, int y)
    {
        super.setPos(x, y);
        setGhostPosition(0, 0);
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostX()
    {
        return ghostX;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostY()
    {
        return ghostY;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostWidth()
    {
        return ghostWidth;
    }

    /**
     * @return Returns the ghostX.
     */
    public int getGhostHeight()
    {
        return ghostHeight;
    }

    /**
     * Set the position of the ghost image given a delta to the real size.
     */
    public void setGhostPosition(int deltaX, int deltaY)
    {
        this.ghostX = getX() + deltaX;
        this.ghostY = getY() + deltaY;
    }

    /**
     * Set the size of the ghost image given a delta to the real size.
     */
    public void setGhostSize(int deltaX, int deltaY)
    {
        ghostWidth = Math.max(getWidth() + deltaX, MIN_WIDTH);
        ghostHeight = Math.max(getHeight() + deltaY, MIN_HEIGHT);
    }

    /**
     * Set the target's position to its ghost position.
     */
    public void setPositionToGhost()
    {
        super.setPos(ghostX, ghostY);
        setSize(ghostWidth, ghostHeight);
        isDragging = false;
    }

    /** 
     * Ask whether we are currently dragging. 
     */
    public boolean isDragging()
    {
        return isDragging;
    }

    /**
     * Set whether or not we are currently dragging this class
     * (either moving or resizing).
     */
    public void setDragging(boolean isDragging)
    {
        this.isDragging = isDragging;
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Moveable#isMoveable()
     */
    public boolean isMoveable()
    {
        return isMoveable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Moveable#setIsMoveable(boolean)
     */
    public void setIsMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;

    }
}
