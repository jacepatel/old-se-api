package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import play.data.format.Formats;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by michaelsive on 19/07/2014.
 */
@Entity
@Table(name="orders")
public class Order {

    public static final int STATUS_PENDING_CONFIRMATION = 1;
    public static final int STATUS_CONFIRMED = 2;
    public static final int STATUS_READY_TO_COLLECT = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELED = 5;
    public static final int STATUS_PAYMENT_FAILED = 6;
    public static final int STATUS_PENDING_PAYMENT_SCHEDULE = 8;
    public static final int STATUS_ITEMS_UNAVAILABLE = 9;
    public static final int STATUS_USER_CANCELLED = 9;

    //OrderStatus, 1 = mobile, 2 = pos, 3 = eftpos

    public Order(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long orderId;

    @Column(name="userid", nullable=true, updatable=false)
    public Long userId;

    @Column(name="truckid", nullable=false, updatable=false)
    public Long truckId;

    @Column(name="trucksessionid", nullable=false, updatable=false)
    public Long truckSessionId;

    @Column(name="paymentmethodid", nullable=false, updatable=true)
    public Long paymentMethodId;

    @Column(name="orderstatus", nullable=true, updatable=true)
    public Integer orderStatus;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="ordertime", nullable=true, updatable=true)
    public Date orderTime;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="acceptedTime", nullable=true, updatable=true)
    public Date acceptedTime;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="readytime", nullable=true, updatable=true)
    public Date readyTime;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="collecttime", nullable=true, updatable=true)
    public Date collectTime;

    @Column(name="mobilenumber", nullable=true, updatable=true)
    public String mobileNumber;

    @Column(name="ordername", nullable=true, updatable=true)
    public String orderName;

    @Column(name="ordertype", nullable=true, updatable=true)
    public Integer orderType;

    @Column(name="comments", nullable=true, updatable=true)
    public String comments;

    @Column(name="shortOrderId", nullable=true, updatable=true)
    public Long shortOrderId;

//    @JsonIgnore
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    public List<OrderItem> orderItems;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="userid", insertable=false, updatable=false)
    public User user;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckid", insertable=false, updatable=false)
    public Truck truck;


    @Column(name="ordertotalpaid")
    public BigDecimal orderTotal;

    @Column(name="discount", nullable = true)
    public BigDecimal discount;

    @Column(name="paymentToken", insertable=true, updatable=true)
    public String paymentToken;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="paymentmethodid", insertable=false, updatable=false)
    public PaymentMethod paymentMethod;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="trucksessionid", insertable=false, updatable=false)
    public TruckSession truckSession;

    @JsonProperty("placeInQueue")
    @Transient
    public Long placeInQueue;

    @JsonIgnore
    @Column(name="spreedlyorderid", insertable=true, updatable=true)
    public String spreedlyOrderId;

    @Column(name="deviceid", nullable=true, insertable=true)
    public Long deviceId;

    @Column(name="capturetoken", nullable=true, insertable=true)
    public String captureToken;

    public boolean addItemsToOrder(JsonNode items){
        Long orderToAddToId = this.orderId;
        this.orderTotal = BigDecimal.ZERO;

        for (JsonNode item : items){
            long itemId = item.findPath("itemId").asLong();
            int qty = item.findPath("qty").asInt();
            Item itemToAdd = JPA.em().find(Item.class, itemId);

            //Cancels the order if the item isn't active
            if (!itemToAdd.isActive){
                return false;
            }

            //Create new orderItem
            OrderItem orderItem = new OrderItem();
            orderItem.itemId = itemToAdd.itemId;
            orderItem.quantity = item.findPath("qty").asInt();
            orderItem.itemPrice = itemToAdd.price;
            orderItem.orderId = orderToAddToId;

            //Set these to empty for later calculation
            orderItem.totalPaid = new BigDecimal(0);
            orderItem.discountAmount = new BigDecimal(0);

            orderItem.save();
            JPA.em().flush();

            //Declare these for calculating price
            BigDecimal itemPrice = itemToAdd.price;
            BigDecimal optionsPrice = new BigDecimal(0);

            //Item Options
            JsonNode options = item.findPath("itemOptions");
            for (JsonNode option : options){


                Long optionId = option.findPath("itemOptionId").asLong();
                //Basic check to see the optionId is found
                if (optionId != 0) {

                    ItemOption itemOption = JPA.em().find(ItemOption.class, optionId);

                    if (itemOption.type.equals("Boolean")) {
                        if (option.findPath("added").asBoolean()) {
                            //Create New Order Item Option

                            OrderItemOption oiOption = new OrderItemOption();

                            oiOption.orderItemId = orderItem.orderItemId;
                            oiOption.cost = itemOption.cost;
                            oiOption.name = itemOption.name;
                            oiOption.shortName = itemOption.shortName;
                            oiOption.type = itemOption.type;
                            oiOption.save();

                            //Increment the price to be added
                            optionsPrice = optionsPrice.add(itemOption.cost);
                        }
                    } else if (itemOption.type.equals("Select")) {
                            Long selectedId = option.findPath("selectedId").asLong();
                            if (selectedId != 0) {

                                //Find the orderItemOption and the Selected Value
                                ItemOptionSelect optSelect = JPA.em().find(ItemOptionSelect.class, selectedId);

                                //New Order Item Option
                                OrderItemOption oiOption = new OrderItemOption();

                                oiOption.orderItemId = orderItem.orderItemId;
                                oiOption.type = itemOption.type;
                                oiOption.selectedId = selectedId;
                                oiOption.cost = optSelect.cost;
                                oiOption.name = optSelect.name;
                                oiOption.shortName = optSelect.shortName;
                                oiOption.save();

                                //Increment hte price to be added
                                optionsPrice = optionsPrice.add(optSelect.cost);
                            }

                    }
                }

            }

            //Add the optionsPrice to the itemsPrice
            itemPrice = itemPrice.add(optionsPrice);
            orderItem.totalPaid = itemPrice;
            BigDecimal itemsPrice = itemPrice.multiply(new BigDecimal(qty));
            BigDecimal newOrderTotal = this.orderTotal.add(itemsPrice);
            this.orderTotal = newOrderTotal;
        }
        this.save();
        return true;
    }

    public static BigDecimal getTotalFromJson(JsonNode itemsJson){
        BigDecimal total = new BigDecimal(0);
        for (JsonNode itemJson : itemsJson){
            Long itemId = itemJson.findPath("itemId").asLong();
            Long itemQty = itemJson.findPath("qty").asLong();
            Item item = JPA.em().find(Item.class, itemId);
            BigDecimal itemPrice = item.price;
            JsonNode optionsJson = itemJson.findPath("itemOptions");
            for (JsonNode optionJson : optionsJson){
                Long optionId = optionJson.findPath("itemOptionId").asLong();
                ItemOption option = JPA.em().find(ItemOption.class, optionId);
                if (option.type.equals("Boolean")){
                    Boolean added = optionJson.findPath("added").asBoolean();
                    if (added == true){
                        itemPrice = itemPrice.add(option.cost);
                    }
                }
                else if (option.type.equals("Select")){
                        Long selectedId = optionJson.findPath("selectedId").asLong();
                        if (selectedId != 0) {
                            ItemOptionSelect selectedOption = JPA.em().find(ItemOptionSelect.class, selectedId);
                            itemPrice = itemPrice.add(selectedOption.cost);
                        }
                }
            }
            total = total.add(itemPrice.multiply(new BigDecimal(itemQty)));
        }
        return total;
    }

    public void save(){
        JPA.em().persist(this);
    }
}
