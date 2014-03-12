package com.evolveum.midpoint.repo.sql.data.poc;

import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@IdClass(RACId.class)
@Entity
public class RAA implements Serializable {

    private RAO owner;
    private String ownerOid;

    private Short id;

    @Id
    @ForeignKey(name = "fk_raa_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    public RAO getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    public String getOwnerOid() {
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    public Short getId() {
        return id;
    }

    public void setOwner(RAO owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Short id) {
        this.id = id;
    }

    private REmbeddedReference targetRef;

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }
}
