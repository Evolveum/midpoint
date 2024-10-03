/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RCertWorkItemReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * @author lazyman
 * <p>
 * Reference contained in a certification work item.
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCertWorkItemReferenceId.class)
@Table(name = RCertWorkItemReference.TABLE, indexes = {
        @jakarta.persistence.Index(name = "iCertWorkItemRefTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCertWorkItemReference extends RReference {

    public static final String TABLE = "m_acc_cert_wi_reference";

    private RAccessCertificationWorkItem owner;
    private String ownerOwnerOwnerOid;                    // campaign OID
    private Integer ownerOwnerId;                        // case ID
    private Integer ownerId;                            // work item ID

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumns(
            value = {
                    @JoinColumn(name = "owner_owner_owner_oid", referencedColumnName = "owner_owner_oid"),
                    @JoinColumn(name = "owner_owner_id", referencedColumnName = "owner_id"),
                    @JoinColumn(name = "owner_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(name = "fk_acc_cert_wi_ref_owner")
    )
    public RAccessCertificationWorkItem getOwner() {
        return owner;
    }

    public void setOwner(RAccessCertificationWorkItem owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerOwnerOwnerOid = owner.getOwnerOwnerOid();
            this.ownerOwnerId = owner.getOwnerId();
            this.ownerId = owner.getId();
        }
    }

    @Id
    @Column(name = "owner_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOwnerOwnerOid() {
        return ownerOwnerOwnerOid;
    }

    public void setOwnerOwnerOwnerOid(String ownerOwnerOwnerOid) {
        this.ownerOwnerOwnerOid = ownerOwnerOwnerOid;
    }

    @Id
    @Column(name = "owner_owner_id", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public Integer getOwnerOwnerId() {
        return ownerOwnerId;
    }

    public void setOwnerOwnerId(Integer ownerOwnerId) {
        this.ownerOwnerId = ownerOwnerId;
    }

    @Id
    @Column(name = "owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    @org.hibernate.annotations.ForeignKey(name = "none")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false)
    // commented because of The NotFoundAction.IGNORE @ManyToOne and @OneToOne associations are always fetched eagerly. (HHH-12770)
//    @NotFound(action = NotFoundAction.IGNORE)
    @NotQueryable
    // only for HQL use
    public RObject getTarget() {
        return null;
    }

    @Id
    @Column(name = "targetOid", length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Id
    @Column(name = "relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return super.getRelation();
    }

    @JdbcType(IntegerJdbcType.class)
    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getTargetType() {
        return super.getTargetType();
    }

    public static Set<RCertWorkItemReference> safeListReferenceToSet(List<ObjectReferenceType> list,
            RAccessCertificationWorkItem owner, RelationRegistry relationRegistry) {
        Set<RCertWorkItemReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RCertWorkItemReference rRef = jaxbRefToRepo(ref, owner, relationRegistry);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RCertWorkItemReference jaxbRefToRepo(ObjectReferenceType reference,
            RAccessCertificationWorkItem owner, RelationRegistry relationRegistry) {
        if (reference == null) {
            return null;
        }
        Objects.requireNonNull(owner, "Owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCertWorkItemReference repoRef = new RCertWorkItemReference();
        repoRef.setOwner(owner);
        RCertWorkItemReference.fromJaxb(reference, repoRef, relationRegistry);
        return repoRef;
    }
}
