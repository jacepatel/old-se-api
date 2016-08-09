package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.JsonValidator;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 18/07/2014.
 
 */
@CorsComposition.Cors
public class ItemsController extends Controller {


    //Create a test for Create Item
    //HTTP Post Call to Create an Item for a Truck
    @Transactional
    public static Result createItem() {

        //Validates JSON Parsed from HTTP Post
//        if (!JsonValidator.validate(JsonValidator.ITEM_VAL,request().body().asJson())){
//            HashMap<String, String> reply = new HashMap<String, String>();
//            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
//            return ok(Json.toJson(reply));
//        }

        //Checks if the truck is active
        Truck truck = Application.currentVendor();

        if (truck != null) {

            //Create the Item Object
            Item newItem = new Item();
            newItem.truckId = truck.truckId;
            newItem.createdDate = new Date();

            //Update the item details with posted information
            newItem.updateItemDetails(request().body().asJson());

            JPA.em().flush();
            Item newItemReturn = JPA.em().find(Item.class, newItem.itemId);

            //Return the new item in JSON
            return ok(Json.toJson(newItemReturn));
        }

        //If there was no truck, it returns unauthorized
        HashMap<String, String> reply = new HashMap<String, String>();
            reply.put(StatusCodes.ERROR,StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
    }

    @play.db.jpa.Transactional
    public static Result updateAllItems() {

        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        Truck vendor  = Application.currentVendor();

        //Check if the current session has a user
        if (vendor == null)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        JsonNode itemsRequest = request().body().asJson();


        for (JsonNode jsonItem : itemsRequest.findPath("items")){
            Long itemId =  new Long(jsonItem.get("itemId").asText());
            Item itemToUpdate = JPA.em().find(Item.class, itemId);
            itemToUpdate.sort =  new Long(jsonItem.get("sort").asText());
            itemToUpdate.save();
        }

        reply.put(StatusCodes.SUCCESS, "Bulk items successfully updated");
        return ok(Json.toJson(reply));
    }

    //This is the update item API Call, it receives an item model in json format and updates all fields at once
    @play.db.jpa.Transactional
    public static Result updateItem(Long itemId) {

        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        //Calls the item validator to check the validity of the json parsed through from the post request
//        if (!JsonValidator.validate(JsonValidator.ITEM_UPDATE_VAL ,request().body().asJson())){
//            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
//            return ok(Json.toJson(reply));
//        }

        Item itemToUpdate = JPA.em().find(Item.class, itemId);
        Truck vendor  = Application.currentVendor();

        //Check if the current session has a user
        if (vendor == null)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        //Checks the the item to update has the current truck
        if (vendor != null && itemToUpdate.truckId == vendor.truckId) {
            itemToUpdate.updateItemDetails(request().body().asJson());
            return ok(Json.toJson(itemToUpdate));
        }

        //Note to self, build a library of return strings maybe
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(reply));
    }

    @play.db.jpa.Transactional
    public static Result applyOptionToAllItems(Long itemOptionId, Long truckId)
    {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        Truck vendor  = Application.currentVendor();

        ItemOption itemOptionToAdd = JPA.em().find(ItemOption.class, itemOptionId);

        if (itemOptionToAdd == null) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
            return ok(Json.toJson(reply));
        }

        //Check if the current session has a user and they are valid
        if (vendor.truckId != truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        for (Item i : vendor.items) {

            //Find if the mapping exists already
            Query existingItemQuery = JPA.em().createQuery("SELECT iom FROM ItemOptionMapping iom WHERE iom.itemId = :litemId AND iom.itemOptionId = :litemOptionId");
            existingItemQuery.setParameter("litemId", i.itemId);
            existingItemQuery.setParameter("litemOptionId", itemOptionId);
            List<ItemOptionMapping> mappedItemOptions = existingItemQuery.getResultList();

            if (mappedItemOptions.isEmpty()) {
                ItemOptionMapping mapNewItem = new ItemOptionMapping();
                mapNewItem.itemId = i.itemId;
                mapNewItem.itemOptionId = itemOptionId;
                mapNewItem.save();
            }
        }

        reply.put(StatusCodes.SUCCESS, StatusCodes.SUCCESS);
        return ok(Json.toJson(reply));
    }

    @play.db.jpa.Transactional
    public static Result deleteOptionFromAllItems(Long itemOptionId)
    {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        Truck vendor  = Application.currentVendor();

        ItemOption itemOptionToDelete = JPA.em().find(ItemOption.class, itemOptionId);
        System.out.println(itemOptionToDelete.itemOptionId + " being deleted");

        if (itemOptionToDelete == null) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
            return ok(Json.toJson(reply));
        }

        //Check if the current session has a user and they are valid
        if (vendor.truckId != itemOptionToDelete.truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        Query removeUnnecessaryItemOptions = JPA.em().createQuery("DELETE FROM ItemOptionMapping o " +
                "WHERE o.itemOptionId = :itemOptionId ");

        removeUnnecessaryItemOptions.setParameter("itemOptionId", itemOptionId).executeUpdate();

        itemOptionToDelete.isDeleted = true;
        itemOptionToDelete.save();
        JPA.em().flush();

        reply.put(StatusCodes.SUCCESS, StatusCodes.SUCCESS);
        return ok(Json.toJson(reply));
    }

