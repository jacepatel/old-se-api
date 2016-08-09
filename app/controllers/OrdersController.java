package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import models.*;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by michaelsive on 19/07/2014.
 */
@CorsComposition.Cors
public class OrdersController extends Controller {

    //Create order and schedule payment with Spreedly
    @Transactional
    public static Result createMobileOrderWithSpreedly(final Long truckId) throws IOException {
        HashMap<String, String> reply = new HashMap<String, String>();
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
//            Validates the JSON for creating
            if (!JsonValidator.validate(JsonValidator.MOBILE_ORDER_VAL, request().body().asJson())) {
                if (!JsonValidator.validate(JsonValidator.ORDER_VAL, request().body().asJson())) {
                    reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
                    return ok(Json.toJson(reply));
                }
            }
            //Comment
            final User currentUser = User.findByEmail(usrEmail);
            final PaymentMethod defaultPaymentMethod = currentUser.getDefaultPaymentMethod();
            Truck currentTruck = JPA.em().find(Truck.class, truckId);
            final TruckSession currentTruckSession = currentTruck.getActiveSession();

            if (currentTruckSession != null && currentTruckSession.isActive != true && currentTruckSession.isActiveForOrders != true) {
                HashMap<String, Object> response = new HashMap<>();
                response.put(StatusCodes.ERROR, "Truck is not currently open for orders. Your payment has not been processed.");
                return ok(Json.toJson(response));
            }
            Query queueQuery = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
            queueQuery.setParameter("truckSessionId", currentTruckSession.truckSessionId);
            Long queueSize = (Long) queueQuery.getSingleResult();
            if (currentTruckSession.maximumOrders <= queueSize){
                HashMap<String, Object> response = new HashMap<>();
                response.put(StatusCodes.ERROR, "This vendor has temporarily disabled mobile orders due to a large amount of orders. Your order has not been placed.");
                return ok(Json.toJson(response));
            }
            final JsonNode items = request().body().asJson().findPath("items");
            Long deviceId = request().body().asJson().findPath("deviceId").asLong();
            BigDecimal orderTotal = Order.getTotalFromJson(items);
            Order order = new Order();
            order.truckId = truckId;
            order.truckSessionId = currentTruckSession.truckSessionId;
            order.userId = currentUser.userId;
            order.orderStatus = Order.STATUS_PENDING_PAYMENT_SCHEDULE;
            order.orderTime = new Date();
            order.paymentMethodId = defaultPaymentMethod.paymentMethodId;
            order.orderName = currentUser.firstName;
            order.mobileNumber = currentUser.mobNumber;
            order.orderType = 1;
            order.deviceId = deviceId;
            order.shortOrderId = currentTruckSession.orderCount;
            order.save();
            currentTruckSession.orderCount += 1;
            currentTruckSession.save();
            Device deviceOrderedFrom = JPA.em().find(Device.class, deviceId);
            if (deviceOrderedFrom != null) {
                deviceOrderedFrom.lastused = new Date();
            }
            JPA.em().flush();
            order.addItemsToOrder(items);
            F.Promise<HashMap> promise = SpreedlyHandler.asyncSchedulePayment(orderTotal, defaultPaymentMethod.paymentMethodToken);
            HashMap result = promise.get(64000);
            if (result.containsKey(StatusCodes.ERROR)) {
                order.orderStatus = Order.STATUS_PAYMENT_FAILED;
                return ok(Json.toJson(result));
            } else {
                String pmTokenFieldName = "paymentToken";
                String pToken = result.get(pmTokenFieldName).toString();
                order.paymentToken = pToken;
                order.orderStatus = Order.STATUS_PENDING_CONFIRMATION;
                order.save();
                HashMap hm = new HashMap();
                hm.put("order", order);
                return ok(Json.toJson(hm));
            }
        } catch (SignatureException | NullPointerException | IllegalArgumentException ne) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    //Asynchronous Route to Execute a Payment with Spreedly
    @Transactional
    public static Result confirmOrder(final Long orderId) throws Throwable {
        HashMap<String, Object> reply = new HashMap<>();
        Truck vendor = Application.currentVendor();
        Order orderToUpdate = JPA.em().find(Order.class, orderId);
        if (vendor == null || orderToUpdate == null || vendor.truckId != orderToUpdate.truckId) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        if (orderToUpdate.orderStatus != Order.STATUS_PENDING_CONFIRMATION) {
            reply.put(StatusCodes.ERROR, "Order Status does not match database. Please wait 10 seconds and try again");
            return ok(Json.toJson(reply));
        } else {
            F.Promise<HashMap> promise = SpreedlyHandler.asyncExecutePayment(orderToUpdate.orderId);
            HashMap spreedlyResponse = promise.get(64000);
            if (spreedlyResponse.containsKey(StatusCodes.ERROR)) {
                orderToUpdate.orderStatus = Order.STATUS_PAYMENT_FAILED;
                orderToUpdate.acceptedTime = new Date();
                JPA.em().flush();
            } else {
                orderToUpdate.orderStatus = Order.STATUS_CONFIRMED;
                orderToUpdate.acceptedTime = new Date();
                if (spreedlyResponse.containsKey("paymentToken")) {
                    orderToUpdate.captureToken = spreedlyResponse.get("paymentToken").toString();
                }
                JPA.em().flush();
                MailHandler.sendReceipt(orderToUpdate);
            }
            HashMap result = new HashMap();
            result.put("order", orderToUpdate);
            return ok(Json.toJson(result));
        }
    }

