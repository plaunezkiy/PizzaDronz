package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.path.Cell;
import uk.ac.ed.inf.utils.LngLatHandler;

import java.io.File;
import java.util.List;

public class FlightPlannerUtils {
    public static class FlightMove {
        // [{orderNo, fromLatitude, fromLongitude, angle, toLatitude, toLongitude}]
        String orderNo;
        double fromLatitude, fromLongitude, angle, toLatitude, toLongitude;
        public FlightMove(String orderNo, double fromLatitude, double fromLongitude,
                          double angle, double toLatitude, double toLongitude) {
            this.orderNo = orderNo;
            this.fromLongitude = fromLongitude;
            this.fromLatitude = fromLatitude;
            this.angle = angle;
            this.toLongitude = toLongitude;
            this.toLatitude = toLatitude;
        }
    }

    // instrumentation
    public static boolean allAdjacentPathMovesAreClose(List<Cell> path) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        Cell prevMove = null;
        for (Cell move : path) {
            if (prevMove == null) {
                prevMove = move;
                continue;
            }
            boolean isClose = lngLatHandler.isCloseTo(prevMove.getCoordinates(), move.getCoordinates());
            System.out.println(isClose);
            if (!isClose) return false;
        }
        return true;
    }

    public static boolean checkNoPathMoveIsInNoFlyZone(List<Cell> path, NamedRegion[] noFlyZones) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        for (Cell move : path) {
            for (NamedRegion noFlyZone : noFlyZones) {
                boolean isInFlyZone = lngLatHandler.isInRegion(move.getCoordinates(), noFlyZone);
                if (isInFlyZone) return false;
            }
        }
        return true;
    }

    public static boolean checkCloseToPositionAppearsInPathNTimes(List<Cell> path, LngLat position, int N) {
        LngLatHandler lngLatHandler = new LngLatHandler();
        int counter = 0;
        for (Cell move : path) {
            boolean isClose = lngLatHandler.isCloseTo(position, move.getCoordinates());
            if (isClose) counter++;
        }
        System.out.println(counter);
        return counter == N;
    }

    public static boolean checkValidPath(LngLat start, LngLat dest, List<Cell> path, NamedRegion[] noFlyZones) {
        boolean isEmpty = path.toArray().length == 0;
        if (isEmpty) return false;
        boolean allAreClose = allAdjacentPathMovesAreClose(path);
//        if (!allAreClose) return false;

        boolean outsideNoFlyZones = checkNoPathMoveIsInNoFlyZone(path, noFlyZones);
        if (!outsideNoFlyZones) return false;

        boolean startAppearsTwice = checkCloseToPositionAppearsInPathNTimes(path, start, 2);
        if (!startAppearsTwice) return false;

        boolean destAppearsTwice = checkCloseToPositionAppearsInPathNTimes(path, dest, 2);
        if (!destAppearsTwice) return false;
        return true;
    }

    public static boolean checkFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        file.delete();
    }
}
