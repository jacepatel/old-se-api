package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import org.mindrot.jbcrypt.BCrypt;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by michaelsive on 17/07/2014.
 */
@Entity
@Table(name="Subscriptions")
public class Subscription {

    public Subscription(){}

    @Id @Constraints.Email
    @Column(name="email", nullable=false, updatable=true)
    public String email;


    @Column(name="isactive", nullable=true, updatable=true)
    public boolean isactive;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="joineddate", nullable=true, updatable=true)
    public Date joinedDate;

    @Column(name="isvendor", nullable=false, updatable=true)
    public boolean isvendor;

    @Column(name="name", nullable=true, updatable=true)
    public String name;

    @Column(name="mobilenumber", nullable=true, updatable=true)
    public String mobilenumber;


    public static Subscription findByEmail(String email){
        try{

            TypedQuery<Subscription> queryEmail = JPA.em().createQuery("FROM Subscription WHERE email = :email", Subscription.class);
            Subscription subscription = queryEmail.setParameter("email", email).getSingleResult();
            if (subscription.email != null) {
                return subscription;
            }
            else { return null; }
        }catch (NoResultException e) {
            return null;
        }

    }

    public void save(){
        JPA.em().persist(this);
    }
}
