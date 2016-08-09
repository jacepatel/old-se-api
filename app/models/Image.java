package models;
import play.Play;
import play.db.jpa.JPA;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by michaelsive on 19/07/2014.
 */
@Entity
@Table(name="images")
public class Image {

    public Image() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long imageId;

    @Column(name="itemId", nullable=false, updatable=false)
    public Long itemId;

    @Column(name="path", nullable=false, updatable=true)
    public String path;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="itemId", insertable=false, updatable=false)
    public Item item;

    public void updateImage(String imageString, Item item) throws IOException {

        byte[] data = DatatypeConverter.parseBase64Binary(imageString);

        Truck truck = item.truck;
        String path = "/public/truck_images/"+truck.email+"/"+item.name+"-image.jpg";
        FileOutputStream fos = new FileOutputStream(Play.application().path() + path);
        fos.write(data);
        this.itemId = item.itemId;
        this.path = path;

    }

    public void save(){
        JPA.em().persist(this);
    }
}
