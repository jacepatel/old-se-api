package services;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.*;
import com.twilio.sdk.resource.instance.*;
import com.twilio.sdk.resource.list.*;
import controllers.StatusCodes;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;


/**
 * Created by jacepatel on 13/12/2014.
 */
public class TwilioMessaging {
    // Find your Account Sid and Token at twilio.com/user/account
    private static final String ACCOUNT_SID = "AC74d20a428cfb2f9f79a166b64149875b";
    private static final String AUTH_TOKEN = "0127a2261486bcf6f265be74bffff9b2";
    private static final String TWILIO_URL = "https://api.twilio.com/2010-04-01";

    //public static void main(String[]args) throws TwilioRestException {
    public static String sendMessage (String toNumber, String messageTxt) throws TwilioRestException{
    TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);

        // Build the parameters
        //THIS NEEDS SOME SETUP AROUND NUMBER VALIDATIOn
        if (toNumber.substring(0, 1) == "0") {
            toNumber = "+61" + toNumber.substring(1);
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("To", toNumber));
        params.add(new BasicNameValuePair("From", "+16514336317"));
        params.add(new BasicNameValuePair("Body", messageTxt));

        MessageFactory messageFactory = client.getAccount().getMessageFactory();
        Message message = messageFactory.create(params);
        String error = message.getErrorMessage();
        return error;
    }

    public static F.Promise<HashMap> sendMsg (String toNumber, String messageTxt) {
        String auth = System.getenv("TWILIO_AUTH_TOKEN");
        String SENDMSG_URL = TWILIO_URL + "/Accounts/" + ACCOUNT_SID + "/Messages.json";
        LinkedHashMap params = new LinkedHashMap();
        params.put("From", "+16514336317");
        params.put("To", toNumber);
        params.put("Body", messageTxt);

        System.out.println("PRE SEND");

        String queryParams = "From=+61409568791&To=" + URLEncoder.encode(toNumber) + "&Body=" + URLEncoder.encode(messageTxt);
        F.Promise<HashMap> twilioResponse = WS.url(SENDMSG_URL).setAuth(ACCOUNT_SID, auth).setContentType("application/x-www-form-urlencoded").post(queryParams).map(
                new F.Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        JsonNode json = response.asJson();
                        HashMap sendMsgResult = new HashMap();
                        String status = json.get("status").asText();
                        if (status.equals("failed")) {
                            sendMsgResult.put(StatusCodes.ERROR, json.get("error_message").asText());
                        } else {
                            sendMsgResult.put(StatusCodes.SUCCESS, "Twilio message sent.");
                        }
                        return sendMsgResult;
                    }
                }
        );
        return twilioResponse;
    }
}