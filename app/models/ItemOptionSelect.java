package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;


/*

CREATE TABLE ItemOptionsSelects (
ItemOptionSelectId SERIAL PRIMARY KEY, ItemOptionId Integer REFERENCES ItemOptions (ItemOptionId), Name VARCHAR(100), ShortName VARCHAR(100),
Cost MONEY);
 */

@Entity
@Table(name="itemOptionSelects")
public class ItemOptionSelect {

    public ItemOptionSelect(){}

    @Column(name="itemOptionId", nullable=false, updatable=false)
    public Long itemOptionId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long itemOptionSelectId;

    @Column(name="name", nullable=true, updatable=true)
    @Constraints.Required
    public String name;

    @Column(name="shortname", nullable=true, updatable=true)
    public String shortName;

    @Column(name="cost", nullable=true, updatable=true)
    public BigDecimal cost;

    @Column(name="sort", nullable=true, updatable=true)
    public Long sort;

    @Column(name="isdefault", nullable=true, updatable=true)
    public Boolean isDefault;

    @Where(clause = "isDeleted='false'")
    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @JsonIgnore
    @Where(clause = "isDeleted='false'")
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="itemOptionId", insertable=false, updatable=false)
    public ItemOption itemOption;

    public void save(){
        JPA.em().persist(this);
    }
}
