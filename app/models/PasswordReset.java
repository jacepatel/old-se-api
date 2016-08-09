package models;

import play.db.jpa.JPA;

import javax.persistence.*;

/**
 * Created by michaelsive on 12/02/15.
 */
@Entity
@Table(name="passwordresets")
public class PasswordReset {

    public PasswordReset(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long passwordResetId;

    @Column(name="resettoken")
    public String resetToken;

    @Column(name="userId")
    public Long userId;

    @Column(name="isused")
    public boolean isUsed;

    public static PasswordReset findByToken(String token){
        try{
            TypedQuery<PasswordReset> queryPWToken = JPA.em().createQuery("FROM PasswordReset WHERE resetToken = :token", PasswordReset.class);
            PasswordReset pwr = queryPWToken.setParameter("token", token).getSingleResult();
            return pwr;
        }catch (NoResultException e) {
            return null;
        }

    }

    public void save(){
        JPA.em().persist(this);
    }
}
