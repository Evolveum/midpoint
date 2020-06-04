/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Where;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RAutoassignSpecification;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author lazyman
 */
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "inducement"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "ABSTRACT_ROLE") }, collectionType = RAssignment.class) })

@Entity
@org.hibernate.annotations.ForeignKey(name = "fk_abstract_role")
@Table(indexes = {
        @Index(name = "iAbstractRoleIdentifier", columnList = "identifier"),
        @Index(name = "iRequestable", columnList = "requestable"),
        @Index(name = "iAutoassignEnabled", columnList = "autoassign_enabled")
})
@Persister(impl = MidPointJoinedPersister.class)
public abstract class RAbstractRole extends RFocus {

    private String identifier;
    private String riskLevel;
    private RPolyString displayName;
    private Boolean requestable;
    private Set<RObjectReference<RFocus>> approverRef;
    private String approvalProcess;

    private REmbeddedReference ownerRef;

    private RAutoassignSpecification autoassign;

    public Boolean getRequestable() {
        return requestable;
    }

    @Column
    public String getApprovalProcess() {
        return approvalProcess;
    }

    @Transient
    public Set<RAssignment> getInducement() {
        return getAssignments(RAssignmentOwner.ABSTRACT_ROLE);
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 3")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectReference<RFocus>> getApproverRef() {
        if (approverRef == null) {
            approverRef = new HashSet<>();
        }
        return approverRef;
    }

    @Embedded
    public REmbeddedReference getOwnerRef() {
        return ownerRef;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    @Embedded
    public RPolyString getDisplayName() {
        return displayName;
    }

    public void setApproverRef(Set<RObjectReference<RFocus>> approverRef) {
        this.approverRef = approverRef;
    }

    public void setApprovalProcess(String approvalProcess) {
        this.approvalProcess = approvalProcess;
    }

    public void setOwnerRef(REmbeddedReference ownerRef) {
        this.ownerRef = ownerRef;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setDisplayName(RPolyString displayName) {
        this.displayName = displayName;
    }

    public RAutoassignSpecification getAutoassign() {
        return autoassign;
    }

    public void setAutoassign(RAutoassignSpecification autoassign) {
        this.autoassign = autoassign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        RAbstractRole that = (RAbstractRole) o;

        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) { return false; }
        if (riskLevel != null ? !riskLevel.equals(that.riskLevel) : that.riskLevel != null) { return false; }
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) { return false; }
        if (requestable != null ? !requestable.equals(that.requestable) : that.requestable != null) { return false; }
        if (approverRef != null ? !approverRef.equals(that.approverRef) : that.approverRef != null) { return false; }
        if (approvalProcess != null ? !approvalProcess.equals(that.approvalProcess) : that.approvalProcess != null) {
            return false;
        }
        if (ownerRef != null ? !ownerRef.equals(that.ownerRef) : that.ownerRef != null) { return false; }
        return autoassign != null ? autoassign.equals(that.autoassign) : that.autoassign == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (riskLevel != null ? riskLevel.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (requestable != null ? requestable.hashCode() : 0);
        result = 31 * result + (approvalProcess != null ? approvalProcess.hashCode() : 0);
        result = 31 * result + (ownerRef != null ? ownerRef.hashCode() : 0);
        result = 31 * result + (autoassign != null ? autoassign.hashCode() : 0);
        return result;
    }

    // dynamically called
    public static void copyFromJAXB(AbstractRoleType jaxb, RAbstractRole repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {

        copyFocusInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setRequestable(jaxb.isRequestable());

        repo.setDisplayName(RPolyString.copyFromJAXB(jaxb.getDisplayName()));
        repo.setIdentifier(jaxb.getIdentifier());
        repo.setRiskLevel(jaxb.getRiskLevel());

        if (jaxb.getAutoassign() != null) {
            RAutoassignSpecification aa = new RAutoassignSpecification();
            RAutoassignSpecification.formJaxb(jaxb.getAutoassign(), aa);
            repo.setAutoassign(aa);
        }

        for (AssignmentType inducement : jaxb.getInducement()) {
            RAssignment rInducement = new RAssignment(repo, RAssignmentOwner.ABSTRACT_ROLE);
            RAssignment.fromJaxb(inducement, rInducement, jaxb, repositoryContext, generatorResult);

            repo.getAssignments().add(rInducement);
        }
    }
}