    //User cancels pending order
    @Transactional
    public static Result userCancelOrder(Long orderId) {
        String usrEmail;
        HashMap<String, String> reply = new HashMap<String, String>();
        try {
            usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
        } catch (SignatureException e) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        User user = User.findByEmail(usrEmail);
        Order orderToCancel = JPA.em().find(Order.class, orderId);
        if (user != null && orderToCancel != null) {
            if (orderToCancel.orderStatus != Order.STATUS_PENDING_CONFIRMATION) {
                reply.put(StatusCodes.ERROR, "The vendor has already confirmed your order.");
                return ok(Json.toJson(reply));
            }
            if (orderToCancel.userId == user.userId) {
                HashMap voidResult = SpreedlyHandler.asyncVoidPayment(orderToCancel.orderId).get(64000);
                if (voidResult.containsKey(StatusCodes.ERROR)) {
                    reply.put(StatusCodes.ERROR, "Your order could not be cancelled.");
                    return ok(Json.toJson(reply));
                } else {
                    orderToCancel.orderStatus = Order.STATUS_USER_CANCELLED;
                    orderToCancel.acceptedTime = new Date();
                    reply.put(StatusCodes.SUCCESS, "Your order has succesfully been cancelled.");
                    return ok(Json.toJson(reply));
                }
            }
        }
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
        return ok(Json.toJson(reply));
    }

    //Create an order from vendor side
    @Transactional
    public static Result createPOSOrder(Long truckSessionId) throws IOException {

        //Put the HTTP Request in a json node
        JsonNode request = request().body().asJson();

        //Validate the Json, bring this back when everyone is updated
//        if (!JsonValidator.validate(JsonValidator.POS_ORDER_VAL,request().body().asJson())){
//            HashMap<String, String> reply = new HashMap<String, String>();
//            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
//            return ok(Json.toJson(reply));
//        }

        //Response HashMap
        HashMap<String, String> response = new HashMap<String, String>();

        //Current Vendor
        Truck vendor = Application.currentVendor();

        //Check if the current session has a vendor
        if (vendor == null) {
            response.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(response));
        }

        TruckSession currentTruckSession = vendor.getActiveSession();

        if (currentTruckSession.isActive != true && currentTruckSession.isActiveForOrders != true) {
            response.put(StatusCodes.ERROR, "Truck is closed. Please log out.");
            return ok(Json.toJson(response));
        }


        //If the vendor is valid and build the order
        if (vendor != null) {
            //paymentMethodId = 1 is for cash
            Long pmId = new Long(1);
            Order newOrder = new Order();
            //Create the new order
            newOrder.truckId = request.get("truckId").asLong();
            newOrder.truckSessionId = truckSessionId;
            newOrder.userId = null;
            newOrder.orderStatus = Order.STATUS_CONFIRMED;
            newOrder.orderTime = new Date();
            newOrder.acceptedTime = new Date();
            newOrder.paymentMethodId = pmId;
            //THIS SHOULD BE LOOKED AT FURTHER, RECEIVING STRING WRAPPED IN ""
            newOrder.orderName = request.get("orderName").toString().replace("\"", "");
            newOrder.mobileNumber = request.get("mobileNumber").toString().replace("\"", "");

            newOrder.comments = request.get("comments").toString().replace("\"", "");

            newOrder.orderType = 2;
            if (!request.findPath("orderType").isMissingNode()) {
                newOrder.orderType = request.get("orderType").asInt();
            }

            //May be unnecessary, all people are up to version. Remove this in July 2015
            if (!request.findPath("discount").isMissingNode()) {
                newOrder.discount = new BigDecimal(request.get("discount").asDouble());
            } else {
                newOrder.discount = new BigDecimal(0);
            }

            newOrder.shortOrderId = currentTruckSession.orderCount;
            newOrder.save();
            currentTruckSession.orderCount += 1;
            currentTruckSession.save();
            newOrder.save();

            //Add items to the order
            JsonNode items = request().body().asJson().findPath("items");
            newOrder.addItemsToOrder(items);
            newOrder.orderTotal = newOrder.orderTotal.subtract(newOrder.discount);
            newOrder.save();


            response.put(StatusCodes.SUCCESS, "Successfully created new order. Order Id: " + newOrder.shortOrderId.toString());
            return ok(Json.toJson(response));
        }

