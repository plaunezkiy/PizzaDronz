package uk.ac.ed.inf.client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static uk.ac.ed.inf.utils.JsonValidator.isValidJson;

public class ApiClient {
    String baseUrl;
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     *
     * @param path API endpoint url
     * @param cls class of the data to be received
     * @return Instance(s) of data received from the API url endpoint
     * @param <T> Generic - describes the data to be received
     */
    private <T> T getDataAndParseJSON(String path, Class<T> cls) throws Exception {
        URL url = new URL(this.baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "application/json");

        InputStream inputStream = connection.getInputStream();
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        if (!isValidJson(response)) {
            throw new Exception("Invalid Json Response from API");
        }
        ObjectMapper mapper = new ObjectMapper();
        // enable default LocalDate support by Jackson
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(response, cls);
    }

    public boolean isAlive() {
        try {
            return getDataAndParseJSON("/isAlive", boolean.class);
        } catch (Exception e){
            return false;
        }
    }

    public Order[] retrieveOrdersOnDate(String date) throws Exception {
        return getDataAndParseJSON("/orders/" + date, Order[].class);
    }

    public Restaurant[] retrieveRestaurants() throws Exception {
        return getDataAndParseJSON("/restaurants", Restaurant[].class);
    }

    public NamedRegion[] retrieveNoFlyZones() throws Exception {
        return getDataAndParseJSON("/noFlyZones", NamedRegion[].class);
    }

    public NamedRegion retrieveCentralArea() throws Exception {
        return getDataAndParseJSON("/centralArea", NamedRegion.class);
    }

}
