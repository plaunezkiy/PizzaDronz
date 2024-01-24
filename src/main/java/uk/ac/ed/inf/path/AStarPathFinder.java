package uk.ac.ed.inf.path;

import uk.ac.ed.inf.utils.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

public class AStarPathFinder {
    private NamedRegion[] noFlyZones;
    private NamedRegion centralArea;

    // lowest `f` bubbles to the top
    static PriorityQueue<Cell> nextCellQueue;
    static HashSet<Cell> visited;
    static HashMap<Cell, Cell> cameFrom;
    static List<Cell> path;
    static LngLatHandler lngLatHandler;

    public AStarPathFinder() {
        lngLatHandler = new LngLatHandler();
    }

    /**
     * Updates the state for the object
     * @param noFlyZones - active noFlyZones
     * @param centralArea - active centralArea
     */
    public void updateZones(NamedRegion[] noFlyZones, NamedRegion centralArea) {
        this.noFlyZones = noFlyZones;
        this.centralArea = centralArea;
    }

    public List<Cell> getPath() {
        return path;
    }

    private void resetState() {
        // instantiate queue (comparing f scores for priority)
        nextCellQueue = new PriorityQueue<>(Comparator.comparing(c -> c.f));
        nextCellQueue.clear();
        visited = new HashSet<>();
        cameFrom = new HashMap<>();
        path = new ArrayList<>();
    }

    /**
     * If exists, find the shortest path between origin and destination
     * at every call, resets Queues and Maps and Sets to start over
     * while maintaining the outer state (eg noFlyZones)
     * @return bool whether the path exists
     */
    public boolean findShortestPath(LngLat origin, LngLat destination) {
        resetState();
        // initialise scores (starting point)
        Cell start = new Cell(origin, 999);
        // hover at the goal (999)
        Cell goal = new Cell(destination, 999);
        nextCellQueue.add(start);
        visited.add(start);
        // tracks distance up to the cell
        HashMap<Cell, Double> gScore = new HashMap<>();
        gScore.put(start, (double) 0);
        // do a BFS search
        while (!nextCellQueue.isEmpty()) {
            Cell current = nextCellQueue.poll();
            // If at destination, found the path
            // destination place located in the circle, this circle is centered at the endpoint with a radius of 1.5E-4
            if (lngLatHandler.isCloseTo(current.getCoordinates(), goal.getCoordinates())) {
                // link goal and its closest point (helps with hovering)
                cameFrom.put(goal, current);
                // save the path to the destination
                reconstructPathFromGoal(goal);
                return true;
            }
            for (Cell neighbor : findNeighbors(current)) {
                double tentativeScore = gScore.get(current) +
                        lngLatHandler.distanceTo(
                                current.getCoordinates(),
                                neighbor.getCoordinates()
                        );
                // if closer than currently known (infinity at start)
                if (tentativeScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    // update previous node
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeScore);
                    // update the best path through current
                    double fScore = heuristic(neighbor, goal);
                    neighbor.setF(tentativeScore + fScore);
                    // if not visited, add to queue
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        nextCellQueue.add(neighbor);
                    }
                }
            }
        }
        return false;
    }

    /**
     * reconstructs a path up to a given point
     * saves into a class variable `path`
     * @param goal - node to go back from
     */
    private void reconstructPathFromGoal(Cell goal) {
        Cell current = goal;
        while (cameFrom.containsKey(current)) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
    }

    /**
     * Check if the current point is in any know noFlyZone
     * @param current - point
     * @return boolean(point in noFlyZone)
     */
    private boolean isPointInNoFlyZone(LngLat current) {
        for (NamedRegion noFlyZone : noFlyZones) {
            if (lngLatHandler.isInRegion(current, noFlyZone)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds all valid neighbors of a current point
     * @param current current point
     * @return list of neighbors that are valid moves
     */
    private ArrayList<Cell> findNeighbors(Cell current) {
        ArrayList<Cell> neighbors = new ArrayList<>();
        // Check moves in all 16 directions
        for (int i=0; i<16; i++) {
            double angle = i * 22.5;
            LngLat nextPoint = lngLatHandler.nextPosition(current.coordinates, angle);
            // Check the move is valid and does not lay in a noFlyZone
            if (isPointInNoFlyZone(nextPoint)) {
//                System.out.println(nextPoint);
                continue;
            }
            neighbors.add(new Cell(nextPoint, angle));
        }
        return neighbors;
    }

    /**
     *
     * @param origin - starting point
     * @param destination - goal point
     * @return a way of estimating the probable distance
     */
    private double heuristic(Cell origin, Cell destination) {
        // Manhattan distance
        return Math.abs(origin.coordinates.lat() - destination.coordinates.lat()) +
                Math.abs(origin.coordinates.lng() - destination.coordinates.lng());
    }
}
