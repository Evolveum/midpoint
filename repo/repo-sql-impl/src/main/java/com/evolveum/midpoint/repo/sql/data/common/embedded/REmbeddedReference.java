/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.embedded;

import static com.evolveum.midpoint.repo.sql.util.RUtil.*;

import java.util.Objects;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * This class is a superclass for all embedded references.
 *
 * Previously this one was directly marked as {@link Embeddable}, however since it has child classes
 * marked with {@link Embeddable}, hibernate now expects discriminator column + custom handling for
 * such embeddable class hierarchy.
 * This is unnecessary for our use case, so we are marking this class as {@link MappedSuperclass} and
 * we created {@link RSimpleEmbeddedReference} as a direct embeddable subclass.
 *
 * @author lazyman
 */
@MappedSuperclass
public abstract class REmbeddedReference implements ObjectReference {

    //target
    private String targetOid;
    //other fields
    private RObjectType targetType;
    //relation qname
    private String relation;

    @Column(length = COLUMN_LENGTH_QNAME)
    @Override
    public String getRelation() {
        return relation;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotQueryable
    public RObject getTarget() {
        return null;
    }

    @Column(length = COLUMN_LENGTH_OID)
    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    @Override
    public RObjectType getTargetType() {
        return targetType;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setTargetType(RObjectType type) {
        this.targetType = type;
    }

    // only for ORM/JPA
    public void setTarget(@SuppressWarnings("unused") RObject target) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        REmbeddedReference that = (REmbeddedReference) o;
        return Objects.equals(targetOid, that.targetOid)
                && Objects.equals(targetType, that.targetType)
                && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        // TODO: before june 2020 targetOid field was missing here (but was in equals)
        return Objects.hash(targetOid, targetType, relation);
    }

    @Override
    public String toString() {
        return "REmbeddedReference{" + "targetOid='" + targetOid + '\''
                + ", type=" + targetType
                + ", relation='" + relation + '\''
                + '}';
    }

    public static void copyToJAXB(REmbeddedReference repo, ObjectReferenceType jaxb,
            @SuppressWarnings("unused") PrismContext prismContext) {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getTargetType()));
        jaxb.setRelation(stringToQName(repo.getRelation()));
        if (StringUtils.isNotEmpty(repo.getTargetOid())) {
            jaxb.setOid(repo.getTargetOid());
        }
    }

    public static REmbeddedReference fromJaxb(ObjectReferenceType jaxb, REmbeddedReference repo,
            RelationRegistry relationRegistry) {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        repo.setTargetType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
        repo.setRelation(qnameToString(relationRegistry.normalizeRelation(jaxb.getRelation())));
        repo.setTargetOid(jaxb.getOid());
        return repo;
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);
        return ref;
    }
}
