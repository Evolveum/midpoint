/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@JaxbType(type = AssignmentType.class)
@Entity
@IdClass(RContainerId.class)
@Table(name = "m_assignment", indexes = {
        @Index(name = "iAssignmentAdministrative", columnList = "administrativeStatus"),
        @Index(name = "iAssignmentEffective", columnList = "effectiveStatus"),
        @Index(name = "iAssignmentValidFrom", columnList = "validFrom"),
        @Index(name = "iAssignmentValidTo", columnList = "validTo"),
        @Index(name = "iTargetRefTargetOid", columnList = "targetRef_targetOid"),
        @Index(name = "iTenantRefTargetOid", columnList = "tenantRef_targetOid"),
        @Index(name = "iOrgRefTargetOid", columnList = "orgRef_targetOid"),
        @Index(name = "iResourceRefTargetOid", columnList = "resourceRef_targetOid") })
@Persister(impl = MidPointSingleTablePersister.class)
@DynamicUpdate
public class RAssignment implements Container<RObject>, Metadata<RAssignmentReference> {

    public static final String F_OWNER = "owner";

    private Boolean trans;

    private RObject owner;
    private String ownerOid;
    private Integer id;

    private RAssignmentOwner assignmentOwner;
    //extension
    private RAssignmentExtension extension;
    //assignment fields
    private RActivation activation;
    private REmbeddedReference targetRef;
    private Integer order;
    private REmbeddedReference tenantRef;
    private REmbeddedReference orgRef;
    private REmbeddedReference resourceRef;
    private String lifecycleState;
    private Set<String> policySituation;
    //metadata
    private XMLGregorianCalendar createTimestamp;
    private REmbeddedReference creatorRef;
    private Set<RAssignmentReference> createApproverRef;
    private String createChannel;
    private XMLGregorianCalendar modifyTimestamp;
    private REmbeddedReference modifierRef;
    private Set<RAssignmentReference> modifyApproverRef;
    private String modifyChannel;

    public RAssignment() {
        this(null, null);
    }

    public RAssignment(RObject owner, RAssignmentOwner assignmentOwner) {
        this.setOwner(owner);
        this.assignmentOwner = assignmentOwner;
    }

