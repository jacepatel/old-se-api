package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import models.EventManager;
import models.Truck;
import models.User;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.JsonValidator;

import java.util.HashMap;
/**
 * Created by michaelsive on 17/07/2014.
 */
@CorsComposition.Cors
public class SessionsController extends Controller {

    @play.db.jpa.Transactional
    public static Result createSession() {
        JsonNode request = request().body().asJson();
        String email = request.get("email").asText();
        Truck truck = Truck.findByEmail(email);
        if (truck != null && truck.authPass(request.get("password").asText())) {
            session().clear();
            session().put("email", truck.email);
            session().put("session_type", "Vendor");
            return ok(Json.toJson(truck));
        }

        HashMap<String, String> reply = new HashMap<String, String>();
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
        return ok(Json.toJson(reply));
    }

  @play.db.jpa.Transactional
  public static Result createEventManagerSession() {
    JsonNode request = request().body().asJson();
    String username = request.get("username").asText();
    EventManager eventManagerLogin = EventManager.findByUsername(username);
    if (eventManagerLogin != null && eventManagerLogin.authPass(request.get("password").asText())) {
      session().clear();
      session().put("username", eventManagerLogin.username);
      session().put("session_type", Application.account_type_EVENTMANAGER);
      return ok(Json.toJson(eventManagerLogin));
    }

    HashMap<String, String> reply = new HashMap<String, String>();
    reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
    return ok(Json.toJson(reply));
  }

    public static Result destroySession(){
        session().clear();
        HashMap<String, String> reply = new HashMap<String, String>();
            reply.put("result","Logged out.");
            return ok(Json.toJson(reply));
    }
}
