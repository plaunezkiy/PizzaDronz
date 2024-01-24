package uk.ac.ed.inf;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import uk.ac.ed.inf.client.ApiClient;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.HashMap;

import static junit.framework.Assert.*;
import static uk.ac.ed.inf.utils.JsonValidator.isValidJson;

public class TestApiClient extends TestCase {
    int port = 8005;
    private static MockRestServer mockServer;

    @BeforeClass
    public void setUp() throws IOException {
        mockServer = new MockRestServer(port);
        mockServer.start();
    }
    @AfterClass
    public void tearDown() {
        mockServer.stop();
    }

    // Tests

    public void testEmptyJson() {
        String emptyJson = "";
        assertTrue(isValidJson(emptyJson));
    }

    public void testMalformedJson() {
        String malformedJson = "{";
        assertFalse(isValidJson(malformedJson));
    }

    public void testValidJson() {
        String validJson = "{'object': 'valid object'}";
        assertTrue(isValidJson(validJson));
    }

    public void testUnreachableApiUrl() {
        String unreachableUrl = "http://1.1.1.1:1111";
        ApiClient client = new ApiClient(unreachableUrl);
        assertFalse(client.isAlive());
    }

    public void testValidApiUrl() {
        String validUrl = "https://ilp-rest.azurewebsites.net";
        ApiClient client = new ApiClient(validUrl);
        assertTrue(client.isAlive());
    }

    public void testOrders() throws Exception {
        String validUrl = "https://ilp-rest.azurewebsites.net";
        ApiClient client = new ApiClient(validUrl);
        Order[] orders = client.retrieveOrdersOnDate("2023-09-02");
        assertNotSame(0, orders.length);
    }

    public void testRestaurants() throws Exception {
        String validUrl = "https://ilp-rest.azurewebsites.net";
        ApiClient client = new ApiClient(validUrl);
        Restaurant[] restaurants = client.retrieveRestaurants();
        assertNotSame(0, restaurants.length);
    }

    public void testNoFlyZones() throws Exception {
        String validUrl = "https://ilp-rest.azurewebsites.net";
        ApiClient client = new ApiClient(validUrl);
        NamedRegion[] zones = client.retrieveNoFlyZones();
        assertNotSame(0, zones.length);
    }

    public void testCentralArea() throws Exception {
        String validUrl = "https://ilp-rest.azurewebsites.net";
        ApiClient client = new ApiClient(validUrl);
        NamedRegion area = client.retrieveCentralArea();
        assertEquals("central", area.name());
    }

    public void testInvalidJsonFromServer() throws Exception {
        String serverUrl = "http://localhost:" + port;

        String invalidJsonData = "{]";

        HttpContext context = mockServer.getContextToServeDataOnUrl("/isAlive", invalidJsonData);
        ApiClient client = new ApiClient(serverUrl);
        try {
            client.isAlive();
        } catch (Exception e) {
            boolean containsError = e.getMessage().contains("Invalid Json");
            assertTrue(containsError);
        }
        mockServer.removeContext(context);
    }

    public void testServerAliveButInvalidRestaurants() throws Exception {
        String invalidJsonData = "{]";
        String serverUrl = "http://localhost:" + port;

        HttpContext aliveContext = mockServer.getContextToServeDataOnUrl("/isAlive", "true");
        HttpContext dataContext = mockServer.getContextToServeDataOnUrl("/restaurants", invalidJsonData);

        ApiClient client = new ApiClient(serverUrl);
        try {
            boolean isAlive = client.isAlive();
            assertTrue(isAlive);
            client.retrieveRestaurants();
        } catch (Exception e) {
            boolean containsError = e.getMessage().contains("Invalid Json");
            assertTrue(containsError);
        }
        mockServer.removeContext(aliveContext);
        mockServer.removeContext(dataContext);
    }
    public void testServerAliveButInvalidFlyZones() throws Exception {
        String invalidJsonData = "{]";
        String serverUrl = "http://localhost:" + port;

        HttpContext aliveContext = mockServer.getContextToServeDataOnUrl("/isAlive", "true");
        HttpContext dataContext = mockServer.getContextToServeDataOnUrl("/noFlyZones", invalidJsonData);

        ApiClient client = new ApiClient(serverUrl);
        try {
            boolean isAlive = client.isAlive();
            assertTrue(isAlive);
            client.retrieveNoFlyZones();
        } catch (Exception e) {
            boolean containsError = e.getMessage().contains("Invalid Json");
            assertTrue(containsError);
        }
        mockServer.removeContext(aliveContext);
        mockServer.removeContext(dataContext);
    }
    public void testServerAliveButInvalidCentralArea() throws Exception {
        String invalidJsonData = "{]";
        String serverUrl = "http://localhost:" + port;

        HttpContext aliveContext = mockServer.getContextToServeDataOnUrl("/isAlive", "true");
        HttpContext dataContext = mockServer.getContextToServeDataOnUrl("/centralArea", invalidJsonData);

        ApiClient client = new ApiClient(serverUrl);
        try {
            boolean isAlive = client.isAlive();
            assertTrue(isAlive);
            client.retrieveCentralArea();
        } catch (Exception e) {
            boolean containsError = e.getMessage().contains("Invalid Json");
            assertTrue(containsError);
        }
        mockServer.removeContext(aliveContext);
        mockServer.removeContext(dataContext);
    }
    public void testServerAliveButInvalidOrdersForADay() throws Exception {
        String invalidJsonData = "{]";
        String date = "2023-12-31";

        String serverUrl = "http://localhost:" + port;

        HttpContext aliveContext = mockServer.getContextToServeDataOnUrl("/isAlive", "true");
        HttpContext dataContext = mockServer.getContextToServeDataOnUrl("/orders/"+date, invalidJsonData);

        ApiClient client = new ApiClient(serverUrl);
        try {
            boolean isAlive = client.isAlive();
            assertTrue(isAlive);
            client.retrieveOrdersOnDate(date);
        } catch (Exception e) {
            boolean containsError = e.getMessage().contains("Invalid Json");
            assertTrue(containsError);
        }
        mockServer.removeContext(aliveContext);
        mockServer.removeContext(dataContext);
    }
}
