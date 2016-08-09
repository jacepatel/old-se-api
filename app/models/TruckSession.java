package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import play.data.format.Formats;
import play.db.jpa.JPA;
import play.libs.F;
import services.TrelloHandler;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 THIS SHIT NEEDS TO BE FILLED OUT
 */
@Entity
@Table(name="trucksessions")
public class TruckSession {

    public TruckSession(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long truckSessionId;

    @Column(name="truckId", nullable=false, updatable=false)
    public Long truckId;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="startTime", nullable=true, updatable=true)
    public Date startTime;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="endTime", nullable=true, updatable=true)
    public Date endTime;

    @Column(name="lat", nullable=true, updatable=true)
    public double latitude;

    @Column(name="lng", nullable=true, updatable=true)
    public double longitude;

    @Column(name="locationDirections", nullable=true, updatable=true)
    public String locationDirections;

    @Column(name="address", nullable=true, updatable=true)
    public String address;

    @Column(name="isActive", nullable=true, updatable=true)
    public boolean isActive;

    @Column(name="isActiveForOrders", nullable=true, updatable=true)
    public boolean isActiveForOrders;

    @Column(name="maximumOrders", nullable=true, updatable=true)
    public Integer maximumOrders;

    @Column(name="minimumOrderValue", nullable=true, updatable=true)
    public BigDecimal minimumOrderValue;

    @Column(name="eventId", nullable=true, updatable=false)
    public Long eventId;

    @Column(name="isUsed", nullable=true, updatable=true)
    public boolean isUsed;

    @Where(clause = "isDeleted='false'")
    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckId", insertable=false, updatable=false)
    public Truck truck;

    @JsonIgnore
    @OneToMany(mappedBy = "truckSession", cascade = CascadeType.PERSIST)
    public List<Order> orders;

    @JsonIgnore
    @Column(name="orderCount", nullable=true, updatable=true)
    public Long orderCount;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="eventId", insertable=false, updatable=false)
    public Event event;

