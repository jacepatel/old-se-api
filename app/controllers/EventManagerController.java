package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.JsonValidator;
import services.TrelloHandler;

import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by michaelsive on 16/07/2014.
 */
@CorsComposition.Cors
public class EventManagerController extends Controller {

    //Creates a truck
    @Transactional
    public static Result createEventManager() {
        JsonNode json = request().body().asJson();

      if (!JsonValidator.validate(JsonValidator.EVENTMANAGER_VAL, request().body().asJson())){
        HashMap<String, String> reply = new HashMap<String, String>();
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
        return ok(Json.toJson(reply));
      }

        if(Application.checkEmailUniqueness(json.get("username").asText())) {
            EventManager newEventManager = new EventManager();
            newEventManager.updateDetails(json);
            newEventManager.save();
            return ok(Json.toJson(newEventManager));
        }

        else {
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR,"Username is already in use.");
            return ok(Json.toJson(reply));
        }
    }

    //Returns just a truck by truckId
    @Transactional
    public static Result getEventManager(Long eventManagerId){
        EventManager foundEventmanager = JPA.em().find(EventManager.class, eventManagerId);
        return ok(Json.toJson(foundEventmanager));
    }


    @Transactional
    public static Result updateEventManager(Long eventManagerId) {

      if (!JsonValidator.validate(JsonValidator.EVENTMANAGER_VAL, request().body().asJson())){
        HashMap<String, String> reply = new HashMap<String, String>();
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
        return ok(Json.toJson(reply));
      }

        EventManager eventManagerToUpdate = JPA.em().find(EventManager.class, eventManagerId);

        if (Application.isCurrentAccount(eventManagerToUpdate.username, Application.account_type_EVENTMANAGER)) {
            eventManagerToUpdate.updateDetails(request().body().asJson());
            eventManagerToUpdate.save();
            return ok(Json.toJson(eventManagerToUpdate));
        }

        HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
    }

}
