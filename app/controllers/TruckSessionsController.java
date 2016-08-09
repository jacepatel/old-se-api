package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import com.newrelic.agent.deps.org.json.simple.JSONArray;
import org.apache.commons.mail.EmailException;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.mvc.*;
import play.libs.Json;
import scala.util.parsing.json.JSON;
import scala.util.parsing.json.JSONObject;
import services.*;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jacepatel on 29/11/2014.
 */
@CorsComposition.Cors
public class TruckSessionsController extends Controller {


    //Creates a truck session
    @play.db.jpa.Transactional
    public static Result createTruckSession() {
        JsonNode request = request().body().asJson();
        HashMap<String, String> reply = new HashMap<String, String>();

//        if (!JsonValidator.validate(JsonValidator.TRUCK_SESSION_VAL, request)){
//            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
//            return ok(Json.toJson(reply));
//        }
        Truck vendor  = Application.currentVendor();
        //Check if the current session has a user
        if (vendor.truckId != request.get("truckId").asLong())
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        else {
            TruckSession newTruckSession = vendor.getActiveSession();

            //This cancels the create if a truck session already exists
            if (newTruckSession.isActive == true)
            {
                return ok(Json.toJson(newTruckSession));
            }

            //This updates the details with posted informaiton adn returns it
            newTruckSession.updateTruckSessionDetails(request);
            newTruckSession.orderCount = 0L;
            newTruckSession.save();

            return ok(Json.toJson(newTruckSession));
        }
        //Note to self, build a library of return strings maybe
    }

