package uk.ac.ed.inf.path;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.client.ApiClient;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;

import javax.naming.Name;

public class FlightPlanner {
    private final ApiClient apiClient;
    private NamedRegion[] noFlyZones;
    private NamedRegion centralArea;
    private final AStarPathFinder pathFinder;

    private HashMap<String, List<Cell>> orderPathHistory;
    private final String rootDirectory;

    public FlightPlanner(String rootDirectory, ApiClient apiClient) throws Exception {
        this.rootDirectory = rootDirectory;
        this.apiClient = apiClient;
        this.pathFinder = new AStarPathFinder();

        updateZoneData();
        resetHistory();
    }

    public List<Cell> getOrderPath(String orderId) {
        return this.orderPathHistory.get(orderId);
    }

    /**
     * Resets the collection of all computed and recorder paths
     */
    private void resetHistory() {
        this.orderPathHistory = new HashMap<>();
    }

    /**
     * Fetches the data from ApiClient and updates the PathFinder
     */
    private void updateZoneData() throws Exception {
        this.noFlyZones = apiClient.retrieveNoFlyZones();
        // Not really needed - remove?
        this.centralArea = apiClient.retrieveCentralArea();
        // new data is propagated
        this.pathFinder.updateZones(noFlyZones, centralArea);
    }

    /**
     * @return Appleton Tower coordinates
     */
    public LngLat getAppletonTowerCoordinates() {
        // fetch from api?
        return new LngLat(-3.186874, 55.944494);
    }

    /**
     * Computes a flight path between two points and saves it for a given orderNo
     * into the orderPaths HashMap
     * @param origin - start
     * @param destination - destination
     */
    public void calculateOrderFlightPath(LngLat origin, LngLat destination, String orderNumber) {
        if (pathFinder.findShortestPath(origin, destination)) {
            List<Cell> path = pathFinder.getPath();
            // Add all nodes of the path
            List<Cell> orderPaths = orderPathHistory.getOrDefault(
                    orderNumber,
                    new ArrayList<>()
            );
            orderPaths.addAll(path);
            orderPathHistory.put(orderNumber, orderPaths);
        }
    }

    /**
     * given a filename, creates or overwrites the file
     * in the rootDirectory with provided data
     * TODO: export to a separate module
     * @param filename - name of the file to write into
     * @param data - data to write into the file
     */
    private void createOrOverwriteDataFile(String filename, Object data) throws IOException {
        // check root dir
        File directory = new File(rootDirectory);
        if (!directory.exists()) {
            directory.mkdir();
        }
        // create file
        ObjectMapper objectMapper = new ObjectMapper();
        // enable default LocalDate support by Jackson
        objectMapper.registerModule(new JavaTimeModule());
        File file = new File(rootDirectory + "/" + filename);
        if (!file.exists())
            file.createNewFile();
        objectMapper.writeValue(file, data);
    }

    /**
     * Exports the list of orders to a file with a given name
     * Only exports: orderNo, orderStatus, orderValidationCode, costInPence
     * @param filename - name of the file
     * @param orders - list of Order instances to be exported
     */
    public void exportOrdersToJson(String filename, Order[] orders) throws IOException {
        List<HashMap<String, Object>> ordersData = new ArrayList<>();
        // have to drop fields (date, items, paymentInfo)
        for (Order order : orders) {
            HashMap<String, Object> orderData = new HashMap<>();
            orderData.put("orderNo", order.getOrderNo());
            orderData.put("orderStatus", order.getOrderStatus());
            orderData.put("orderValidationCode", order.getOrderValidationCode());
            orderData.put("costInPence", order.getPriceTotalInPence());
            ordersData.add(orderData);
        }
        createOrOverwriteDataFile(filename, ordersData);
    }

