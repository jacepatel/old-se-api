package services;

import com.braintreegateway.*;
import com.braintreegateway.Environment;
import com.braintreegateway.exceptions.NotFoundException;
import controllers.StatusCodes;
import models.Order;
import models.User;
import play.db.jpa.JPA;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by michaelsive on 29/12/14.
 */
public class PaymentHandler {

    //Singleton instance
    private static PaymentHandler instance = null;

    // Braintree Authorisation Details
    private static final String MERCHANT_ID = System.getenv("BRAINTREE_MERCHANT_ID");
    private static final String PUBLIC_KEY = System.getenv("BRAINTREE_PUBLIC_KEY");
    private static final String PRIVATE_KEY = System.getenv("BRAINTREE_PRIVATE_KEY");
    private static final String ENVIRONMENT = System.getenv("BRAINTREE_ENVIRONMENT");
    private BraintreeGateway gateway;


    private PaymentHandler(){

        Environment e;
        if (ENVIRONMENT.equals("SANDBOX")) {
            e = Environment.SANDBOX;
        }
        else {
            e = Environment.PRODUCTION;
        }

        gateway = new BraintreeGateway(
                e,
                MERCHANT_ID,
                PUBLIC_KEY,
                PRIVATE_KEY
        );
    }


    //Returns singleton instance of gateway
    public static PaymentHandler getGateway() {
        if(instance == null) {
            instance = new PaymentHandler();
        }
        return instance;
    }

    //Creates a new customer on BrainTree server
    public HashMap createCustomer(String customerRef){
        User user = JPA.em().find(User.class, Long.parseLong(customerRef));
        CustomerRequest request = new CustomerRequest()
                .id(customerRef)
                .firstName(user.firstName)
                .lastName(user.lastName);
        Result<Customer> result = gateway.customer().create(request);
        HashMap<String, Object> serviceResponse = new HashMap<String, Object>();
        if (result.isSuccess()) {
            serviceResponse.put("customerRecord", result.getTarget());
        }
        else {
            serviceResponse.put("errors", result.getErrors());
        }
        return serviceResponse;
    }

    //Add new payment method to a customer (specify desired token)
    public HashMap createPaymentMethod(String customerRef, String paymentMethodNonce){
        try {
            gateway.customer().find(customerRef);
        }
        catch (NotFoundException E){
            createCustomer(customerRef);
        }
        PaymentMethodRequest request = new PaymentMethodRequest()
                .customerId(customerRef)
        .paymentMethodNonce(paymentMethodNonce).options().failOnDuplicatePaymentMethod(true).done();

        Result<? extends PaymentMethod> result = gateway.paymentMethod().create(request);
        HashMap<String, Object> serviceResponse = new HashMap<String, Object>();

        if (result.isSuccess()) {
            try {
                CreditCard card = gateway.creditCard().find(result.getTarget().getToken());
                String type = card.getCardType();
                String lastFour = card.getLast4();
                serviceResponse.put("paymentMethodToken", result.getTarget().getToken());
                serviceResponse.put("lastFour", lastFour);
                serviceResponse.put("cardType", type);
            }
            catch (NotFoundException e){
                System.out.println("Not found. Searching PayPal");
                PayPalAccount pm = gateway.paypalAccount().find(result.getTarget().getToken());
                serviceResponse.put("paymentMethodToken", result.getTarget().getToken());
                serviceResponse.put("lastFour", pm.getEmail());
                serviceResponse.put("cardType", "PayPal");
            }
        }
        else {
            serviceResponse.put("errors", result.getErrors().getAllDeepValidationErrors().get(0).getMessage());
            serviceResponse.put("message", result.getMessage());
        }

        return serviceResponse;
    }

    //Schedules a new payment (no funds actually transferred)
    public HashMap schedulePayment(Long orderId, BigDecimal price, String pmToken){
        TransactionRequest request = new TransactionRequest()
                .orderId(orderId.toString())
                .paymentMethodToken(pmToken)
                .amount(price);
        Result<Transaction> result = gateway.transaction().sale(request);
        HashMap<String, Object> serviceResponse = new HashMap<String, Object>();

        if (result.isSuccess()) {
            serviceResponse.put("scheduledPayment", result.getTarget());
            Order order = JPA.em().find(Order.class, orderId);
            order.paymentToken = result.getTarget().getId();
            order.save();
        }
        else {
            serviceResponse.put("errors", result.getErrors().getAllDeepValidationErrors().get(0).getMessage());
        }

        return serviceResponse;
    }

    //Actuates a scheduled transaction (once order is confirmed by vendor)
    public HashMap submitPayment(Long orderId){
        Order order = JPA.em().find(Order.class, orderId);
        Result<Transaction> result = gateway.transaction().submitForSettlement(order.paymentToken);
        HashMap<String, Object> serviceResponse = new HashMap<>();

        if (result.isSuccess()) {
            serviceResponse.put("processedPayment", result.getTarget());
        }
        else {
            serviceResponse.put("errors", result.getErrors().getAllDeepValidationErrors().get(0).getMessage());
        }

        return serviceResponse;
    }

    public String getClientToken(){
        return gateway.clientToken().generate();
    }

    public void deletePaymentMethod(String token){
        try{
            gateway.paymentMethod().delete(token);
        }
        catch(NotFoundException e){
            System.out.println(e);
        }

    }

