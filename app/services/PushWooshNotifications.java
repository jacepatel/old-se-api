package services;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.StatusCodes;
import models.PushNotification;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import play.db.DB;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by michaelsive on 23/04/15.
 */
public class PushWooshNotifications {

    private static String PushWooshSendUrl = "https://cp.pushwoosh.com/json/1.3/createMessage";
    private static String STATUS_SUCCESS = "200";
    private static String STATUS_ARG_ERROR = "210";
    private static String STATUS_SERVER_ERROR = "500";


    private static PushWooshNotifications instance = null;

    public static PushWooshNotifications getInstance() {
        if(instance == null) {
            instance = new PushWooshNotifications();
        }
        return instance;
    }

    public F.Promise<HashMap> sendNotification(String deviceToken, String deviceType, String title, String message, final HashMap extraData) throws Throwable {

        final HashMap payload = compilePayload(deviceToken, deviceType, title, message, extraData);
                        return WS.url(PushWooshSendUrl).setHeader("Content-Type", "application/json").setHeader("Accept", "application/json").post(Json.toJson(payload)).map(
                                new F.Function<WS.Response, HashMap>() {
                                    public HashMap apply(WS.Response response) {
                                        HashMap serviceResponse = new HashMap();
                                        JsonNode json = response.asJson();
                                        System.out.println(response.getBody());
                                        if (response.getStatus() == 200) {
                                            String statusCode = json.get("status_code").asText();
                                            if (statusCode.equals(STATUS_SUCCESS)) {
                                                serviceResponse.put(StatusCodes.SUCCESS, json.get("status_message"));
                                                if (json.has("response")) {
                                                    if (json.get("response").has("Messages")) {
                                                        serviceResponse.put("messagetoken", json.get("response").get("Messages").get(0).asText());
                                                    }
                                                }
                                            } else {
                                                serviceResponse.put(StatusCodes.ERROR, json.get("status_message").asText());
                                            }
                                        } else if (response.getStatus() == 400) {
                                            serviceResponse.put(StatusCodes.ERROR, "Malformed request.");
                                        } else if (response.getStatus() == 500) {
                                            serviceResponse.put(StatusCodes.ERROR, json.get("status_message"));
                                        }
                                        return serviceResponse;
                                    }
                                }
                        );
    }

    private HashMap compilePayload(String deviceToken, String deviceType,  String title, String message, HashMap extraData){
        String auth = System.getenv("PUSH_AUTH_TOKEN");
        HashMap payload = new HashMap();
        HashMap request = new HashMap();
        HashMap notifications = new HashMap();
        HashMap content = new HashMap();
        ArrayList platforms = new ArrayList<>();
        ArrayList devices = new ArrayList<>();
        devices.add(deviceToken);
        platforms.add(1);
        platforms.add(3);
        request.put("application", "0D74B-00E31");
        request.put("auth", auth);
        notifications.put("send_date", "now");
        notifications.put("ignore_user_timezone", true);
        if (deviceType.equals("ios")) {
            content.put("en", title);
        }
        else if (deviceType.equals("android")){
            content.put("en", message);
        }
        notifications.put("content", content);
        notifications.put("data", extraData);
        notifications.put("platforms", platforms);
        if (deviceType.equals("ios")){
            notifications.put("ios_badges", "1");
            notifications.put("ios_sound", "default");
        }
        else if (deviceType.equals("android")){
            notifications.put("android_header", title);
            notifications.put("android_vibration", true);
        }
        notifications.put("devices", devices);
        ArrayList notificationsArray = new ArrayList();
        notificationsArray.add(notifications);
        request.put("notifications", notificationsArray);
        payload.put("request", request);
        return payload;
    }
}
