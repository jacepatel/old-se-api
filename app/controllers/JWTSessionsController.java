package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import static io.jsonwebtoken.SignatureAlgorithm.*;

import models.Truck;
import models.User;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.JsonValidator;

import java.util.HashMap;

/**
 * Created by michaelsive on 8/02/15.
 */
@CorsComposition.Cors
public class JWTSessionsController extends Controller {
    @Transactional
    public static Result createUserSession(){
        JsonNode request = request().body().asJson();
        String email = request.get("email").asText();
        User user = User.findByEmail(email);
        if (user != null) {
            if (!JsonValidator.validate(JsonValidator.SESSION_VAL, request)) {
                HashMap<String, String> reply = new HashMap<String, String>();
                reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);

                return ok(Json.toJson(reply));
            }

            if (user != null && user.authPass(request.get("password").asText())) {
                String compact = Jwts.builder().setSubject(user.email).signWith(HS256, Application.jwtSecret).compact();
                response().setHeader("JWT-Token", compact);
                return ok(Json.toJson(user));
            }

        }

        HashMap<String, String> reply = new HashMap<String, String>();
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_LOGIN);
        return ok(Json.toJson(reply));
    }

    @Transactional
    public static Result createVendorSession(){
        JsonNode request = request().body().asJson();
        String email = request.get("email").asText();
        Truck truck = Truck.findByEmail(email);
        if (truck != null) {
            if (!JsonValidator.validate(JsonValidator.SESSION_VAL, request)) {
                HashMap<String, String> reply = new HashMap<String, String>();
                reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);

                return ok(Json.toJson(reply));
            }

            if (truck != null && truck.authPass(request.get("password").asText())) {
                if (truck.apikey == null || truck.apikey.isEmpty()){
                    truck.apikey = java.util.UUID.randomUUID().toString();
                    truck.save();
                    JPA.em().flush();
                }
                String compact = Jwts.builder().setSubject(truck.apikey).signWith(HS256, Application.jwtSecret).compact();
                response().setHeader("JWT-Token", compact);
                return ok(Json.toJson(truck));
            }

        }

        HashMap<String, String> reply = new HashMap<String, String>();
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_LOGIN);
        return ok(Json.toJson(reply));
    }
}