    public void cancelPayment(String transactionToken) {
        try {
            gateway.transaction().voidTransaction(transactionToken);

        } catch (NotFoundException e) {

        }
    }

    public HashMap getSubscriptionDetails(String subscriptionToken){
        HashMap subscriptionDetails = null;
        try {
            //Get subscription from Braintree
            Subscription subsc = gateway.subscription().find(subscriptionToken);
            CreditCard cc = gateway.creditCard().find(subsc.getPaymentMethodToken());
            String planId = subsc.getPlanId();

            //Find the plan
            List<Plan> plans = gateway.plan().all();
            Plan plan = null;
            for (Plan p : plans){
                if (p.getId().equals(planId)){
                    plan = p;
                    break;
                }
            }
            String planName = plan.getName();

            // Chcek if still in trial period.
            Boolean trial = false;
            Calendar firstBillCalendar = subsc.getFirstBillingDate();
            Date firstBillingDate = firstBillCalendar.getTime();
            if (subsc.hasTrialPeriod()) {
                Date now = new Date();
                if (firstBillingDate.after(now)) {
                    trial = true;
                }
            }

            subscriptionDetails = new HashMap();
            HashMap paymentMethod = new HashMap();

            //Add card and plan details to result
            paymentMethod.put("imgUrl", cc.getImageUrl());
            paymentMethod.put("maskedNumber", cc.getMaskedNumber());
            subscriptionDetails.put("paymentMethod", paymentMethod);
            subscriptionDetails.put("name", planName);
            subscriptionDetails.put("trial", trial);
            if (trial) {
                subscriptionDetails.put("trialEnds", firstBillingDate);
            }
            else {
                HashMap currentBillingCycle = new HashMap();
                currentBillingCycle.put("start", subsc.getBillingPeriodStartDate().getTime());
                currentBillingCycle.put("end", subsc.getBillingPeriodEndDate().getTime());
                subscriptionDetails.put("currentBillingCycle", currentBillingCycle);
            }
        }
        catch (NotFoundException | NullPointerException e){
            subscriptionDetails = null;
        }
        return subscriptionDetails;
    }

    public HashMap updateSubscriptionPaymentMethod(String subscriptionId, String paymentMethodNonce){
        HashMap result = new HashMap();
        Subscription subscription = gateway.subscription().find(subscriptionId);
        PaymentMethodRequest pmr = new PaymentMethodRequest();
        pmr.paymentMethodNonce(paymentMethodNonce);
        Result updateResult = gateway.paymentMethod().update(subscription.getPaymentMethodToken(), pmr);
        if (updateResult.isSuccess()){
            result.put(StatusCodes.SUCCESS, "Subscription updated.");
        }
        else {
            String errorsString = "";
            List<ValidationError> errors = updateResult.getErrors().getAllDeepValidationErrors();
            for (ValidationError e : errors) {
                errorsString = e.getMessage() + "\n";
            }
            result.put(StatusCodes.ERROR, errorsString);
        }

        return result;
    }

    public HashMap createPlanSubscription(String paymentMethodToken, String planId, HashMap<String, Integer> addons, Calendar startDate){
        HashMap result = new HashMap();
        SubscriptionRequest subscRequest = new SubscriptionRequest();
        subscRequest.paymentMethodToken(paymentMethodToken);
        subscRequest.neverExpires(true);
        subscRequest.planId(planId);
        subscRequest.firstBillingDate(startDate);
        if (!addons.isEmpty()) {
            for (Map.Entry<String, Integer> entry : addons.entrySet()) {
                String addonId = entry.getKey();
                Integer quantity = entry.getValue();
                subscRequest.addOns().update(addonId).quantity(quantity);
            }
        }

        Result<Subscription> subscResult = gateway.subscription().create(subscRequest);
        if (subscResult.isSuccess()){
            result.put("subscriptionId", subscResult.getTarget().getId());
        }
        else {
            String errorsString = "";
            List<ValidationError> errors = subscResult.getErrors().getAllDeepValidationErrors();
            for (ValidationError e : errors) {
                errorsString = e.getMessage() + "\n";
            }
            result.put(StatusCodes.ERROR, errorsString);
        }
        return result;
    }

    public HashMap createCustomer(Long truckId, String contactEmail, String truckName, String firstName, String lastName, String paymentMethodNonce){
        HashMap result = new HashMap();
        CustomerRequest custRequest = new CustomerRequest();
        custRequest.customerId(truckId.toString());
        custRequest.company(truckName);
        custRequest.paymentMethodNonce(paymentMethodNonce);
        custRequest.firstName(firstName);
        custRequest.lastName(lastName);
        custRequest.email(contactEmail);
        Result<Customer> custRequestResult = gateway.customer().create(custRequest);
        if (custRequestResult.isSuccess()){
            result.put("customerId", custRequestResult.getTarget().getId());
            result.put("paymentMethodToken", custRequestResult.getTarget().getPaymentMethods().get(0).getToken());
        }
        else {
            String errorsString = "";
            List<ValidationError> errors = custRequestResult.getErrors().getAllDeepValidationErrors();
            for (ValidationError e : errors) {
                errorsString = e.getMessage() + "\n";
            }
            result.put(StatusCodes.ERROR, errorsString);
        }
        return result;
    }
}
