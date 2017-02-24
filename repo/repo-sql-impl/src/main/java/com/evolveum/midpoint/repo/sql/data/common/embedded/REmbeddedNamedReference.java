/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Reference containing the name of referenced object.
 * Originally thought to be used in certifications, but replaced by dynamically joining RObject table.
 * So keeping this for possible future use only.
 *
 * @author mederly
 */
@Embeddable
public class REmbeddedNamedReference extends REmbeddedReference {

    private RPolyString targetName;

    public RPolyString getTargetName() {
        return targetName;
    }

    public void setTargetName(RPolyString targetName) {
        this.targetName = targetName;
    }

    // these three methods are repeated here because of some hibernate weirdness

    @Column(length = RUtil.COLUMN_LENGTH_QNAME)
    @Override
    public String getRelation() {
        return super.getRelation();
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID, insertable = true, updatable = true, nullable = true)
    @Override
    public String getTargetOid() {
        return super.getTargetOid();
    }

    @Enumerated(EnumType.ORDINAL)
    @Override
    public RObjectType getType() {
        return super.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof REmbeddedNamedReference)) return false;
        if (!super.equals(o)) return false;

        REmbeddedNamedReference that = (REmbeddedNamedReference) o;

        return !(targetName != null ? !targetName.equals(that.targetName) : that.targetName != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(REmbeddedNamedReference repo, ObjectReferenceType jaxb, PrismContext prismContext) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        REmbeddedReference.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setTargetName(RPolyString.copyToJAXB(repo.getTargetName()));
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, REmbeddedNamedReference repo) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notEmpty(jaxb.getOid(), "Target oid must not be null.");
        REmbeddedReference.copyFromJAXB(jaxb, repo);

        repo.setTargetName(RPolyString.copyFromJAXB(jaxb.getTargetName()));
    }

    public ObjectReferenceType toJAXB(PrismContext prismContext) {
        ObjectReferenceType ref = new ObjectReferenceType();
        copyToJAXB(this, ref, prismContext);
        return ref;
    }
}