        response.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(response));
    }

    //Updates the order status to new order
    @play.db.jpa.Transactional
    public static Result updateOrderStatus(Long orderId, Integer currentStatus) throws Throwable {
        //Setup the reply
        HashMap<String, Object> reply = new HashMap<String, Object>();

        Truck vendor = Application.currentVendor();
        Order orderToUpdate = JPA.em().find(Order.class, orderId);

        //Check if the current session has a vendor
        if (vendor == null || orderToUpdate == null || vendor.truckId != orderToUpdate.truckId) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        if (orderToUpdate.orderStatus != currentStatus) {
            reply.put(StatusCodes.ERROR, "This order has already been updated. Please wait 10 seconds and try again");
        }

        //TEST
        //Progress Order from Pending to Confirmed
        if (orderToUpdate.orderStatus == Order.STATUS_PENDING_CONFIRMATION) {
            orderToUpdate.orderStatus = Order.STATUS_CONFIRMED;
            orderToUpdate.save();
            HashMap paymentResult = PaymentHandler.getGateway().submitPayment(orderToUpdate.orderId);
            if (paymentResult.containsKey("errors")) {
                orderToUpdate.orderStatus = Order.STATUS_CANCELED;
                reply.put(StatusCodes.ERROR, paymentResult.get("errors"));
                return ok(Json.toJson(reply));
            }
            if (orderToUpdate.userId != null) {
                MailHandler.sendReceipt(orderToUpdate);
            }
            reply.put("order", orderToUpdate);
            return ok(Json.toJson(orderToUpdate));
        }

        Truck truckForName = JPA.em().find(Truck.class, vendor.truckId);
        String truckName = truckForName.name;
        String truckMessage = truckForName.SMSMessage;
        String messageToSend = "";

        if (orderToUpdate.orderType == 1) {
          messageToSend = "Your order #" + orderId + " from " + truckName + " is ready to collect! " +
            truckMessage + " Thanks again for using StreetEats.";
        } else {
          messageToSend = "Your order #" + orderId + " from " + truckName + " is ready to collect! " +
            truckMessage + " Follow us on StreetEats http://bit.ly/1QKGxe5. Receipt: http://bit.ly/1NdcHBN";
        }

        //Progress Order from Confirmed to Ready
        if (orderToUpdate.orderStatus == Order.STATUS_CONFIRMED) {
            //order now ready to collect
            orderToUpdate.orderStatus = Order.STATUS_READY_TO_COLLECT;
            orderToUpdate.readyTime = new Date();
            orderToUpdate.save();

            //tries to send a text message to say that the order is ready
            if (orderToUpdate.mobileNumber.length() > 8) {
                HashMap twilioResponse = TwilioMessaging.sendMsg(orderToUpdate.mobileNumber, messageToSend).get();
                if (twilioResponse.containsKey(StatusCodes.ERROR)){
                    orderToUpdate.mobileNumber = "";
                    orderToUpdate.save();
                }
            }

            //Checks if there is a push device for the order.
            if (orderToUpdate.deviceId != null) {
                Device orderedFromDevice = JPA.em().find(Device.class, orderToUpdate.deviceId);
                if (orderedFromDevice != null) {
                    //If there is a device associated, send a push notification to the device.
                    orderedFromDevice.lastused = new Date();
                    String title = "Order #" + orderToUpdate.orderId.toString() + " is ready to collect!";
                    final String pushMessage = "Please go and tell them your order number. Order Ref: " + orderId + ".";
                    String type = "orderready";
                    HashMap extra = new HashMap();
                    extra.put("orderId", orderToUpdate.orderId);
                    extra.put("type", type);
                    PushWooshNotifications pwn = PushWooshNotifications.getInstance();
                    HashMap pw = pwn.sendNotification(orderedFromDevice.deviceToken, orderedFromDevice.deviceType, title, pushMessage, extra).get();
                    PushNotification pn = new PushNotification();
                    if (pw.containsKey(StatusCodes.ERROR)){
                        pn.errorMessage = pw.get(StatusCodes.ERROR).toString();
                    }
                    else if (pw.containsKey("messagetoken")) {
                        pn.pushWooshToken = pw.get("messagetoken").toString();
                    }
                    pn.orderId = orderId;
                    pn.sentTime = new Date();
                    pn.save();
                    reply.put("order", orderToUpdate);
                    return ok(Json.toJson(reply));
                }
            }


            reply.put("order", orderToUpdate);
            return ok(Json.toJson(reply));
        }


        if (orderToUpdate.orderStatus == Order.STATUS_READY_TO_COLLECT) {
            //order now collected
            orderToUpdate.orderStatus = Order.STATUS_COMPLETED;
            orderToUpdate.collectTime = new Date();
            orderToUpdate.save();
            reply.put("order", orderToUpdate);
            return ok(Json.toJson(reply));
        }

        //Note to self, build a library of return strings maybe
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(reply));
    }


    @play.db.jpa.Transactional
    public static Result cancelOrder(final Long orderId, Integer currentStatus) throws Throwable {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();
        final Order orderToUpdate = JPA.em().find(Order.class, orderId);
        if (orderToUpdate == null) {
            //Note to self, build a library of return strings maybe
            reply.put(StatusCodes.ERROR, "Order not found. Something went wrong");
            return ok(Json.toJson(reply));
        }
        Truck vendor = Application.currentVendor();

        //Check if the current session has a vendor
        if (vendor.truckId != orderToUpdate.truckId) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        if (orderToUpdate.orderStatus != currentStatus) {
            reply.put(StatusCodes.ERROR, "Order Status does not match database. Please wait 10 seconds and try again");
            return ok(Json.toJson(reply));
        }
        Truck truckForName = JPA.em().find(Truck.class, vendor.truckId);
        String truckName = truckForName.name;
        final String userMob = orderToUpdate.mobileNumber;

        if (orderToUpdate.orderStatus == Order.STATUS_PENDING_CONFIRMATION) {
            //Void the payment
            SpreedlyHandler.asyncVoidPayment(orderId);
            orderToUpdate.orderStatus = Order.STATUS_CANCELED;
            orderToUpdate.acceptedTime = new Date();
            orderToUpdate.save();
            JPA.em().flush();

            //Send text message
            //Check here
            final String txtMessage = "Hi " + orderToUpdate.orderName + ", unfortunately your order from " + truckName + " can't be processed. " +
                    "Please contact the vendor to find out why. You have not been charged. Order Ref: " + orderToUpdate.orderId + ".";
            TwilioMessaging.sendMsg(userMob, txtMessage);
            if (orderToUpdate.deviceId != null) {
                Device orderedFromDevice = JPA.em().find(Device.class, orderToUpdate.deviceId);
                if (orderedFromDevice != null) {
                    //If there is a device associated, send a push notification to the device.
                    orderedFromDevice.lastused = new Date();
                    String title = "Order #" + orderToUpdate.orderId.toString() + " has not been approved.";
                    final String pushMessage = "Please contact " + truckName + " to find out why. You have not been charged.";
                    String type = "orderready";
                    HashMap extra = new HashMap();
                    extra.put("orderId", orderToUpdate.orderId);
                    extra.put("type", type);
                    PushWooshNotifications pwn = PushWooshNotifications.getInstance();
                    HashMap pw = pwn.sendNotification(orderedFromDevice.deviceToken, orderedFromDevice.deviceType, title, pushMessage, extra).get();
                    PushNotification pn = new PushNotification();
                    if (pw.containsKey(StatusCodes.ERROR)){
                        pn.errorMessage = pw.get(StatusCodes.ERROR).toString();
                    }
                    else if (pw.containsKey("messagetoken")) {
                        pn.pushWooshToken = pw.get("messagetoken").toString();
                    }
                    pn.orderId = orderId;
                    pn.sentTime = new Date();
                    pn.save();
                    return ok(Json.toJson(orderToUpdate));
                }
            }
            return ok(Json.toJson(orderToUpdate));
        } else if (orderToUpdate.orderStatus == Order.STATUS_CONFIRMED) {
            orderToUpdate.orderStatus = Order.STATUS_CANCELED;
            orderToUpdate.readyTime = new Date();
            orderToUpdate.save();
            JPA.em().flush();
            return ok(Json.toJson(orderToUpdate));
        }
        else {
            reply.put(StatusCodes.ERROR, "Order Status does not match database. Please wait 10 seconds and try again");
        }
        return ok(Json.toJson(reply));
    }


}
