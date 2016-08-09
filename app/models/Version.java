package models;

import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaelsive on 20/02/15.
 */
@Entity
@Table(name="versions")
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="versionid")
    public long versionid;

    @Column(name="code")
    public String code;

    @Column(name="iscritical")
    public boolean isCritical;

    public static List<Version> getMoreRecent(Long versionId){
        List<Version> moreRecentVersions = new ArrayList<Version>();
        Query moreRecentVersionsQuery = JPA.em().createQuery("select v from Version v where v.versionid > :userVersion");
        moreRecentVersionsQuery.setParameter("userVersion", versionId);
        try {
            moreRecentVersions = moreRecentVersionsQuery.getResultList();
            return moreRecentVersions;
        }
        catch (NoResultException e) {
            return moreRecentVersions;
        }
    }

    public static Version getByCode(String code){
        TypedQuery<Version> versionQuery = JPA.em().createQuery("select v from Version v where v.code = :code", Version.class);
        versionQuery.setParameter("code", code);
        Version version;
        try {
            version = versionQuery.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
        return version;
    }
}
