/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.namespace.QName;

import static com.evolveum.midpoint.repo.sql.util.RUtil.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;

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

    //@MapsId("target")
    @ForeignKey(name="none")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(referencedColumnName = "oid", updatable = false, insertable = false, nullable = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @NotQueryable
    public RObject getTarget() {
        return null;
    }

    @Column(length = COLUMN_LENGTH_OID, insertable = true, updatable = true, nullable = true /*, insertable = false, updatable = false */)
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

    public static void copyFromJAXB(ObjectReferenceType jaxb, REmbeddedReference repo) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        if (jaxb.getFilter() == null) {
            Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");
        }
        repo.setType(ClassMapper.getHQLTypeForQName(jaxb.getType()));
        repo.setRelation(qnameToString(normalizeRelation(jaxb.getRelation())));
        repo.setTargetOid(jaxb.getOid());

    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);
        return ref;
    }
}
