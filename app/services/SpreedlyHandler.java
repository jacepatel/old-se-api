package services;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import controllers.StatusCodes;
import models.Order;
import models.PaymentMethod;
import models.User;
import org.w3c.dom.Document;
import play.api.mvc.Results;
import play.db.jpa.JPA;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Result;

import static play.libs.F.Function;
import static play.libs.F.Promise;
import static play.mvc.Results.ok;

import java.beans.XMLDecoder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 2/03/15.
 */
public class SpreedlyHandler {

    private static String spreedlyUrlDomain = "https://core.spreedly.com/v1/";
    private static String spreedlyGatewayToken = System.getenv("SPREEDLY_GATEWAY_KEY");
    //Schedule Spreedly Payment
    public static Promise<HashMap> asyncSchedulePayment(BigDecimal price, String pmToken){
        String url = spreedlyUrlDomain + "gateways/" + spreedlyGatewayToken + "/authorize.xml";
        HashMap requestData = new HashMap<>();
        HashMap transaction = new HashMap();
        BigDecimal amount = price.multiply(new BigDecimal(100));
        transaction.put("payment_method_token", pmToken);
        transaction.put("amount", amount.intValue());
        transaction.put("currency_code", "AUD");
        requestData.put("transaction", transaction);
        final Promise<HashMap> resultPromise = WS.url(url).setAuth(Application.SPREEDLY_EnvKey, Application.SPREEDLY_AppSecret).post(Json.toJson(requestData)).map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        Document xmlDoc = response.asXml();
                        HashMap<String, String> responseMap = new HashMap<>();
                        boolean succeeded = xmlDoc.getElementsByTagName("succeeded").item(0).getTextContent().contains("true");
                        if (!succeeded) {
                            responseMap.put(StatusCodes.ERROR, "Payment failed. Please check your credit card.");
                        } else {
                            responseMap.put("paymentToken", xmlDoc.getElementsByTagName("token").item(0).getTextContent());
                        }
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }

    public static Promise<HashMap> asyncExecutePayment(Long orderId){
        Order order = JPA.em().find(Order.class, orderId);
        String url = spreedlyUrlDomain + "transactions/" + order.paymentToken + "/capture.xml";
        HashMap requestData = new HashMap<>();
        HashMap transaction = new HashMap<>();
        transaction.put("transaction_token", order.paymentToken);
        requestData.put("transaction", transaction);
        final Promise<HashMap> resultPromise = WS.url(url).setAuth(Application.SPREEDLY_EnvKey, Application.SPREEDLY_AppSecret).post(Json.toJson(requestData)).map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        Document xmlDoc = response.asXml();
                        HashMap<String, Object> responseMap = new HashMap<>();
                        boolean succeeded = xmlDoc.getElementsByTagName("succeeded").item(0).getTextContent().contains("true");
                        if (!succeeded) {
                            responseMap.put(StatusCodes.ERROR, "Payment failed. Please check your credit card.");
                        } else {
                            responseMap.put("paymentToken", xmlDoc.getElementsByTagName("token").item(0).getTextContent());
                        }
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }

    public static Promise<HashMap> asyncVoidPayment(Long orderId){
        Order order = JPA.em().find(Order.class, orderId);
        String url = spreedlyUrlDomain + "transactions/" + order.paymentToken + "/void.xml";
        final Promise<HashMap> resultPromise = WS.url(url).setAuth(Application.SPREEDLY_EnvKey, Application.SPREEDLY_AppSecret).post("").map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        Document xmlDoc = response.asXml();
                        HashMap<String, Object> responseMap = new HashMap<>();
                        boolean succeeded = xmlDoc.getElementsByTagName("succeeded").item(0).getTextContent().contains("true");
                        if (!succeeded) {
                            responseMap.put(StatusCodes.ERROR, "Could not void transaction. Please check the transaction token.");
                        } else {
                            responseMap.put(StatusCodes.SUCCESS, "Payment has been voided.");
                        }
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }

    //This should be executed after client sends a payment method token through
    //Used to verify and vault the card with Spreedly and save token in our DB
    public static Promise<HashMap> asyncVerifyAndRetainPaymentMethod(String pmToken){
        String url = spreedlyUrlDomain + "gateways/" + spreedlyGatewayToken + "/verify.xml";
        HashMap requestData = new HashMap<>();
        HashMap transaction = new HashMap();
        transaction.put("payment_method_token", pmToken);
        transaction.put("retain_on_success", true);
        transaction.put("currency_code", "AUD");
        requestData.put("transaction", transaction);
        final Promise<HashMap> resultPromise = WS.url(url).setAuth(Application.SPREEDLY_EnvKey, Application.SPREEDLY_AppSecret).post(Json.toJson(requestData)).map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        Document xmlDoc = response.asXml();
                        HashMap<String, Object> responseMap = new HashMap<>();
                        boolean succeeded = xmlDoc.getElementsByTagName("succeeded").item(0).getTextContent().contains("true");
                        if (!succeeded) {
                            responseMap.put(StatusCodes.ERROR, xmlDoc.getElementsByTagName("message").item(0).getTextContent());
                        } else {
                            String lastFour = xmlDoc.getElementsByTagName("last_four_digits").item(0).getTextContent();
                            String cardType = xmlDoc.getElementsByTagName("card_type").item(0).getTextContent();
                            String pmToken = xmlDoc.getElementsByTagName("token").item(1).getTextContent();
                            responseMap.put("cardType", cardType);
                            responseMap.put("lastFour", lastFour);
                            responseMap.put("paymentMethodToken", pmToken);
                        }
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }

    public static Promise<HashMap> asyncDeletePaymentMethod(String pmToken){
        String url = spreedlyUrlDomain + "payment_methods/" + pmToken + "/redact.xml";
        final Promise<HashMap> resultPromise = WS.url(url).setAuth(Application.SPREEDLY_EnvKey, Application.SPREEDLY_AppSecret).put("").map(
                new Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        Document xmlDoc = response.asXml();
                        HashMap<String, Object> responseMap = new HashMap<>();
                        boolean succeeded = xmlDoc.getElementsByTagName("succeeded").item(0).getTextContent().contains("true");
                        if (!succeeded) {
                            responseMap.put(StatusCodes.ERROR, xmlDoc.getElementsByTagName("message").item(0).getTextContent());
                        } else {
                            responseMap.put(StatusCodes.SUCCESS, "Payment method deleted from Spreedly.");
                        }
                        return responseMap;
                    }
                }
        );
        return resultPromise;
    }
}
