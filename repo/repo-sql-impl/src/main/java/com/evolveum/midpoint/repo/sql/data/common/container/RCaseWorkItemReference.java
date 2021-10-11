/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RCaseWorkItemReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCaseWorkItemReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 * @author mederly
 *
 * Reference contained in a case work item.
 *
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

    @ForeignKey(name = "fk_case_wi_reference_owner")
    @MapsId("workItem")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
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

    //@MapsId("target")
    @ForeignKey(name="none")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false, nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
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
    @Column(name="relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return super.getRelation();
    }

    @Id
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RCaseWorkItemReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCaseWorkItemReferenceOwner referenceType) {
        this.referenceType = referenceType;
    }

    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getType() {
        return super.getType();
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
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RCaseWorkItemReference repoRef = new RCaseWorkItemReference();
        repoRef.setOwner(owner);
        RReference.fromJaxb(reference, repoRef, relationRegistry);
        repoRef.setReferenceType(type);
        return repoRef;
    }
}
