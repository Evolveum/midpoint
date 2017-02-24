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
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RObjectTemplateType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_object_template")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_object_template_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
public class RObjectTemplate extends RObject<ObjectTemplateType> {

    private RPolyString name;
    private RObjectTemplateType type;
    private Set<RObjectReference<RObjectTemplate>> includeRef;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 7")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference<RObjectTemplate>> getIncludeRef() {
        if (includeRef == null) {
            includeRef = new HashSet<>();
        }
        return includeRef;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectTemplateType getType() {
        return type;
    }

    public void setType(RObjectTemplateType type) {
        this.type = type;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setIncludeRef(Set<RObjectReference<RObjectTemplate>> includeRef) {
        this.includeRef = includeRef;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RObjectTemplate that = (RObjectTemplate) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null)
            return false;
        if (includeRef != null ? !includeRef.equals(that.includeRef) : that.includeRef != null)
            return false;


        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(ObjectTemplateType jaxb, RObjectTemplate repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setType(RUtil.getRepoEnumValue(jaxb.asPrismObject().getElementName(), RObjectTemplateType.class));
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));

        repo.getIncludeRef().addAll(RUtil.safeListReferenceToSet(
                jaxb.getIncludeRef(), repositoryContext.prismContext, repo, RReferenceOwner.INCLUDE));
    }

    @Override
    public ObjectTemplateType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        ObjectTemplateType object = new ObjectTemplateType();
        RUtil.revive(object, prismContext);
        RObjectTemplate.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
