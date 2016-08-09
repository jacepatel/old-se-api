package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.Order;
import models.Subscription;
import models.Truck;
import models.TruckSession;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.JsonValidator;
import services.MailHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jacepatel on 29/11/2014.
 */
@CorsComposition.Cors
public class SubscriptionController extends Controller {

    @Transactional
    public static Result createSubscription(String email) {
        String userListId = "9e12999835";
        Subscription checkExisting  = Subscription.findByEmail(email);

        HashMap<String, String> reply = new HashMap<String, String>();
        //Check if the current session has a user
        if (checkExisting == null)
        {
            Subscription newSubscription = new Subscription();
            newSubscription.email = email;
            newSubscription.isactive = true;
            newSubscription.joinedDate = new Date();
            newSubscription.isvendor = false;
            newSubscription.name = "";
            newSubscription.mobilenumber = "";
            newSubscription.save();
            MailHandler.subscribeToMailChimp(email, userListId);
        }
        else {
            checkExisting.isactive = true;
            checkExisting.save();
        }
            reply.put(StatusCodes.SUCCESS, email);
            return ok(Json.toJson(reply));
        //Note to self, build a library of return strings maybe
    }

    @Transactional
    public static Result createVendorSubscription(String email, String name, String mobileNumber) {

        Subscription checkExisting  = Subscription.findByEmail(email);

        HashMap<String, String> reply = new HashMap<String, String>();
        //Check if the current session has a user
        if (checkExisting == null)
        {
            Subscription newSubscription = new Subscription();
            newSubscription.email = email;
            newSubscription.isactive = true;
            newSubscription.joinedDate = new Date();
            newSubscription.isvendor = true;
            newSubscription.name = name;
            newSubscription.mobilenumber = mobileNumber;
            newSubscription.save();
        }
        else {
            checkExisting.isactive = true;
            checkExisting.save();
        }
        MailHandler.sendEnquiryToSales(email, name, mobileNumber);
        reply.put(StatusCodes.SUCCESS, email);
        return ok(Json.toJson(reply));
        //Note to self, build a library of return strings maybe
    }


}
