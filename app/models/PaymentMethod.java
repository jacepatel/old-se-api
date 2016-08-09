package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.List;

/**
 * Created by michaelsive on 30/12/14.
 */

@Entity
@Table(name="paymentmethods")
public class PaymentMethod {

    public PaymentMethod(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long paymentMethodId;

    @Column(name="userId")
    public Long userId;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="userId", insertable=false, updatable=false)
    public User user;

    @Constraints.Required
    @Column(name="paymentmethodtoken", nullable=false, updatable=true)
    public String paymentMethodToken;

    @Constraints.Required
    @Column(name="identifier", nullable=false, updatable=true)
    public String identifier;

    @Column(name="cardtype", nullable=false, updatable=true)
    public String cardType;

    @Column(name="defaultmethod", nullable=true, updatable=true)
    public boolean defaultMethod;

    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @JsonIgnore
    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.PERSIST)
    public List<Order> orders;


    public static PaymentMethod findByToken(String token){
            TypedQuery<PaymentMethod> queryToken = JPA.em().createQuery("FROM PaymentMethod WHERE paymentMethodToken = :pmToken", PaymentMethod.class);
            queryToken.setParameter("pmToken", token);
            PaymentMethod pm =  queryToken.getSingleResult();
            if (pm != null) {
                return pm;
            }
            return null;
    }

    public void save(){
        JPA.em().persist(this);
    }
}
