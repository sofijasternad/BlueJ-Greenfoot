package greenfoot.collision;

import greenfoot.World;
import greenfoot.TestObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 * 
 * Tests the method that gets the neighbours.
 * 
 * @author Poul Henriksen
 */
public class NeighbourTest extends TestCase
{
    private World world;

 /*   public void testDiagonal()
    {
        world = new World(10, 10, 10){};
        world.setWrapped(true);
        int d = 2;

        TestObject me = new TestObject();
        int xStart = 4;
        int yStart = 4;
        me.setLocation(xStart, yStart);
        world.addObject(me);

        Collection neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        Collection c = me.getNeighboursP(2, true, TestObject.class);

        assertEquals(24, c.size());
        assertTrue(c.containsAll(neighbours));
    }*/

   /* public void testNoDiagonal()
    {
        world = new World(10, 10, 10){};
        world.setWrapped(true);
        TestObject me = new TestObject();
        me.setLocation(4, 4);
        world.addObject(me);

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                TestObject actor = new TestObject();
                actor.setLocation(x, y);
                world.addObject(actor);
            }
        }

        Collection neighbours = new ArrayList();
        int d = 4;
        int xStart = 4;
        int yStart = 4;
        int[][] arr = new int[d * 2 + 1][d * 2 + 1];
        int dyEnd = d;
        for (int dx = 0; dx <= d; dx++) {
            for (int dy = dx - d; dy <= dyEnd; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                makeNeighbour(neighbours, xStart + dx, yStart + dy);
                if (dx != 0) {
                    makeNeighbour(neighbours, xStart - dx, yStart + dy);
                }
            }
            dyEnd--;
        }

        Collection c = me.getNeighboursP(d, false, TestObject.class);

        // To calculate number of neighbours in distance d:
        //     2(d^2 + d)

        assertEquals(2 * (d * d + d), c.size());
        assertTrue(c.containsAll(neighbours));
    }*/

   /* public void testWraping()
    {
        world = new World(10, 10, 10){};
        world.setWrapped(true);
        TestObject topLeft = new TestObject();
        topLeft.setLocation(0, 0);
        world.addObject(topLeft);

        TestObject topRight = new TestObject();
        topRight.setLocation(9, 0);
        world.addObject(topRight);

        TestObject bottomLeft = new TestObject();
        bottomLeft.setLocation(0, 9);
        world.addObject(bottomLeft);

        TestObject bottomRight = new TestObject();
        bottomRight.setLocation(9, 9);
        world.addObject(bottomRight);

        wrapTest(topLeft);
        wrapTest(topRight);
        wrapTest(bottomLeft);
        wrapTest(bottomRight);
    }*/

    private void wrapTest(TestObject actor)
    {
        Collection c = actor.getNeighboursP(1, true, TestObject.class);
        assertEquals(3, c.size());
        assertFalse(c.contains(actor));
    }

 /*   public void testNoWrapNoDiagonal()
    {

        world = new World(10, 10, 10){};

        int d = 2;
        int xStart = 0;
        int yStart = 0;

        TestObject me = new TestObject();
        me.setLocation(xStart, yStart);
        world.addObject(me);

        Collection neighbours = createNeighboursWithoutDiagonal(xStart, yStart, d);

        Collection c = me.getNeighboursP(d, false, TestObject.class);

        // To calculate number of neighbours within distance d:
        //     2(d^2 + d)

        assertEquals(5, c.size());
        assertTrue(c.containsAll(neighbours));
    }*/

    public void testNoWrapDiagonal()
    {

        int d = 2;
        world = new World(10, 10, 10){};

        int xStart = 9;
        int yStart = 9;
        TestObject me = new TestObject();
        world.addObject(me,xStart, yStart);
        Collection neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        Collection c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(8, c.size());
        assertTrue(c.containsAll(neighbours));

        world = new World(10, 10, 10){};
        xStart = 0;
        yStart = 0;
        world.addObject(me, xStart, yStart);
        neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(8, c.size());
        assertTrue(c.containsAll(neighbours));
    }

