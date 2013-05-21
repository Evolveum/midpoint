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
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.id.RMetadataId;
import com.evolveum.midpoint.repo.sql.data.common.type.RCreateApproverRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RModifyApproverRef;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MetadataType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@IdClass(RMetadataId.class)
public class RMetadata implements Serializable {

    public static final String F_OWNER = "owner";

    //owner
    private RContainer owner;
    private String ownerOid;
    private Long ownerId;

    private XMLGregorianCalendar createTimestamp;
    private REmbeddedReference creatorRef;
    private Set<RObjectReference> createApproverRef;
    private String createChannel;
    private XMLGregorianCalendar modifyTimestamp;
    private REmbeddedReference modifierRef;
    private Set<RObjectReference> modifyApproverRef;
    private String modifyChannel;

    @ForeignKey(name = "fk_metadata_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RContainer getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
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

    public void setOwner(RContainer owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RMetadata rMetadata = (RMetadata) o;

        if (createApproverRef != null ? !createApproverRef.equals(rMetadata.createApproverRef) : rMetadata.createApproverRef != null)
            return false;
        if (createChannel != null ? !createChannel.equals(rMetadata.createChannel) : rMetadata.createChannel != null)
            return false;
        if (createTimestamp != null ? !createTimestamp.equals(rMetadata.createTimestamp) : rMetadata.createTimestamp != null)
            return false;
        if (creatorRef != null ? !creatorRef.equals(rMetadata.creatorRef) : rMetadata.creatorRef != null) return false;
        if (modifierRef != null ? !modifierRef.equals(rMetadata.modifierRef) : rMetadata.modifierRef != null)
            return false;
        if (modifyApproverRef != null ? !modifyApproverRef.equals(rMetadata.modifyApproverRef) : rMetadata.modifyApproverRef != null)
            return false;
        if (modifyChannel != null ? !modifyChannel.equals(rMetadata.modifyChannel) : rMetadata.modifyChannel != null)
            return false;
        if (modifyTimestamp != null ? !modifyTimestamp.equals(rMetadata.modifyTimestamp) : rMetadata.modifyTimestamp != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = createTimestamp != null ? createTimestamp.hashCode() : 0;
        result = 31 * result + (creatorRef != null ? creatorRef.hashCode() : 0);
        result = 31 * result + (createChannel != null ? createChannel.hashCode() : 0);
        result = 31 * result + (modifyTimestamp != null ? modifyTimestamp.hashCode() : 0);
        result = 31 * result + (modifierRef != null ? modifierRef.hashCode() : 0);
        result = 31 * result + (modifyChannel != null ? modifyChannel.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RMetadata repo, MetadataType jaxb, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo metadata must not be null.");
        Validate.notNull(jaxb, "Jaxb metadata must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        jaxb.setCreateChannel(repo.getCreateChannel());
        jaxb.setCreateTimestamp(repo.getCreateTimestamp());
        jaxb.setModifyChannel(repo.getModifyChannel());
        jaxb.setModifyTimestamp(repo.getModifyTimestamp());

        if (repo.getCreatorRef() != null) {
            jaxb.setCreatorRef(repo.getCreatorRef().toJAXB(prismContext));
        }
        if (repo.getModifierRef() != null) {
            jaxb.setModifierRef(repo.getModifierRef().toJAXB(prismContext));
        }

        List refs = RUtil.safeSetReferencesToList(repo.getCreateApproverRef(), prismContext);
        if (!refs.isEmpty()) {
            jaxb.getCreateApproverRef().addAll(refs);
        }
        refs = RUtil.safeSetReferencesToList(repo.getModifyApproverRef(), prismContext);
        if (!refs.isEmpty()) {
            jaxb.getModifyApproverRef().addAll(refs);
        }
    }

    public static void copyFromJAXB(MetadataType jaxb, RMetadata repo, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo metadata must not be null.");
        Validate.notNull(jaxb, "Jaxb metadata must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        repo.setCreateChannel(jaxb.getCreateChannel());
        repo.setCreateTimestamp(jaxb.getCreateTimestamp());
        repo.setModifyChannel(jaxb.getModifyChannel());
        repo.setModifyTimestamp(jaxb.getModifyTimestamp());

        repo.setCreatorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getCreatorRef(), prismContext));
        repo.setModifierRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getModifierRef(), prismContext));

        repo.getCreateApproverRef().addAll(RUtil.safeListReferenceToSet(jaxb.getCreateApproverRef(), prismContext,
                repo.owner, RReferenceOwner.CREATE_APPROVER));
        repo.getModifyApproverRef().addAll(RUtil.safeListReferenceToSet(jaxb.getModifyApproverRef(), prismContext,
                repo.owner, RReferenceOwner.MODIFY_APPROVER));
    }

    public MetadataType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        MetadataType object = new MetadataType();
        RMetadata.copyToJAXB(this, object, prismContext);

        return object;
    }
}
