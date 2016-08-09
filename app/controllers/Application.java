package controllers;

import actions.CorsComposition;
import models.*;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CorsComposition.Cors
public class Application extends Controller {


    //Strings for declaring the account types user vs vendor
    public static final String account_type_VENDOR = "Vendor";
    public static final String account_type_USER = "User";
    public static final String account_type_EVENTMANAGER = "EventManager";
    public static final String jwtSecret = "Gq4oUIhZgP";
    public static final String SPREEDLY_AppSecret = "E4bqrfStbKCX1EWZw0Ds6Y0vQJq4W2GTu5ggWVPYdvpPxSZQudw189bpka6PtYAf";
    public static final String SPREEDLY_EnvKey = "Bv5jG9JAFtg3SeQqpwCFpTQu1PJ";

    //Used for CORS - initial request to provide CORS option
    public static Result preflight(String path) {
        return ok("");
    }

    //Default rendering for index
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    @Transactional
    public static Result checkForUpdate(String version){
        List<Version> moreRecentVersions = new ArrayList<Version>();
        Version userVersion = Version.getByCode(version);
        if (userVersion != null) {
            moreRecentVersions = Version.getMoreRecent(userVersion.versionid);
            return ok(Json.toJson(moreRecentVersions));
        }
        else {
            return ok(Json.toJson(moreRecentVersions));
        }
    }

    @Transactional
    public static Result getSpreedlyEnvKey(){
        String currentKey = SpreedlyVariable.getCurrentEnvironmentKey();
        HashMap result = new HashMap();
        result.put("spreedlyKey", currentKey);
        return ok(Json.toJson(result));
    }

    public static Result returnOk() {
        return ok("loaderio-2ec52278cad4af47840c75721b144fa1");
    }

    //Recieves an email and checks if this is a current account
    static boolean isCurrentAccount(String email, String accountType) {
        if (accountType == account_type_VENDOR) {
            return email.equals(Truck.findByEmail(session().get("email")).email);
        }
        else {
            return email.equals(EventManager.findByUsername(session().get("username")).username);
        }
    }

    //Gets the current vendor from cookie sessions in play
    public static Truck currentVendor() {
        if  (session().get("session_type").contains(account_type_VENDOR)){
            return Truck.findByEmail(session().get("email"));
        }
        return null;
    }

    static EventManager currentEventManager() {
      if (session().get("session_type").contains(account_type_EVENTMANAGER)) {
        return EventManager.findByUsername(session().get("username"));
      }
      return null;
    }

    //Checks if email address is in use
    static boolean checkEmailUniqueness(String email){
        User user = User.findByEmail(email);
        Truck truck = Truck.findByEmail(email);
        EventManager eventManager = EventManager.findByUsername(email);

        if (user != null || truck != null || eventManager != null){
             return false;
        }
        return true;
    }

    static boolean checkMobNumberUniqueness(String mobNum){
        User user = User.findByMobNum(mobNum);
        if (user != null){
            return false;
        }
        return true;
    }

}
