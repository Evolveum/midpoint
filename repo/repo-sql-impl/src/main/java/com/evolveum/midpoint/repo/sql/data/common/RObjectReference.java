/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import static com.evolveum.midpoint.repo.sql.util.RUtil.qnameToString;

import java.util.Objects;
import javax.persistence.*;

import org.apache.commons.lang.Validate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Persister;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.id.RObjectReferenceId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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

    private RReferenceOwner referenceType;

    //owner
    private RObject owner;
    private String ownerOid;

    //other primary key fields
    private String targetOid;
    private String relation;
    private RObjectType type;

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

    @JoinColumn(foreignKey = @ForeignKey(name = "fk_reference_owner"))
    @MapsId("owner")
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RObject.class)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
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
    @Column(name = REFERENCE_TYPE, nullable = false)
    public RReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RReferenceOwner referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * Represents {@link javax.xml.namespace.QName} type attribute in reference e.g.
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_3.UserType} represented
     * as enum {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType#USER}
     *
     * @return null if not defined, otherwise value from {@link com.evolveum.midpoint.repo.sql.data.common.other.RObjectType} enum
     */
    @Column(name = "targetType")
    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getType() {
        return type;
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
    public void setType(RObjectType type) {
        this.type = type;
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
        final StringBuilder sb = new StringBuilder("RObjectReference{");
        sb.append("targetOid='").append(targetOid).append('\'');
        sb.append(", relation='").append(relation).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }

    public static void copyToJAXB(RObjectReference repo, ObjectReferenceType jaxb) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(RUtil.stringToQName(repo.getRelation()));
    }

    public static ObjectReference copyFromJAXB(ObjectReferenceType jaxb, ObjectReference repo, RelationRegistry relationRegistry) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");

        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
        repo.setRelation(qnameToString(relationRegistry.normalizeRelation(jaxb.getRelation())));
        repo.setTargetOid(jaxb.getOid());

        return repo;
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref);

        return ref;
    }
}
