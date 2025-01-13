/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;

import jakarta.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Where;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RAutoassignSpecification;
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
@DynamicUpdate
public abstract class RAbstractRole extends RFocus {

    private String identifier;
    private String riskLevel;
    private RPolyString displayName;
    private Boolean requestable;
    private Set<RObjectReference<RFocus>> approverRef;
    private String approvalProcess;

    private RSimpleEmbeddedReference ownerRef;

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
    public RSimpleEmbeddedReference getOwnerRef() {
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

    public void setOwnerRef(RSimpleEmbeddedReference ownerRef) {
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