 /*   public void testWrapDiagonal()
    {
        world = new World(10, 10, 10){};
        world.setWrapped(true);
        int d = 2;

        int xStart = 9;
        int yStart = 9;
        TestObject me = new TestObject();
        me.setLocation(xStart, yStart);
        world.addObject(me);
        Collection neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        Collection c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(24, c.size());
        assertTrue(c.containsAll(neighbours));

        world = new World(10, 10, 10){};
        world.setWrapped(true);
        xStart = 0;
        yStart = 0;
        me.setLocation(xStart, yStart);
        world.addObject(me);
        neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(24, c.size());
        assertTrue(c.containsAll(neighbours));
    }
*/
 /*   public void testWrapNoDiagonal()
    {
        world = new World(10, 10, 10){};
        world.setWrapped(true);
        int d = 2;

        int xStart = 9;
        int yStart = 9;
        TestObject me = new TestObject();
        me.setLocation(xStart, yStart);
        world.addObject(me);
        Collection neighbours = createNeighboursWithoutDiagonal(xStart, yStart, d);
        Collection c = me.getNeighboursP(d, false, TestObject.class);
        assertEquals(12, c.size());
        assertTrue(c.containsAll(neighbours));

        world = new World(10, 10, 10){};
        world.setWrapped(true);
        xStart = 0;
        yStart = 0;
        me.setLocation(xStart, yStart);
        world.addObject(me);
        neighbours = createNeighboursWithoutDiagonal(xStart, yStart, d);
        c = me.getNeighboursP(d, false, TestObject.class);
        assertEquals(12, c.size());
        assertTrue(c.containsAll(neighbours));
    }
*/
    // =====================
    //  UTILITY METHODS
    // =====================

    private Collection createNeigboursWithDiagonal(int xStart, int yStart, int d)
    {
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                TestObject actor = new TestObject();
                world.addObject(actor, x, y);
            }
        }

        Collection neighbours = new ArrayList();
        for (int x = xStart - d; x <= xStart + d; x++) {
            for (int y = yStart - d; y <= yStart + d; y++) {
                if (/*!world.isWrapped() &&*/ (x < 0 || y < 0 || x >= world.getWidth() || y >= world.getHeight())) {
                    continue;
                }
                Collection remove = world.getObjectsAt(x, y, TestObject.class);
                for (Iterator iter = remove.iterator(); iter.hasNext();) {
                    TestObject element = (TestObject) iter.next();
                    if (!(x == xStart && y == yStart)) {
                        neighbours.add(element);
                    }
                }
            }
        }
        return neighbours;
    }

    private Collection createNeighboursWithoutDiagonal(int xStart, int yStart, int d)
    {

        //TODO NOT WORKING WITH WRAP! becuase of getObjectsAt not working with
        // wrap.
        Collection neighbours = new ArrayList();
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                TestObject actor = new TestObject();
                world.addObject(actor,x,y);
            }
        }

        int[][] arr = new int[d * 2 + 1][d * 2 + 1];
        int dyEnd = d;
        for (int dx = 0; dx <= d; dx++) {
            for (int dy = dx - d; dy <= dyEnd; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int xPos = xStart + dx;
                int xNeg = xStart - dx;
                int y = yStart + dy;

                if (!withinBounds(y, world.getHeight())) {
                    continue;
                }

                if (withinBounds(xPos, world.getWidth())) {
                    makeNeighbour(neighbours, xPos, y);
                }
                if (dx != 0 && withinBounds(xNeg, world.getWidth())) {
                    makeNeighbour(neighbours, xNeg, y);
                }
            }
            dyEnd--;
        }
        return neighbours;
    }

    /**
     * 
     * Determines if x lies between 0 and width. If wrapping is on this will
     * always return true.
     * 
     * @param xPos
     * @return
     */
    private boolean withinBounds(int x, int width)
    {
        return /*world.isWrapped() || */( /*!world.isWrapped() && */x >= 0 && x < width);
    }

    private void makeNeighbour(Collection neighbours, int x, int y)
    {
        Collection here = world.getObjectsAt(x, y, TestObject.class);
        TestObject neighbour = (TestObject) here.iterator().next();
        neighbours.add(neighbour);
    }
}
