/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RCObjectReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceType;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * @author lazyman
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RCObjectReferenceId.class)
@Table(name = "m_assignment_reference", indexes = {
        @jakarta.persistence.Index(name = "iAssignmentReferenceTargetOid", columnList = "targetOid")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RAssignmentReference extends RContainerReference {

    private RAssignment owner;

    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumns(
            value = {
                    @JoinColumn(name = "owner_owner_oid", referencedColumnName = "owner_oid"),
                    @JoinColumn(name = "owner_id", referencedColumnName = "id")
            },
            foreignKey = @ForeignKey(name = "fk_assignment_reference")
    )
    public RAssignment getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        return super.getOwnerOid();
    }

    @Id
    @Column(name = "owner_id")
    @NotQueryable
    public Integer getOwnerId() {
        return super.getOwnerId();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    // commented because of The NotFoundAction.IGNORE @ManyToOne and @OneToOne associations are always fetched eagerly. (HHH-12770)
//    @NotFound(action = NotFoundAction.IGNORE)
    @NotQueryable
    // declared for HQL use only
    public RObject getTarget() {
        return null;
    }

    @Id
    @Column(name = "targetOid", length = RUtil.COLUMN_LENGTH_OID)
    @Override
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Id
    @Column(name = "relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return super.getRelation();
    }

    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link UserType} represented as enum {@link RObjectType#USER}.
     *
     * @return null if not defined, otherwise value from {@link RObjectType} enum
     */
    @JdbcType(IntegerJdbcType.class)
    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getTargetType() {
        return super.getTargetType();
    }

    @Id
    @JdbcType(IntegerJdbcType.class)
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RCReferenceType getReferenceType() {
        return super.getReferenceType();
    }

    public void setOwner(RAssignment owner) {
        this.owner = owner;
    }
}
