package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by michael on 29/07/14.
 */
public class JsonValidator {

    public static final String USER_VAL = "user";
    public static final String USER_UPDATE_VAL = "user_update";
    public static final String TRUCKS_VAL = "truck";
    public static final String EVENTMANAGER_VAL = "eventManager";
    public static final String EVENT_VAL = "event";
    public static final String SESSION_VAL = "session";
    public static final String LOC_TRUCK_VAL = "loc_truck";
    public static final String ITEM_VAL = "item";
    public static final String ITEM_UPDATE_VAL = "item_update";
    public static final String ORDER_VAL = "order";
    public static final String MOBILE_ORDER_VAL = "mobile_order";
    public static final String POS_ORDER_VAL = "pos_order";
    public static final String TRUCK_SESSION_VAL = "truck_session";
    public static final String ORDER_UPDATE_STATUS_VAL = "order_update_status";
    public static final String PAYMENT_METHOD_VAL = "payment_method";
    public static final String PASSWORD_RESET_REQUEST_VAL = "password_reset_request";
    public static final String PASSWORD_RESET_VAL = "password_reset";
    public static final String TRUCK_FROM_MANAGER = "truck_from_manager";

    private static final String[] userParamNames = {"firstName", "lastName", "email", "password", "mobileNumber", "locality"};
    private static final String[] userUpdateParamNames = {"firstName", "lastName", "mobileNumber", "password", "locality"};
    private static final String[]  eventManagerParamNames = {"name","username", "contactEmail", "contactNumber", "isDeleted", "password"};
    private static final String[]  eventParamNames = {"name", "latitude", "longitude", "eventManagerId", "description", "address", "startTime", "endTime", "isDeleted"};

    private static String[] itemParamNames = {"truckId", "name", "price", "shortDescription", "description", "maxQuantity", "options"};

    private static final String[] truckParamNames = {"name","firstName", "description", "lastName", "email", "password", "registration"};

    private static String[] itemUpdateParamNames = {"truckId", "name", "price", "shortDescription", "description", "maxQuantity", "isActive", "itemId", "options"};
    private static String[] posOrderParamNames = {"truckId", "items", "orderName", "mobileNumber", "comments" };
    private static String[] orderParamNames = {"truckId", "items"};
    private static String[] mobileOrderParamNames = {"truckId", "items", "deviceId"};

    private static String[] sessionParamNames = {"email", "password"};
    private static String[] truckSessionParamNames = {"truckId", "startTime", "endTime", "lat", "lng", "locationDirections", "isActive", "eventId"};
    private static String[] geoParamNames = {"lat", "lng", "m"};
    private static String[] orderUpdateStatusParamNames = {"orderId"};
    private static String[] paymentMethodParamNames = {"paymentMethodToken"};

    private static String[] passwordResetRequestParamNames = {"mobileNumber"};
    private static String[] passwordResetParamNames = {"newPassword"};

    private static String[] truckFromManagerParamNames = {"name", "abn", "firstName", "lastName", "email", "password", "mobNum"};

    public static boolean validate(String type, JsonNode json){
        Boolean correct;
        switch (type) {
            case USER_VAL:
                correct = validator(userParamNames, json);
                break;
            case USER_UPDATE_VAL:
                correct = validator(userUpdateParamNames, json);
                break;
            case TRUCKS_VAL:
                correct = validator(truckParamNames, json);
                break;
            case EVENTMANAGER_VAL:
              correct = validator(eventManagerParamNames, json);
              break;
            case EVENT_VAL:
              correct = validator(eventParamNames, json);
              break;
            case SESSION_VAL:
                correct = validator(sessionParamNames, json);
                break;
            case LOC_TRUCK_VAL:
                correct = validator(geoParamNames, json);
                break;
            case ITEM_VAL:
                correct = validator(itemParamNames, json);
                break;
            case ITEM_UPDATE_VAL:
                correct = validator(itemUpdateParamNames, json);
                break;
            case ORDER_VAL:
                correct = validator(orderParamNames, json);
                break;
            case MOBILE_ORDER_VAL:
                correct = validator(mobileOrderParamNames, json);
                break;
            case POS_ORDER_VAL:
                correct = validator(posOrderParamNames, json);
                break;
            case TRUCK_SESSION_VAL:
                correct = validator(truckSessionParamNames, json);
                break;
            case ORDER_UPDATE_STATUS_VAL:
                correct = validator(orderUpdateStatusParamNames, json);
                break;
            case PAYMENT_METHOD_VAL:
                correct = validator(paymentMethodParamNames, json);
                break;
            case PASSWORD_RESET_REQUEST_VAL:
                correct = validator(passwordResetRequestParamNames, json);
                break;
            case PASSWORD_RESET_VAL:
                correct = validator(passwordResetParamNames, json);
                break;
            case TRUCK_FROM_MANAGER:
                correct = validator(truckFromManagerParamNames, json);
                break;
            default:
                correct = false;
        }
        return correct;
    }

    private static boolean validator(String[] params, JsonNode json){
        ArrayList<String> keys = Lists.newArrayList(json.fieldNames());
        for (String key : keys) {
            if (!Arrays.asList(params).contains(key)) {
                return false;
            }
        }
//        if (params.length != keys.size()) {
//            return false;
//        }
        return true;
    }
}