//    public List<Order> getCurrentOrders(Long truckSessionId) {
//        Query qryActiveOrders = JPA.em().createQuery("SELECT o FROM Order AS o " +
//                "WHERE o.truckSessionId = :truckSessionId " +
//                "AND o.orderStatus IN (1,2,3) ");
//        qryActiveOrders.setParameter("truckSessionId", truckSessionId);
//        List<Order> activeOrders = qryActiveOrders.getResultList();
//        return activeOrders;
//    }

    public Long orderCountOfType(int type){
        Query getCountOfMobileOrders = JPA.em().createQuery("SELECT COUNT(o) From Order o where truckSessionId = :tsId and orderType = :ordertype and orderStatus = :completed");
        getCountOfMobileOrders.setParameter("tsId", this.truckSessionId);
        getCountOfMobileOrders.setParameter("ordertype", type);
        getCountOfMobileOrders.setParameter("completed", Order.STATUS_COMPLETED);
        return (Long) getCountOfMobileOrders.getSingleResult();
    }

    public List<Long> distinctItemIds(){
        Query getDistinctItemIds = JPA.em().createQuery("SELECT distinct itemId FROM OrderItem oi where oi.order.truckSessionId = :tsId and oi.order.orderStatus = :completed");
        getDistinctItemIds.setParameter("completed", Order.STATUS_COMPLETED);
        getDistinctItemIds.setParameter("tsId", this.truckSessionId);
        List<Long> itemIds = (List<Long>) getDistinctItemIds.getResultList();
        return itemIds;
    }

    public ArrayList<String> itemTypeCountandPaid(Long itemId){
        Query getCountOfItemType = JPA.em().createQuery("SELECT oi FROM OrderItem oi where oi.order.truckSessionId = :tsId and oi.order.orderStatus = :completed and itemId = :itemId");
        getCountOfItemType.setParameter("tsId", this.truckSessionId);
        getCountOfItemType.setParameter("itemId", itemId);
        getCountOfItemType.setParameter("completed", Order.STATUS_COMPLETED);
        List<OrderItem> orderItems = getCountOfItemType.getResultList();
        Long itemQty = new Long(0);
        for (OrderItem oi : orderItems){
            itemQty += oi.quantity;
        }
        BigDecimal totalPaid = new BigDecimal(0).setScale(2, RoundingMode.DOWN);
        for (OrderItem oi : orderItems){
            totalPaid = totalPaid.add(oi.totalPaid.multiply(new BigDecimal(oi.quantity)));
        }

        ArrayList<String> result = new ArrayList<String>();
        result.add(itemQty.toString());
        result.add(totalPaid.toString());
        return result;
    }

    public Long orderCountofSMSOrders(){
        Query getCountOfMobileOrders = JPA.em().createQuery("SELECT COUNT(o) From Order o where truckSessionId = :tsId and orderType = 2 and orderStatus = :completed and mobileNumber != '' and mobileNumber is not null");
        getCountOfMobileOrders.setParameter("tsId", this.truckSessionId);
        getCountOfMobileOrders.setParameter("completed", Order.STATUS_COMPLETED);
        return (Long) getCountOfMobileOrders.getSingleResult();
    }

    public List<Order> getOrdersUpdatedAfterTime (Long truckSessionId, String lastUpdateTime) {

        Date lastUpdate = new Date(Long.parseLong(lastUpdateTime)*1000);
        Query qryAllOrdersAfterTime = JPA.em().createQuery("Select o FROM Order AS o " +
                "WHERE (o.orderTime > :updateTime " +
                "OR o.readyTime > :updateTime " +
                "OR o.collectTime > :updateTime " +
                "OR o.acceptedTime > :updateTime ) AND " +
                "o.truckSessionId = :truckSessionId");

        qryAllOrdersAfterTime.setParameter("truckSessionId", truckSessionId);
        List<Order> allOrdersUpdatedAfterTime = qryAllOrdersAfterTime.setParameter("updateTime", lastUpdate).getResultList();

        return allOrdersUpdatedAfterTime;
    }

    public static List<Object[]> orderCount(Long truckSessionId){

        Query qryOrderSummary = JPA.em().createQuery("SELECT o.orderStatus, count(o.orderId) AS orderCount FROM Order o " +
                "WHERE o.truckSessionId = :truckSessionId " +
                "AND o.orderStatus IN (1,2,3) " +
                "GROUP BY o.orderStatus");

        List<Object[]> getOrderCount = qryOrderSummary.setParameter("truckSessionId", truckSessionId).getResultList();

        return getOrderCount;
    }

    public List<Order> getOrdersAfterOrderId (Long truckSessionId, Long lastOrderId) {

        Query qryAllOrdersAfterOrderId = JPA.em().createQuery("Select o FROM Order AS o " +
                "WHERE o.orderId > :lastOrderId AND " +
                "o.truckSessionId = :truckSessionId " +
                " ORDER BY o.orderId");

        qryAllOrdersAfterOrderId.setParameter("truckSessionId", truckSessionId);
        qryAllOrdersAfterOrderId.setParameter("lastOrderId", lastOrderId);

        List<Order> allOrdersAfterOrderId = qryAllOrdersAfterOrderId.setFirstResult(0).setMaxResults(20).getResultList();

        return allOrdersAfterOrderId;
    }

    public List<Order> getOrdersBeforeOrderId (Long truckSessionId, Long lastOrderId) {

        Query qryAllOrdersAfterOrderId = JPA.em().createQuery("Select o FROM Order AS o " +
                "WHERE o.shortOrderId < :lastOrderId AND " +
                "o.truckSessionId = :truckSessionId " +
                " ORDER BY o.shortOrderId desc");

        qryAllOrdersAfterOrderId.setParameter("truckSessionId", truckSessionId);
        qryAllOrdersAfterOrderId.setParameter("lastOrderId", lastOrderId);

        List<Order> allOrdersAfterOrderId = qryAllOrdersAfterOrderId.setFirstResult(0).setMaxResults(20).getResultList();

        return allOrdersAfterOrderId;
    }


    public static Boolean activeOrderCount(Long truckSessionId){

        Query qryOrderSummary = JPA.em().createQuery("SELECT o FROM Order o " +
                "WHERE o.truckSessionId = :truckSessionId " +
                "AND o.orderStatus IN (1,2,3) ");

        List<Order> currentOrderCount = qryOrderSummary.setParameter("truckSessionId", truckSessionId).getResultList();

        return !currentOrderCount.isEmpty();

    }



    public void updateTruckSessionDetails(JsonNode truckSessionParams) {
        System.out.println(truckSessionParams);
        this.truckId = truckSessionParams.get("truckId").asLong();
        this.startTime = new Date(Long.parseLong(truckSessionParams.get("startTime").asText()));
        this.endTime = new Date(Long.parseLong(truckSessionParams.get("endTime").asText()));
        this.latitude = Double.parseDouble(truckSessionParams.get("lat").asText());
        this.longitude = Double.parseDouble(truckSessionParams.get("lng").asText());
        this.isActive = Boolean.parseBoolean(truckSessionParams.get("isActive").asText());

        if(!truckSessionParams.findPath("minimumOrderValue").isMissingNode()) {
            this.minimumOrderValue = new BigDecimal(truckSessionParams.get("minimumOrderValue").asText());
        } else {
            this.minimumOrderValue = new BigDecimal(0);
        }

        if(!truckSessionParams.findPath("maximumOrders").isMissingNode()) {
            this.maximumOrders = Integer.parseInt(truckSessionParams.get("maximumOrders").asText());
        } else {
            this.maximumOrders = 30;
        }

        if (truckSessionParams.get("eventId").asLong() != 0) {
            this.eventId = truckSessionParams.get("eventId").asLong();
        }

        if(!truckSessionParams.findPath("isActiveForOrders").isMissingNode()) {
            this.isActiveForOrders = Boolean.parseBoolean(truckSessionParams.get("isActiveForOrders").asText());
        } else {
            this.isActiveForOrders = true;
        }

        if(!truckSessionParams.findPath("isUsed").isMissingNode()) {
            this.isUsed = Boolean.parseBoolean(truckSessionParams.get("isUsed").asText());
        } else {
            this.isUsed = true;
        }

        if(!truckSessionParams.findPath("isDeleted").isMissingNode()) {
            this.isDeleted = Boolean.parseBoolean(truckSessionParams.get("isDeleted").asText());
        } else {
            this.isDeleted = false;
        }

        if(!truckSessionParams.findPath("address").isMissingNode()) {
            this.address = truckSessionParams.get("address").asText();
        }

        this.isActiveForOrders = true;
        this.locationDirections = truckSessionParams.get("locationDirections").asText();
        this.save();
        JPA.em().flush();

    }

    public void save(){
        JPA.em().persist(this);
    }

}
