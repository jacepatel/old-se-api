package models;

import play.data.format.Formats;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by michaelsive on 29/04/15.
 */
@Entity
@Table(name="pushnotifications")
public class PushNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long pushNotificationId;

    @Column(name="orderid", nullable = true, insertable = true)
    public Long orderId;

    @Column(name="messagetoken", nullable = true, insertable = true)
    public String pushWooshToken;

    @Column(name="errormessage", nullable = true, insertable = true)
    public String errorMessage;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="senttime", nullable=true, updatable=true)
    public Date sentTime;

    public PushNotification(){}

    public void save(){
        JPA.em().persist(this);
    }
}
