package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Where;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;



/*CREATE TABLE ItemOptions (ItemOptionId Serial PRIMARY KEY, Name VARCHAR(100),
ShortName VARCHAR(100), Type VARCHAR(200), Cost MONEY);
*/

@Entity
@Table(name="itemoptions")
public class ItemOption {

    public ItemOption(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long itemOptionId;

    @Column(name="name", nullable=true, updatable=true)
    @Constraints.Required
    public String name;

    @Column(name="shortname", nullable=true, updatable=true)
    public String shortName;

    @Column(name="type", nullable=true, updatable=true)
    public String type;

    @Column(name="cost", nullable=true, updatable=true)
    public BigDecimal cost;

    @Where(clause = "isDeleted='false'")
    @Column(name="isdeleted", nullable=true, updatable=true)
    public boolean isDeleted;

    @Column(name="isUserVisible", nullable=true, updatable=true)
    public boolean isUserVisible;

    @Column(name="truckId", nullable=false, updatable=false)
    public Long truckId;

    @Column(name="color", nullable=true, updatable=true)
    public String color;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckId", insertable=false, updatable=false)
    public Truck truck;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name="itemoptionmapping",
            joinColumns={@JoinColumn(name="ItemOptionId", referencedColumnName="ItemOptionId")},
            inverseJoinColumns={@JoinColumn(name="ItemId", referencedColumnName="ItemId")}
            )
    public List<Item> items;

    @Where(clause = "isDeleted='false'")
    @OneToMany(mappedBy = "itemOption", cascade = CascadeType.PERSIST)
    public List<ItemOptionSelect> itemOptionSelects;

//mappedBy reference an unknown target entity property: models.ItemOption.itemOption in models.Item.options
    //This is called from the Items Controller, to set the new values of the item when updating
    //It receives the Json from the HTTP Post and then sets the new model values - ItemsController.updateItem
    public void updateItemOptionDetails(JsonNode itemOptionParams) {
        this.isDeleted = new Boolean(itemOptionParams.get("isDeleted").asText());
        this.name = itemOptionParams.get("name").asText();
        this.cost = new BigDecimal(itemOptionParams.get("cost").asText());
        this.shortName = itemOptionParams.get("shortName").asText();
        this.type = itemOptionParams.get("type").asText();
        this.save();
    }

    public void addSelectsToOption(JsonNode selects){

        Long optionToAddToId = this.itemOptionId;

        for (JsonNode select : selects){

            //New ItemOptionSelect
            ItemOptionSelect newItemOptionSelect = new ItemOptionSelect();

            if (!select.findPath("itemOptionSelectId").isMissingNode()) {
                //Set it to the existing item option if there is one
                newItemOptionSelect = JPA.em().find(ItemOptionSelect.class, Long.parseLong(select.get("itemOptionSelectId").asText()));
            }

            //Handle if deleted or not
            Boolean itemOptionSelectDelete = false;
            if(!select.findPath("isDeleted").isMissingNode()) {
                itemOptionSelectDelete = new Boolean(select.get("isDeleted").asText());
            };

            //Set the values
            newItemOptionSelect.name = select.get("name").asText();
            newItemOptionSelect.shortName = select.get("shortName").asText();
            newItemOptionSelect.cost = new BigDecimal(select.get("cost").asText());
            newItemOptionSelect.isDeleted = itemOptionSelectDelete;
            newItemOptionSelect.itemOptionId = optionToAddToId;

            //Save it
            newItemOptionSelect.save();
            JPA.em().flush();
            newItemOptionSelect.sort = newItemOptionSelect.itemOptionSelectId;
        }
        this.save();
        JPA.em().flush();
    }

    public void save(){
        JPA.em().persist(this);
    }
}
