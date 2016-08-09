package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import models.*;
import org.mindrot.jbcrypt.BCrypt;
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

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

/**
 * Created by michaelsive on 16/07/2014.
 */
@CorsComposition.Cors
public class TrucksController extends Controller {

    //Creates a truck
    @Transactional
    public static Result createTruck() {
        JsonNode json = request().body().asJson();

//        if (!JsonValidator.validate(JsonValidator.TRUCKS_VAL, json)){
//            HashMap<String, String> reply = new HashMap<String, String>();
//            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
//            return ok(Json.toJson(reply));
//        }

        if(Application.checkEmailUniqueness(json.get("email").asText())) {
            Truck newTruck = new Truck();
            newTruck.updateDetails(json);
            newTruck.SMSMessage = "#" + newTruck.name + " on instagram with your amazing food!";
            newTruck.save();

            TrelloHandler.createCard("NEW DEMO REGISTER FOR " + newTruck.name, newTruck.name + " ph: " + newTruck.contactNumber + " em: " + newTruck.contactEmail);

            JPA.em().flush();
            Item newItem = new Item();
            newItem.truckId = newTruck.truckId;
            newItem.color = "colorA";
            newItem.createdDate = new Date();
            newItem.description = "Double fried beer battered paprika salted delicious chips.";
            newItem.isActive = true;
            newItem.isActiveForUsers = true;
            newItem.isDeleted = false;
            newItem.maxQuantity = 20;
            newItem.name = "Chips";
            newItem.price = new BigDecimal(2);
            newItem.sort = 1L;
            newItem.shortDescription = "Chips";
            newItem.save();
            return ok(Json.toJson(newTruck));
        }

        else {
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR,"Email already in use.");
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result createTruckFromManager(){
        JsonNode json = request().body().asJson();
        if (!JsonValidator.validate(JsonValidator.TRUCK_FROM_MANAGER, json)){
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        String email = json.get("email").asText();
        if (!Application.checkEmailUniqueness(email)){
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR, "Email already in use.");
            return ok(Json.toJson(reply));
        }
        Truck newTruck = new Truck();
        newTruck.firstName = json.get("firstName").asText();
        newTruck.lastName = json.get("lastName").asText();
        newTruck.contactEmail = email;
        newTruck.email = email;
        newTruck.contactNumber = json.get("mobNum").asText();
        newTruck.name = json.get("name").asText();
        newTruck.passwordHash = BCrypt.hashpw(json.get("password").asText(), BCrypt.gensalt());
        newTruck.apikey = java.util.UUID.randomUUID().toString();
        newTruck.abn = json.get("abn").asText();
        newTruck.save();
        JPA.em().flush();
        String compact = Jwts.builder().setSubject(newTruck.email).signWith(HS256, Application.jwtSecret).compact();
        response().setHeader("JWT-Token", compact);
        TrelloHandler.createCard("NEW DEMO REGISTER FOR " + newTruck.name, newTruck.name + " ph: " + newTruck.contactNumber + " em: " + newTruck.contactEmail);
        return ok(Json.toJson(newTruck));
    }

    //Returns just a truck by truckId
    @Transactional
    public static Result getTruck(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        return ok(Json.toJson(truck));
    }

    @Transactional
    public static Result updateTruckDetails(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token")) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            truck.updateDetails(request().body().asJson());
            return ok(Json.toJson(truck));
        }
    }

    @Transactional
    public static Result deleteTruck(Long truckId) {
        Truck truckToDel = JPA.em().find(Truck.class, truckId);
        if (Application.isCurrentAccount(truckToDel.email, Application.account_type_VENDOR)) {
            truckToDel.isDeleted = true;
            return ok(StatusCodes.SUCCESS, "Truck succesfully deleted");
        }
         HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
    }
    //V1
    //Returns trucks that are open for mobile orders
    @Transactional
    public static Result userActiveTrucks() {
        List<Truck> trucks = Truck.getActiveTrucks();
        for (Truck t : trucks){
            TruckSession ts = t.getActiveSession();
            Query queueQuery = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
            queueQuery.setParameter("truckSessionId", ts.truckSessionId);
            t.queueSize = (Long) queueQuery.getSingleResult();
        }
        HashMap<String, List> trucksMap = new HashMap<String, List>();
        trucksMap.put("trucks", trucks);
        return ok(Json.toJson(trucksMap));
    }

    //Returns all events that are starting within 3 hours
    @Transactional
    public static Result possibleUpcomingEvents() {

        List<Event> upcomingEvents = Event.upcomingEvents();
        HashMap<String, List> eventsMap = new HashMap<String, List>();
        eventsMap.put("events", upcomingEvents);
        return ok(Json.toJson(eventsMap));
    }

    //Returns items and truck
    @Transactional
    public static Result getTruckMenuItems(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        Query queueQuery = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
        queueQuery.setParameter("truckSessionId", truck.getActiveSession().truckSessionId);
        truck.queueSize = (Long) queueQuery.getSingleResult();
        List<Item> menu = truck.getActiveMenuItems();

        Collections.sort(menu, new Comparator<Item>() {
            @Override
            public int compare(Item p1, Item p2) {
                //return (int) (p1.acceptedTime.getTime()/1000) - (int) (p2.acceptedTime.getTime()/1000);
                return Integer.parseInt(p1.sort.toString()) - Integer.parseInt(p2.sort.toString());
            }
        });


        HashMap<String, JsonNode> itemsMap = new HashMap<String, JsonNode>();
        itemsMap.put("items", Json.toJson(menu));
        itemsMap.put("truck", Json.toJson(truck));
        return ok(Json.toJson(itemsMap));
    }

    //Returns active menu items only
    @Transactional
    public static Result getCurrentMenuItems(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        List<Item> items = truck.items;

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item p1, Item p2) {
                //return (int) (p1.acceptedTime.getTime()/1000) - (int) (p2.acceptedTime.getTime()/1000);
                return Integer.parseInt(p1.sort.toString()) - Integer.parseInt(p2.sort.toString());
            }
        });

        HashMap<String, JsonNode> itemsMap = new HashMap<String, JsonNode>();
        itemsMap.put("items", Json.toJson(items));
        return ok(Json.toJson(itemsMap));
    }

    @Transactional
    public static Result resetPassword(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        HashMap result = new HashMap();
        if (!truck.validateToken(request().getHeader("jwt-token")) || truck == null) {
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            JsonNode json = request().body().asJson();
            if (BCrypt.checkpw(json.get("currentPass").asText(), truck.passwordHash)){
                truck.passwordHash = BCrypt.hashpw(json.get("currentPass").asText(), BCrypt.gensalt());
                result.put("success", "Password changed.");
            }
            else {
                result.put(StatusCodes.ERROR, "Current password was incorrect.");
            }
        }
        return ok(Json.toJson(result));
    }
}