    @play.db.jpa.Transactional
    public static Result copyOptionsFromItem(Long copyItemId, Long toItemId)
    {
        //Setup the reply
        HashMap<String, JsonNode> reply = new HashMap<String, JsonNode>();

        Truck vendor  = Application.currentVendor();

        Item copyItem = JPA.em().find(Item.class, copyItemId);
        Item toItem = JPA.em().find(Item.class, toItemId);

        if (toItem == null || copyItem == null) {
            reply.put(StatusCodes.ERROR, Json.toJson(StatusCodes.ERROR_UNKNOWN));
            return ok(Json.toJson(reply));
        }

        //Check if the current session has a user and they are valid
        if (vendor.truckId != copyItem.truckId || vendor.truckId != toItem.truckId)
        {
            reply.put(StatusCodes.ERROR, Json.toJson(StatusCodes.ERROR_UNAUTH));
            return ok(Json.toJson(reply));
        }

        List<Long> existingItemOptions = new ArrayList<Long>();


        Query getExistingItemOptionsQry = JPA.em().createQuery("SELECT iom.itemOptionId FROM ItemOptionMapping iom WHERE iom.itemId = :toItemId");
        existingItemOptions = getExistingItemOptionsQry.setParameter("toItemId", toItem.itemId).getResultList();

        existingItemOptions.add((Long.parseLong("0")));

        Query newItemOptionsQry = JPA.em().createQuery("SELECT iom FROM ItemOptionMapping iom WHERE iom.itemId = :fromItemId AND iom.itemOptionId NOT IN (:existingItemOptions)");
        newItemOptionsQry.setParameter("fromItemId", copyItem.itemId);
        newItemOptionsQry.setParameter("existingItemOptions", existingItemOptions);
        List<ItemOptionMapping> mappedItemOptions = newItemOptionsQry.getResultList();

        for (ItemOptionMapping iom : mappedItemOptions) {
            ItemOptionMapping mapNewItem = new ItemOptionMapping();
            mapNewItem.itemId = toItem.itemId;
            mapNewItem.itemOptionId = iom.itemOptionId;
            mapNewItem.isActive = iom.isActive;
            mapNewItem.isDeleted = iom.isDeleted;
            mapNewItem.save();
        }

        JPA.em().flush();

        reply.put(StatusCodes.SUCCESS, Json.toJson(toItem));
        reply.put("item", Json.toJson(toItem));

        return ok(Json.toJson(reply));
    }

    @play.db.jpa.Transactional
    public static Result toggleOptionForItemId(Long itemOptionId, Long itemId, String trueOrFalse)
    {
        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();
        boolean tOrF = Boolean.parseBoolean(trueOrFalse);

        Truck vendor  = Application.currentVendor();

        ItemOption itemOptionToAdd = JPA.em().find(ItemOption.class, itemOptionId);
        Item itemToAdd = JPA.em().find(Item.class, itemId);

        if (itemOptionToAdd == null || itemToAdd == null) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
            return ok(Json.toJson(reply));
        }

        //Check if the current session has a user and they are valid
        if (vendor.truckId != itemToAdd.truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        Query existingItemOptionMapping = JPA.em().createQuery("SELECT iom FROM ItemOptionMapping iom WHERE iom.itemOptionId = :litemOptionId AND iom.itemId = :litemId");
        existingItemOptionMapping.setParameter("litemOptionId", itemOptionId);
        existingItemOptionMapping.setParameter("litemId", itemId);
        List<ItemOptionMapping> mappedItemOptions = existingItemOptionMapping.getResultList();

        for (ItemOptionMapping iom : mappedItemOptions) {
            iom.isActive = tOrF;
            iom.save();
        }

        reply.put(StatusCodes.SUCCESS, StatusCodes.SUCCESS);
        return ok(Json.toJson(reply));
    }


    @play.db.jpa.Transactional
    public static Result enableOrDisableOptionForAllItems(Long itemOptionId, String trueOrFalse)
    {

        boolean tOrF = Boolean.parseBoolean(trueOrFalse);

        //Setup the reply
        HashMap<String, String> reply = new HashMap<String, String>();

        Truck vendor  = Application.currentVendor();

        ItemOption itemOptionToAdd = JPA.em().find(ItemOption.class, itemOptionId);

        if (itemOptionToAdd == null) {
            reply.put(StatusCodes.ERROR, "Option not found");
            return ok(Json.toJson(reply));
        }

        //Check if the current session has a user and they are valid
        if (vendor.truckId != itemOptionToAdd.truckId)
        {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }

        Query existingItemQuery = JPA.em().createQuery("SELECT iom FROM ItemOptionMapping iom WHERE iom.itemOptionId = :litemOptionId");
        existingItemQuery.setParameter("litemOptionId", itemOptionId);
        List<ItemOptionMapping> mappedItemOptions = existingItemQuery.getResultList();

        for (ItemOptionMapping iom : mappedItemOptions) {
            iom.isActive = tOrF;
            iom.save();
        }

        reply.put(StatusCodes.SUCCESS, StatusCodes.SUCCESS);
        return ok(Json.toJson(reply));
    }


    //Deletes Item from DB
    @play.db.jpa.Transactional
    public static Result deleteItem(Long itemId) {
        Item itemToDelete = JPA.em().find(Item.class, itemId);
        Truck truck = Application.currentVendor();

        //setup the response
        HashMap<String, String> reply = new HashMap<String, String>();

        //Check if all is valid
        if (truck != null && truck.truckId == itemToDelete.truckId) {
            itemToDelete.isDeleted = true;
            itemToDelete.save();
            reply.put(StatusCodes.SUCCESS,"Item  " +itemToDelete.shortDescription +" successfully deleted.");
            return ok(Json.toJson(reply));
        }

        if (truck != null) {
            reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(reply));
        }
        reply.put(StatusCodes.ERROR, StatusCodes.ERROR_UNKNOWN);
        return ok(Json.toJson(reply));
    }
}
