/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

public abstract class RReference implements ObjectReference {

    //other primary key fields
    private String targetOid;
    private String relation;
    private RObjectType targetType;

    // for HQL use only
    public RObject getTarget() {
        return null;
    }

    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Override
    public String getRelation() {
        return relation;
    }

    @Enumerated
    @JdbcType(IntegerJdbcType.class)
    @Override
    public RObjectType getTargetType() {
        return targetType;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    // only for ORM/JPA, shouldn't be called
    public void setTarget(@SuppressWarnings("unused") RObject target) {
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setTargetType(RObjectType type) {
        this.targetType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RReference ref = (RReference) o;
        return Objects.equals(targetOid, ref.targetOid)
                && Objects.equals(targetType, ref.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetOid, targetType);
    }

    public static void fromJaxb(ObjectReferenceType jaxb, RReference repo, RelationRegistry relationRegistry) {
        RObjectReference.copyFromJAXB(jaxb, repo, relationRegistry);
    }
}
