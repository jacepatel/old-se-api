package controllers;

import actions.CorsComposition;
import com.twilio.sdk.TwilioRestException;
import models.Order;
import play.db.jpa.JPA;
import play.mvc.Result;
import play.libs.Json;
import play.mvc.Controller;
import services.PaymentHandler;
import services.TwilioMessaging;

import java.util.HashMap;

/**
 * Created by michaelsive on 30/12/14.
 */
@CorsComposition.Cors
public class PaymentsController extends Controller {

    //Gets client token from BrainTree server.
    public static Result getBrainTreeClientToken(){
        PaymentHandler gateway = PaymentHandler.getGateway();
        String clientToken = gateway.getClientToken();
        HashMap<String, String> response = new HashMap<String, String>();
        response.put("clientToken", clientToken);
        return ok(Json.toJson(response));
    }
}
