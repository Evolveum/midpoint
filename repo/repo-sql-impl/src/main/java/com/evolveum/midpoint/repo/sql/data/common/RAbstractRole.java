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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RRoleApproverRef;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.commons.lang.StringUtils;
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
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "inducement"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "ABSTRACT_ROLE")}, collectionType = RAssignment.class)})

@Entity
@ForeignKey(name = "fk_abstract_role")
@org.hibernate.annotations.Table(appliesTo = "m_abstract_role",
        indexes = {@Index(name = "iRequestable", columnNames = "requestable")})
public abstract class RAbstractRole<T extends AbstractRoleType> extends RFocus<T> {

    private Set<RExclusion> exclusion;
    private Boolean requestable;
    private Set<RObjectReference> approverRef;
    private String approvalProcess;
    private String approvalSchema;
    private String approvalExpression;
    private String automaticallyApproved;
    private Set<RAuthorization> authorization;

    @OneToMany(mappedBy = RAuthorization.F_OWNER, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAuthorization> getAuthorization() {
        if (authorization == null) {
            authorization = new HashSet<RAuthorization>();
        }
        return authorization;
    }

    public Boolean getRequestable() {
        return requestable;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getAutomaticallyApproved() {
        return automaticallyApproved;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getApprovalExpression() {
        return approvalExpression;
    }

    @Column(nullable = true)
    public String getApprovalProcess() {
        return approvalProcess;
    }

    @Column(nullable = true)
    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getApprovalSchema() {
        return approvalSchema;
    }

    @Transient
    public Set<RAssignment> getInducement() {
        return getAssignments(RAssignmentOwner.ABSTRACT_ROLE);
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RExclusion> getExclusion() {
        if (exclusion == null) {
            exclusion = new HashSet<RExclusion>();
        }
        return exclusion;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RRoleApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getApproverRef() {
        if (approverRef == null) {
            approverRef = new HashSet<RObjectReference>();
        }
        return approverRef;
    }

    public void setApproverRef(Set<RObjectReference> approverRef) {
        this.approverRef = approverRef;
    }

    public void setExclusion(Set<RExclusion> exclusion) {
        this.exclusion = exclusion;
    }

    public void setApprovalProcess(String approvalProcess) {
        this.approvalProcess = approvalProcess;
    }

    public void setApprovalSchema(String approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    public void setApprovalExpression(String approvalExpression) {
        this.approvalExpression = approvalExpression;
    }

    public void setAutomaticallyApproved(String automaticallyApproved) {
        this.automaticallyApproved = automaticallyApproved;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }

    public void setAuthorization(Set<RAuthorization> authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RAbstractRole that = (RAbstractRole) o;

        if (exclusion != null ? !exclusion.equals(that.exclusion) : that.exclusion != null)
            return false;
        if (approverRef != null ? !approverRef.equals(that.approverRef) : that.approverRef != null)
            return false;
        if (approvalProcess != null ? !approvalProcess.equals(that.approvalProcess) : that.approvalProcess != null)
            return false;
        if (approvalSchema != null ? !approvalSchema.equals(that.approvalSchema) : that.approvalSchema != null)
            return false;
        if (approvalExpression != null ? !approvalExpression.equals(that.approvalExpression) : that.approvalExpression != null)
            return false;
        if (automaticallyApproved != null ? !automaticallyApproved.equals(that.automaticallyApproved) : that.automaticallyApproved != null)
            return false;
        if (requestable != null ? !requestable.equals(that.requestable) : that.requestable != null)
            return false;
        if (authorization != null ? !authorization.equals(that.authorization) : that.authorization != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (approvalProcess != null ? approvalProcess.hashCode() : 0);
        result = 31 * result + (approvalSchema != null ? approvalSchema.hashCode() : 0);
        result = 31 * result + (approvalExpression != null ? approvalExpression.hashCode() : 0);
        result = 31 * result + (automaticallyApproved != null ? automaticallyApproved.hashCode() : 0);
        result = 31 * result + (requestable != null ? requestable.hashCode() : 0);
        return result;
    }

    public static <T extends AbstractRoleType> void copyToJAXB(RAbstractRole<T> repo, AbstractRoleType jaxb,
                                                               PrismContext prismContext,
                                                               Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        RFocus.copyToJAXB(repo, jaxb, prismContext, options);

        jaxb.setRequestable(repo.getRequestable());
        if (SelectorOptions.hasToLoadPath(AbstractRoleType.F_INDUCEMENT, options)) {
            if (repo.getInducement() != null) {
                for (RAssignment inducement : repo.getInducement()) {
                    jaxb.getInducement().add(inducement.toJAXB(prismContext));
                }
            }
        }
        if (SelectorOptions.hasToLoadPath(AbstractRoleType.F_EXCLUSION, options)) {
            if (repo.getExclusion() != null) {
                for (RExclusion rExclusion : repo.getExclusion()) {
                    jaxb.getExclusion().add(rExclusion.toJAXB(prismContext));
                }
            }
        }
        if (SelectorOptions.hasToLoadPath(AbstractRoleType.F_AUTHORIZATION, options)) {
            if (repo.getAuthorization() != null) {
                for (RAuthorization rAuth : repo.getAuthorization()) {
                    jaxb.getAuthorization().add(rAuth.toJAXB(prismContext));
                }
            }
        }

        if (SelectorOptions.hasToLoadPath(AbstractRoleType.F_APPROVER_REF, options)) {
            if (repo.getApproverRef() != null) {
                for (RObjectReference repoRef : repo.getApproverRef()) {
                    jaxb.getApproverRef().add(repoRef.toJAXB(prismContext));
                }
            }
        }

        jaxb.setApprovalProcess(repo.getApprovalProcess());
        try {
            jaxb.setApprovalSchema(RUtil.toJAXB(RoleType.class, RoleType.F_APPROVAL_SCHEMA,
                    repo.getApprovalSchema(), ApprovalSchemaType.class, prismContext));

            if (SelectorOptions.hasToLoadPath(AbstractRoleType.F_APPROVER_EXPRESSION, options)) {
                if (StringUtils.isNotEmpty(repo.getApprovalExpression())) {
                    RoleType expressions = RUtil.toJAXB(RoleType.class, RoleType.F_APPROVER_EXPRESSION,
                            repo.getApprovalExpression(), RoleType.class, prismContext);
                    jaxb.getApproverExpression().addAll(expressions.getApproverExpression());
                }
            }

            if (StringUtils.isNotEmpty(repo.getAutomaticallyApproved())) {
                jaxb.setAutomaticallyApproved(RUtil.toJAXB(RoleType.class, RoleType.F_AUTOMATICALLY_APPROVED,
                        repo.getAutomaticallyApproved(), ExpressionType.class, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static <T extends AbstractRoleType> void copyFromJAXB(AbstractRoleType jaxb, RAbstractRole<T> repo,
                                                                 PrismContext prismContext) throws DtoTranslationException {
        RFocus.copyFromJAXB(jaxb, repo, prismContext);
        repo.setRequestable(jaxb.isRequestable());

        for (AssignmentType inducement : jaxb.getInducement()) {
            RAssignment rInducement = new RAssignment(repo, RAssignmentOwner.ABSTRACT_ROLE);
            RAssignment.copyFromJAXB(inducement, rInducement, jaxb, prismContext);

            repo.getAssignments().add(rInducement);
        }

        for (ExclusionType exclusion : jaxb.getExclusion()) {
            RExclusion rExclusion = new RExclusion(repo);
            RExclusion.copyFromJAXB(exclusion, rExclusion, jaxb, prismContext);

            repo.getExclusion().add(rExclusion);
        }

        for (AuthorizationType authorization : jaxb.getAuthorization()) {
            RAuthorization rAuth = new RAuthorization(repo);
            RAuthorization.copyFromJAXB(authorization, rAuth, jaxb, prismContext);

            repo.getAuthorization().add(rAuth);
        }

        for (ObjectReferenceType approverRef : jaxb.getApproverRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(approverRef, prismContext, repo, RReferenceOwner.ROLE_APPROVER);
            if (ref != null) {
                repo.getApproverRef().add(ref);
            }
        }
        
        PrismObjectDefinition<AbstractRoleType> roleDefinition = jaxb.asPrismObject().getDefinition();

        repo.setApprovalProcess(jaxb.getApprovalProcess());
        try {
            repo.setApprovalSchema(RUtil.toRepo(jaxb.getApprovalSchema(), prismContext));

            repo.setApprovalExpression(RUtil.toRepo(roleDefinition, AbstractRoleType.F_APPROVER_EXPRESSION, jaxb.getApproverExpression(), prismContext));
            repo.setAutomaticallyApproved(RUtil.toRepo(roleDefinition, AbstractRoleType.F_AUTOMATICALLY_APPROVED, jaxb.getAutomaticallyApproved(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
