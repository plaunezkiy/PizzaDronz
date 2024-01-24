package uk.ac.ed.inf;

import junit.framework.TestCase;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.utils.OrderValidator;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class TestOrderValidator extends TestCase {
    OrderValidator validator;
    Restaurant[] restaurants;
    Pizza pizza_1, pizza_2;
    String validExpiryDate;
    public TestOrderValidator () {
        validator = new OrderValidator();
        pizza_1 = new Pizza("Pizza_1", 200);
        pizza_2 = new Pizza("Pizza_2", 150);
        restaurants = new Restaurant[] {
                new Restaurant(
                        "Rest_1",
                        new LngLat(0, 0),
                        new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY},
                        new Pizza[] {pizza_1}
                ),
                new Restaurant(
                        "Rest_2",
                        new LngLat(0, 0),
                        new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY},
                        new Pizza[] {pizza_2}
                ),
        };
        validExpiryDate = String.format("12/%d", LocalDate.now().getYear() % 100);
    }
    public Restaurant[] getRestaurants() {
        return restaurants;
    }
    /**
     * Make sure the validation fails for the order with no ID provided
     */
    public void testOrderNoId () {
        Order orderNoId = new Order(
                null,
                LocalDate.now(),
                0,
                new Pizza[] {},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(orderNoId, getRestaurants());
        assertEquals(
                orderNoId.getOrderStatus(),
            OrderStatus.INVALID
        );
        assertEquals(
            orderNoId.getOrderValidationCode(),
            OrderValidationCode.UNDEFINED
        );
        Order orderEmptyId = new Order(
                "",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                0,
                new Pizza[] {},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(orderEmptyId, getRestaurants());
        assertEquals(orderEmptyId.getOrderStatus(), OrderStatus.INVALID);
        assertEquals(orderEmptyId.getOrderValidationCode(), OrderValidationCode.UNDEFINED);
    }
    /**
     * Make sure the validation fails for orders that are not today
     */
    public void testOrderWrongDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        Order orderYday = new Order(
                "OrderID",
                yesterday,
                0,
                new Pizza[] {},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(orderYday, getRestaurants());
        assertEquals(orderYday.getOrderStatus(), OrderStatus.INVALID);
        assertEquals(orderYday.getOrderValidationCode(), OrderValidationCode.UNDEFINED);

        Order orderTmr = new Order(
                "OrderID",
                tomorrow,
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                0,
                new Pizza[] {},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(orderTmr, getRestaurants());
        assertEquals(orderTmr.getOrderStatus(), OrderStatus.INVALID);
        assertEquals(orderTmr.getOrderValidationCode(), OrderValidationCode.UNDEFINED);
    }

    public void testWrongItemsNumber() {
        Order emptyOrder = new Order(
                "OrderID",
                LocalDate.now(),
                0,
                new Pizza[] {},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(emptyOrder, getRestaurants());
        assertEquals(
                emptyOrder.getOrderStatus(),
                OrderStatus.INVALID
        );
        assertEquals(
                emptyOrder.getOrderValidationCode(),
                OrderValidationCode.UNDEFINED
        );
        Order largeOrder = new Order(
                "OrderID",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                0,
                new Pizza[] {
                        pizza_1,
                        pizza_1,
                        pizza_1,
                        pizza_1,
                        pizza_1,
                },
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(largeOrder, getRestaurants());
        assertEquals(
                largeOrder.getOrderStatus(),
                OrderStatus.INVALID
        );
        assertEquals(
                largeOrder.getOrderValidationCode(),
                OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED
        );
    }

    public void testWrongPizza() {
        Order imaginaryOrder = new Order(
                "OrderID",
                LocalDate.now(),
                0,
                new Pizza[] {
                        new Pizza("New Pizza", 300)
                },
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(imaginaryOrder, getRestaurants());
        assertEquals(
                imaginaryOrder.getOrderStatus(),
                OrderStatus.INVALID
        );
        assertEquals(
                imaginaryOrder.getOrderValidationCode(),
                OrderValidationCode.PIZZA_NOT_DEFINED
        );
        Order diffRestOrder = new Order(
                "OrderID",
                LocalDate.now(),
                0,
                new Pizza[] {
                        pizza_1,
                        pizza_2
                },
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(diffRestOrder, getRestaurants());
        assertEquals(
                diffRestOrder.getOrderStatus(),
                OrderStatus.INVALID
        );
        assertEquals(
                diffRestOrder.getOrderValidationCode(),
                OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS
        );
    }

    public void testWrongTotalPrice() {
        Order orderNoFee = new Order(
                "OrderID",
                LocalDate.now(),
                pizza_1.priceInPence(),
                new Pizza[] {pizza_1},
                new CreditCardInformation("", "", "")
        );
        validator.validateOrder(orderNoFee, getRestaurants());
        assertEquals(
                orderNoFee.getOrderStatus(),
                OrderStatus.INVALID
        );
    }

    public void testWrongCreditCard() {
        Order orderInvalidExpiryDate = new Order(
                "OrderID",
                LocalDate.now(),
                pizza_1.priceInPence() + 100,
                new Pizza[] {pizza_1},
                new CreditCardInformation("", "99/99", "")
        );
        validator.validateOrder(orderInvalidExpiryDate, getRestaurants());
        assertEquals(
                OrderStatus.INVALID,
                orderInvalidExpiryDate.getOrderStatus()
        );
        assertEquals(
                OrderValidationCode.EXPIRY_DATE_INVALID,
                orderInvalidExpiryDate.getOrderValidationCode()
        );

        Order orderWrongCCNumber = new Order(
                "OrderID",
                LocalDate.now(),
                pizza_1.priceInPence() + 100,
                new Pizza[] {pizza_1},
                new CreditCardInformation("some_nonsense", validExpiryDate, "")
        );
        validator.validateOrder(orderWrongCCNumber, getRestaurants());
        assertEquals(
                OrderStatus.INVALID,
                orderWrongCCNumber.getOrderStatus()
        );
        assertEquals(
                OrderValidationCode.CARD_NUMBER_INVALID,
                orderWrongCCNumber.getOrderValidationCode()
        );
        Order orderWrongCvv = new Order(
                "OrderID",
                LocalDate.now(),
                pizza_1.priceInPence() + 100,
                new Pizza[] {pizza_1},
                new CreditCardInformation("1234567890123456", validExpiryDate, "ab")
        );
        validator.validateOrder(orderWrongCvv, getRestaurants());
        assertEquals(
                OrderStatus.INVALID,
                orderWrongCvv.getOrderStatus()
        );
        assertEquals(
                OrderValidationCode.CVV_INVALID,
                orderWrongCvv.getOrderValidationCode()
        );
    }
}
