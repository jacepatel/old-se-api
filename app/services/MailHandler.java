package services;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import controllers.StatusCodes;
import models.ItemOption;
import models.Order;
import models.OrderItem;
import models.OrderItemOption;
import org.apache.commons.mail.*;
import org.w3c.dom.Document;
import play.db.jpa.JPA;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by michaelsive on 27/02/15.
 */
public class MailHandler {

    final private static String mandrillEndpoint = "https://mandrillapp.com/api/1.0/";

    private static String getMessageBody(Order order){
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy  HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
        return "RECEIPT\n" +
                "\n" +
                "-------------------------------------------------------------\n" +
                order.truck.name + "\n" +
                "\n" +
                "Date/Time:  " + df.format(order.orderTime) + "\n" +
                "\n" +
                "To:  " + order.user.firstName + " " + order.user.lastName + "\n" +
                "\n" +
                "-------------------------------------------------------------\n" +
                "ITEM                                                    PRICE\n" +
                prepareOrderSummary(order) +
                "\n" +
                "\n" +
                "-------------------------------------------------------------\n" +
                "TOTAL PRICE INCLUDING GST           $" + order.orderTotal +"\n" +
                "-------------------------------------------------------------\n" +
                "\n" +
                "POWERED BY StreetEatsApp.co";
    }

    private static String getMessageBodyNoUser(Order order){
      DateFormat df = new SimpleDateFormat("dd/MM/yyyy  HH:mm");
      df.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
      return "RECEIPT\n" +
        "\n" +
        "-------------------------------------------------------------\n" +
        order.truck.name + "\n" +
        "\n" +
        "Date/Time:  " + df.format(order.orderTime) + "\n" +
        "\n" +
        "\n" +
        "-------------------------------------------------------------\n" +
        "ITEM                                                    PRICE\n" +
        prepareOrderSummary(order) +
        "\n" +
        "\n" +
        "-------------------------------------------------------------\n" +
        "TOTAL PRICE INCLUDING GST           $" + order.orderTotal +"\n" +
        "-------------------------------------------------------------\n" +
        "\n" +
        "";
    }

    private static String getFormattedItemLine(OrderItem orderItem){
        StringBuilder sb = new StringBuilder();
        String itemName =  orderItem.item.name;
        String itemQty = orderItem.quantity.toString();
        String itemPrice = "$" + orderItem.totalPaid.toString();
        String itemNameAndQty = itemName + " x" + itemQty;
        sb.append(String.format("%-56s%s", itemNameAndQty, itemPrice));
        return sb.toString();
    }

