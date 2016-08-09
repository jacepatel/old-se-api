package models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import org.mindrot.jbcrypt.BCrypt;
import play.Play;
import play.data.format.Formats;
import play.db.jpa.JPA;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by michaelsive on 19/07/2014.
 */
@Entity
@Table(name="events")
public class Event {

    public Event() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long eventId;

    @Column(name="name", nullable=false, updatable=true)
    public String name;

    @Column(name="lat", nullable=true, updatable=true)
    public double latitude;

    @Column(name="lng", nullable=true, updatable=true)
    public double longitude;

    @Column(name="ParentId", nullable=true, updatable=true)
    public int parentId;

    @Column(name="eventManagerId", nullable=true, updatable=true)
    public long eventManagerId;

    @Column(name="markerUrl", nullable=true, updatable=true)
    public String markerUrl;

    @Column(name="bannerUrl", nullable=true, updatable=true)
    public String bannerUrl;

    @Column(name="description", nullable=true, updatable=true)
    public String description;

    @Column(name="address", nullable=true, updatable=true)
    public String address;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="starttime", nullable=true, updatable=true)
    public Date startTime;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="endtime", nullable=true, updatable=true)
    public Date endTime;

    @Where(clause = "isDeleted='false'")
    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @Column(name="createdDate", nullable=false, updatable=true)
    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    public Date createdDate;

    @JsonIgnore
    @OneToMany(mappedBy = "event", cascade = CascadeType.PERSIST)
    public List<TruckSession> truckSessions;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="eventManagerId", insertable=false, updatable=false)
    public EventManager eventManager;

    @Transient
    public List<Truck> trucks;

    public void updateDetails(JsonNode eventParams) {

      this.name = eventParams.get("name").asText();
      this.latitude = Double.parseDouble(eventParams.get("latitude").asText());
      this.longitude = Double.parseDouble(eventParams.get("longitude").asText());
      this.eventManagerId = eventParams.get("eventManagerId").asLong();
      this.description = eventParams.get("description").asText();
      this.address = eventParams.get("address").asText();
      this.startTime = new Date(Long.parseLong(eventParams.get("startTime").asText()));
      this.endTime = new Date(Long.parseLong(eventParams.get("endTime").asText()));
      this.isDeleted = eventParams.get("isDeleted").asBoolean();

      if (this.createdDate == null) {
        this.createdDate = new Date();
      }

      if (this.markerUrl == null) {
        this.markerUrl = "/assets/markers/"+this.name +".png";
      }

      if (this.bannerUrl == null) {
        this.bannerUrl = "/assets/banners/"+this.name +".png";
      }

    }

  //name, latitude, longitude, eventManagerId, description, address, startTime, endTime, isDeleted

  public static Event findEvent(TruckSession activeTruckSession){
        Query query = JPA.em().createQuery("select e from Event e where e.latitude = :lat and e.longitude = :lng");
        query.setParameter("lat", activeTruckSession.latitude);
        query.setParameter("lng", activeTruckSession.longitude);
        Event event = (Event) query.getSingleResult();
        return event;
    }

    public static List<Event> upcomingEvents () {

        Date tenHoursAhead = new Date(System.currentTimeMillis() + 3600 * 10000);

        Date rightNow = new Date(System.currentTimeMillis());

        Query qryUpcomingEvents = JPA.em().createQuery("Select e FROM Event AS e " +
                "WHERE e.endTime > :rightNow " +
                "AND e.startTime < :tenHoursAhead ");

        qryUpcomingEvents.setParameter("tenHoursAhead", tenHoursAhead);
        qryUpcomingEvents.setParameter("rightNow", rightNow);


        List<Event> upcomingEvents = qryUpcomingEvents.getResultList();

        return upcomingEvents;
    }

    public static List<Event> activeEventsNoTrucks() {
        Date now = new Date();
        Query queryActiveEvents = JPA.em().createQuery("Select e from Event e left join e.truckSessions ts where ts.truckSessionId is null");
        List<Event> events = queryActiveEvents.getResultList();
        return events;
    }

    public void save(){
        JPA.em().persist(this);
    }
}
