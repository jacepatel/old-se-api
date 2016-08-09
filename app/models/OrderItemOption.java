package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;



/*CREATE TABLE ItemOptions (ItemOptionId Serial PRIMARY KEY, Name VARCHAR(100),
ShortName VARCHAR(100), Type VARCHAR(200), Cost MONEY);
*/

@Entity
@Table(name="orderitemoptions")
public class OrderItemOption {

    public OrderItemOption(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long orderItemOptionId;

    @Column(name="orderItemId", nullable=false, updatable=false)
    public Long orderItemId;

    @Column(name="name", nullable=true, updatable=true)
    @Constraints.Required
    public String name;

    @Column(name="shortname", nullable=true, updatable=true)
    public String shortName;

    @Column(name="cost", nullable=true, updatable=true)
    public BigDecimal cost;

    @Column(name="itemOptionSelectId", nullable=true, updatable=true)
    public Long selectedId;

    @Column(name="type", nullable=false, updatable=false)
    public String type;
    
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="orderItemId", insertable=false, updatable=false)
    public OrderItem orderItem;


    public void save(){
        JPA.em().persist(this);
    }
}
