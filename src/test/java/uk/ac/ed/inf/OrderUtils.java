package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import javax.swing.text.Caret;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class OrderUtils {
    public static Order getValidOrder(String orderId) {
        return new Order(
                orderId,
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.NO_ERROR,
                0,
                new Pizza[] {},
                new CreditCardInformation()
        );
    }

    public static CreditCardInformation getValidPaymentDetails() {
        CreditCardInformation details = new CreditCardInformation();
        details.setCreditCardNumber("1234567812345678");
        details.setCvv("123");
        details.setCreditCardExpiry("12/2030");
        return details;
    }

    public static Restaurant getRestaurant(LngLat coordinates) {
        return new Restaurant(
                "Restaurant",
                coordinates,
                new DayOfWeek[] {},
                new Pizza[] {}
        );
    }
}
