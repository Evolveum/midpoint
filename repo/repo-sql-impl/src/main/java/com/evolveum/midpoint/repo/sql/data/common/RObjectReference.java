/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import static com.evolveum.midpoint.repo.sql.util.RUtil.qnameToString;

import java.util.Objects;
import jakarta.persistence.*;

import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.id.RObjectReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * @author lazyman
 */
@JaxbType(type = ObjectReferenceType.class)
@Entity
@IdClass(RObjectReferenceId.class)
@Table(name = "m_reference", indexes = {
        @Index(name = "iReferenceTargetTypeRelation", columnList = "targetOid, reference_type, relation")
})
@Persister(impl = MidPointSingleTablePersister.class)
public class RObjectReference<T extends RObject> implements ObjectReference, EntityState {

    public static final String REFERENCE_TYPE = "reference_type";

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RReferenceType referenceType;

    //owner
    private RObject owner;
    private String ownerOid;

    //other primary key fields
    private String targetOid;
    private String relation;
    private RObjectType targetType;

    private T target;

    public RObjectReference() {
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @JoinColumn(name = "owner_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_reference_owner"))
    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }


    @ManyToOne(targetEntity = RObject.class, fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    // commented because of The NotFoundAction.IGNORE @ManyToOne and @OneToOne associations are always fetched eagerly. (HHH-12770)
//    @NotFound(action = NotFoundAction.IGNORE)
    @NotQueryable
    public T getTarget() {
        return target;
    }

    @Id
    @Column(name = "targetOid", length = RUtil.COLUMN_LENGTH_OID)
    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Id
    @Column(name = "relation", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getRelation() {
        return relation;
    }

    @Id
    @JdbcType(IntegerJdbcType.class)
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.UserType} represented
     * as enum {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType#USER}
     *
     * @return null if not defined, otherwise value from {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType} enum
     */
    @JdbcType(IntegerJdbcType.class)
    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getTargetType() {
        return targetType;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public void setRelation(String relation) {
        this.relation = relation;
    }

    // only for ORM/JPA, shouldn't be called
    public void setTarget(T target) {
        this.target = target;
    }

    @Override
    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    @Override
    public void setTargetType(RObjectType type) {
        this.targetType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RObjectReference)) { return false; }
        RObjectReference<?> that = (RObjectReference<?>) o;
        return referenceType == that.referenceType &&
                Objects.equals(targetOid, that.targetOid) &&
                Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceType, targetOid, relation);
    }

    @Override
    public String toString() {
        return "RObjectReference{" + "targetOid='" + targetOid + '\''
                + ", relation='" + relation + '\''
                + ", type=" + targetType
                + '}';
    }

    public static void copyToJAXB(RObjectReference<?> repoObject, ObjectReferenceType jaxbObject) {
        Objects.requireNonNull(repoObject, "Repo object must not be null.");
        Objects.requireNonNull(jaxbObject, "JAXB object must not be null.");

        jaxbObject.setType(ClassMapper.getQNameForHQLType(repoObject.getTargetType()));
        jaxbObject.setOid(repoObject.getTargetOid());
        jaxbObject.setRelation(RUtil.stringToQName(repoObject.getRelation()));
    }

    public static ObjectReference copyFromJAXB(
            ObjectReferenceType jaxbObject, ObjectReference repoObject, RelationRegistry relationRegistry) {
        Objects.requireNonNull(repoObject, "Repo object must not be null.");
        Objects.requireNonNull(jaxbObject, "JAXB object must not be null.");
        Validate.notEmpty(jaxbObject.getOid(), "Target oid must not be null.");

        repoObject.setTargetType(ClassMapper.getHQLTypeForQName(jaxbObject.getType()));
        repoObject.setRelation(qnameToString(relationRegistry.normalizeRelation(jaxbObject.getRelation())));
        repoObject.setTargetOid(jaxbObject.getOid());

        return repoObject;
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref);

        return ref;
    }
}
