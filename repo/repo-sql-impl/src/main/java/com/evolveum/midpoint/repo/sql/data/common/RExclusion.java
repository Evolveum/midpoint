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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExclusionPolicy;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExclusionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author lazyman
 */
@JaxbType(type = ExclusionType.class)
@Entity
@ForeignKey(name = "fk_exclusion")
public class RExclusion extends RContainer {

    //exclusion
    private REmbeddedReference targetRef;
    private RExclusionPolicy policy;

    public RExclusion() {
        this(null);
    }

    public RExclusion(RObject owner) {
        setOwner(owner);
    }

    @Enumerated(EnumType.ORDINAL)
    public RExclusionPolicy getPolicy() {
        return policy;
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    public void setPolicy(RExclusionPolicy policy) {
        this.policy = policy;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RExclusion that = (RExclusion) o;

        if (policy != that.policy) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (policy != null ? policy.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RExclusion repo, ExclusionType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setId(RUtil.toLong(repo.getId()));
        if (repo.getPolicy() != null) {
            jaxb.setPolicy(repo.getPolicy().getSchemaValue());
        }

        if (repo.getTargetRef() != null) {
            jaxb.setTargetRef(repo.getTargetRef().toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(ExclusionType jaxb, RExclusion repo, ObjectType parent, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setOwnerOid(parent.getOid());
        repo.setId(RUtil.toShort(jaxb.getId()));

        repo.setPolicy(RUtil.getRepoEnumValue(jaxb.getPolicy(), RExclusionPolicy.class));
        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), prismContext));
    }

    public ExclusionType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ExclusionType object = new ExclusionType();
        RExclusion.copyToJAXB(this, object, prismContext);
        return object;
    }
}
