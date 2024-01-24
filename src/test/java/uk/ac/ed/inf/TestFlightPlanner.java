package uk.ac.ed.inf;

import junit.framework.TestCase;
import uk.ac.ed.inf.client.ApiClient;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.path.Cell;
import uk.ac.ed.inf.path.FlightPlanner;
import java.time.DayOfWeek;
import java.time.LocalDate;

import java.util.List;

import static uk.ac.ed.inf.utils.JsonValidator.isValidJson;

public class TestFlightPlanner extends TestCase {

    public MockRestServer getMockServer() {
        return null;
    }

    public void initResturantsAndMenu() {
        Pizza pizza = new Pizza("Pepperoni Pizza", 500);
        CreditCardInformation validPaymentDetails = new CreditCardInformation();
        validPaymentDetails.setCreditCardNumber("1234567812345678");
        validPaymentDetails.setCvv("123");
        validPaymentDetails.setCreditCardExpiry("09/2026");

        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                new LngLat(10.0, 10.0),
                new DayOfWeek[] {
                        DayOfWeek.MONDAY,
                },
                new Pizza[] {
                        pizza
                }
        );

        NamedRegion noFlyZone = new NamedRegion(
                "Test NoFlyZone",
                new LngLat[] {
                        new LngLat(2, 2),
                        new LngLat(4, 4)
                }
        );
        Order validOrder = new Order(
                "UNIQUD",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                600,
                new Pizza[] {pizza},
                new CreditCardInformation(

                )


        );
    }
    // tests
    public void testFlightPlanner() throws Exception {
        String dir = "";
        String baseUrl = "localhost";
        ApiClient client = new ApiClient(baseUrl);
//        FlightPlanner planner = new FlightPlanner(dir, client);
    }

    public void testFlightPathNotEmpty() throws Exception {
        int port = 8005;
        String dir = "resultfiles";
        String baseUrl = "http://localhost:"+port;
        MockRestServer server = new MockRestServer(port);
        server.getContextToServeDataOnUrl("/noFlyZones", "[]");
        server.getContextToServeDataOnUrl("/centralArea", "{\"name\":\"central\",\"vertices\":[{\"lng\":-3.192473,\"lat\":55.946233},{\"lng\":-3.192473,\"lat\":55.942617},{\"lng\":-3.184319,\"lat\":55.942617},{\"lng\":-3.184319,\"lat\":55.946233}]}");
        server.start();
        ApiClient client = new ApiClient(baseUrl);

        FlightPlanner planner = new FlightPlanner(dir, client);
        LngLat coordinateOne = new LngLat(0, 0);
        LngLat coordinateTwo = new LngLat(0, 0.001);
        String orderId = "ORDR1";
        planner.calculateOrderFlightPath(coordinateOne, coordinateTwo, orderId);
        List<Cell> path = planner.getOrderPath(orderId);
        for (Cell move : path) {
            System.out.println(move.getCoordinates());
        }

        boolean isValidPath = FlightPlannerUtils.checkValidPath(coordinateOne, coordinateTwo, path, new NamedRegion[] {});
//        assertTrue(isValidPath);

        server.stop();
    }

    public void testFilesCreated() throws Exception {
        int port = 8005;
        String baseUrl = "http://localhost:"+port;
        MockRestServer server = new MockRestServer(port);
        server.getContextToServeDataOnUrl("/noFlyZones", "[]");
        server.getContextToServeDataOnUrl("/centralArea", "{\"name\":\"central\",\"vertices\":[]}");
        server.start();

        ApiClient client = new ApiClient(baseUrl);
        String dir = "resultfiles";
        FlightPlanner planner = new FlightPlanner(dir, client);
        Order[] orders = new Order[] {};

        String ordersFileName = "orders-test.json";
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + ordersFileName));
        planner.exportOrdersToJson(ordersFileName, orders);
        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + ordersFileName));
        FlightPlannerUtils.deleteFile(dir + "/" + ordersFileName);

        String pathFileName = "deliveries-test.json";
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + pathFileName));
        planner.exportOrderPathHistoryToJson(pathFileName);
        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + pathFileName));
        FlightPlannerUtils.deleteFile(dir + "/" + pathFileName);

        String geoJsonFileName = "drone-test.json";
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + geoJsonFileName));
        planner.exportFlightHistoryToGeoJson(geoJsonFileName);
        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + geoJsonFileName));
        FlightPlannerUtils.deleteFile(dir + "/" + geoJsonFileName);

        server.stop();
    }
}
