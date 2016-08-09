package services;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import controllers.StatusCodes;
import models.Order;
import org.w3c.dom.Document;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.HashMap;

import static play.libs.F.Function;
import static play.libs.F.Promise;

/**
 * Created by jace on 1/5/15.
 */
public class TrelloHandler {


    private static String trelloUrl = "https://api.trello.com/1/cards";
    //Schedule Spreedly Payment
    public static Promise<HashMap> createCard(String name, String description){
        String url = trelloUrl;
        HashMap transaction = new HashMap();
        transaction.put("key", "213ff25f238f57ad6a4547f0e036b6ad");
        transaction.put("token", "d8f9de141bc197a64859539e31bda1aeaef3d0f74b787834bae39b1ca2e1ec28");
        transaction.put("name", name);
        transaction.put("desc", description);
        transaction.put("idList", "553c6867eac2a1b61f77cab1");

        String queryParams = "&name=name&desc=description&idList=553c6867eac2a1b61f77cab1";

        String fullUrl = url + queryParams;
        System.out.println(url);
        System.out.println(Json.toJson(transaction));
        final Promise<HashMap> resultPromise = WS.url(url).post(Json.toJson(transaction)).map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        System.out.println(response.getBody());
                        HashMap<String, String> responseMap = new HashMap<>();
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }

}
