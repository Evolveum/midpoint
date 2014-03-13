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
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RCreateApproverRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RModifyApproverRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_object")
@org.hibernate.annotations.Table(appliesTo = "m_object",
        indexes = {@Index(name = "iObjectNameOrig", columnNames = "name_orig"),
                @Index(name = "iObjectNameNorm", columnNames = "name_norm")})
@ForeignKey(name = "fk_object")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class RObject<T extends ObjectType> implements Metadata<RObjectReference>, Serializable {

    private String oid;
    private int version;
    //full XML
    private String fullObject;
    //org. closure table
    private Set<ROrgClosure> descendants;
    private Set<ROrgClosure> ancestors;
    //ObjectType
    private RPolyString name;
    private RAnyContainer extension;
    private Set<RObjectReference> parentOrgRef;
    private Set<RTrigger> trigger;
    private REmbeddedReference tenantRef;
    //Metadata
    private XMLGregorianCalendar createTimestamp;
    private REmbeddedReference creatorRef;
    private Set<RObjectReference> createApproverRef;
    private String createChannel;
    private XMLGregorianCalendar modifyTimestamp;
    private REmbeddedReference modifierRef;
    private Set<RObjectReference> modifyApproverRef;
    private String modifyChannel;

    @Id
    @GeneratedValue(generator = "ContainerOidGenerator")
    @GenericGenerator(name = "ContainerOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ObjectOidGenerator")
    @Column(name = "oid", nullable = false, updatable = false, length = RUtil.COLUMN_LENGTH_OID)
    public String getOid() {
        return oid;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    @ForeignKey(name = "none")
    @OneToMany(mappedBy = RTrigger.F_OWNER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RTrigger> getTrigger() {
        if (trigger == null) {
            trigger = new HashSet<>();
        }
        return trigger;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RParentOrgRef.DISCRIMINATOR)
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getParentOrgRef() {
        if (parentOrgRef == null) {
            parentOrgRef = new HashSet<>();
        }
        return parentOrgRef;
    }

    @com.evolveum.midpoint.repo.sql.query.definition.Any(jaxbNameLocalPart = "extension")
    @OneToOne(optional = true, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns({@JoinColumn(name = "extOid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "extType", referencedColumnName = "owner_type")})
    public RAnyContainer getExtension() {
        return extension;
    }

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "descendant")
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getDescendants() {
        return descendants;
    }

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "ancestor")//, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getAncestors() {
        return ancestors;
    }

    public int getVersion() {
        return version;
    }

    @Embedded
    public REmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getFullObject() {
        return fullObject;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RCreateApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getCreateApproverRef() {
        if (createApproverRef == null) {
            createApproverRef = new HashSet<RObjectReference>();
        }
        return createApproverRef;
    }

    public String getCreateChannel() {
        return createChannel;
    }

    public XMLGregorianCalendar getCreateTimestamp() {
        return createTimestamp;
    }

    @Embedded
    public REmbeddedReference getCreatorRef() {
        return creatorRef;
    }

    @Embedded
    public REmbeddedReference getModifierRef() {
        return modifierRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RModifyApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getModifyApproverRef() {
        if (modifyApproverRef == null) {
            modifyApproverRef = new HashSet<RObjectReference>();
        }
        return modifyApproverRef;
    }

    public String getModifyChannel() {
        return modifyChannel;
    }

    public XMLGregorianCalendar getModifyTimestamp() {
        return modifyTimestamp;
    }

    public void setCreateApproverRef(Set<RObjectReference> createApproverRef) {
        this.createApproverRef = createApproverRef;
    }

    public void setCreateChannel(String createChannel) {
        this.createChannel = createChannel;
    }

    public void setCreateTimestamp(XMLGregorianCalendar createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public void setCreatorRef(REmbeddedReference creatorRef) {
        this.creatorRef = creatorRef;
    }

    public void setModifierRef(REmbeddedReference modifierRef) {
        this.modifierRef = modifierRef;
    }

    public void setModifyApproverRef(Set<RObjectReference> modifyApproverRef) {
        this.modifyApproverRef = modifyApproverRef;
    }

    public void setModifyChannel(String modifyChannel) {
        this.modifyChannel = modifyChannel;
    }

    public void setModifyTimestamp(XMLGregorianCalendar modifyTimestamp) {
        this.modifyTimestamp = modifyTimestamp;
    }

    public void setFullObject(String fullObject) {
        this.fullObject = fullObject;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setTenantRef(REmbeddedReference tenantRef) {
        this.tenantRef = tenantRef;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setTrigger(Set<RTrigger> trigger) {
        this.trigger = trigger;
    }

    public void setDescendants(Set<ROrgClosure> descendants) {
        this.descendants = descendants;
    }

    public void setAncestors(Set<ROrgClosure> ancestors) {
        this.ancestors = ancestors;
    }

    public void setExtension(RAnyContainer extension) {
        this.extension = extension;
        if (this.extension != null) {                 //todo fix
//            this.extension.setOwnerType(RContainerType.OBJECT);
        }
    }

    public void setParentOrgRef(Set<RObjectReference> parentOrgRef) {
        this.parentOrgRef = parentOrgRef;
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

        if (name != null ? !name.equals(rObject.name) : rObject.name != null)
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
        if (!MetadataFactory.equals(this, rObject)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);

        result = 31 * result + (createTimestamp != null ? createTimestamp.hashCode() : 0);
        result = 31 * result + (creatorRef != null ? creatorRef.hashCode() : 0);
        result = 31 * result + (createChannel != null ? createChannel.hashCode() : 0);
        result = 31 * result + (modifyTimestamp != null ? modifyTimestamp.hashCode() : 0);
        result = 31 * result + (modifierRef != null ? modifierRef.hashCode() : 0);
        result = 31 * result + (modifyChannel != null ? modifyChannel.hashCode() : 0);

        return result;
    }

    public static <T extends ObjectType> void copyToJAXB(RObject<T> repo, ObjectType jaxb, PrismContext prismContext,
                                                         Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setOid(repo.getOid());
        jaxb.setVersion(Integer.toString(repo.getVersion()));

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
            MetadataType metadata = MetadataFactory.toJAXB(repo, prismContext);
            jaxb.setMetadata(metadata);
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

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setOid(jaxb.getOid());

        String strVersion = jaxb.getVersion();
        int version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*") ? Integer.parseInt(jaxb
                .getVersion()) : 0;
        repo.setVersion(version);

        //todo fix
//        if (jaxb.getExtension() != null) {
//            RAnyContainer extension = new RAnyContainer();
//            extension.setOwner(repo);
//
//            repo.setExtension(extension);
//            RAnyContainer.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
//        }

        repo.getParentOrgRef().addAll(RUtil.safeListReferenceToSet(jaxb.getParentOrgRef(), prismContext,
                repo, RReferenceOwner.OBJECT_PARENT_ORG));

        for (TriggerType trigger : jaxb.getTrigger()) {
            RTrigger rTrigger = new RTrigger(repo);
            RTrigger.copyFromJAXB(trigger, rTrigger, jaxb, prismContext);

            repo.getTrigger().add(rTrigger);
        }

        //todo fix
//        if (jaxb.getMetadata() != null) {
//            RMetadata metadata = new RMetadata();
//            metadata.setOwner(repo);
//            RMetadata.copyFromJAXB(jaxb.getMetadata(), metadata, prismContext);
//            repo.setMetadata(metadata);
//        }

        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), prismContext));
    }

    public abstract T toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException;

    @Override
    public String toString() {
        return RUtil.getDebugString(this);
    }
}
