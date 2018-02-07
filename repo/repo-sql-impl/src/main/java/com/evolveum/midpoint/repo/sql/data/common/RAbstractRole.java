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

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RExclusion;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RAutoassignSpecification;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "inducement"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "ABSTRACT_ROLE")}, collectionType = RAssignment.class)})

@Entity
@org.hibernate.annotations.ForeignKey(name = "fk_abstract_role")
@Table(indexes = {
        @Index(name = "iAbstractRoleIdentifier", columnList = "identifier"),
        @Index(name = "iRequestable", columnList = "requestable"),
        @Index(name = "iAutoassignEnabled", columnList = "autoassign_enabled")
})
@Persister(impl = MidPointJoinedPersister.class)
public abstract class RAbstractRole<T extends AbstractRoleType> extends RFocus<T> {

	private String identifier;
	private String riskLevel;
	private RPolyString displayName;
    private Set<RExclusion> exclusion;
    private Boolean requestable;
    private Set<RObjectReference<RFocus>> approverRef;
    private String approvalProcess;

    private REmbeddedReference ownerRef;

    private RAutoassignSpecification autoassign;

    public Boolean getRequestable() {
        return requestable;
    }

    @Column(nullable = true)
    public String getApprovalProcess() {
        return approvalProcess;
    }

    @Transient
    public Set<RAssignment> getInducement() {
        return getAssignments(RAssignmentOwner.ABSTRACT_ROLE);
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RExclusion> getExclusion() {
        if (exclusion == null) {
            exclusion = new HashSet<>();
        }
        return exclusion;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 3")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
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

    public void setExclusion(Set<RExclusion> exclusion) {
        this.exclusion = exclusion;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RAbstractRole<?> that = (RAbstractRole<?>) o;

        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;
        if (riskLevel != null ? !riskLevel.equals(that.riskLevel) : that.riskLevel != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (exclusion != null ? !exclusion.equals(that.exclusion) : that.exclusion != null) return false;
        if (requestable != null ? !requestable.equals(that.requestable) : that.requestable != null) return false;
        if (approverRef != null ? !approverRef.equals(that.approverRef) : that.approverRef != null) return false;
        if (approvalProcess != null ? !approvalProcess.equals(that.approvalProcess) : that.approvalProcess != null)
            return false;
        if (ownerRef != null ? !ownerRef.equals(that.ownerRef) : that.ownerRef != null) return false;
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

    public static <T extends AbstractRoleType> void copyFromJAXB(AbstractRoleType jaxb, RAbstractRole<T> repo,
                                                                 RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
			throws DtoTranslationException {

        RFocus.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);
        repo.setRequestable(jaxb.isRequestable());

		repo.setDisplayName(RPolyString.copyFromJAXB(jaxb.getDisplayName()));
		repo.setIdentifier(jaxb.getIdentifier());
		repo.setRiskLevel(jaxb.getRiskLevel());

		if (jaxb.getAutoassign() != null) {
		    RAutoassignSpecification aa = new RAutoassignSpecification();
		    RAutoassignSpecification.copyFromJAXB(jaxb.getAutoassign(), aa);
            repo.setAutoassign(aa);
        }

		for (AssignmentType inducement : jaxb.getInducement()) {
            RAssignment rInducement = new RAssignment(repo, RAssignmentOwner.ABSTRACT_ROLE);
            RAssignment.copyFromJAXB(inducement, rInducement, jaxb, repositoryContext, generatorResult);

            repo.getAssignments().add(rInducement);
        }

        for (ExclusionPolicyConstraintType exclusion : jaxb.getExclusion()) {
            RExclusion rExclusion = new RExclusion(repo);
            RExclusion.copyFromJAXB(exclusion, rExclusion, jaxb, repositoryContext, generatorResult);

            repo.getExclusion().add(rExclusion);
        }

        for (ObjectReferenceType approverRef : jaxb.getApproverRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(approverRef, repositoryContext.prismContext, repo, RReferenceOwner.ROLE_APPROVER);
            if (ref != null) {
                repo.getApproverRef().add(ref);
            }
        }

        //PrismObjectDefinition<AbstractRoleType> roleDefinition = jaxb.asPrismObject().getDefinition();

        repo.setApprovalProcess(jaxb.getApprovalProcess());

        repo.setOwnerRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getOwnerRef(), repositoryContext.prismContext));
    }
}
