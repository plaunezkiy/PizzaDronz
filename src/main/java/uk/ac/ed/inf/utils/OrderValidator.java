package uk.ac.ed.inf.utils;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

public class OrderValidator implements OrderValidation {
    /**
     * Static method that validates the format of the date
     * @param date String in the form yyyy-MM-dd
     * @return is date valid
     */
    public static boolean isValidDate(String date) {
        Date _date;
        Calendar calendar = Calendar.getInstance();
        try {
            _date = new SimpleDateFormat("yyyy-MM-dd").parse(date);

            String[] data = date.split("-");
            int year = Integer.parseInt(data[0]);
            int month = Integer.parseInt(data[1]);
            int day = Integer.parseInt(data[2]);

            calendar.setTime(_date);
            if (year != calendar.get(Calendar.YEAR)) return false;
            if (month != calendar.get(Calendar.MONTH) + 1) return false;
            if (day != calendar.get(Calendar.DAY_OF_MONTH)) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a string is entirely numeric (any length)
     * parsing int is not feasible limited to ~2^16
     * @param string string to check
     * @return bool - is `string` numeric
     */
    public static boolean isNumeric(String string) {
        if (string == null) {
            return false;
        }
        for (char c : string.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    /**
     * find the first restaurant whose menu contains the pizza or null
     * @param pizza
     * @param restaurants
     * @return restaurant or null
     */
    public static Restaurant findRestaurantByPizza(Pizza pizza, Restaurant[] restaurants) {
        Optional<Restaurant> maybeRestaurant = Arrays.stream(restaurants).filter(
                restaurant -> checkPizzaInRestaurantMenu(pizza, restaurant)
        ).findFirst();
        return maybeRestaurant.orElse(null);
    }

    /**
     * Find order restaurant by first pizza
     * @param order
     * @param restaurants
     * @return restaurant or null
     */
    public static Restaurant findOrderRestaurant(Order order, Restaurant[] restaurants) {
        Pizza firstPizza = order.getPizzasInOrder()[0];
        return findRestaurantByPizza(firstPizza, restaurants);
    }

    /**
     * Checks if the restaurant's menu contains the pizza
     * @param pizza Pizza to be found
     * @param restaurant Restaurant whose menu is to be checked
     */
    public static boolean checkPizzaInRestaurantMenu(Pizza pizza, Restaurant restaurant) {
        Pizza[] menu = restaurant.menu();
        for (Pizza item : menu) {
            if (item.name().equals(pizza.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the order based on a set of criteria:
     * OrderNo, Date,
     *
     * @param orderToValidate    - Order instance to be validated
     * @param definedRestaurants - list of available restaurants to check menus
     * @return validated order instance
     */
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        // check status (delivered or already invalid)
        OrderStatus status = orderToValidate.getOrderStatus();
        if (status == OrderStatus.INVALID || status == OrderStatus.DELIVERED || status == OrderStatus.VALID_BUT_NOT_DELIVERED)
            return orderToValidate;

        String orderId = orderToValidate.getOrderNo();
        // check the order has an id
        if (orderId == null || orderId.isEmpty()) {
            // OrderIdNotAssigned. TODO: add to Enum
            invalidateOrder(orderToValidate, OrderValidationCode.UNDEFINED);
            return orderToValidate;
        }

        LocalDate orderDate = orderToValidate.getOrderDate();
        if (!isValidDate(orderDate.toString())) {
            invalidateOrder(orderToValidate, OrderValidationCode.UNDEFINED);
        }

        // check items:
        Pizza[] orderItems = orderToValidate.getPizzasInOrder();
        // number of items is (1 <= N <= 4)
        if (orderItems.length > 4) {
            invalidateOrder(orderToValidate, OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            return orderToValidate;
        }
        if (orderItems.length == 0) {
            // EmptyOrder. TODO: add to Enum
            invalidateOrder(orderToValidate);
            return orderToValidate;
        }

        // keeps track of the relevant restaurant
        // to make sure no different restaurants are involved
        Restaurant originalRestaurant = null;

        // while checking items, keep track of price to save time
        // start with the delivery fee
        int totalPrice = SystemConstants.ORDER_CHARGE_IN_PENCE;
        // check the rest of the items are valid
        for (Pizza orderItem : orderItems) {
            // try to find the relevant restaurant
            // p.s. checks restaurants for every single pizza
            // can be avoided by storing restaurant id with the pizza
            Restaurant restaurant = findRestaurantByPizza(orderItem, definedRestaurants);
            if (restaurant == null) {
                invalidateOrder(orderToValidate, OrderValidationCode.PIZZA_NOT_DEFINED);
                return orderToValidate;
            }
            // check if restaurant has already been found
            if (originalRestaurant == null) {
                // not that the restaurant is established
                // check if open
                if (Arrays.stream(restaurant.openingDays()).noneMatch(day -> day == orderDate.getDayOfWeek())) {
                    invalidateOrder(orderToValidate, OrderValidationCode.RESTAURANT_CLOSED);
                    return orderToValidate;
                }
                originalRestaurant = restaurant;
            }
            // if the original restaurant has already been identified
            // check the item is from there
            if (restaurant != originalRestaurant) {
                invalidateOrder(orderToValidate, OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                return orderToValidate;
            }
            totalPrice += orderItem.priceInPence();
        }

        // check the price adds up + delivery fee
        if (totalPrice != orderToValidate.getPriceTotalInPence()) {
            invalidateOrder(orderToValidate, OrderValidationCode.TOTAL_INCORRECT);
            return orderToValidate;
        }

        // check the payment card details
        CreditCardInformation cardInformation = orderToValidate.getCreditCardInformation();
        String expiryDate = cardInformation.getCreditCardExpiry();
        // check the expiry date is valid
        if (!isOrderCreditCardExpiryDateValid(orderDate, expiryDate)) {
            invalidateOrder(orderToValidate, OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        String cardNumber = cardInformation.getCreditCardNumber();
        // check number is 16 digits and numeric
        if (cardNumber.length() != 16 || !isNumeric(cardNumber)) {
            invalidateOrder(orderToValidate, OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }

        String cvv = cardInformation.getCvv();
        // check cvv is 3 digits and numeric
        if (cvv.length() != 3 || !isNumeric(cvv)) {
            invalidateOrder(orderToValidate, OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }
        // the order is validated, set to valid and return
        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        return orderToValidate;
    }

    /**
     * Validates the mm/YY expiry date of a credit card
     * @param expiryDate string of format mm/YY
     * @return is the date valid
     */
    private static boolean isOrderCreditCardExpiryDateValid(LocalDate orderDate, String expiryDate) {
        String[] expiryDateArray = expiryDate.split("/");
        // validate expiry year
        String YY = expiryDateArray[1];
        int year = Integer.parseInt(YY);
        if (!isNumeric(YY) || orderDate.getYear() % 100 > year)
            return false;
        // validate expiry month (provided year is valid)
        String mm = expiryDateArray[0];
        if (!isNumeric(mm)) return false;
        int month = Integer.parseInt(mm);
        if ((orderDate.getYear() == year && orderDate.getMonth().getValue() > month) || month > 12)
            return false;
        return true;
    }

    /**
     * Invalidates the order with no code provided - sets to default: UNDEFINED
     * @param orderToInvalidate order instance to be invalidated
     */
    private void invalidateOrder(Order orderToInvalidate) {
        orderToInvalidate.setOrderStatus(OrderStatus.INVALID);
        orderToInvalidate.setOrderValidationCode(OrderValidationCode.UNDEFINED);
    }

    /**
     * Invalidates the order with the given OrderValidationCode
     * @param orderToInvalidate order instance to be invalidated
     * @param orderValidationCode code for invalidation (reason)
     */
    private void invalidateOrder(Order orderToInvalidate, OrderValidationCode orderValidationCode) {
        orderToInvalidate.setOrderStatus(OrderStatus.INVALID);
        orderToInvalidate.setOrderValidationCode(orderValidationCode);
    }
}
