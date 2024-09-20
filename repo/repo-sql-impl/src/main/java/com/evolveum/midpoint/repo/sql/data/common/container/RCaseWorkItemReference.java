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
import com.evolveum.midpoint.repo.sql.data.common.id.RCaseWorkItemReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCaseWorkItemReferenceOwner;
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
 * Reference contained in a case work item.
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCaseWorkItemReferenceId.class)
@Table(name = RCaseWorkItemReference.TABLE, indexes = {
        @Index(name = "iCaseWorkItemRefTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RCaseWorkItemReference extends RReference {

    public static final String TABLE = "m_case_wi_reference";
    public static final String REFERENCE_TYPE = "reference_type";

    private RCaseWorkItem owner;
    private String ownerOwnerOid;                        // case OID
    private Integer ownerId;                            // work item ID
    private RCaseWorkItemReferenceOwner referenceType;

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumns(
            value = {
                    @JoinColumn(name = "owner_owner_oid", referencedColumnName = "owner_oid"),
                    @JoinColumn(name = "owner_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(name = "fk_case_wi_reference_owner"))
    public RCaseWorkItem getOwner() {
        return owner;
    }

    public void setOwner(RCaseWorkItem owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerOwnerOid = owner.getOwnerOid();
            this.ownerId = owner.getId();
        }
    }

    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOwnerOid() {
        if (ownerOwnerOid == null && getOwner() != null) {
            ownerOwnerOid = getOwner().getOwnerOid();
        }
        return ownerOwnerOid;
    }

    public void setOwnerOwnerOid(String ownerOwnerOid) {
        this.ownerOwnerOid = ownerOwnerOid;
    }

    @Column(name = "owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        if (ownerId == null && getOwner() != null) {
            ownerId = getOwner().getId();
        }
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            referencedColumnName = "oid",
            updatable = false,
            insertable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
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

    @Id
    @JdbcType(IntegerJdbcType.class)
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RCaseWorkItemReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCaseWorkItemReferenceOwner referenceType) {
        this.referenceType = referenceType;
    }

    @JdbcType(IntegerJdbcType.class)
    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getTargetType() {
        return super.getTargetType();
    }

    public static Set<RCaseWorkItemReference> safeListReferenceToSet(List<ObjectReferenceType> list,
            RCaseWorkItem owner, RelationRegistry relationRegistry,
            RCaseWorkItemReferenceOwner type) {
        Set<RCaseWorkItemReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RCaseWorkItemReference rRef = jaxbRefToRepo(ref, owner, relationRegistry, type);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RCaseWorkItemReference jaxbRefToRepo(ObjectReferenceType reference, RCaseWorkItem owner,
            RelationRegistry relationRegistry, RCaseWorkItemReferenceOwner type) {
        if (reference == null) {
            return null;
        }
        Objects.requireNonNull(owner, "Owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCaseWorkItemReference repoRef = new RCaseWorkItemReference();
        repoRef.setOwner(owner);
        RReference.fromJaxb(reference, repoRef, relationRegistry);
        repoRef.setReferenceType(type);
        return repoRef;
    }
}
