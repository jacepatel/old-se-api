package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import play.db.jpa.JPA;

import javax.persistence.*;

/**
 * Created by michaelsive on 13/08/15.
 */
@Entity
@Table(name="bankaccounts")
public class BankAccount {

    public BankAccount(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long bankAccountId;

    @Column(name="bsb")
    public String bsb;

    @Column(name="accountnumber")
    public String accountNumber;

    @Column(name="accountname")
    public String accountName;

    @Column(name="isDeleted")
    public Boolean isDeleted;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="truckId", insertable=true, updatable=false)
    public Truck truck;

    public void save(){
        JPA.em().persist(this);
    }
}
