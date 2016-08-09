package controllers;

import actions.CorsComposition;
import models.Order;
import models.Truck;
import models.TruckSession;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.OrdersReport;
import services.Report;
import services.SalesData;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 26/07/15.
 */
@CorsComposition.Cors
public class ReportsController extends Controller {
    @Transactional
    public static Result getSalesData(Long truckId, Long from, Long to){
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token").toString()) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        if (truck == null){
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            Date fromDate = new Date(from);
            Date toDate = new Date(to);
            SalesData sd = new SalesData(truck.truckId, fromDate, toDate);
            return ok(Json.toJson(sd.compiledData()));
        }
    }

    @Transactional
    public static Result downloadReport(Long truckSessionId) throws IOException {
        TruckSession ts = JPA.em().find(TruckSession.class, truckSessionId);
        Truck truck = ts.truck;
        if (!truck.validateToken(request().getHeader("jwt-token").toString()) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        if (truck == null || truck.truckId != ts.truckId){
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            Report report = new Report(ts);
            return ok(new File(report.generateFile()));
        }
    }

    @Transactional
    public static Result downloadOrdersReport(Long truckId, Long from, Long to) throws IOException {
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token").toString()) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        if (truck == null){
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            Date fromDate = new Date(from);
            Date toDate = new Date(to);
            OrdersReport or = new OrdersReport(truck.truckId, fromDate, toDate);
            return ok(new File(or.generateFile()));
        }
    }
}