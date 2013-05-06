/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_object_template")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
public class RObjectTemplate extends RObject {

    private RPolyString name;
    private String propertyConstruction;
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
    public String getPropertyConstruction() {
        return propertyConstruction;
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

    public void setPropertyConstruction(String propertyConstruction) {
        this.propertyConstruction = propertyConstruction;
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
        if (propertyConstruction != null ? !propertyConstruction.equals(that.propertyConstruction) : that.propertyConstruction != null)
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
        result = 31 * result + (propertyConstruction != null ? propertyConstruction.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RObjectTemplate repo, ObjectTemplateType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

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

            if (StringUtils.isNotEmpty(repo.getPropertyConstruction())) {
                ObjectTemplateType holder = RUtil.toJAXB(repo.getPropertyConstruction(), ObjectTemplateType.class, prismContext);
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
                repo.setPropertyConstruction(RUtil.toRepo(template, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ObjectTemplateType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ObjectTemplateType object = new ObjectTemplateType();
        RUtil.revive(object, prismContext);
        RObjectTemplate.copyToJAXB(this, object, prismContext);

        return object;
    }
}
