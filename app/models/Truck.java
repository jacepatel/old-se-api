package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.hibernate.annotations.Where;
import org.mindrot.jbcrypt.BCrypt;
import play.data.validation.Constraints;
import play.db.jpa.JPA;
import javax.persistence.Transient;
import javax.persistence.*;
import java.util.*;

/**
 * Created by michaelsive on 17/07/2014.
 */
@Entity
@Table(name="trucks")
public class Truck {

    public Truck(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long truckId;

    //Email is the username now.
    @Constraints.Required
    @Column(name="email", nullable=false, updatable=true)
    public String email;

    @Column(name="contactEmail", nullable=true, updatable=true)
    public String contactEmail;

    @Constraints.Required
    @Column(name="name", nullable=false, updatable=true)
    public String name;

    @Column(name="description", nullable=true, updatable=true)
    public String description;

    @Column(name="SMSMessage", nullable=true, updatable=true)
    public String SMSMessage;

    @JsonIgnore
    @Constraints.Required
    @Column(name="password_hash", nullable=false, updatable=true)
    public String passwordHash;

    @Column(name="facebookPageId", nullable=true, updatable=true)
    public String facebookPageId;

    @Column(name="registration", nullable=false, updatable=true)
    public String registration;

    @Column(name="firstName", nullable=true, updatable=true)
    public String firstName;

    @Column(name="lastName", nullable=true, updatable=true)
    public String lastName;

    @Column(name="markerUrl", nullable=true, updatable=true)
    public String markerUrl;

    @Constraints.Required
    @Column(name="contactNumber", nullable=false, updatable=true)
    public String contactNumber;

    @Column(name="timezoneName", nullable=false, updatable=true)
    public String timezoneName;

    @Column(name="currency", nullable=false, updatable=true)
    public String currency;

    @JsonProperty("queueSize")
    @Transient
    public Long queueSize;

    @Column(name="lat", nullable=true, updatable=true)
    public double latitude;

    @Column(name="defaultAddress", nullable=true, updatable=true)
    public String defaultAddress;

    @Column(name="lng", nullable=true, updatable=true)
    public double longitude;

    @JsonProperty("distance")
    @Transient
    private double distance;

    @JsonIgnore
    @Where(clause = "isDeleted='false'")
    @OneToMany(mappedBy = "truck", cascade = CascadeType.PERSIST)
    public List<Item> items;

    @JsonIgnore
    @Where(clause = "isDeleted='false'")
    @OneToMany(mappedBy = "truck", cascade = CascadeType.PERSIST)
    public List<ItemOption> itemOptions;

    @JsonIgnore
    @OneToMany(mappedBy="truck", cascade = CascadeType.PERSIST)
    public List<Order> orders;

    @JsonIgnore
    @OneToMany(mappedBy = "truck", cascade = CascadeType.PERSIST)
    public List<TruckSession> truckSessions;

    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @Transient
    public List<TruckSession> futureSessions;

    public List<TruckSession> getFutureSessions(){
        return this.futureSessions;
    }

    @JsonIgnore
    @OneToMany(mappedBy="truck", cascade = CascadeType.PERSIST)
    @Where(clause="isDeleted='false' and current='true'")
    public List<BillingPlan> currentBillingPlans;

    @JsonIgnore
    @OneToMany(mappedBy="truck", cascade = CascadeType.PERSIST)
    @Where(clause="isDeleted='false' and current='false'")
    public List<BillingPlan> pastBillingPlans;

    @JsonIgnore
    @OneToMany(mappedBy="truck", cascade = CascadeType.PERSIST)
    @Where(clause="isDeleted='false'")
    public List<BankAccount> bankAccounts;

    @JsonIgnore
    @Column(name="apikey")
    public String apikey;

    @JsonIgnore
    @Column(name="abn")
    public String abn;

    public void setFutureSessions(List<TruckSession> truckSessions){
        this.futureSessions = truckSessions;
    }

    public static Truck findByEmail(String email){
        try {
            TypedQuery<Truck> queryEmail = JPA.em().createQuery("FROM Truck WHERE email = :email", Truck.class);
            Truck truck = queryEmail.setParameter("email", email).getSingleResult();
            return truck;
        }
        catch(NoResultException e) {
            return null;
        }
    }

    public void updateDetails(JsonNode truckParams) {
        this.isDeleted = false;
        this.name = truckParams.get("name").asText();
        this.description = truckParams.get("description").asText();
        this.firstName = truckParams.get("firstName").asText();
        this.lastName = truckParams.get("lastName").asText();

        this.email = truckParams.get("email").asText();
        this.facebookPageId = truckParams.get("facebookPageId").asText();
        this.isDeleted = new Boolean(truckParams.get("isDeleted").asText());
        this.latitude = new Double(truckParams.get("latitude").asText());
        this.longitude = new Double(truckParams.get("longitude").asText());
        this.defaultAddress = truckParams.get("defaultAddress").asText();
        this.SMSMessage = truckParams.get("SMSMessage").asText();
        this.contactNumber = truckParams.get("contactNumber").asText();
        this.contactEmail = truckParams.get("contactEmail").asText();

        if (this.markerUrl == null) {
            this.markerUrl = "/assets/markers/"+this.email +".png";
        }

        //set password on the first time only, use alternate route to update otherwise
        if (this.passwordHash == null) {
            this.passwordHash = BCrypt.hashpw(truckParams.get("password").asText(), BCrypt.gensalt());
        }

    }

    public boolean authPass(String password) {
        return BCrypt.checkpw(password, this.passwordHash);
    }

    public static List<Truck> getActiveTrucks(){
        Query query = JPA.em().createQuery("select t from Truck t inner join t.truckSessions ts WHERE ts.isActive = :active and t.isDeleted != :deleted");
        query.setParameter("active", true);
        query.setParameter("deleted", true);
        List<Truck> activeTrucks = query.getResultList();
        return activeTrucks;
    }

    public static List<Truck> getNonClientTrucks(){
        Date now = new Date();
        Query query = JPA.em().createQuery("select t from Truck t inner join t.truckSessions ts WHERE t.isDeleted != :deleted and (ts.isUsed = :notactive OR (ts.isUsed = :notactive AND ts.isActive = :active AND ts.isActiveForOrders = :notactive) ) and ts.startTime < :now and ts.endTime > :now and ts.eventId is null");
        query.setParameter("notactive", false);
        query.setParameter("active", true);
        query.setParameter("deleted", true);
        query.setParameter("now", now);
        List<Truck> activeTrucks = query.getResultList();
        return activeTrucks;
    }

    public static List<Truck> getFutureTrucks(){
        Date twentyEightHours = new Date(System.currentTimeMillis() + 3600 * 28000);
        Date now = new Date();
        Query query = JPA.em().createQuery("select distinct t from Truck t inner join fetch t.truckSessions ts WHERE ts.isUsed = :isUsed and ts.isDeleted = :isUsed and ts.endTime > :now and ts.endTime < :tfh and ts.startTime > :now");
        query.setParameter("isUsed", false);
        query.setParameter("now", now);
        query.setParameter("tfh", twentyEightHours);
        List<Truck> activeTrucks = query.getResultList();
        for (Truck t : activeTrucks){
            Query sessionquery = JPA.em().createQuery("select ts from TruckSession ts WHERE ts.isUsed = :isUsed and ts.isDeleted = :isUsed and ts.endTime > :now and ts.endTime < :tfh and ts.startTime > :now and ts.truckId = :truckId order by startTime DESC");
            sessionquery.setParameter("isUsed", false);
            sessionquery.setParameter("now", now);
            sessionquery.setParameter("tfh", twentyEightHours);
            sessionquery.setParameter("truckId", t.truckId);
            List<TruckSession> futureSessions = sessionquery.getResultList();
            t.setFutureSessions(futureSessions);
        }
        return activeTrucks;
    }


    @JsonIgnore
    public List<Item> getActiveMenuItems(){
        Query query = JPA.em().createQuery("select distinct i from Item i " +
                "left join i.itemOptions io " +
                "left join io.itemOptionSelects ios " +
                "WHERE (i.isActive != :notactive AND i.isActiveForUsers != :notactive and i.truckId = :truckId and i.isDeleted != :isTrue) " +
                " AND (io.isUserVisible = :isTrue or io.isUserVisible IS NULL) " +
                " AND (ios.isDeleted != :isTrue OR ios.isDeleted IS NULL)" );
        query.setParameter("notactive", false);
        query.setParameter("truckId", this.truckId);
        query.setParameter("isTrue", true);
        List<Item> activeMenuItems = query.getResultList();
        return activeMenuItems;
    }

    public TruckSession getActiveSession(){
        Query query = JPA.em().createQuery("select ts from TruckSession ts where ts.truckId = :truckId and ts.isActive = :active");
        query.setParameter("active", true);
        query.setParameter("truckId", this.truckId);
        TruckSession activeTruckSession = new TruckSession();
        activeTruckSession.isActive = false;
        try {
            activeTruckSession = (TruckSession) query.getSingleResult();
            return activeTruckSession;
        }
        catch (Exception ex){
            try {
                Date now = new Date();
                Query queryNonClient = JPA.em().createQuery("select ts from TruckSession ts where ts.truckId = :truckId and ts.isUsed = :notused and ts.startTime < :now and ts.endTime > :now");
                queryNonClient.setParameter("notused", false);
                queryNonClient.setParameter("now", now);
                queryNonClient.setParameter("truckId", this.truckId);
                activeTruckSession = (TruckSession) queryNonClient.getSingleResult();
                return activeTruckSession;
            }
            catch (Exception e){
                return activeTruckSession;
            }
        }
    }

    public static HashMap<String, Object> getMapData(){
        //Get all non-event trucks
        Date twentyEightHours = new Date(System.currentTimeMillis() + 3600 * 28000);
        Date now = new Date();
        Query activeTrucksNoEventQuery = JPA.em().createQuery("select distinct t from Truck t inner join t.truckSessions ts WHERE ts.isActive = :active and t.isDeleted != :deleted and (ts.eventId is null or ts.eventId = 0)");
        activeTrucksNoEventQuery.setParameter("active", true);
        activeTrucksNoEventQuery.setParameter("deleted", true);
        List<Truck> activeNoEventTrucks = activeTrucksNoEventQuery.getResultList();
        for (Truck t : activeNoEventTrucks){
            TruckSession ts = t.getActiveSession();
            Query queueQuery = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
            queueQuery.setParameter("truckSessionId", ts.truckSessionId);
            t.queueSize = (Long) queueQuery.getSingleResult();
        }
        //Add non-StreetEats trucks
        activeNoEventTrucks.addAll(getNonClientTrucks());

        //Get all future non-event trucks
        Query futureTrucksNoEventQuery = JPA.em().createQuery("select distinct t from Truck t inner join fetch t.truckSessions ts WHERE (ts.isUsed = :isUsed and ts.isDeleted = :isUsed and ts.endTime > :now and ts.endTime < :tfh and ts.startTime > :now and (ts.eventId is null or ts.eventId = 0))");
        futureTrucksNoEventQuery.setParameter("isUsed", false);
        futureTrucksNoEventQuery.setParameter("now", now);
        futureTrucksNoEventQuery.setParameter("tfh", twentyEightHours);
        List<Truck> futureNonEventTrucks = futureTrucksNoEventQuery.getResultList();
        for (Truck t : futureNonEventTrucks){
            Query sessionquery = JPA.em().createQuery("select ts from TruckSession ts WHERE (ts.isUsed = :isUsed and ts.isDeleted = :isUsed and ts.endTime > :now and ts.endTime < :tfh and ts.startTime > :now and ts.truckId = :truckId and (ts.eventId is null OR ts.eventId = 0)) order by startTime ASC");
            sessionquery.setParameter("isUsed", false);
            sessionquery.setParameter("now", now);
            sessionquery.setParameter("tfh", twentyEightHours);
            sessionquery.setParameter("truckId", t.truckId);
            List<TruckSession> futureSessions = sessionquery.getResultList();
            t.setFutureSessions(futureSessions);
        }

        //Get all current events
//        Query currentEventsQuery = JPA.em().createQuery("select e from Event e WHERE e.startTime < :now and endTime > :now");
        Query currentEventsQuery = JPA.em().createQuery("SELECT DISTINCT e FROM Event e LEFT JOIN e.truckSessions ts WHERE (ts.isDeleted = :isfalse AND ts.isActive = :istrue) OR (e.startTime < :now AND e.endTime > :now)");
        currentEventsQuery.setParameter("now", now);
        currentEventsQuery.setParameter("istrue", true);
        currentEventsQuery.setParameter("isfalse", false);

        //Create a list of event id's so future ones dont overlap. Initiate with a 0 for query purposes
        List<Integer> activeEventParentIds = new ArrayList<Integer>();
        activeEventParentIds.add(0);

        List<Event> currentEvents = currentEventsQuery.getResultList();
        for (Event e : currentEvents){

            //Add the parent Id to a list to check in the next query
            activeEventParentIds.add(e.parentId);

            Query truckQuery = JPA.em().createQuery("select distinct t from Truck t inner join fetch t.truckSessions ts WHERE ts.eventId = :eventid AND ts.isActive = :istrue");
            truckQuery.setParameter("eventid", e.eventId);
            truckQuery.setParameter("istrue", true);
            List<Truck> eventTrucks = truckQuery.getResultList();
            e.trucks = eventTrucks;
            for (Truck t : e.trucks){
                Query queueQuery = JPA.em().createQuery("SELECT COUNT(e) FROM Order e WHERE e.orderStatus < 3 AND e.truckSessionId = :truckSessionId");
                queueQuery.setParameter("truckSessionId", t.getActiveSession().truckSessionId);
                t.queueSize = (Long) queueQuery.getSingleResult();
            }
        }

        //Get all future events
//        Query futureEventsQuery = JPA.em().createQuery("select e from Event e WHERE e.startTime > :now and startTime < :tfh");
        Query futureEventsQuery = JPA.em().createQuery("SELECT DISTINCT e FROM Event e LEFT JOIN e.truckSessions ts WHERE e.startTime < :tfh AND e.startTime > :now AND (ts.truckSessionId is null OR (ts.isDeleted = :istrue OR ts.isActive = :isfalse)) AND e.parentId NOT IN (:existingEvents) order by e.startTime ASC");
        futureEventsQuery.setParameter("now", now);
        futureEventsQuery.setParameter("tfh", twentyEightHours);
        futureEventsQuery.setParameter("istrue", true);
        futureEventsQuery.setParameter("isfalse", false);
        futureEventsQuery.setParameter("existingEvents", activeEventParentIds);
        List<Event> futureEvents = futureEventsQuery.getResultList();
        for (Event e : futureEvents){
            Query truckQuery = JPA.em().createQuery("select distinct t from Truck t inner join fetch t.truckSessions ts WHERE ts.eventId = :eventid AND ts.isUsed = :isFalse AND ts.isDeleted = :isFalse");
            truckQuery.setParameter("eventid", e.eventId);
            truckQuery.setParameter("isFalse", false);
            List<Truck> eventTrucks = truckQuery.getResultList();
            for (Truck t : eventTrucks){
                Query sessionquery = JPA.em().createQuery("select ts from TruckSession ts WHERE ts.isUsed = :isUsed and ts.isDeleted = :isUsed and ts.endTime > :now and ts.endTime < :tfh and ts.startTime > :now and ts.truckId = :truckId and ts.eventId is not null order by startTime ASC");
                sessionquery.setParameter("isUsed", false);
                sessionquery.setParameter("now", now);
                sessionquery.setParameter("tfh", twentyEightHours);
                sessionquery.setParameter("truckId", t.truckId);
                List<TruckSession> futureSessions = sessionquery.getResultList();
                t.setFutureSessions(futureSessions);
            }
            e.trucks = eventTrucks;
        }

        HashMap data = new HashMap<String, Object>();
        data.put("trucks", activeNoEventTrucks);
        data.put("futureTrucks", futureNonEventTrucks);
        data.put("events", currentEvents);
        data.put("futureEvents", futureEvents);
        return data;
    }

    public HashMap facebookData(){
        Query sessionQuery = JPA.em().createQuery("SELECT t from TruckSession t where t.truckId = :truckId and t.endTime > :now and t.isDeleted = :isFalse order by startTime asc");
        sessionQuery.setParameter("now", new Date());
        sessionQuery.setParameter("truckId", truckId);
        sessionQuery.setParameter("isFalse", false);
        ArrayList<TruckSession> truckSessions = (ArrayList<TruckSession>) sessionQuery.setMaxResults(5).getResultList();
        HashMap data = new HashMap();
        data.put("truck", this);
        data.put("sessions", truckSessions);
        return data;
    }

    public Boolean validateToken(String token){
        try {
            String apikey = Jwts.parser().setSigningKey(Application.jwtSecret).parseClaimsJws(token).getBody().getSubject();
        }
        catch (JwtException e) {
            return false;
        }
        return this.apikey.equals(apikey);
    }

    @JsonProperty("registered")
    public Boolean regestrationCompleted(){
        try {

            Query bpq = JPA.em().createQuery("select bp from BillingPlan bp where bp.truck.truckId = :truckId");
            bpq.setParameter("truckId", this.truckId);
            return (bpq.getResultList().size() > 0);
        }
        catch (NullPointerException e) {
            return false;
        }

    }

    public void save(){
        JPA.em().persist(this);
    }
}
