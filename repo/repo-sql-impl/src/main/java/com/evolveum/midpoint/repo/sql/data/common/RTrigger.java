package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.id.RTriggerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

@JaxbType(type = TriggerType.class)
@Entity
@IdClass(RTriggerId.class)
@org.hibernate.annotations.Table(appliesTo = "m_trigger",
        indexes = {@Index(name = "iTimestamp", columnNames = "timestamp")})
public class RTrigger implements Serializable {

    public static final String F_OWNER = "owner";

    //owner
    private RContainer owner;
    private String ownerOid;
    private Long ownerId;

    private String handlerUri;
    private XMLGregorianCalendar timestamp;


    @ForeignKey(name = "fk_trigger_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RContainer getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Column(nullable = true)
    public String getHandlerUri() {
        return handlerUri;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    public void setHandlerUri(String handlerUri) {
        this.handlerUri = handlerUri;
    }

    public void setOwner(RContainer owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTrigger that = (RTrigger) o;

        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) :
                that.handlerUri != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) :
                that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = handlerUri != null ? handlerUri.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static RTrigger copyFromJAXB(RContainer owner, TriggerType jaxb) {
        Validate.notNull(jaxb, "JAXB object must not be null.");

        RTrigger repo = new RTrigger();

        repo.setTimestamp(jaxb.getTimestamp());
        repo.setHandlerUri(jaxb.getHandlerUri());
        repo.setOwner(owner);
        return repo;
    }

    public static TriggerType copyToJAXB(RTrigger repo) {
        Validate.notNull(repo, "Repo object must not be null.");

        TriggerType jaxb = new TriggerType();

        jaxb.setHandlerUri(repo.getHandlerUri());
        jaxb.setTimestamp(repo.getTimestamp());

        return jaxb;
    }
}
