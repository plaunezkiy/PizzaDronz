package uk.ac.ed.inf;

import junit.framework.TestCase;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.utils.LngLatHandler;

public class TestLngLatHandler extends TestCase {
    LngLatHandler lngLatHandler;

    public TestLngLatHandler () {
        lngLatHandler = new LngLatHandler();
    }

    public void testIsInRegion() {
        LngLat[] vertices = {
                new LngLat(3, 3),
                new LngLat(0, 3),
                new LngLat(0, 0),
                new LngLat(3, 0)
        };
        NamedRegion polygon = new NamedRegion("polygon", vertices);
        LngLat pointInside = new LngLat(1, 1);
        LngLat pointOutside = new LngLat(4, 4);
        assertTrue(lngLatHandler.isInRegion(pointInside, polygon));
        assertFalse(lngLatHandler.isInRegion(pointOutside, polygon));
    }

    public void testIsCloseTo() {
        LngLat position = new LngLat(0, 0);
        // Check the position itself is within close distance
        assertTrue(lngLatHandler.isCloseTo(position, position));
        //
    }

    public void testNextPosition() {
        LngLat position = new LngLat(0, 0);

        for (int i=0; i<16; i++) {
            double angle = i * 22.5;
            // bottom of every of the 16 directions starter rays is when the direction changes (eg 0forE,22.5forNEE etc.)
            LngLat nextPosition = lngLatHandler.nextPosition(position, angle);
            assertEquals(
                    nextPosition.lat(),
                    position.lat() + SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(Math.toRadians(angle))
            );
            assertEquals(
                    nextPosition.lng(),
                    position.lng() + SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(Math.toRadians(angle))
            );
        }
    }
}
