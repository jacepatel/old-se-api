package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by michaelsive on 18/07/2014.
  isActive bit,
  maxQuantity INT,
  createdDate DATETIME,
  PRIMARY KEY (itemId),
  CONSTRAINT fk_Items_TruckId FOREIGN KEY (TruckId) REFERENCES Trucks (TruckId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 */
@Entity
@Table(name="items")
public class Item {

    public Item(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long itemId;

    @Column(name="truckId", nullable=false, updatable=false)
    public Long truckId;

    @Column(name="name", nullable=true, updatable=true)
    @Constraints.Required
    public String name;

    @Column(name="description", nullable=true, updatable=true)
    public String description;

    @Column(name="shortdescription", nullable=true, updatable=true)
    public String shortDescription;

    @Column(name="price", nullable=true, updatable=true)
    public BigDecimal price;

    @Column(name="maxQuantity", nullable=true, updatable=true)
    public int maxQuantity;

    @Column(name="sort", nullable=true, updatable=true)
    public Long sort;

    @Column(name="isActive", nullable=true, updatable=true)
    public Boolean isActive;

    @Column(name="isActiveForUsers", nullable=false, updatable=true)
    public Boolean isActiveForUsers;

    @Column(name="color", nullable=true, updatable=true)
    public String color;

    @Formats.DateTime(pattern="dd/MM/yyyy HH:mm:ss")
    @Column(name="createdDate", nullable=true, updatable=true)
    public Date createdDate;

//    @JsonIgnore
//    @OneToOne(mappedBy = "item", cascade = CascadeType.PERSIST)
//    public Image image;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckId", insertable=false, updatable=false)
    public Truck truck;

    @JsonIgnore
    @OneToMany(mappedBy = "item", cascade = CascadeType.PERSIST)
    public List<OrderItem> orderItems;

    @Where(clause = "isDeleted='false'")
    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;


    @ManyToMany
    @Where(clause = "isDeleted='false'")
    @JoinTable(
            name="itemoptionmapping",
            joinColumns={@JoinColumn(name="ItemId", referencedColumnName="ItemId")},
            inverseJoinColumns={@JoinColumn(name="ItemOptionId", referencedColumnName="ItemOptionId")})
    public List<ItemOption> itemOptions;

    //This is called from the Items Controller, to set the new values of the item when updating
    //It receives the Json from the HTTP Post and then sets the new model values - ItemsController.updateItem
    public void updateItemDetails(JsonNode itemParams) {
        this.isDeleted = false;
        this.name = itemParams.get("name").asText();
        this.price = new BigDecimal(itemParams.get("price").asText());
        this.description = itemParams.get("description").asText();
        this.shortDescription = itemParams.get("shortDescription").asText();
        this.isActive = new Boolean(itemParams.get("isActive").asText());
        this.maxQuantity = new Integer(itemParams.get("maxQuantity").asText());

        if (!itemParams.findPath("isActiveForUsers").isMissingNode()) {
            this.isActiveForUsers = new Boolean(itemParams.get("isActiveForUsers").asText());
        }


        if (itemParams.hasNonNull("color")) {
            this.color = itemParams.get("color").asText();
        }

        if (itemParams.hasNonNull("sort")) {
            this.sort = new Long(itemParams.get("sort").asText());
        } else {
            this.sort = 14L;
        }


        this.save();
        JPA.em().flush();


        if (!itemParams.findPath("options").isMissingNode()) {

            this.addOptionsToItem(itemParams.findPath("options"));
        }
    }

    public void addOptionsToItem(JsonNode options){
        Long itemToAddToId = this.itemId;

        List<Long> activeItemOptions = new ArrayList<Long>();


        for (JsonNode option : options){

            //Initiate new itemOption
            boolean isNewOption = true;
            ItemOption newItemOption = new ItemOption();
            if (    !option.findPath("itemOptionId").isMissingNode()) {
                newItemOption = JPA.em().find(ItemOption.class, Long.parseLong(option.get("itemOptionId").asText()));
                isNewOption = false;
            }

            //Set the values
            newItemOption.name = option.get("name").asText();
            newItemOption.shortName = option.get("shortName").asText();
            newItemOption.type = option.get("type").asText();
            newItemOption.cost = new BigDecimal(option.get("cost").asText());
            newItemOption.isDeleted = false;
            newItemOption.isUserVisible = true;
            newItemOption.truckId = this.truckId;

            if (!option.findPath("color").isMissingNode()) {
                newItemOption.color = option.get("color").asText();
            }

            //Save it
            newItemOption.save();
            JPA.em().flush();

            //This creates the many to many


            Query getExistingItemOptionsQry = JPA.em().createQuery("SELECT iom.itemOptionId FROM ItemOptionMapping iom WHERE iom.itemId = :itemId AND iom.itemOptionId = :itemOptionId");
            getExistingItemOptionsQry.setParameter("itemId", this.itemId);
            List<ItemOptionMapping> existingItemOptions = getExistingItemOptionsQry.setParameter("itemOptionId", newItemOption.itemOptionId).getResultList();

            if (isNewOption || existingItemOptions.isEmpty()) {
                ItemOptionMapping mapNewItem = new ItemOptionMapping();
                mapNewItem.itemId = itemToAddToId;
                mapNewItem.itemOptionId = newItemOption.itemOptionId;
                mapNewItem.save();
                JPA.em().flush();
            }

            System.out.println(newItemOption.type);

            if (!option.findPath("itemOptionSelects").isMissingNode()) {
                System.out.println("Item Option type is Select");
                //
                JsonNode selectOptions = option.findPath("itemOptionSelects");
                newItemOption.addSelectsToOption(selectOptions);
            }

            activeItemOptions.add(newItemOption.itemOptionId);

            //Save it

        }

        this.save();
        JPA.em().flush();

            //delete itemToAddToId
            activeItemOptions.add((Long.parseLong("0")));

            Query removeUnnecessaryItemOptions = JPA.em().createQuery("DELETE FROM ItemOptionMapping o " +
                    "WHERE o.itemId = :itemId " +
                    "AND o.itemOptionId NOT IN :activeItemOptions");

            removeUnnecessaryItemOptions.setParameter("itemId", itemToAddToId);
            removeUnnecessaryItemOptions.setParameter("activeItemOptions", activeItemOptions).executeUpdate();

    }

    public void save(){
        JPA.em().persist(this);
    }
}
