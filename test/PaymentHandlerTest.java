import models.Order;
import models.Truck;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;
import play.db.jpa.JPA;
import services.PaymentHandler;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

/**
 * Created by michaelsive on 29/01/15.
 */
public class PaymentHandlerTest {
    Order testOrder;
    Truck truck;
    User user;

//    @Before
//    public void setup(){
//        PaymentHandler pmHa = PaymentHandler.getGateway();
//        running(fakeApplication(), new Runnable() {
//            public void run() {
//                JPA.withTransaction(new play.libs.F.Callback0() {
//                    public void invoke() {
//                        truck = JPA.em().createQuery("FROM Truck", Truck.class).getResultList().get(0);
//                        user = JPA.em().createQuery("FROM User", User.class).getResultList().get(0);
//                        testOrder = new Order();
//                        testOrder.userId = user.userId;
//                        testOrder.truckId = truck.truckId;
//                        testOrder.orderStatus = Order.STATUS_CONFIRMED;
//                        testOrder.orderType = 1;
//                        testOrder.orderTotal = new BigDecimal(10.00);
//                        testOrder.orderTime = new Date();
//                        testOrder.truckSessionId = truck.getActiveSession().truckSessionId;
//                        testOrder.paymentMethodId = user.getDefaultPaymentMethod().paymentMethodId;
//                        testOrder.save();
//                    }
//                });
//            }
//        });
//    }
//
//    @Test
//    public void scheduleAndSubmitPayment(){
//        running(fakeApplication(), new Runnable() {
//            public void run() {
//                JPA.withTransaction(new play.libs.F.Callback0() {
//                    public void invoke() {
//                        PaymentHandler pmHa = PaymentHandler.getGateway();
//
//                        HashMap paySchedResult = pmHa.schedulePayment(testOrder.orderId, new BigDecimal(10.00), user.getDefaultPaymentMethod().braintreeToken);
//                        assertThat(paySchedResult.containsKey("scheduledPayment"));
//                        if (paySchedResult.containsKey("scheduledPayment")) {
//                            HashMap paySubmitResult = pmHa.submitPayment(testOrder.orderId);
//                            assertThat(paySubmitResult.containsKey("processedPayment"));
//                        }
//                    }
//                });
//            }
//        });
//    }
//
//    @After
//    public void cleanUp(){
//        running(fakeApplication(), new Runnable() {
//            public void run() {
//                JPA.withTransaction(new play.libs.F.Callback0() {
//                    public void invoke() {
//                        JPA.em().remove(JPA.em().contains(testOrder) ? testOrder : JPA.em().merge(testOrder));
//                    }
//                });
//            }
//        });
//    }
}
