package uk.ac.ed.inf;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.client.ApiClient;
import uk.ac.ed.inf.path.FlightPlanner;
import uk.ac.ed.inf.utils.OrderValidator;

import java.net.URL;

public class App
{
    public static void mainLoop(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Invalid number of arguments. Please provide: Date; Api Base Url;");
        }
        String date = args[0];
        String apiUrl = args[1];
        // validate day
        if (!OrderValidator.isValidDate(date)) {
            throw new Exception("Invalid Date");
        };
        // validate URL
        new URL(apiUrl);
        // instantiate api client
        ApiClient apiClient = new ApiClient(apiUrl);
        if (!apiClient.isAlive()) {
            throw new Exception("Client is Inactive");
        }

        OrderValidator validator = new OrderValidator();
        // retrieve data from api
        Order[] orders = apiClient.retrieveOrdersOnDate(date);
        Restaurant[] restaurants = apiClient.retrieveRestaurants();

        FlightPlanner flightPlanner = new FlightPlanner("resultfiles", apiClient);

        for (Order order : orders) {
            validator.validateOrder(order, restaurants);
            if (order.getOrderStatus() == OrderStatus.VALID_BUT_NOT_DELIVERED) {
                // implies that the order restaurant has already been identified
                Restaurant restaurant = OrderValidator.findOrderRestaurant(order, restaurants);
                // fly to the restaurant
                flightPlanner.calculateOrderFlightPath(
                        flightPlanner.getAppletonTowerCoordinates(),
                        restaurant.location(),
                        order.getOrderNo()
                );
                // fly back
                flightPlanner.calculateOrderFlightPath(
                        restaurant.location(),
                        flightPlanner.getAppletonTowerCoordinates(),
                        order.getOrderNo()
                );
                order.setOrderStatus(OrderStatus.DELIVERED);
            }
        }

        flightPlanner.exportOrdersToJson("deliveries-%s.json".formatted(date), orders);
        flightPlanner.exportOrderPathHistoryToJson("flightpath-%s.json".formatted(date));
        flightPlanner.exportFlightHistoryToGeoJson("drone-%s.json".formatted(date));
    }
    public static void main( String[] args ) {
        try {
            mainLoop(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
