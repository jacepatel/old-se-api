package controllers;

import actions.CorsComposition;
import com.fasterxml.jackson.databind.JsonNode;
import models.BankAccount;
import models.Truck;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 13/08/15.
 */
@CorsComposition.Cors
public class BankAccountsController extends Controller {

    @Transactional
   public static Result addBankAccount(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token").toString()) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
       else {
           JsonNode bankDetails = request().body().asJson();
           if (bankDetails != null){
               String acctNum = bankDetails.get("accountNumber").asText();
               String bsb = bankDetails.get("bsb").asText();
               String acctName = bankDetails.get("accountName").asText();
               List<BankAccount> truckBas = truck.bankAccounts;
               for (BankAccount ba : truckBas){
                   ba.isDeleted = true;
                   ba.save();
                   JPA.em().flush();
               }
               try {
                   Query q = JPA.em().createQuery("select ba from BankAccount ba where ba.accountName = :accountName and ba.accountNumber = :accountNum and ba.bsb = :bsb and ba.truck.truckId = :truckId");
                   q.setParameter("accountName", acctName);
                   q.setParameter("accountNum", acctNum);
                   q.setParameter("bsb", bsb);
                   q.setParameter("truckId", truck.truckId);
                   BankAccount existing = (BankAccount) q.getSingleResult();
                   existing.isDeleted = false;
                   existing.save();
                   return ok(Json.toJson(existing));
               }
               catch (NoResultException e) {
                   BankAccount bankAccount = new BankAccount();
                   bankAccount.accountName = acctName;
                   bankAccount.accountNumber = acctNum;
                   bankAccount.bsb = bsb;
                   bankAccount.isDeleted = false;
                   bankAccount.truck = truck;
                   bankAccount.save();
                   return ok(Json.toJson(bankAccount));
               }
           }
           HashMap reply = new HashMap();
           reply.put(StatusCodes.ERROR, StatusCodes.ERROR_BAD_JSON);
           return ok(Json.toJson(reply));
       }
   }

    @Transactional
    public static Result getCurrentBankAccount(Long truckId){
        Truck truck = JPA.em().find(Truck.class, truckId);
        if (!truck.validateToken(request().getHeader("jwt-token")) || truck == null) {
            HashMap result = new HashMap();
            result.put(StatusCodes.ERROR, StatusCodes.ERROR_UNAUTH);
            return ok(Json.toJson(result));
        }
        else {
            Query baQuery = JPA.em().createQuery("select ba from BankAccount ba where ba.truck.truckId = :truckId and ba.isDeleted = :notDeleted");
            baQuery.setParameter("truckId", truckId);
            baQuery.setParameter("notDeleted", false);
            try {
                BankAccount ba = (BankAccount) baQuery.getSingleResult();
                return ok(Json.toJson(ba));
            }
            catch (NoResultException | IndexOutOfBoundsException e){
                HashMap reply = new HashMap();
                reply.put(StatusCodes.SUCCESS, "No account.");
                return ok(Json.toJson(reply));
            }
        }
    }
}
