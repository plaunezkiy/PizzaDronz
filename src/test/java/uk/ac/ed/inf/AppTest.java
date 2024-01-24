package uk.ac.ed.inf;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.Test;
import uk.ac.ed.inf.client.ApiClient;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import uk.ac.ed.inf.path.FlightPlanner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ed.inf.App.main;
import static uk.ac.ed.inf.App.mainLoop;

/*
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    public void testInvalidDateArguments() {
        String[] invalidDates = new String[] {
                "2023-31-12",
                "31-12-2023",
                "31-2023-12",
                "12-31-2023",
                "12-2023-31",
        };
        String validApiUrl = "https://ilp-rest.azurewebsites.net";
        for (String invalidDate : invalidDates) {
            String[] argList = new String[] {invalidDate, validApiUrl};
            try {
                mainLoop(argList);
            } catch (Exception e) {
                assertEquals("Invalid Date", e.getMessage());
            }
        }
    }
    public void testInvalidUrlArguments() {
        String invalidUrl = "::noprotocol";
        String validDate = "2023-12-31";
        String[] argList = new String[] {validDate, invalidUrl};
        try {
            mainLoop(argList);
        } catch (Exception e) {
            assertEquals(MalformedURLException.class, e.getClass());
        }
    }
    public void testInvalidNumberOfArguments() {
        // 0, 1, 3 or more args should be erroneous
        int[] invalidArgNums = new int[] {0, 1, 3, 10};
        for (int argNum : invalidArgNums) {
            // fill the array with `argNum` number of elements
            String[] argList = new String[argNum];
            Arrays.fill(argList, "argFiller");
            try {
                mainLoop(argList);
            } catch (Exception e) {
                boolean containsErrorMessage = e.getMessage().contains("Invalid number of arguments");
                assertTrue(containsErrorMessage);
            }
        }
    }

    public void testAppProducesThreeFiles() throws Exception {
        // fetch orders (valid1, invalid, valid2)
        // mainLoop(orders)
        // check the files are created
        String date = "2023-12-31";
        CreditCardInformation validPaymentDetails = OrderUtils.getValidPaymentDetails();

        Order validOrder1 = OrderUtils.getValidOrder("ORDR1");
        validOrder1.setCreditCardInformation(validPaymentDetails);
        validOrder1.setOrderDate(LocalDate.parse(date));

        Order invalidOrder = OrderUtils.getValidOrder("ORDR2");
        invalidOrder.setOrderStatus(OrderStatus.INVALID);

        Order validOrder2 = OrderUtils.getValidOrder("ORDR3");
        validOrder2.setCreditCardInformation(validPaymentDetails);
        validOrder2.setOrderDate(LocalDate.parse(date));

        Order[] orders = new Order[] {validOrder1, invalidOrder, validOrder2};
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String ordersJson = mapper.writeValueAsString(orders);

        int port = 8005;
        String baseUrl = "http://localhost:"+port;
        MockRestServer server = new MockRestServer(port);
        server.getContextToServeDataOnUrl("/isAlive", "true");
        server.getContextToServeDataOnUrl("/noFlyZones", "[]");
        server.getContextToServeDataOnUrl("/centralArea", "{\"name\":\"central\",\"vertices\":[]}");
        server.getContextToServeDataOnUrl("/restaurants", "[]");
        server.getContextToServeDataOnUrl("/orders/"+date, ordersJson);
        server.start();

        String dir = "resultfiles";
        String ordersFileName = "deliveries-" + date + ".json";
        String pathFileName = "flightpath-" + date + ".json";
        String geoJsonFileName = "drone-" + date + ".json";
        // clean up
        FlightPlannerUtils.deleteFile(dir + "/" + ordersFileName);
        FlightPlannerUtils.deleteFile(dir + "/" + pathFileName);
        FlightPlannerUtils.deleteFile(dir + "/" + geoJsonFileName);

        // test
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + ordersFileName));
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + pathFileName));
        assertFalse(FlightPlannerUtils.checkFileExists(dir + "/" + geoJsonFileName));

        String[] argList = new String[] {date, baseUrl};
        main(argList);

        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + ordersFileName));
        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + pathFileName));
        assertTrue(FlightPlannerUtils.checkFileExists(dir + "/" + geoJsonFileName));

        FlightPlannerUtils.deleteFile(dir + "/" + ordersFileName);
        FlightPlannerUtils.deleteFile(dir + "/" + pathFileName);
        FlightPlannerUtils.deleteFile(dir + "/" + geoJsonFileName);

        server.stop();
    }

    public void testAppOrdersDeliveredSequentially() throws Exception {
        // fetch orders (valid1, invalid, valid2)
        // mainLoop(orders)
        // check in files the order is valid1, valid2 and no invalid
        String date = "2023-12-31";
        CreditCardInformation validPaymentDetails = OrderUtils.getValidPaymentDetails();

        Order validOrder1 = OrderUtils.getValidOrder("ORDR1");
        validOrder1.setCreditCardInformation(validPaymentDetails);
        validOrder1.setOrderDate(LocalDate.parse(date));

        Order invalidOrder = OrderUtils.getValidOrder("ORDR2");
        invalidOrder.setOrderStatus(OrderStatus.INVALID);

        Order validOrder2 = OrderUtils.getValidOrder("ORDR3");
        validOrder2.setCreditCardInformation(validPaymentDetails);
        validOrder2.setOrderDate(LocalDate.parse(date));

        Order[] orders = new Order[] {validOrder1, invalidOrder, validOrder2};
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String ordersJson = mapper.writeValueAsString(orders);

        int port = 8005;
        String baseUrl = "http://localhost:"+port;
        MockRestServer server = new MockRestServer(port);
        server.getContextToServeDataOnUrl("/isAlive", "true");
        server.getContextToServeDataOnUrl("/noFlyZones", "[]");
        server.getContextToServeDataOnUrl("/centralArea", "{\"name\":\"central\",\"vertices\":[]}");
        server.getContextToServeDataOnUrl("/restaurants", "[]");
        server.getContextToServeDataOnUrl("/orders/"+date, ordersJson);
        server.start();

        String[] argList = new String[] {date, baseUrl};
        main(argList);

        File pathFile = new File("resultfiles/flightpath-" + date + ".json");

        FlightPlannerUtils.FlightMove[] moves = mapper.readValue(pathFile, FlightPlannerUtils.FlightMove[].class);

        Stream<Order> validOrders = Arrays.stream(orders).filter(order -> order.getOrderStatus() == OrderStatus.VALID_BUT_NOT_DELIVERED);
        ArrayList<String> validOrdersIdsSequence = validOrders.map(Order::getOrderNo).collect(Collectors.toCollection(ArrayList::new));

        ArrayList<String> pathOrderIdsSequence = new ArrayList<>();
        for (FlightPlannerUtils.FlightMove move : moves) {
            if (!pathOrderIdsSequence.contains(move.orderNo)) {
                pathOrderIdsSequence.add(move.orderNo);
            }
        }

        assertEquals(validOrdersIdsSequence, pathOrderIdsSequence);

        server.stop();
    }

    public void testAppRunsUnder60secsForADay() {
        int maxDurationSec = 60;
        String baseUrl = "https://ilp-rest.azurewebsites.net";
        int noDays = 5;
        for (int day=0; day<noDays; day++) {
            int paddedDay = day + 10;
            String date = "2023-09-" + paddedDay;
            String[] argList = new String[] {date, baseUrl};

            ExecutionTimer timer = new ExecutionTimer();
            timer.start();
            main(argList);
            timer.stop();
            System.out.println(date + " " + timer.getDuration() + "ms");
            assertTrue(timer.getDuration() < maxDurationSec * 1000);
        }
    }
}
