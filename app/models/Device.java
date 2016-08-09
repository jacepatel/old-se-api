package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;
import play.data.format.Formats;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by michaelsive on 20/04/15.
 */
@Entity
@Table(name="devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long deviceId;

    @Column(name="userid")
    public Long userId;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="userid", insertable=false, updatable=false)
    public User user;

    @Column(name="devicetype")
    public String deviceType;

    @Column(name="devicetoken")
    public String deviceToken;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="lastused", nullable=true, updatable=true)
    public Date lastused;

    public Device(){
    }

    public void save(){
        JPA.em().persist(this);
    }

    public static Device findByToken(String token){
        try{
            TypedQuery<Device> queryDevice = JPA.em().createQuery("FROM Device d WHERE d.deviceToken = :deviceToken", Device.class);
            queryDevice.setParameter("deviceToken", token);
            Device device = queryDevice.getSingleResult();
            return device;
        }catch (NoResultException e) {
            return null;
        }

    }

}
