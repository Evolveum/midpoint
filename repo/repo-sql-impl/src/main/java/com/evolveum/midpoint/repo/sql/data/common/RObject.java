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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualProperty;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@QueryEntity(properties = {@VirtualProperty(jaxbName = @JaxbName(localPart = "name"), jaxbType = PolyString.class,
        jpaName = "name", jpaType = RPolyString.class)})
@Entity
@ForeignKey(name = "fk_object")
public abstract class RObject<T extends ObjectType> extends RContainer {//implements FieldHandled {

    private String description;
    private RAnyContainer extension;
    private long version;
    private Set<ROrgClosure> descendants;
    private Set<ROrgClosure> ancestors;
    private Set<RObjectReference> parentOrgRef;
    private Set<RTrigger> trigger;
    private RMetadata metadata;
    private REmbeddedReference tenantRef;

//    /**
//     * Used for lazy loading properties (entities)
//     */
//    private FieldHandler fieldHandler;
//
//    public FieldHandler getFieldHandler() {
//        return fieldHandler;
//    }
//
//    public void setFieldHandler(FieldHandler fieldHandler) {
//        this.fieldHandler = fieldHandler;
//    }

    @Transient
    public abstract RPolyString getName();

    public abstract void setName(RPolyString name);

//    @LazyToOne(LazyToOneOption.NO_PROXY)
    @OneToOne(mappedBy = RMetadata.F_OWNER, optional = true, orphanRemoval = true)//, fetch = FetchType.LAZY)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RMetadata getMetadata() {
//        if (fieldHandler != null) {
//            return (RMetadata) fieldHandler.readObject(this, "metadata", metadata);
//        }
        return metadata;
    }

    public void setMetadata(RMetadata metadata) {
//        if (fieldHandler != null) {
//            this.metadata = (RMetadata) fieldHandler.writeObject(this, "metadata", this.metadata, metadata);
//            return;
//        }
        this.metadata = metadata;
    }

    @ForeignKey(name = "none")
    @OneToMany(mappedBy = RTrigger.F_OWNER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RTrigger> getTrigger() {
        if (trigger == null) {
            trigger = new HashSet<RTrigger>();
        }
        return trigger;
    }

    public void setTrigger(Set<RTrigger> trigger) {
        this.trigger = trigger;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RParentOrgRef.DISCRIMINATOR)
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getParentOrgRef() {
        if (parentOrgRef == null) {
            parentOrgRef = new HashSet<RObjectReference>();
        }
        return parentOrgRef;
    }

    public void setParentOrgRef(Set<RObjectReference> parentOrgRef) {
        this.parentOrgRef = parentOrgRef;
    }

    @com.evolveum.midpoint.repo.sql.query.definition.Any(jaxbNameLocalPart = "extension")
    @OneToOne(optional = true, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns({@JoinColumn(name = "extOid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "extId", referencedColumnName = "owner_id"),
            @JoinColumn(name = "extType", referencedColumnName = "owner_type")})
    public RAnyContainer getExtension() {
        return extension;
    }

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "descendant")
//, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getDescendants() {
        return descendants;
    }

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "ancestor")//, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getAncestors() {
        return ancestors;
    }

    public void setDescendants(Set<ROrgClosure> descendants) {
        this.descendants = descendants;
    }

    public void setAncestors(Set<ROrgClosure> ancestors) {
        this.ancestors = ancestors;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExtension(RAnyContainer extension) {
        this.extension = extension;
        if (this.extension != null) {
            this.extension.setOwnerType(RContainerType.OBJECT);
        }
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
    
    @Embedded
    public REmbeddedReference getTenantRef() {
		return tenantRef;
	}

	public void setTenantRef(REmbeddedReference tenantRef) {
		this.tenantRef = tenantRef;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RObject rObject = (RObject) o;

        if (description != null ? !description.equals(rObject.description) : rObject.description != null)
            return false;
        if (extension != null ? !extension.equals(rObject.extension) : rObject.extension != null)
            return false;
        if (descendants != null ? !descendants.equals(rObject.descendants) : rObject.descendants != null)
            return false;
        if (ancestors != null ? !ancestors.equals(rObject.ancestors) : rObject.ancestors != null)
            return false;
        if (parentOrgRef != null ? !parentOrgRef.equals(rObject.parentOrgRef) : rObject.parentOrgRef != null)
            return false;
        if (trigger != null ? !trigger.equals(rObject.trigger) : rObject.trigger != null)
            return false;
        if (tenantRef != null ? !tenantRef.equals(rObject.tenantRef) : rObject.tenantRef != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    public static <T extends ObjectType> void copyToJAXB(RObject<T> repo, ObjectType jaxb, PrismContext prismContext,
                                  Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setOid(repo.getOid());
        jaxb.setVersion(Long.toString(repo.getVersion()));

        if (SelectorOptions.hasToLoadPath(ObjectType.F_EXTENSION, options)) {
            if (repo.getExtension() != null) {
                ExtensionType extension = new ExtensionType();
                jaxb.setExtension(extension);
                RAnyContainer.copyToJAXB(repo.getExtension(), extension, prismContext);
            }
        }

        if (SelectorOptions.hasToLoadPath(ObjectType.F_PARENT_ORG_REF, options)) {
            List orgRefs = RUtil.safeSetReferencesToList(repo.getParentOrgRef(), prismContext);
            if (!orgRefs.isEmpty()) {
                jaxb.getParentOrgRef().addAll(orgRefs);
            }
        }

        if (SelectorOptions.hasToLoadPath(ObjectType.F_TRIGGER, options)) {
            if (repo.getTrigger() != null) {
                for (RTrigger trigger : repo.getTrigger()) {
                    jaxb.getTrigger().add(trigger.toJAXB(prismContext));
                }
            }
        }

        if (SelectorOptions.hasToLoadPath(ObjectType.F_METADATA, options)) {
            if (repo.getMetadata() != null) {
                jaxb.setMetadata(repo.getMetadata().toJAXB(prismContext));
            }
        }
        
        if (SelectorOptions.hasToLoadPath(ObjectType.F_TENANT_REF, options)) {
        	if (repo.getTenantRef() != null) {
        		jaxb.setTenantRef(repo.getTenantRef().toJAXB(prismContext));
        	}
        }
    }

    public static <T extends ObjectType> void copyFromJAXB(ObjectType jaxb, RObject<T> repo, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setOid(jaxb.getOid());
        repo.setId(0L); // objects types have default id

        String strVersion = jaxb.getVersion();
        long version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*") ? Long.parseLong(jaxb
                .getVersion()) : 0;
        repo.setVersion(version);

        if (jaxb.getExtension() != null) {
            RAnyContainer extension = new RAnyContainer();
            extension.setOwner(repo);

            repo.setExtension(extension);
            RAnyContainer.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
        }

        repo.getParentOrgRef().addAll(RUtil.safeListReferenceToSet(jaxb.getParentOrgRef(), prismContext,
                repo, RReferenceOwner.OBJECT_PARENT_ORG));

        for (TriggerType trigger : jaxb.getTrigger()) {
            RTrigger rTrigger = new RTrigger(repo);
            RTrigger.copyFromJAXB(trigger, rTrigger, jaxb, prismContext);

            repo.getTrigger().add(rTrigger);
        }

        if (jaxb.getMetadata() != null) {
            RMetadata metadata = new RMetadata();
            metadata.setOwner(repo);
            RMetadata.copyFromJAXB(jaxb.getMetadata(), metadata, prismContext);
            repo.setMetadata(metadata);
        }
        
        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), prismContext));
    }

    public abstract T toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException;
}
