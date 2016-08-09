package models;

import java.util.*;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import org.mindrot.jbcrypt.BCrypt;
import play.data.format.Formats;
import play.data.validation.*;
import play.db.jpa.JPA;
import play.libs.F;
import services.PaymentHandler;
import services.SpreedlyHandler;

/**
 * Created by michaelsive on 16/07/2014.
 */
@Entity
@Table(name="users")
public class User {
    public User(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long userId;

    @JsonIgnore
    @Constraints.Required
    @Column(name="password_hash", nullable=false, updatable=true)
    public String passwordHash;

    @Constraints.Required
    @Column(name="firstName", nullable=false, updatable=true)
    public String firstName;

    @Constraints.Required
    @Column(name="lastName", nullable=false, updatable=true)
    public String lastName;

    @Constraints.Email
    @Column(name="email", nullable=false, updatable=true)
    public String email;

    @Constraints.Required
    @Column(name="mobilenumber", nullable=false, updatable=true)
    public String mobNumber;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<Order> orders;

    @JsonIgnore
    @OrderBy("orderTime DESC")
    @Where(clause = "orderStatus < 4")
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<Order> currentOrders;

    @JsonIgnore
    @OrderBy("orderTime DESC")
    @Where(clause = "orderStatus = 4")
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<Order> pastOrders;

    @JsonIgnore
    @OrderBy("orderTime DESC")
    @Where(clause = "orderStatus > 4")
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<Order> cancelledOrders;

    @Where(clause = "isDeleted='false'")
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<PaymentMethod> paymentMethods;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    public List<Device> devices;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="createddate", nullable=true, updatable=true)
    public Date createddate;

    @Column(name="locality")
    public String locality;

    public static User findByEmail(String email){
        try{
            TypedQuery<User> queryEmail = JPA.em().createQuery("FROM User WHERE email = :email", User.class);
            User user = queryEmail.setParameter("email", email).getSingleResult();
            return user;
        }catch (NoResultException e) {
            return null;
        }

    }

    public static User findByMobNum(String mobNum){
        try{
            TypedQuery<User> queryEmail = JPA.em().createQuery("FROM User WHERE mobNumber = :mobNum", User.class);
            User user = queryEmail.setParameter("mobNum", mobNum).getSingleResult();
            return user;
        }catch (NoResultException e) {
            return null;
        }

    }

    @JsonIgnore
    public PaymentMethod getDefaultPaymentMethod(){
        for (PaymentMethod p : this.paymentMethods){
            if (p.defaultMethod && !p.isDeleted){
                return p;
            }
        }
        return null;
    }

    public void updateDetails(JsonNode userParams) {
        this.firstName = userParams.get("firstName").asText();
        this.lastName = userParams.get("lastName").asText();
        this.passwordHash = BCrypt.hashpw(userParams.get("password").asText(), BCrypt.gensalt());
        this.email = userParams.get("email").asText();
        this.mobNumber = userParams.get("mobileNumber").asText();
        if (userParams.has("locality")) {
            this.locality = userParams.get("locality").asText();
        }
    }

    public boolean authPass(String password) {
        return BCrypt.checkpw(password, this.passwordHash);
    }

    public HashMap addPaymentMethod(String nonce){
        PaymentHandler braintreeHandler = PaymentHandler.getGateway();
        HashMap result = braintreeHandler.createPaymentMethod(this.userId.toString(), nonce);
        if (result.containsKey("errors")){
            System.out.println(result.get("errors").toString());
        }
        else {
            PaymentMethod pm = new PaymentMethod();
            pm.paymentMethodToken = result.get("paymentMethodToken").toString();
            pm.userId = this.userId;
            pm.identifier = result.get("lastFour").toString();
            pm.cardType = result.get("cardType").toString();
            pm.isDeleted = false;
            pm.save();
            makeDefaultPaymentMethod(pm.paymentMethodId);
            HashMap hm = new HashMap();
            hm.put("paymentMethod", pm);
            return hm;
        }
        return result;
    }

    public HashMap addSpreedlyPaymentMethod(String token, String lastFour, String cardType){
        PaymentMethod pm = new PaymentMethod();
        pm.paymentMethodToken = token;
        pm.userId = this.userId;
        pm.identifier = lastFour;
        pm.cardType = cardType;
        pm.isDeleted = false;
        pm.save();
        makeDefaultPaymentMethod(pm.paymentMethodId);
        HashMap hm = new HashMap();
        hm.put("paymentMethod", pm);
        return hm;
    }

    public void makeDefaultPaymentMethod(Long paymentMethodId){
        List<PaymentMethod> userPMs = this.paymentMethods;
        for (PaymentMethod listed : userPMs){
            if (listed.paymentMethodId != paymentMethodId) {
                listed.defaultMethod = false;
            }
            else {
                listed.defaultMethod = true;
            }
        }
    }

    public String fullName(){
        return this.firstName + " " + this.lastName;
    }

    public void save(){
        JPA.em().persist(this);
    }

    public void nullifyResetTokens(){
        Query pwrQuery = JPA.em().createQuery("select pwr from PasswordReset pwr where pwr.userId = :userId and pwr.isUsed = :notUsed");
        pwrQuery.setParameter("userId", this.userId);
        pwrQuery.setParameter("notUsed", false);
        List<PasswordReset> userPwrs = pwrQuery.getResultList();
        for (PasswordReset pwr : userPwrs){
            pwr.isUsed = true;
            pwr.save();
        }
    }

    public Device addDeviceToUser(String deviceToken, String deviceType){
        Device newDevice = new Device();
        newDevice.userId = this.userId;
        newDevice.deviceToken = deviceToken;
        newDevice.deviceType = deviceType;
        newDevice.lastused = new Date();
        newDevice.save();
        return newDevice;
    }
}
