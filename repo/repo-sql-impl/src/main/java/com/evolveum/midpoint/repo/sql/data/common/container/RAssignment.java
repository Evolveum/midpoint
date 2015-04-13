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

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@JaxbType(type = AssignmentType.class)
@Entity
@IdClass(RContainerId.class)
@Table(name = "m_assignment", indexes = {
        @Index(name = "iAssignmentAdministrative", columnList = "administrativeStatus"),
        @Index(name = "iAssignmentEffective", columnList = "effectiveStatus")})
public class RAssignment implements Container, Metadata<RAssignmentReference> {

    public static final String F_OWNER = "owner";
    /**
     * enum identifier of object class which owns this assignment. It's used because we have to
     * distinguish between assignments and inducements (all of them are the same kind) in {@link com.evolveum.midpoint.repo.sql.data.common.RAbstractRole}.
     */
    public static final String F_ASSIGNMENT_OWNER = "assignmentOwner";

    private static final Trace LOGGER = TraceManager.getTrace(RAssignment.class);

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

    @Id
    @org.hibernate.annotations.ForeignKey(name = "fk_assignment_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinTable(foreignKey = @ForeignKey(name = "fk_assignment_owner"))
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

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

    @org.hibernate.annotations.ForeignKey(name = "none")
    @com.evolveum.midpoint.repo.sql.query.definition.Any(jaxbNameLocalPart = "extension")
    @OneToOne(optional = true, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns(value = {
            @JoinColumn(name = "extOid", referencedColumnName = "owner_owner_oid"),
            @JoinColumn(name = "extId", referencedColumnName = "owner_id")
    }, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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

    @Where(clause = RAssignmentReference.REFERENCE_TYPE + "= 0")
    @OneToMany(mappedBy = RAssignmentReference.F_OWNER, orphanRemoval = true)
    @org.hibernate.annotations.ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    //@JoinTable(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    public Set<RAssignmentReference> getCreateApproverRef() {
        if (createApproverRef == null) {
            createApproverRef = new HashSet<>();
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

    @Where(clause = RAssignmentReference.REFERENCE_TYPE + "= 1")
    @OneToMany(mappedBy = RAssignmentReference.F_OWNER, orphanRemoval = true)
//    @JoinTable(foreignKey = @ForeignKey(name = "none"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignmentReference> getModifyApproverRef() {
        if (modifyApproverRef == null) {
            modifyApproverRef = new HashSet<>();
        }
        return modifyApproverRef;
    }

    public String getModifyChannel() {
        return modifyChannel;
    }

    public XMLGregorianCalendar getModifyTimestamp() {
        return modifyTimestamp;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCreateApproverRef(Set<RAssignmentReference> createApproverRef) {
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

    public void setModifyApproverRef(Set<RAssignmentReference> modifyApproverRef) {
        this.modifyApproverRef = modifyApproverRef;
    }

    public void setModifyChannel(String modifyChannel) {
        this.modifyChannel = modifyChannel;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAssignment that = (RAssignment) o;

        if (activation != null ? !activation.equals(that.activation) : that.activation != null) return false;
        if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;
        if (assignmentOwner != null ? !assignmentOwner.equals(that.assignmentOwner) : that.assignmentOwner != null)
            return false;
        if (order != null ? !order.equals(that.order) : that.order != null)
            return false;
        if (tenantRef != null ? !tenantRef.equals(that.tenantRef) : that.tenantRef != null) return false;

        if (!MetadataFactory.equals(this, that)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        result = 31 * result + (order != null ? order.hashCode() : 0);

        result = 31 * result + (createTimestamp != null ? createTimestamp.hashCode() : 0);
        result = 31 * result + (creatorRef != null ? creatorRef.hashCode() : 0);
        result = 31 * result + (createChannel != null ? createChannel.hashCode() : 0);
        result = 31 * result + (modifyTimestamp != null ? modifyTimestamp.hashCode() : 0);
        result = 31 * result + (modifierRef != null ? modifierRef.hashCode() : 0);
        result = 31 * result + (modifyChannel != null ? modifyChannel.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(AssignmentType jaxb, RAssignment repo, ObjectType parent, PrismContext prismContext,
                                    IdGeneratorResult generatorResult) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));

        repo.setOwnerOid(parent.getOid());
        repo.setId(RUtil.toInteger(jaxb.getId()));
        repo.setOrder(jaxb.getOrder());

        if (jaxb.getExtension() != null) {
            RAssignmentExtension extension = new RAssignmentExtension();
            extension.setOwner(repo);

            repo.setExtension(extension);
            RAssignmentExtension.copyFromJAXB(jaxb.getExtension(), extension, RAssignmentExtensionType.EXTENSION,
                    prismContext);
        }

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);
        }

        if (jaxb.getTarget() != null) {
            LOGGER.warn("Target from assignment type won't be saved. It should be translated to target reference.");
        }

        repo.setTargetRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTargetRef(), prismContext));

        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), prismContext));

        MetadataFactory.fromJAXB(jaxb.getMetadata(), repo, prismContext);
    }
}
