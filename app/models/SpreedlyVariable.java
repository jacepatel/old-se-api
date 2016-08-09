package models;

import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.List;

/**
 * Created by michaelsive on 27/03/15.
 */
@Entity
@Table(name="spreedlyvariables")
public class SpreedlyVariable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="spreedlyid")
    public long spreedlyid;

    @Column(name="environmentkey")
    public String key;

    @Column(name="current")
    public boolean isCurrent;

    public static String getCurrentEnvironmentKey(){
        TypedQuery<SpreedlyVariable> q = JPA.em().createQuery("select e from SpreedlyVariable e where e.isCurrent = :current", SpreedlyVariable.class);
        q.setParameter("current", true);
        SpreedlyVariable se = q.getSingleResult();
        return se.key;
    }
}
