package uk.ac.ed.inf.utils;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;
import java.lang.Math;

public class LngLatHandler implements LngLatHandling {
    /**
     * get the distance between two positions
     * @param startPosition is where the start is
     * @param endPosition is where the end is
     * @return the Euclidean distance between the positions
     */
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        double lat = endPosition.lat() - startPosition.lat();
        double lng = endPosition.lng() - startPosition.lng();
        return Math.sqrt(lat * lat + lng * lng);
    }

    /**
     * check if two positions are close (<= than SystemConstants.DRONE_IS_CLOSE_DISTANCE)
     * @param startPosition is the starting position
     * @param otherPosition is the position to check
     * @return if the positions are close
     */
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
      double distance = distanceTo(startPosition, otherPosition);
//        double distance = (double) Math.round(distanceTo(startPosition, otherPosition) * 10000) / 10000;
//        System.out.print(distance);
//        System.out.print(" ");
//        System.out.print((SystemConstants.DRONE_IS_CLOSE_DISTANCE + 0.1 * SystemConstants.DRONE_IS_CLOSE_DISTANCE));
//        System.out.println();
        return distance <= SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    /**
     * check if the @position is in the @region (includes the border)
     * basic implementation of ray-caster algorithm
     * @param position to check
     * @param region as a closed polygon
     * @return if the position is inside the region (including the border)
     */
    public boolean isInRegion(LngLat position, NamedRegion region) {
        final LngLat[] vertices = region.vertices();
        int intersectionCounter = 0;
        final double x = position.lng();
        final double y = position.lat();
        for (int i=0; i < vertices.length; i++) {
            final LngLat vertex = vertices[i];
            final double x1 = vertex.lng();
            final double y1 = vertex.lat();

            final LngLat nextVertex = vertices[(i+1) % vertices.length];
            final double x2 = nextVertex.lng();
            final double y2 = nextVertex.lat();

            if (
                ((y < y1) != (y <= y2)) &&
                (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)
            ) {
                intersectionCounter++;
            }
        }
        return intersectionCounter % 2 == 1;
    }

    /**
     * find the next position if an @angle is applied to a @startPosition
     * @param startPosition is where the start is
     * @param angle is the angle to use in degrees
     * @return the new position after the angle is used
     */
    public LngLat nextPosition(LngLat startPosition, double angle) {
        double angleRounded = Math.toRadians(22.5 * angle / 22.5);
        double y = startPosition.lat();
        double x = startPosition.lng();
        return new LngLat(
                x + SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(angleRounded),
                y + SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(angleRounded)
        );
    }
}
