package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;
import play.db.jpa.JPA;

import javax.persistence.*;

/**
 * Created by jacepatel on 12/03/2015.
 */

@Entity
@Table(name="ItemOptionMapping")
public class ItemOptionMapping {

    public ItemOptionMapping(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long itemOptionMappingId;

    @Column(name="itemOptionId", nullable=true, updatable=true)
    public Long itemOptionId;

    @Column(name="itemId", nullable=true, updatable=true)
    public Long itemId;

    @Where(clause = "isDeleted='false'")
    @Column(name="isDeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @Column(name="isActive", nullable=true, updatable=true)
    public boolean isActive;


    public void save(){
        JPA.em().persist(this);
    }
}
