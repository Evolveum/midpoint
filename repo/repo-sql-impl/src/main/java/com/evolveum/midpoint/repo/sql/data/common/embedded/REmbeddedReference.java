/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import static com.evolveum.midpoint.repo.sql.util.RUtil.*;

import javax.persistence.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lazyman
 */
@Embeddable
public class REmbeddedReference implements ObjectReference {

    //target
    private String targetOid;
    //other fields
    private RObjectType type;
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
    @NotFound(action = NotFoundAction.IGNORE)
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
    @Override
    public RObjectType getType() {
        return type;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setType(RObjectType type) {
        this.type = type;
    }

    // only for ORM/JPA
    public void setTarget(RObject target) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        REmbeddedReference that = (REmbeddedReference) o;

        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null)
            return false;
        if (type != that.type) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (relation != null ? relation.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("REmbeddedReference{");
        sb.append("targetOid='").append(targetOid).append('\'');
        sb.append(", type=").append(type);
        sb.append(", relation='").append(relation).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void copyToJAXB(REmbeddedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setRelation(stringToQName(repo.getRelation()));
        if (StringUtils.isNotEmpty(repo.getTargetOid())) {
            jaxb.setOid(repo.getTargetOid());
        }
    }

    public static REmbeddedReference fromJaxb(ObjectReferenceType jaxb, REmbeddedReference repo,
            RelationRegistry relationRegistry) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
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
