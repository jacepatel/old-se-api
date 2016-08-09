package controllers;

import actions.CorsComposition;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import models.Device;
import models.User;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;

/**
 * Created by michaelsive on 6/05/15.
 */
@CorsComposition.Cors
public class DevicesController extends Controller {

    @Transactional
    public static Result registerDevice(String deviceToken, String deviceType){
        HashMap<String, String> reply = new HashMap<String, String>();
        String usrEmail;
        User user;
        try {
            usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            user = User.findByEmail(usrEmail);
        }
        catch(SignatureException | NullPointerException e){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        if (user != null){
            Device currentDeviceWithToken = Device.findByToken(deviceToken);
            if (currentDeviceWithToken == null || currentDeviceWithToken.userId != user.userId) {
                return ok(Json.toJson(user.addDeviceToUser(deviceToken, deviceType)));
            }
            else {
                return ok(Json.toJson(currentDeviceWithToken));
            }
        }
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
        return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result updateDeviceToken(Long deviceId, String deviceToken){
        HashMap<String, String> reply = new HashMap<String, String>();
        String usrEmail;
        User user;
        try {
            usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            user = User.findByEmail(usrEmail);
        }
        catch(SignatureException | NullPointerException e){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        Device currentDevice = JPA.em().find(Device.class, deviceId);
        if (currentDevice == null){
            reply.put(StatusCodes.ERROR, "No such device.");
            return ok(Json.toJson(reply));
        }
        if (user == null || currentDevice.userId != user.userId){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        if (!currentDevice.deviceToken.equals(deviceToken)) {
            currentDevice.deviceToken = deviceToken;
            currentDevice.save();
            JPA.em().flush();
            return ok(Json.toJson(currentDevice));
        }
        return ok(Json.toJson(currentDevice));
    }
}
