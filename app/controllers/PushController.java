package controllers;

import actions.CorsComposition;
import models.Device;
import models.User;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.MailHandler;
import services.PushWooshNotifications;
import services.TwilioMessaging;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by michaelsive on 20/04/15.
 */
@CorsComposition.Cors
public class PushController extends Controller {
    @Transactional
    public static Result sendTestNotification(Long deviceId) throws Throwable {
        Device userDevice = JPA.em().find(Device.class, deviceId);
        if (userDevice != null) {
            userDevice.lastused = new Date();
            String title = "Order #2 is ready to collect!";
            String message = "Tap here to bring up your order.";
            String type = "orderready";
            HashMap extra = new HashMap();
            extra.put("test", "test");
            PushWooshNotifications pwn = PushWooshNotifications.getInstance();
            HashMap result = pwn.sendNotification(userDevice.deviceToken, userDevice.deviceType, title, message, extra).get();
            System.out.println(result.toString());
            return ok(Json.toJson(result));
        }
        return ok();
    }

    @Transactional
    public static F.Promise<Result> sendTestTextMessage(Long userId){
        User user = JPA.em().find(User.class, userId);
        return TwilioMessaging.sendMsg(user.mobNumber, "Test message.").map(
                new F.Function<HashMap, Result>() {
                    public Result apply(HashMap hm){
                        return ok(Json.toJson(hm));
                    }
                }
        );
    }

    public static F.Promise<Result> sendTestSignupEmail(String email){
        return MailHandler.sendMailChimpTemplateWithMandrill("User Signup", email).map(
                new F.Function<HashMap, Result>() {
                    public Result apply(HashMap hm){
                        return ok(Json.toJson(hm));
                    }
                }
        );
    }
}
