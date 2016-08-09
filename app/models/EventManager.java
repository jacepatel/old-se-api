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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 17/07/2014.
 */
@Entity
@Table(name="eventManagers")
public class EventManager {

    public EventManager(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long eventManagerId;

    //Email is the username now.
    @Constraints.Required
    @Column(name="username", nullable=false, updatable=true)
    public String username;

    @Column(name="contactEmail", nullable=true, updatable=true)
    public String contactEmail;

    @Column(name="name", nullable=false, updatable=true)
    public String name;

    @JsonIgnore
    @Constraints.Required
    @Column(name="password", nullable=false, updatable=true)
    public String password;

    @Column(name="contactNumber", nullable=false, updatable=true)
    public String contactNumber;


    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @Column(name="createdDate", nullable=false, updatable=true)
    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    public Date createdDate;

    @JsonIgnore
    @OneToMany(mappedBy = "eventManager", cascade = CascadeType.PERSIST)
    public List<Event> events;

    public static EventManager findByUsername(String username){
      try {
        TypedQuery<EventManager> queryEmail = JPA.em().createQuery("FROM EventManager WHERE username = :username", EventManager.class);
        EventManager foundEventManager = queryEmail.setParameter("username", username).getSingleResult();
        return foundEventManager;
      }
      catch(NoResultException e) {
        return null;
      }
    }

    public void updateDetails(JsonNode eventManagerParams) {
        this.isDeleted = false;
        this.name = eventManagerParams.get("name").asText();
        this.username = eventManagerParams.get("username").asText();
        this.contactEmail = eventManagerParams.get("contactEmail").asText();
        this.contactNumber = eventManagerParams.get("contactNumber").asText();
        this.isDeleted = eventManagerParams.get("isDeleted").asBoolean();

        if (this.createdDate == null) {
            this.createdDate = new Date();
        }
        //set password on the first time only, use alternate route to update otherwise
        if (this.password == null) {
            this.password = BCrypt.hashpw(eventManagerParams.get("password").asText(), BCrypt.gensalt());
        }
    }

    public boolean authPass(String password) {
        return BCrypt.checkpw(password, this.password);
    }

    public void save(){
        JPA.em().persist(this);
    }
}
