package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by michaelsive on 19/07/2014.
 */
@Entity
@Table(name="OrderItems")
public class OrderItem {

    public OrderItem(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long orderItemId;

    @Column(name="orderId", nullable=false, updatable=false)
    public Long orderId;

    @Column(name="itemId", nullable=false, updatable=false)
    public Long itemId;

    @Column(name="quantity", nullable=false, updatable=true)
    public Integer quantity;

    @Column(name="itemPrice", precision = 15, scale = 2, nullable=false, updatable=true)
    public BigDecimal itemPrice;

    @Column(name="discountAmount", precision = 15, scale = 2, nullable=false, updatable=true)
    public BigDecimal discountAmount;

    @Column(name="totalPaid", precision = 15, scale = 2, nullable=false, updatable=true)
    public BigDecimal totalPaid;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="orderId", insertable=false, updatable=false)
    public Order order;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="itemId", insertable=false, updatable=false)
    public Item item;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.PERSIST)
    public List<OrderItemOption> orderItemOptions;

    public void save(){
        JPA.em().persist(this);
    }
}
