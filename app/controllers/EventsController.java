package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.Event;
import models.EventManager;
import models.Item;
import models.Truck;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.JsonValidator;

import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 16/07/2014.
 */
@CorsComposition.Cors
public class EventsController extends Controller {

    //Creates a truck
    @Transactional
    public static Result createEvent() {

      HashMap<String, String> reply = new HashMap<String, String>();
      if (!JsonValidator.validate(JsonValidator.EVENT_VAL, request().body().asJson())){
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
        return ok(Json.toJson(reply));
      }


      JsonNode json = request().body().asJson();

      EventManager currentEventManager = Application.currentEventManager();

      Event newEvent = new Event();

      if (currentEventManager != null && json.get("eventManagerId").asLong() == currentEventManager.eventManagerId) {
        newEvent.updateDetails(json);
        newEvent.save();
        return ok(Json.toJson(newEvent));
      }

      reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
      return ok(Json.toJson(reply));

    }

    @Transactional
    public static Result updateEvent(Long eventId) {

      HashMap<String, String> reply = new HashMap<String, String>();
      if (!JsonValidator.validate(JsonValidator.EVENT_VAL, request().body().asJson())){
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
        return ok(Json.toJson(reply));
      }

      JsonNode json = request().body().asJson();

      EventManager currentEventManager = Application.currentEventManager();

      Event eventToUpdate = JPA.em().find(Event.class, eventId);

      if (currentEventManager != null && eventToUpdate.eventManagerId == currentEventManager.eventManagerId) {
        eventToUpdate.updateDetails(json);
        eventToUpdate.save();
        return ok(Json.toJson(eventToUpdate));
      }

      reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
      return ok(Json.toJson(reply));

    }

    @Transactional
    public static Result getAllEvents() {

      HashMap<String, String> reply = new HashMap<String, String>();
//      if (!JsonValidator.validate(JsonValidator.EVENT_VAL, request().body().asJson())){
//        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
//        return ok(Json.toJson(reply));
//      }

      JsonNode json = request().body().asJson();

      EventManager currentEventManager = Application.currentEventManager();


      if (currentEventManager != null) {

        List<Event> eventsToReturn = currentEventManager.events;

        return ok(Json.toJson(eventsToReturn));
      }

      reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
      return ok(Json.toJson(reply));

    }

}
