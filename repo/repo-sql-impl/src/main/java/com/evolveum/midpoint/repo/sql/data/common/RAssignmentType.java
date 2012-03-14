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
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@Table(name = "assignment")
@ForeignKey(name = "fk_assignment")
public class RAssignmentType extends RContainer implements ROwnable {

    private static final Trace LOGGER = TraceManager.getTrace(RAssignmentType.class);
    //owner
    private RObjectType owner;
    private String ownerOid;
    private Long ownerId;
    //extension
    private RAnyContainer extension;
    //assignment fields
    private RActivationType activation;
    private String accountConstruction;
    private RObjectReferenceType targetRef;

    @ForeignKey(name = "fk_assignment_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "oid", referencedColumnName = "oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RObjectType getOwner() {
        return owner;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getTargetRef() {
        return targetRef;
    }

    @Column(name = "owner_id", nullable = false)
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Column(name = "owner_oid", length = 36, nullable = false)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @ManyToOne(optional = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "extOid", referencedColumnName = "owner_oid"),
            @PrimaryKeyJoinColumn(name = "extId", referencedColumnName = "owner_id"),
            @PrimaryKeyJoinColumn(name = "extType", referencedColumnName = "ownerType")
    })
    public RAnyContainer getExtension() {
        return extension;
    }

    @Embedded
    public RActivationType getActivation() {
        if (activation == null) {
            activation = new RActivationType();
        }
        return activation;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getAccountConstruction() {
        return accountConstruction;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    public void setExtension(RAnyContainer extension) {
        this.extension = extension;
        if (extension != null) {
            extension.setOwnerType(RContainerType.ASSIGNMENT);
        }
    }

    public void setAccountConstruction(String accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setTargetRef(RObjectReferenceType targetRef) {
        this.targetRef = targetRef;
    }

    public void setOwner(RObjectType owner) {
        this.owner = owner;
    }

    @Transient
    @Override
    public RContainer getContainerOwner() {
        return getOwner();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RAssignmentType that = (RAssignmentType) o;

        if (accountConstruction != null ? !accountConstruction.equals(that.accountConstruction) : that.accountConstruction != null)
            return false;
//        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
//        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetRef != null ? targetRef.hashCode() : 0);
//        result = 31 * result + (ownerOid != null ? ownerOid.hashCode() : 0);
//        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RAssignmentType repo, AssignmentType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

//        jaxb.setId(Long.toString(repo.getContainerId()));
//        try {
//            jaxb.setAccountConstruction(RUtil.toJAXB(repo.getAccountConstruction(), AccountConstructionType.class, prismContext));
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }
//
//        com.evolveum.midpoint.repo.sql.data.common.RActivationType activation = repo.getActivation();
//        if (activation != null) {
//            jaxb.setActivation(activation.toJAXB(prismContext));
//        }
//
//        AnyContainer extension = repo.getExtension();
//        if (extension != null) {
//            jaxb.setExtension(extension.toJAXB(prismContext));
//        }
//
//        if (repo.getTargetRef() != null) {
//            jaxb.setTargetRef(repo.getTargetRef().toJAXB(prismContext));
//        }
    }

    public static void copyFromJAXB(AssignmentType jaxb, RAssignmentType repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

//        repo.setContainerId(com.evolveum.midpoint.repo.sql.data.common.RUtil.getLongFromString(jaxb.getId()));
//
//        try {
//            repo.setAccountConstruction(RUtil.toRepo(jaxb.getAccountConstruction(), prismContext));
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }
//
//        if (jaxb.getActivation() != null) {
//            com.evolveum.midpoint.repo.sql.data.common.RActivationType activation = new com.evolveum.midpoint.repo.sql.data.common.RActivationType();
//            com.evolveum.midpoint.repo.sql.data.common.RActivationType.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
//            repo.setActivation(activation);
//        }
//
//        if (jaxb.getExtension() != null) {
//            AnyContainer extension = new AnyContainer();
//            AnyContainer.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
//            repo.setExtension(extension);
//        }
//
//        if (jaxb.getTarget() != null) {
//            LOGGER.warn("Target from assignment type won't be saved. It should be translated to target reference.");
//        }
//
//        repo.setTargetRef(RUtil.jaxbRefToRepo(jaxb.getTargetRef(), jaxb.getId(), prismContext));
    }

    public AssignmentType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        AssignmentType object = new AssignmentType();
        RAssignmentType.copyToJAXB(this, object, prismContext);
        return object;
    }
}
