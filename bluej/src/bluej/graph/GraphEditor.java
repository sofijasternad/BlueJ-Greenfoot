package bluej.graph;

import bluej.Config;
import bluej.pkgmgr.PkgFrame;
import bluej.pkgmgr.Package;

import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Canvas to allow editing of general graphs
 *
 * @version $Id: GraphEditor.java 427 2000-04-18 04:33:04Z ajp $
 * @author  Michael Cahill
 */
public class GraphEditor extends JComponent
    implements MouseListener, MouseMotionListener
{
    static final int DEFAULT_WIDTH = 400;
    static final int DEFAULT_HEIGHT = 400;
    static final long DBL_CLICK_TIME = 300;		// milliseconds
    static final Color background = Config.getItemColour("colour.background");
    static final Color realBackground = Config.getItemColour("colour.graph.background");
    private Graph graph;
    PkgFrame frame;
    Vertex activeVertex;
    boolean motionListening;

    private boolean readOnly = false;

    public GraphEditor(Graph graph, PkgFrame frame)
    {
        setGraph(graph);
        this.frame = frame;
        addMouseListener(this);
        motionListening = false;

        setBackground(background);

        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Return the PkgFrame containing this editor.
     */
    public PkgFrame getFrame() {
        return this.frame;
    }

    public void setGraph(Graph graph)
    {
        this.graph = graph;
        this.graph.setEditor(this);
        activeVertex = null;
    }

    public Dimension getPreferredSize()
    {
        return graph.getMinimumSize();
    }

    public Dimension getMinimumSize()
    {
        return graph.getMinimumSize();
    }

    public void paint(Graphics g)
    {
        if(!(g instanceof PrintGraphics)) {
            Dimension d = getSize();
            g.setColor(realBackground);
            g.fillRect(0, 0, d.width, d.height);
        }

        graph.draw(g);
    }

    public Graphics2D getGraphics2D()
    {
        return (Graphics2D) super.getGraphics();
    }

    public void mousePressed(MouseEvent evt)
    {
	if (frame != null)
	    frame.clearStatus();

	int x = evt.getX();
	int y = evt.getY();

	activeVertex = null;

	// Try to find a vertex containing the point
	for(Enumeration e = graph.getVertices(); e.hasMoreElements(); ) {
	    Vertex v = (Vertex)e.nextElement();

	    if((v.x <= x) && (x < v.x + v.width) && (v.y <= y) && (y < v.y + v.height)) {
		activeVertex = v;
		break;
	    }
	}

	graph.setActiveVertex(activeVertex);

	if((activeVertex != null) && !isPopupEvent(evt) && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) ) {
	    activeVertex.mousePressed(evt, x, y, this);
	    if (!motionListening) {

		addMouseMotionListener(this);
		motionListening = true;
	    }
	}
    }

    public void mouseReleased(MouseEvent evt)
    {
	if(activeVertex != null && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
	    activeVertex.mouseReleased(evt, evt.getX(), evt.getY(), this);
	    if ((frame.getPackage().getState() != Package.S_CHOOSE_USES_TO) &&
		(frame.getPackage().getState() != Package.S_CHOOSE_EXT_TO)) {
		// if we're not choosing anymore, remove listener
		removeMouseMotionListener(this);
		motionListening = false;
	    }
	}
	else {
	    if (motionListening) {
		removeMouseMotionListener(this);
		motionListening = false;
		frame.getPackage().setState(Package.S_IDLE);
		repaint();
	    }
	}
    }

    public void mouseClicked(MouseEvent evt)
    {
	if(activeVertex != null) {
	    if(evt.getClickCount() > 1)
		activeVertex.doubleClick(evt, evt.getX(), evt.getY(), this);
	    else
		activeVertex.singleClick(evt, evt.getX(), evt.getY(), this);

	}
    }

    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    public void mouseDragged(MouseEvent evt) {
	if (readOnly)
	    return;

	if(activeVertex != null)
	    activeVertex.mouseDragged(evt, evt.getX(), evt.getY(), this);
    }

    public void mouseMoved(MouseEvent evt)
    {
	if(activeVertex != null)
	    activeVertex.mouseMoved(evt, evt.getX(), evt.getY(), this);
    }

    protected void processMouseEvent(MouseEvent evt)
    {
	super.processMouseEvent(evt);

	if (isPopupEvent(evt))
	    if((activeVertex != null))
		activeVertex.popupMenu(evt, evt.getX(), evt.getY(), this);
    }

    private boolean isPopupEvent(MouseEvent evt)
    {
	return evt.isPopupTrigger()
	    || ((evt.getID() == MouseEvent.MOUSE_PRESSED) && evt.isControlDown());
    }

    public void setReadOnly(boolean state) {
	readOnly = state;
    }
}