    @Override
    @JoinColumn(name = "owner_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_assignment_owner"))
    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    public RObject getOwner() {
        return owner;
    }

    @Id
    @Override
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Override
    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RAssignmentOwner getAssignmentOwner() {
        return assignmentOwner;
    }

    @Embedded
    public REmbeddedReference getTargetRef() {
        return targetRef;
    }

    @Embedded
    public REmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Embedded
    public REmbeddedReference getOrgRef() {
        return orgRef;
    }

    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "construction"), @JaxbName(localPart = "resourceRef") })
    public REmbeddedReference getResourceRef() {
        return resourceRef;
    }

    @com.evolveum.midpoint.repo.sql.query.definition.Any(jaxbNameLocalPart = "extension")
    @OneToOne(orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    @JoinColumns(
            value = {
                    @JoinColumn(name = "extOid", referencedColumnName = "owner_owner_oid"),
                    @JoinColumn(name = "extId", referencedColumnName = "owner_id")
            },
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    public RAssignmentExtension getExtension() {
        return extension;
    }

    @Embedded
    public RActivation getActivation() {
        return activation;
    }

    @Column(name = "orderValue")
    public Integer getOrder() {
        return order;
    }

    @Override
    @Where(clause = RAssignmentReference.REFERENCE_TYPE + "= 0")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAssignmentReference.F_OWNER, orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createApproverRef") })
    public Set<RAssignmentReference> getCreateApproverRef() {
        if (createApproverRef == null) {
            createApproverRef = new HashSet<>();
        }
        return createApproverRef;
    }

    @Override
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createChannel") })
    public String getCreateChannel() {
        return createChannel;
    }

    @Override
    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createTimestamp") })
    public XMLGregorianCalendar getCreateTimestamp() {
        return createTimestamp;
    }

    @Override
    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "creatorRef") })
    public REmbeddedReference getCreatorRef() {
        return creatorRef;
    }

    @Override
    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifierRef") })
    public REmbeddedReference getModifierRef() {
        return modifierRef;
    }

    @Override
    @Where(clause = RAssignmentReference.REFERENCE_TYPE + "= 1")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = RAssignmentReference.F_OWNER, orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyApproverRef") })
    public Set<RAssignmentReference> getModifyApproverRef() {
        if (modifyApproverRef == null) {
            modifyApproverRef = new HashSet<>();
        }
        return modifyApproverRef;
    }

    @Override
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyChannel") })
    public String getModifyChannel() {
        return modifyChannel;
    }

    @Override
    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyTimestamp") })
    public XMLGregorianCalendar getModifyTimestamp() {
        return modifyTimestamp;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    @ElementCollection
    @CollectionTable(name = "m_assignment_policy_situation",
            foreignKey = @ForeignKey(name = "fk_assignment_policy_situation"),
            joinColumns = {
                    @JoinColumn(name = "assignment_oid", referencedColumnName = "owner_oid"),
                    @JoinColumn(name = "assignment_id", referencedColumnName = "id")
            })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getPolicySituation() {
        return policySituation;
    }

    public void setPolicySituation(Set<String> policySituation) {
        this.policySituation = policySituation;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;

        if (!Boolean.TRUE.equals(trans)) {
            return;
        }

        if (getCreateApproverRef() != null) {
            for (RContainerReference ref : getCreateApproverRef()) {
                ref.setTransient(true);
            }
        }

        if (getModifyApproverRef() != null) {
            for (RContainerReference ref : getModifyApproverRef()) {
                ref.setTransient(true);
            }
        }
    }

    @Override
    public void setOwner(RObject owner) {
        this.owner = owner;
        if (owner != null) {
            setOwnerOid(owner.getOid());
        }
    }

    @Override
    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public void setCreateApproverRef(Set<RAssignmentReference> createApproverRef) {
        this.createApproverRef = createApproverRef;
    }

    @Override
    public void setCreateChannel(String createChannel) {
        this.createChannel = createChannel;
    }

    @Override
    public void setCreateTimestamp(XMLGregorianCalendar createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @Override
    public void setCreatorRef(REmbeddedReference creatorRef) {
        this.creatorRef = creatorRef;
    }

    @Override
    public void setModifierRef(REmbeddedReference modifierRef) {
        this.modifierRef = modifierRef;
    }

    @Override
    public void setModifyApproverRef(Set<RAssignmentReference> modifyApproverRef) {
        this.modifyApproverRef = modifyApproverRef;
    }

    @Override
    public void setModifyChannel(String modifyChannel) {
        this.modifyChannel = modifyChannel;
    }

    @Override
    public void setModifyTimestamp(XMLGregorianCalendar modifyTimestamp) {
        this.modifyTimestamp = modifyTimestamp;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setExtension(RAssignmentExtension extension) {
        this.extension = extension;
    }

    public void setTargetRef(REmbeddedReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setAssignmentOwner(RAssignmentOwner assignmentOwner) {
        this.assignmentOwner = assignmentOwner;
    }

    public void setTenantRef(REmbeddedReference tenantRef) {
        this.tenantRef = tenantRef;
    }

    public void setOrgRef(REmbeddedReference orgRef) {
        this.orgRef = orgRef;
    }

    public void setResourceRef(REmbeddedReference resourceRef) {
        this.resourceRef = resourceRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RAssignment)) {
            return false;
        }

        RAssignment that = (RAssignment) o;
        return Objects.equals(getOwnerOid(), that.getOwnerOid())
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerOid(), id);
    }

    public static void fromJaxb(AssignmentType jaxb, RAssignment repo, RObject parent,
            RepositoryContext repositoryContext) throws DtoTranslationException {
        fromJaxb(jaxb, repo, repositoryContext, null);
        repo.setOwner(parent);
    }

    public static void fromJaxb(AssignmentType jaxb, RAssignment repo, ObjectType parent, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        fromJaxb(jaxb, repo, repositoryContext, generatorResult);
        repo.setOwnerOid(parent.getOid());
    }

    private static void fromJaxb(AssignmentType jaxb, RAssignment repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        Objects.requireNonNull(repo, "Repo object must not be null.");
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");

        if (generatorResult != null) {
            repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));
        }

        repo.setId(RUtil.toInteger(jaxb.getId()));
        repo.setOrder(jaxb.getOrder());
        repo.setLifecycleState(jaxb.getLifecycleState());
        repo.setPolicySituation(RUtil.listToSet(jaxb.getPolicySituation()));

        if (jaxb.getExtension() != null) {
            RAssignmentExtension extension = new RAssignmentExtension();
            extension.setOwner(repo);

            repo.setExtension(extension);
            RAssignmentExtension.fromJaxb(jaxb.getExtension(), extension, RAssignmentExtensionType.EXTENSION,
                    repositoryContext);
        }

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.fromJaxb(jaxb.getActivation(), activation);
            repo.setActivation(activation);
        }

        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), repositoryContext.relationRegistry));

        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), repositoryContext.relationRegistry));

        repo.setOrgRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOrgRef(), repositoryContext.relationRegistry));

        if (jaxb.getConstruction() != null) {
            repo.setResourceRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConstruction().getResourceRef(), repositoryContext.relationRegistry));
        }

        MetadataFactory.fromJaxb(jaxb.getMetadata(), repo, repositoryContext.relationRegistry);
    }

    @Override
    public String toString() {
        return "RAssignment{" +
                "id=" + id +
                ", ownerOid='" + ownerOid + '\'' +
                ", owner=" + owner +
                ", targetRef=" + targetRef +
                ", resourceRef=" + resourceRef +
                '}';
    }
}
