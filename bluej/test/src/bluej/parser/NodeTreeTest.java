package bluej.parser;

import junit.framework.TestCase;
import bluej.editor.moe.Token;
import bluej.parser.nodes.ColourNode;
import bluej.parser.nodes.NodeTree;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class NodeTreeTest extends TestCase
{
    private NodeTree nt;
    private ParsedNode pn1;
    private ParsedNode pn2;
    private ParsedNode pn3;
    private ParsedNode pn4;
    private ParsedNode pn5;
    
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {
        nt = new NodeTree();
        pn1 = new ColourNode(null, Token.KEYWORD1);
        pn2 = new ColourNode(null, Token.KEYWORD1);
        pn3 = new ColourNode(null, Token.KEYWORD1);
        pn4 = new ColourNode(null, Token.KEYWORD1);
        pn5 = new ColourNode(null, Token.KEYWORD1);

        nt.insertNode(pn1, 0, 10);
        nt.insertNode(pn2, 20, 10);
        nt.insertNode(pn3, 40, 10);
        nt.insertNode(pn4, 60, 10);
        nt.insertNode(pn5, 80, 10);
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
    }

    public void testBasic()
    {        
        NodeAndPosition np = nt.findNode(5);
        assertNotNull(np);
        assertTrue(np.getNode() == pn1);
        assertTrue(np.getPosition() == 0);

        np = nt.findNode(25);
        assertNotNull(np);
        assertTrue(np.getNode() == pn2);
        assertTrue(np.getPosition() == 20);

        np = nt.findNode(45);
        assertNotNull(np);
        assertTrue(np.getNode() == pn3);
        assertTrue(np.getPosition() == 40);

        np = nt.findNode(65);
        assertNotNull(np);
        assertTrue(np.getNode() == pn4);
        assertTrue(np.getPosition() == 60);

        np = nt.findNode(85);
        assertNotNull(np);
        assertTrue(np.getNode() == pn5);
        assertTrue(np.getPosition() == 80);
    }
    
    public void testRemoval()
    {
        NodeAndPosition np = nt.findNode(65);
        np.getNode().remove();
        
        np = nt.findNode(5);
        assertNotNull(np);
        assertTrue(np.getNode() == pn1);
        assertTrue(np.getPosition() == 0);
        
        np = nt.findNode(65);
        assertNull(np);
        
        np = nt.findNode(85);
        assertNotNull(np);
        assertTrue(np.getNode() == pn5);
        assertTrue(np.getPosition() == 80);
    }
    
    public void testRemoval2()
    {
        // Remove all nodes
        NodeAndPosition np = nt.findNode(65);
        np.getNode().remove();
        np = nt.findNode(45);
        np.getNode().remove();
        np = nt.findNode(85);
        np.getNode().remove();
        np = nt.findNode(25);
        np.getNode().remove();
        np = nt.findNode(5);
        np.getNode().remove();

        np = nt.findNode(5);
        assertNull(np);
        np = nt.findNode(25);
        assertNull(np);
        np = nt.findNode(45);
        assertNull(np);
        np = nt.findNode(65);
        assertNull(np);
        np = nt.findNode(85);
        assertNull(np);
    }
    
    public void testRotation1()
    {
        nt.clear();
        nt.insertNode(pn1, 100, 10);
        nt.insertNode(pn2, 50, 10);
        nt.insertNode(pn3, 70, 10); // causes rotation
        
        NodeAndPosition nap = nt.findNode(105);
        assertTrue(nap.getNode() == pn1);
        nap = nt.findNode(55);
        assertTrue(nap.getNode() == pn2);
        nap = nt.findNode(75);
        assertTrue(nap.getNode() == pn3);
    }
}
