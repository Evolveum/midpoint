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
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RObjectTemplateType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RIncludeRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_object_template")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_object_template",
        indexes = {@Index(name = "iObjectTemplate", columnNames = "name_orig")})
public class RObjectTemplate extends RObject<ObjectTemplateType> {

    private RPolyString name;
    private String mapping;
    private String accountConstruction;
    private RObjectTemplateType type;
    private Set<RObjectReference> includeRef;

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RIncludeRef.DISCRIMINATOR)
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getIncludeRef() {
        if (includeRef == null) {
            includeRef = new HashSet<RObjectReference>();
        }
        return includeRef;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectTemplateType getType() {
        return type;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Column(nullable = true)
    public String getAccountConstruction() {
        return accountConstruction;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Column(nullable = true)
    public String getMapping() {
        return mapping;
    }

    public void setType(RObjectTemplateType type) {
        this.type = type;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setIncludeRef(Set<RObjectReference> includeRef) {
        this.includeRef = includeRef;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setAccountConstruction(String accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RObjectTemplate that = (RObjectTemplate) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (accountConstruction != null ? !accountConstruction.equals(that.accountConstruction) : that.accountConstruction != null)
            return false;
        if (mapping != null ? !mapping.equals(that.mapping) : that.mapping != null)
            return false;
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
        result = 31 * result + (mapping != null ? mapping.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RObjectTemplate repo, ObjectTemplateType jaxb, PrismContext prismContext,
                                  Collection<SelectorOptions<GetOperationOptions>> options) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext, options);

        //set name c:userTemplate or c:objectTemplate
        jaxb.asPrismObject().setName(repo.getType().getSchemaValue());

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));

        List includeRef = RUtil.safeSetReferencesToList(repo.getIncludeRef(), prismContext);
        if (!includeRef.isEmpty()) {
            jaxb.getIncludeRef().addAll(includeRef);
        }

        try {
            if (StringUtils.isNotEmpty(repo.getAccountConstruction())) {
                ObjectTemplateType holder = RUtil.toJAXB(repo.getAccountConstruction(), ObjectTemplateType.class, prismContext);
                jaxb.getAccountConstruction().addAll(holder.getAccountConstruction());
            }

            if (StringUtils.isNotEmpty(repo.getMapping())) {
                ObjectTemplateType holder = RUtil.toJAXB(repo.getMapping(), ObjectTemplateType.class, prismContext);
                jaxb.getMapping().addAll(holder.getMapping());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ObjectTemplateType jaxb, RObjectTemplate repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setType(RUtil.getRepoEnumValue(jaxb.asPrismObject().getName(), RObjectTemplateType.class));
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));

        repo.getIncludeRef().addAll(RUtil.safeListReferenceToSet(
                jaxb.getIncludeRef(), prismContext, repo, RReferenceOwner.INCLUDE));
        try {
            if (!jaxb.getAccountConstruction().isEmpty()) {
                ObjectTemplateType template = new ObjectTemplateType();
                // template needs name for serialization, in here it doesn't matter if it's objectTemplate
                // or userTemplate, it's only wrapper for data
                template.asPrismObject().setName(SchemaConstantsGenerated.C_OBJECT_TEMPLATE);

                template.getAccountConstruction().addAll(jaxb.getAccountConstruction());
                repo.setAccountConstruction(RUtil.toRepo(template, prismContext));
            }

            if (!jaxb.getMapping().isEmpty()) {
                ObjectTemplateType template = new ObjectTemplateType();
                // template needs name for serialization, in here it doesn't matter if it's objectTemplate
                // or userTemplate, it's only wrapper for data
                template.asPrismObject().setName(SchemaConstantsGenerated.C_OBJECT_TEMPLATE);

                template.getMapping().addAll(jaxb.getMapping());
                repo.setMapping(RUtil.toRepo(template, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
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
