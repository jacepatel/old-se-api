import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

/**
 * Created by jacepatel on 29/01/2015.
 */
public class OrdersControllerTest {

//THIS IS JUST A LINE TO TEST CI
//    @Test
//    public void createMobileOrderBadJson() {
//        running(fakeApplication(inMemoryDatabase()), new Runnable() {
//            @Override
//            public void run() {
//                // LOGIN
//                final Map<String, String> data = new HashMap<String, String>();
//
//                Long truckId = new Long(1);
//
//                String route = "/users/" + truckId.toString() + "/orders/hsd4m5m13jir8tcisqv1mbr3v6";
//                FakeRequest request = new FakeRequest(POST, route).withHeader("content-type", "application/json").withBody(Json.toJson(data));
//
//                Result result = routeAndCall(request);
//
//                assertThat(status(result)).isEqualTo(OK);
//                assertThat(contentAsString(result)).contains("JSON");
//            }
//        });
//    }




}
