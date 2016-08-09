package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by michaelsive on 6/08/15.
 */
@Entity
@Table(name="plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long planId;

    @Column(name="braintreetoken")
    public String braintreetoken;

    @Column(name="isDeleted")
    public Boolean isDeleted;

    @Column(name = "addOnId")
    public String addOnId;
    
    @Column(name="includedTabs")
    public int includedTabs;

    @Column(name="price")
    public BigDecimal price;

    @JsonIgnore
    @OneToMany(mappedBy = "plan", cascade = CascadeType.PERSIST)
    public List<BillingPlan> billingPlans;

    public Plan(){
    }

    public void save(){
        JPA.em().persist(this);
    }

    public static Plan getPlanByType(String planType){
        Query planQuery = JPA.em().createQuery("SELECT p from Plan p where p.braintreetoken = :planType");
        planQuery.setParameter("planType", planType);
        return (Plan)planQuery.getSingleResult();
    }
}