    public static void sendReceipt(Order order){
        try {
            String subject = "Receipt for Order #" + order.orderId + " from " + order.truck.name;
            Email email = prepareEmail(order.user.email, subject, getMessageBody(order));
            email.send();
        }
        catch (Exception e) {
            System.out.println("Mailer Error (Receipt): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendReceiptOnRequest(Order order, String emailAddress) {
      try {
        String subject = "Receipt for Order #" + order.orderId + " from " + order.truck.name;
        Email email = prepareEmail(emailAddress, subject, getMessageBodyNoUser(order));
        email.send();
      }
      catch (Exception e) {
        System.out.println("Mailer Error (Receipt): " + e.getMessage());
        e.printStackTrace();
      }
    }

    private static Email prepareEmail(String toEmail, String subject, String message) throws EmailException{
        Email email = new SimpleEmail();
        email.setHostName("smtp.mandrillapp.com");
        email.setSmtpPort(587);
        email.setSSLOnConnect(true);
        email.setAuthentication("app28546938@heroku.com", "JOQj_PJVpPQqNV2uja0dHQ");
        email.setFrom("receipts@streeteatsapp.co");
        email.setSubject(subject);
        email.setMsg(message);
        email.addTo(toEmail);
        return email;
    }

    private static String prepareOrderSummary(Order order){
        StringBuilder sb = new StringBuilder();
        for (OrderItem i : order.orderItems){
            sb.append(getFormattedItemLine(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Boolean subscribeToMailChimp(String email, String listId){
        HashMap params = new HashMap<>();
        params.put("apikey", "ec60888b6e6391057b27056f33781d58-us9");
        params.put("id", listId);
        HashMap emailHash = new HashMap();
        emailHash.put("email", email);
        params.put("email", emailHash);
        String url = "https://us9.api.mailchimp.com/2.0/lists/subscribe.json";
        final F.Promise<Boolean> resultPromise = WS.url(url).post(Json.toJson(params)).map(
                new F.Function<WS.Response, Boolean>() {
                    public Boolean apply(WS.Response response) {
                       return response.asJson().has("status");
                    }
                }
        );
        return resultPromise.get();
    }

    public static void sendEnquiryToSales(String email, String name, String mobileNumber){
        String message = "Hey StreetEats,\n"
                +        "You've got a new enquiry from the vendor web site! Here are the details:\n"
                +        "Name: " + name + "\n"
                +        "Email: " + email + "\n"
                +        "Phone: " + mobileNumber + "\n"
                +        "Close that shit yo!";
        try {
            Email enquiry = prepareEmail("chris@streeteatsapp.co", "Vendor Website Enquiry", message);
            enquiry.send();
        }
        catch (EmailException e){
            System.out.println("Mailer Error (Web Enquiry): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendEmailWithAttachment(String toEmail, String subject, String message, String attachmentPath, String attachmentName) throws EmailException {
        // Create the attachment
        EmailAttachment attachment = new EmailAttachment();
        attachment.setPath(attachmentPath);
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        attachment.setDescription("Sales Report");
        attachment.setName(attachmentName);

        List<String> toList = Arrays.asList(toEmail.split(","));

        // Create the email message
        MultiPartEmail email = new MultiPartEmail();
        email.setHostName("smtp.mandrillapp.com");
        email.setSmtpPort(587);
        email.setSSLOnConnect(true);
        email.setAuthentication("app28546938@heroku.com", "JOQj_PJVpPQqNV2uja0dHQ");
        email.setFrom("reports@streeteatsapp.co");
        email.setSubject(subject);
        email.setMsg(message);

        for (String sendAddress : toList) {
          email.addTo(sendAddress);
        }

        // add the attachment
        email.attach(attachment);

        // send the email
        email.send();
    }

    public static F.Promise<HashMap> sendMailChimpTemplateWithMandrill(String templateName, String toEmail){
        String url = mandrillEndpoint + "messages/send-template.json";
        String key = System.getenv("MANDRILL_APIKEY");
        HashMap params = new HashMap();
        HashMap message = new HashMap();
        HashMap toEmailObj = new HashMap();
        toEmailObj.put("email", toEmail);
        toEmailObj.put("type", "to");
        HashMap toEmails[] = {toEmailObj};
        params.put("key", key);
        params.put("template_name", templateName);
        HashMap templateContent = new HashMap();
        templateContent.put("name", templateName);
        templateContent.put("content", "test");
        HashMap templateContentArray[] = {templateContent};
        params.put("template_content", templateContentArray);
        message.put("to", toEmails);
        params.put("message", message);

        F.Promise<HashMap> mandrillResponse = WS.url(url).setContentType("application/json").post(Json.toJson(params)).map(
                new F.Function<WS.Response, HashMap>() {
                    public HashMap apply(WS.Response response) {
                        JsonNode json = response.asJson();
                        HashMap sendMsgResult = new HashMap();
                        System.out.println("Response: " + response.getBody());
                        String status = json.get(0).get("status").asText();
                        if (status.equals("sent")) {
                            sendMsgResult.put(StatusCodes.SUCCESS, "Mandrill message sent.");

                        } else {
                            sendMsgResult.put(StatusCodes.ERROR, json.get("message").asText());
                        }
                        return sendMsgResult;
                    }
                }
        );
        return mandrillResponse;
    }
}
