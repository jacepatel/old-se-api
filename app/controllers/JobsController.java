package controllers;

import actions.CorsComposition;
import models.Device;
import models.Order;
import models.TruckSession;
import models.User;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.PushWooshNotifications;
import services.Report;
import services.TrelloHandler;
import services.TwilioMessaging;

import javax.persistence.Query;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 20/04/15.
 */
@CorsComposition.Cors
public class JobsController extends Controller {
    @Transactional
    public static Result trucksessionTimeout() {

        Date fifteenMinutesAgo = new Date(System.currentTimeMillis() - 3600 * 300);
        Date rightNow = new Date();

        Query qryStillActiveTruckSessions = JPA.em().createQuery("select ts from TruckSession ts WHERE ts.isActive = :isTrue and ts.endTime < :rightNow");
        qryStillActiveTruckSessions.setParameter("isTrue", true);
        qryStillActiveTruckSessions.setParameter("rightNow", rightNow);
        List<TruckSession> stillActiveTruckSessions = qryStillActiveTruckSessions.getResultList();
        for (TruckSession t : stillActiveTruckSessions){

            String fiftMinAgo = "" + fifteenMinutesAgo.getTime() / 1000;

            System.out.println(fiftMinAgo);

            List<Order> recentOrders = t.getOrdersUpdatedAfterTime(t.truckSessionId, fiftMinAgo);

            if (recentOrders.isEmpty()) {
                t.isActive = false;
                t.endTime = new Date();
                t.save();

                //Add in a send of reports //
                try {
                    Report report = new Report(t);
                    report.send();
                }
                catch (Exception e){
                    System.out.println("Could not generate report.");
                    System.out.println(e.getMessage());
                    TrelloHandler.createCard("Report Generation Failed For TSID " + t.truckSessionId, "Please generate a report manually. Error: " + e.getMessage());
                }
            }

        }

        System.out.println("Closing trucksessions");
        return ok();
    }
}
