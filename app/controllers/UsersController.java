package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import com.twilio.sdk.TwilioRestException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import models.*;
import org.mindrot.jbcrypt.BCrypt;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import javax.persistence.Query;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

/**
 * Created by michaelsive on 16/07/2014.
 */
@CorsComposition.Cors
public class UsersController extends Controller {

    @play.db.jpa.Transactional
    public static Result createUser() {
        JsonNode json = request().body().asJson();
        HashMap<String, Object> reply = new HashMap<String, Object>();
        if (!JsonValidator.validate(JsonValidator.USER_VAL, json)){
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        if (json.get("email").asText().equals("")){
            reply.put(StatusCodes.ERROR, "Please provide an email address.");
            return ok(Json.toJson(reply));
        }
        if (json.get("mobileNumber").asText().equals("+61")){
            reply.put(StatusCodes.ERROR, "Please provide a mobile number.");
            return ok(Json.toJson(reply));
        }
        if (json.get("password").asText().equals("")){
            reply.put(StatusCodes.ERROR, "Please provide a password.");
            return ok(Json.toJson(reply));
        }
        if (json.get("firstName").asText().equals("")){
            reply.put(StatusCodes.ERROR, "Please provide your first name.");
            return ok(Json.toJson(reply));
        }
        if (json.get("lastName").asText().equals("")){
            reply.put(StatusCodes.ERROR, "Please provide your last name.");
            return ok(Json.toJson(reply));
        }
        if(!Application.checkEmailUniqueness(json.get("email").asText())) {
            reply.put(StatusCodes.ERROR, "Email already in use.");
            return ok(Json.toJson(reply));
        }
        if (!Application.checkMobNumberUniqueness(json.get("mobileNumber").asText())){
            reply.put(StatusCodes.ERROR, "Phone number already in use.");
            return ok(Json.toJson(reply));
        }

            User newUser = new User();
            newUser.updateDetails(json);
            newUser.createddate = new Date();
            newUser.save();
            reply.put("user", Json.toJson(newUser));
            MailHandler.sendMailChimpTemplateWithMandrill("User Signup", json.get("email").asText());
            String compact = Jwts.builder().setSubject(newUser.email).signWith(HS256, Application.jwtSecret).compact();
            response().setHeader("JWT-Token", compact);

            return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result addPaymentMethod(){
        JsonNode request = request().body().asJson();
        HashMap<String, String> reply = new HashMap<String, String>();
        if (!JsonValidator.validate(JsonValidator.PAYMENT_METHOD_VAL, request)) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        String usrEmail;
        User user;
        try {
            usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            user = User.findByEmail(usrEmail);
        }
        catch(SignatureException | NullPointerException e){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        String pmToken = request.findPath("paymentMethodToken").asText();
        HashMap addPmResult = SpreedlyHandler.asyncVerifyAndRetainPaymentMethod(pmToken).get(64000);
        if (addPmResult.containsKey(StatusCodes.ERROR)){
            reply.put(StatusCodes.ERROR, addPmResult.get(StatusCodes.ERROR).toString());
            return ok(Json.toJson(reply));
        }
        else {
            String token = addPmResult.get("paymentMethodToken").toString();
            String lastFour = addPmResult.get("lastFour").toString();
            String cardType = addPmResult.get("cardType").toString();
            HashMap newPaymentMethod = user.addSpreedlyPaymentMethod(token, lastFour, cardType);
            return ok(Json.toJson(newPaymentMethod));
        }
    }

    @Transactional
    public static Result deletePaymentMethod(String token){
        HashMap<String, String> reply = new HashMap<String, String>();
        String usrEmail;
        User user;
        try {
            usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            user = User.findByEmail(usrEmail);
        }
        catch(SignatureException | NullPointerException e){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        PaymentMethod pm = PaymentMethod.findByToken(token);
        if (pm != null && pm.userId == user.userId){
            pm.isDeleted = true;
            pm.defaultMethod = false;
            JPA.em().flush();
            List<PaymentMethod> userPMs = user.paymentMethods;
            if (userPMs.size() > 0) {
                for (PaymentMethod p : userPMs) {
                    if (p.paymentMethodId != pm.paymentMethodId) {
                        p.defaultMethod = true;
                        JPA.em().flush();
                        break;
                    }
                }
            }
            SpreedlyHandler.asyncDeletePaymentMethod(token).get(64000);
        }
        else{
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        reply.put(StatusCodes.SUCCESS, "Payment method deleted.");
        return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result makeDefaultPaymentMethod(Long paymentMethodId){
        HashMap<String, String> reply = new HashMap<String, String>();
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
//          User user = Application.currentUserBySession(userSessionToken);
            User user = User.findByEmail(usrEmail);
            PaymentMethod pm = JPA.em().find(PaymentMethod.class, paymentMethodId);
            if (pm.userId == user.userId) {
                user.makeDefaultPaymentMethod(paymentMethodId);
                return ok();
            }
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        catch (SignatureException | NullPointerException ne){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result getOrders(){
        HashMap<String, String> reply = new HashMap<String, String>();
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
//          User user = Application.currentUserBySession(userSessionToken);
            User user = User.findByEmail(usrEmail);
            List<Order> orders = user.currentOrders;
            for (Order o : orders) {
                Query place = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderTime < :orderTime AND e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
                place.setParameter("orderTime", o.orderTime);
                place.setParameter("truckSessionId", o.truckSessionId);
                o.placeInQueue = (Long) place.getSingleResult();
            }
            return ok(Json.toJson(orders));
        }
        catch (SignatureException | NullPointerException ne){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }


    //THIS IS FOR OLDER VERSIONS OF THE APP
    @Transactional
    public static Result getPastOrders(){
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
//          User user = Application.currentUserBySession(userSessionToken);
            User user = User.findByEmail(usrEmail);
            List<Order> orders = user.pastOrders;
            return ok(Json.toJson(orders));
        }
        catch (SignatureException | NullPointerException ne){
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result getPastAndCancelledOrders(){
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
//          User user = Application.currentUserBySession(userSessionToken);
            User user = User.findByEmail(usrEmail);
            List<Order> orders = user.pastOrders;
            orders.addAll(user.cancelledOrders);
            return ok(Json.toJson(orders));
        }
        catch (SignatureException | NullPointerException ne){
            HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result updateAccountDetails() {
        HashMap<String, String> reply = new HashMap<String, String>();
        JsonNode json = request().body().asJson();
        if (!JsonValidator.validate(JsonValidator.USER_UPDATE_VAL, json)){
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            User user = User.findByEmail(usrEmail);
            String password = json.findPath("password").asText();
            if (!user.authPass(password)) {
                reply.put(StatusCodes.ERROR, "Incorrect password provided.");
                return ok(Json.toJson(reply));
            }
            if (!user.mobNumber.equals(json.get("mobileNumber").asText())) {
                if (!Application.checkMobNumberUniqueness(json.get("mobileNumber").asText())) {

                    reply.put(StatusCodes.ERROR, "Phone number already in use.");
                    return ok(Json.toJson(reply));
                }
            }
            user.firstName = json.findPath("firstName").asText();
            user.lastName = json.findPath("lastName").asText();
            user.mobNumber = json.findPath("mobileNumber").asText();
            if (json.has("locality")) {
                user.locality = json.findPath("locality").asText();
            }
            user.save();
            return ok(Json.toJson(user));
        } catch (SignatureException | NullPointerException ne) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result getUserUpdate(){
        HashMap<String, Object> reply = new HashMap<>();
        try {
            String usrEmail = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(request().getHeader("JWT-Token")).getBody().getSubject();
            User user = User.findByEmail(usrEmail);
            reply.put("user", user);
            return ok(Json.toJson(reply));
        }
        catch (SignatureException | NullPointerException | IllegalArgumentException ne) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
    }

    @Transactional
    public static Result getPasswordResetToken(){
        HashMap<String, Object> reply = new HashMap<>();
        JsonNode json = request().body().asJson();
        if (!JsonValidator.validate(JsonValidator.PASSWORD_RESET_REQUEST_VAL, json)){
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        String mobileNumber = json.findPath("mobileNumber").asText();
        User user = User.findByMobNum(mobileNumber);
        if (user != null){
            user.nullifyResetTokens();
            SecureRandom random = new SecureRandom();
            String token = new BigInteger(130, random).toString(32);
            PasswordReset pwr = new PasswordReset();
            pwr.userId = user.userId;
            pwr.resetToken = token.substring(0, 6);
            pwr.isUsed = false;
            pwr.save();
            String messageToSend = "Hi " + user.firstName + ". Your password reset token for " + user.email + " is " + pwr.resetToken + ".";
            TwilioMessaging.sendMsg(user.mobNumber, messageToSend);
            reply.put(StatusCodes.SUCCESS, "A text message has been sent to the provided number with instructions to reset your password.");
            return ok(Json.toJson(reply));
        }
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_INVALID_MOBILE);
        return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result sendReceiptOnRequest(Long orderId, String emailAddress) {

      HashMap<String, Object> reply = new HashMap<>();

      try {
        Order receiptOrder = JPA.em().find(Order.class, orderId);

        if (receiptOrder == null) {
          //Note to self, build a library of return strings maybe
          reply.put(StatusCodes.ERROR, "Supplied OrderID was not found. Please check your SMS and confirm the OrderID");
          return ok(Json.toJson(reply));
        }

        MailHandler.sendReceiptOnRequest(receiptOrder, emailAddress);
        reply.put(StatusCodes.SUCCESS, "Your receipt has been sent to the requested email address.");
      }
      catch (Exception ex) {
        reply.put(StatusCodes.ERROR, "Supplied OrderID was not found. Please check your SMS and confirm the OrderID");
      }

      return ok(Json.toJson(reply));
    }


    @Transactional
    public static Result resetPassword(String passwordResetToken){
        HashMap<String, Object> reply = new HashMap<>();
        JsonNode json = request().body().asJson();
        if (!JsonValidator.validate(JsonValidator.PASSWORD_RESET_VAL, json)){
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_BAD_JSON);
            return ok(Json.toJson(reply));
        }
        PasswordReset pwr = PasswordReset.findByToken(passwordResetToken);
        if (pwr == null || pwr.isUsed){
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_INVALID_TOKEN);
            return ok(Json.toJson(reply));
        }
        User user = JPA.em().find(User.class, pwr.userId);
        String newPass = json.findPath("newPassword").asText();
        user.passwordHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
        user.save();
        pwr.isUsed = true;
        pwr.save();
        String compact = Jwts.builder().setSubject(user.email).signWith(HS256, Application.jwtSecret).compact();
        response().setHeader("JWT-Token", compact);
        return ok(Json.toJson(user));
    }

    @Transactional
    public static Result getMapData(){
        return ok(Json.toJson(Truck.getMapData()));
    }
}
