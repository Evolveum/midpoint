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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
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
public class RAssignment extends RContainer implements ROwnable {

    private static final Trace LOGGER = TraceManager.getTrace(RAssignment.class);
    //owner
    private RObject owner;
    private String ownerOid;
    private Long ownerId;
    //extension
    @QueryEntity(any = true)
    private RAnyContainer extension;
    //assignment fields
    @QueryEntity(embedded = true)
    private RActivation activation;
    private String accountConstruction;
    private RObjectReference targetRef;

    @ForeignKey(name = "fk_assignment_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RObject getOwner() {
        return owner;
    }

    @OneToOne(optional = true, mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReference getTargetRef() {
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
    public RActivation getActivation() {
        return activation;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getAccountConstruction() {
        return accountConstruction;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setExtension(RAnyContainer extension) {
        this.extension = extension;
        if (this.extension != null) {
            this.extension.setOwnerType(RContainerType.ASSIGNMENT);
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

    public void setTargetRef(RObjectReference targetRef) {
        this.targetRef = targetRef;
    }

    public void setOwner(RObject owner) {
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

        RAssignment that = (RAssignment) o;

        if (accountConstruction != null ? !accountConstruction.equals(that.accountConstruction) : that.accountConstruction != null)
            return false;
        if (activation != null ? !activation.equals(that.activation) : that.activation != null) return false;
        if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activation != null ? activation.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RAssignment repo, AssignmentType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setId(RUtil.getStringFromLong(repo.getId()));

        if (repo.getExtension() != null) {
            ExtensionType extension = new ExtensionType();
            jaxb.setExtension(extension);
            RAnyContainer.copyToJAXB(repo.getExtension(), extension, prismContext);
        }
        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }

        try {
            jaxb.setAccountConstruction(RUtil.toJAXB(repo.getAccountConstruction(), AccountConstructionType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        if (repo.getTargetRef() != null) {
            jaxb.setTargetRef(repo.getTargetRef().toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(AssignmentType jaxb, RAssignment repo, ObjectType parent, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setOid(parent.getOid());
        repo.setId(RUtil.getLongWrappedFromString(jaxb.getId()));

        if (jaxb.getExtension() != null) {
            RAnyContainer extension = new RAnyContainer();
            extension.setOwner(repo);
//            extension.setOwnerType(RContainerType.ASSIGNMENT);

            repo.setExtension(extension);
            RAnyContainer.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
        }

        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);
        }

        try {
            repo.setAccountConstruction(RUtil.toRepo(jaxb.getAccountConstruction(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        if (jaxb.getTarget() != null) {
            LOGGER.warn("Target from assignment type won't be saved. It should be translated to target reference.");
        }

        repo.setTargetRef(RUtil.jaxbRefToRepo(jaxb.getTargetRef(), repo, prismContext));
    }

    public AssignmentType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        AssignmentType object = new AssignmentType();
        RAssignment.copyToJAXB(this, object, prismContext);
        return object;
    }
}