    /**
     * Exports the data about computed order paths as an array of the form:
     * [{orderNo, fromLatitude, fromLongitude, angle, toLatitude, toLongitude}]
     * saved in the `rootDirectory`
     * @param filename - name of the file to export data into
     */
    public void exportOrderPathHistoryToJson(String filename) throws IOException {
        List<HashMap<String, Object>> ordersPathData = new ArrayList<>();

        for (String orderNo : orderPathHistory.keySet()) {
            List<Cell> orderPath = orderPathHistory.get(orderNo);
            Cell prevPoint = null;
            for (Cell pathPoint : orderPath) {
                if (prevPoint == null) {
                    prevPoint = pathPoint;
                    continue;
                }
                HashMap<String, Object> pointData = getCurrentPathPointHashMap(orderNo, pathPoint, prevPoint);
                prevPoint = pathPoint;
                ordersPathData.add(pointData);
            }
        }
        createOrOverwriteDataFile(filename, ordersPathData);
    }

    /**
     * Converts the move between 2 points into a HashMap of
     * appropriate export format
     * @param orderNumber - order number
     * @param pathPoint - current point
     * @param prevPoint - previous point
     * @return HashMap of features
     */
    private static HashMap<String, Object> getCurrentPathPointHashMap(String orderNumber, Cell pathPoint, Cell prevPoint) {
        HashMap<String, Object> pointData = new HashMap<>();
        pointData.put("orderNo",       orderNumber);
        pointData.put("fromLatitude",  prevPoint.getCoordinates().lat());
        pointData.put("fromLongitude", prevPoint.getCoordinates().lng());
        pointData.put("angle",         pathPoint.getEnterAngle());
        pointData.put("toLatitude",    pathPoint.getCoordinates().lat());
        pointData.put("toLongitude",   pathPoint.getCoordinates().lng());
        return pointData;
    }

    /**
     * Creates a GeoJSON feature with a given name and a given list of coordinates
     * @param geometryType - type of geometry (LineString, Polygon)
     * @param coordinates coordinates for the geometry
     * @return HashMap of a GeoJSON feature
     */
    public HashMap<String, Object> getGeoJsonTypedGeometryFeatureWithCoordinates(String geometryType, Object coordinates) {
        HashMap<String, Object> geometry = new HashMap<>();
        geometry.put("type", geometryType);
        geometry.put("coordinates", coordinates);

        HashMap<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");
        feature.put("properties", new HashMap<>());
        feature.put("geometry", geometry);
        return feature;
    }

    /**
     * exports current noFlyZones to a file with the given filename
     * in the geojson format to be rendered
     * the path for the file is `rootDirectory`
     */
    public void exportNoFlyZonesToGeoJson() throws IOException {
        HashMap<String, Object> geoJsonData = new HashMap<>();
        List<HashMap<String, Object>> features = new ArrayList<>();
        for (NamedRegion zone : noFlyZones) {
            ArrayList<Double[]> coordinates = new ArrayList<>();
            for (LngLat point : zone.vertices()) {
                coordinates.add(new Double[]{point.lng(), point.lat()});
            }
            HashMap<String, Object> feature =
                    getGeoJsonTypedGeometryFeatureWithCoordinates("Polygon", new Object[]{coordinates});
            features.add(feature);
        }
        geoJsonData.put("type", "FeatureCollection");
        geoJsonData.put("features", features);

        createOrOverwriteDataFile("noFlyZones.json", geoJsonData);
    }

    /**
     * exports current flight path history to a file with the given filename
     * the path for the file is `rootDirectory`
     * @param filename name of the file
     */
    public void exportFlightHistoryToGeoJson(String filename) throws IOException {
        List<Double[]> coordinates = new ArrayList<>();

        for (String orderNumber : orderPathHistory.keySet()) {
            List<Cell> orderPath = orderPathHistory.get(orderNumber);
            for (Cell point : orderPath) {
                LngLat pointCoordinates = point.getCoordinates();
                coordinates.add(new Double[]{pointCoordinates.lng(), pointCoordinates.lat()});
            }
        }

        HashMap<String, Object> feature =
                getGeoJsonTypedGeometryFeatureWithCoordinates("LineString", coordinates);
        ArrayList<HashMap<String, Object>> features = new ArrayList<>();
        features.add(feature);

        HashMap<String, Object> geoJsonData = new HashMap<>();
        geoJsonData.put("type", "FeatureCollection");
        geoJsonData.put("features", features);

        createOrOverwriteDataFile(filename, geoJsonData);
    }
}
