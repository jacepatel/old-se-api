package models;

import com.braintreegateway.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.StatusCodes;
import org.apache.commons.lang.time.DateUtils;
import play.db.jpa.JPA;
import services.PaymentHandler;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by michaelsive on 24/07/15.
 */
@Entity
@Table(name="billingplans")
public class BillingPlan {

    public BillingPlan() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long billingPlanId;

    @Column(name="braintreetoken")
    public String subscriptionToken;

    @Column(name="numberoftabs")
    public int numTabs;

    @Column(name="startdate")
    public Date startDate;

    @Column(name="current")
    public Boolean current;

    @Column(name="isdeleted")
    public Boolean isDeleted;

    @Column(name="price")
    public BigDecimal price;

    @Column(name="trial")
    public Boolean trial;

    @Column(name="standone")
    public String standOne;

    @Column(name="standtwo")
    public String standTwo;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckId", insertable=true, updatable=false)
    public Truck truck;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="planId")
    public Plan plan;

    @JsonProperty("plan")
    public HashMap getPlan(){
        if (!this.trial && this.subscriptionToken != null) {
            return PaymentHandler.getGateway().getSubscriptionDetails(this.subscriptionToken);
        }
        return null;
    }

    @JsonProperty("trialends")
    public Date trialEnds(){
        if (this.trial || this.subscriptionToken == null){
            try {
                Query tsQuery = JPA.em().createQuery("select ts from TruckSession ts where ts.truckId = :truckId order by ts.startTime ASC").setMaxResults(1);
                tsQuery.setParameter("truckId", this.truck.truckId);
                TruckSession ts = (TruckSession) tsQuery.getResultList().get(0);
                return DateUtils.addMonths(ts.startTime, 1);
            }
            catch (EntityNotFoundException | IndexOutOfBoundsException e){
                return DateUtils.addMonths(this.startDate, 1);
            }
        }
        return null;
    }

    public HashMap updatePaymentMethod(String paymentMethodNonce){
        return PaymentHandler.getGateway().updateSubscriptionPaymentMethod(this.subscriptionToken, paymentMethodNonce);
    }

    public HashMap updateBillingPlanFromTrial(String paymentMethodNonce, String planId){
        Truck truck = this.truck;
        HashMap createCustomerResult = PaymentHandler.getGateway().createCustomer(truck.truckId, truck.contactEmail.split(",")[0], truck.name, truck.firstName, truck.lastName, paymentMethodNonce);
        if (createCustomerResult.containsKey(StatusCodes.ERROR)) {
            return createCustomerResult;
        }

        String paymentMethodToken = (String)createCustomerResult.get("paymentMethodToken");

        Query planQuery = JPA.em().createQuery("select p from Plan p where p.braintreetoken = :braintreeId and p.isDeleted = :notDeleted");
        planQuery.setParameter("braintreeId", planId);
        planQuery.setParameter("notDeleted", false);
        Plan plan = (Plan)planQuery.getSingleResult();

        HashMap addons = new HashMap();
        Integer includedTabs = plan.includedTabs;
        Integer extraTabs = this.numTabs - includedTabs;
        if (extraTabs > 0){
            addons.put(plan.addOnId, extraTabs);
        }
        Calendar start = Calendar.getInstance();
        start.setTime(DateUtils.addMonths(this.startDate, 1));
        HashMap createSubscriptionResult = PaymentHandler.getGateway().createPlanSubscription(paymentMethodToken, planId, addons, start);
        if (createSubscriptionResult.containsKey(StatusCodes.ERROR)){
            return createSubscriptionResult;
        }

        this.trial = false;
        this.subscriptionToken = (String)createSubscriptionResult.get("subscriptionId");
        this.save();
        HashMap result = new HashMap();
        result.put(StatusCodes.SUCCESS, "Subscription updated.");
        return result;
    }

    public void save(){
        JPA.em().persist(this);
    }
}