    @play.db.jpa.Transactional
    public static Result getCurrentTruckSession(long truckId) {
        //comment for no reason
        HashMap<String, String> reply = new HashMap<String, String>();

        try {
            Truck vendor = Application.currentVendor();
        }
        catch (Exception ex) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        Truck vendor = Application.currentVendor();

        //Check if the current session has a user
        if (vendor.truckId != truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        else {
            TruckSession newTruckSession = vendor.getActiveSession();
            return ok(Json.toJson(newTruckSession));
        }
    }

    @play.db.jpa.Transactional
    public static Result getFutureTruckSessions(long truckId) {

        HashMap<String, String> reply = new HashMap<String, String>();

        try {
            Truck vendor = Application.currentVendor();
        }
        catch (Exception ex) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        Truck vendor = Application.currentVendor();

        //Check if the current session has a user
        if (vendor.truckId != truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        else {
            //Te two hours
            Date tenHoursAgo = new Date(System.currentTimeMillis() - 3600 * 10000);

            Query futureTruckSessionQry = JPA.em().createQuery("SELECT t FROM TruckSession t WHERE t.truckId = :truckId AND t.startTime > :currentTime " +
                    "AND isDeleted = :isfalse AND isUsed = :isfalse");
            futureTruckSessionQry.setParameter("isfalse", false);
            futureTruckSessionQry.setParameter("truckId", vendor.truckId);
            futureTruckSessionQry.setParameter("currentTime", tenHoursAgo);

            List<TruckSession> futureTruckSessions = futureTruckSessionQry.getResultList();


            HashMap<String, JsonNode> returnMap = new HashMap<String, JsonNode>();
            returnMap.put("futureSessions", Json.toJson(futureTruckSessions));

            List<Event> upcomingEvents = Event.upcomingEvents();
            returnMap.put("events", Json.toJson(upcomingEvents));

            return ok(Json.toJson(returnMap));
        }
    }

    @play.db.jpa.Transactional
    public static Result updateTruckSession(Long truckSessionId) {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        //Calls the item validator to check the validity of the json parsed through from the post request
        //        if (!JsonValidator.validate(JsonValidator.TRUCK_SESSION_VAL, request().body().asJson())){
        //            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
        //            return ok(Json.toJson(reply));
        //        }
        TruckSession truckSessionToUpdate = JPA.em().find(TruckSession.class, truckSessionId);
        Truck vendor  = Application.currentVendor();
        //Check if the current session has a user
        if (vendor == null)
        {
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        //Checks the item to update has the current truck
        if (vendor != null && truckSessionToUpdate.truckId == vendor.truckId) {
            truckSessionToUpdate.updateTruckSessionDetails(request().body().asJson());
            truckSessionToUpdate.save();


            return ok(Json.toJson(truckSessionToUpdate));
        } else {
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    //Updates the close time to the set from settings
    @Transactional
    public static Result updateCloseTime(Long truckSessionId, String CloseTime){
        HashMap<String, String> reply = new HashMap<String, String>();
        TruckSession truckSessionToUpdate = JPA.em().find(TruckSession.class, truckSessionId);
        Truck vendor  = Application.currentVendor();
        if (vendor != null && truckSessionToUpdate.truckId == vendor.truckId){
            try {
                truckSessionToUpdate.endTime = new Date(Long.parseLong(CloseTime));
                truckSessionToUpdate.save();
                reply.put(StatusCodes.SUCCESS, CloseTime);
                return ok(Json.toJson(reply));
            }
            catch (Exception ex) {
                reply.put(StatusCodes.ERROR, "Invalid Date Format" + ex.getMessage());
                return ok(Json.toJson(reply));
            }
        }
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
        return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result closeTruckSession(Long truckSessionId) {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        //Gets the current truck session
        TruckSession truckSessionToUpdate = JPA.em().find(TruckSession.class, truckSessionId);

        //Gets the current vendor
        Truck vendor  = Application.currentVendor();

        //Check if the current vendor owns the truck session
        if (vendor != null && truckSessionToUpdate.truckId == vendor.truckId)
        {
            if(TruckSession.activeOrderCount(truckSessionToUpdate.truckSessionId))
            {
                reply.put(StatusCodes.ERROR,"You currently have active orders. Please complete these before closing the shop");
                return ok(Json.toJson(reply));
            }



            truckSessionToUpdate.endTime = new Date();
            truckSessionToUpdate.isActive = false;
            truckSessionToUpdate.save();
            try {
                Report report = new Report(truckSessionToUpdate);
                report.send();
            }
            catch (Exception e){
                System.out.println("Could not generate report.");
                System.out.println(e.getMessage());
                TrelloHandler.createCard("Report Generation Failed For TSID " + truckSessionToUpdate.truckSessionId, "Please generate a report manually. Error: " + e.getMessage());
            }
            //Returns the full truckSession details with a new close time and isActive set to false
            return ok(Json.toJson(truckSessionToUpdate));
        }

        //Somehting wasn't caught, wild error
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(reply));

    }

    @Transactional
    public static Result toggleUserTruckSession(Long truckSessionId) {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        //Gets the current truck session
        TruckSession truckSessionToUpdate = JPA.em().find(TruckSession.class, truckSessionId);

        //Gets the current vendor
        Truck vendor  = Application.currentVendor();

        //Check if the current vendor owns the truck session
        if (vendor != null && truckSessionToUpdate.truckId == vendor.truckId)
        {
            truckSessionToUpdate.isActiveForOrders = !truckSessionToUpdate.isActiveForOrders;
            truckSessionToUpdate.save();
            //Returns the full truckSession details with a new close time and isActive set to false
            return ok(Json.toJson(truckSessionToUpdate));
        }

        //Somehting wasn't caught, wild error
        reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(reply));

    }

    @play.db.jpa.Transactional
    public static Result getAllOrdersAfterLastUpdate(Long truckSessionId, String lastUpdateTime) {
        TruckSession truckSession = JPA.em().find(TruckSession.class, truckSessionId);
        List<Order> updatedOrders = truckSession.getOrdersUpdatedAfterTime(truckSession.truckSessionId, lastUpdateTime);

        Collections.sort(updatedOrders, new Comparator<Order>() {
            @Override
            public int compare(Order p1, Order p2) {
                //return (int) (p1.acceptedTime.getTime()/1000) - (int) (p2.acceptedTime.getTime()/1000);
                return Integer.parseInt( p1.orderId.toString() ) - Integer.parseInt(p2.orderId.toString());
            }
        });

        HashMap<String, JsonNode> ordersMap = new HashMap<String, JsonNode>();
        ordersMap.put("orders", Json.toJson(updatedOrders));
        return ok(Json.toJson(ordersMap));
    }

    @play.db.jpa.Transactional
    public static Result getAllOrders(Long truckSessionId){
        TruckSession truckSession = JPA.em().find(TruckSession.class, truckSessionId);
        //List<Order> activeOrders = truckSession.orders;
        List<Order> activeOrders = truckSession.orders;

        Collections.sort(activeOrders, new Comparator<Order>() {
            @Override
            public int compare(Order p1, Order p2) {
                return Integer.parseInt( p1.orderId.toString() ) - Integer.parseInt(p2.orderId.toString());
//                long t1 = p1.acceptedTime.getTime();
//                long t2 = p2.acceptedTime.getTime();
//                if(t2 > t1)
//                    return 1;
//                else if(t1 > t2)
//                    return -1;
//                else
//                    return 0;
            }
        });

        HashMap<String, JsonNode> activeOrdersMap = new HashMap<String, JsonNode>();
        activeOrdersMap.put("orders", Json.toJson(activeOrders));
        return ok(Json.toJson(activeOrdersMap));
    }

    @play.db.jpa.Transactional
    public static Result getAllOrdersIncrementally (Long truckSessionId, Long lastOrderId){

        TruckSession truckSession = JPA.em().find(TruckSession.class, truckSessionId);
        //List<Order> activeOrders = truckSession.orders;
        List<Order> activeOrders = truckSession.getOrdersAfterOrderId(truckSession.truckSessionId, lastOrderId);

        Collections.sort(activeOrders, new Comparator<Order>() {
            @Override
            public int compare(Order p1, Order p2) {
                return Integer.parseInt( p1.orderId.toString() ) - Integer.parseInt(p2.orderId.toString());
            }
        });

        HashMap<String, JsonNode> activeOrdersMap = new HashMap<String, JsonNode>();
        activeOrdersMap.put("orders", Json.toJson(activeOrders));
        return ok(Json.toJson(activeOrdersMap));
    }

    @play.db.jpa.Transactional
    public static Result getAllOrdersIncrementallyDescending (Long truckSessionId, Long lastOrderId){

        TruckSession truckSession = JPA.em().find(TruckSession.class, truckSessionId);

        if (lastOrderId == 0) {
            lastOrderId = truckSession.orderCount + 1;
        }

        List<Order> activeOrders = truckSession.getOrdersBeforeOrderId(truckSession.truckSessionId, lastOrderId);

        Collections.sort(activeOrders, new Comparator<Order>() {
            @Override
            public int compare(Order p1, Order p2) {
                return Integer.parseInt(p2.orderId.toString()) - Integer.parseInt( p1.orderId.toString());
            }
        });

        HashMap<String, JsonNode> activeOrdersMap = new HashMap<String, JsonNode>();
        activeOrdersMap.put("orders", Json.toJson(activeOrders));
        return ok(Json.toJson(activeOrdersMap));
    }


    @play.db.jpa.Transactional
    public static Result getOrderCount(Long truckSessionId){
        TruckSession truckSession = JPA.em().find(TruckSession.class, truckSessionId);
        //List<Order> activeOrders = truckSession.orders;
        List<Object []> orderSummary = truckSession.orderCount(truckSessionId);

        HashMap<String, JsonNode> orderSummaryMap = new HashMap<String, JsonNode>();

        orderSummaryMap.put("orderCount", Json.toJson(orderSummary));

        return ok(Json.toJson(orderSummaryMap));
    }

    @Transactional
    public static Result getFacebookSessions(String fbPageId){
        Query truckQ = JPA.em().createQuery("select t from Truck t where t.facebookPageId = :fbId");
        truckQ.setParameter("fbId", fbPageId);
        Truck t = (Truck)truckQ.getSingleResult();
        return ok(Json.toJson(t.facebookData()));
    }
}
