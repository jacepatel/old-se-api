package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import models.BillingPlan;
import models.PaymentMethod;
import models.Plan;
import models.Truck;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.TrelloHandler;

import javax.persistence.Query;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 24/07/15.
 */

@CorsComposition.Cors
public class BillingPlansController extends Controller {

    @Transactional
    public static Result createBillingPlan(Long truckId) {
        Truck truck = JPA.em().find(Truck.class, truckId);
        HashMap result = new HashMap();
        Boolean registration = truck.currentBillingPlans.size() > 0;
        if (!truck.validateToken(request().getHeader("jwt-token"))) {
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        JsonNode json = request().body().asJson();
        BillingPlan bp = new BillingPlan();
        Plan p = Plan.getPlanByType(json.get("hardware").get("plan").asText());
        bp.plan = p;
        bp.numTabs = p.includedTabs;
        bp.current = true;
        bp.trial = true;
        bp.startDate = new Date();
        bp.price = p.price;
        bp.isDeleted = false;
        bp.truck = truck;
        bp.standOne = json.get("hardware").get("standOne").asText();
        bp.standTwo = json.get("hardware").get("standTwo").asText();
        bp.save();
        JPA.em().flush();
        if (registration) {
            TrelloHandler.createCard("Registration Complete: " + truck.name, "Contact them to arrange handover.");
        }
        return ok(Json.toJson(truck));
    }

    @Transactional
    public static Result getBillingPlans(Long truckId) {
        HashMap result = new HashMap();
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token")) || truck == null) {
            result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        } else {
            result.put("current", truck.currentBillingPlans);
            result.put("past", truck.pastBillingPlans);
        }
        return ok(Json.toJson(result));
    }

    @Transactional
    public static Result updateBillingPlan(Long billingPlanId) {
        BillingPlan bp = JPA.em().find(BillingPlan.class, billingPlanId);
        Truck truck = bp.truck;
        if (!truck.validateToken(request().getHeader("jwt-token"))) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        String paymentMethodNonce = request().body().asJson().get("paymentMethodNonce").asText();
        return ok(Json.toJson(bp.updatePaymentMethod(paymentMethodNonce)));
    }

    @Transactional
    public static Result updateBillingPlanFromTrial(Long billingPlanId) {
        JsonNode json = request().body().asJson();
        BillingPlan bp = JPA.em().find(BillingPlan.class, billingPlanId);
        Truck truck = bp.truck;
        if (!truck.validateToken(request().getHeader("jwt-token"))) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        String paymentMethodNonce = json.get("paymentMethodNonce").asText();
        HashMap result = bp.updateBillingPlanFromTrial(paymentMethodNonce, bp.plan.braintreetoken);
        return ok(Json.toJson(result));
    }
}
