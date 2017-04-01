/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
@Component
public class AccCertReviewersHelper {

	private static final transient Trace LOGGER = TraceManager.getTrace(AccCertReviewersHelper.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

	@Autowired private OrgStructFunctions orgStructFunctions;
	@Autowired private PrismContext prismContext;
	@Autowired private AccCertExpressionHelper expressionHelper;

    public AccessCertificationReviewerSpecificationType findReviewersSpecification(AccessCertificationCampaignType campaign,
                                                                                   int stage, Task task, OperationResult result) {
        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage);
        return stageDef.getReviewerSpecification();
    }

    public List<ObjectReferenceType> getReviewersForCase(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
                                      AccessCertificationReviewerSpecificationType reviewerSpec, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (reviewerSpec == null) {
            return Collections.emptyList();     // TODO issue a warning here?
        }

		List<ObjectReferenceType> reviewers = new ArrayList<>();
		if (Boolean.TRUE.equals(reviewerSpec.isUseTargetOwner())) {
            cloneAndMerge(reviewers, getTargetObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetApprover())) {
            cloneAndMerge(reviewers, getTargetObjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectOwner())) {
            cloneAndMerge(reviewers, getObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectApprover())) {
            cloneAndMerge(reviewers, getObjectApprovers(_case, task, result));
        }
        if (reviewerSpec.getUseObjectManager() != null) {
            cloneAndMerge(reviewers, getObjectManagers(_case, reviewerSpec.getUseObjectManager(), task, result));
        }
        for (ExpressionType reviewerExpression : reviewerSpec.getReviewerExpression()) {
			ExpressionVariables variables = new ExpressionVariables();
			variables.addVariableDefinition(ExpressionConstants.VAR_CERTIFICATION_CASE, _case);
			variables.addVariableDefinition(ExpressionConstants.VAR_CAMPAIGN, campaign);
			variables.addVariableDefinition(ExpressionConstants.VAR_REVIEWER_SPECIFICATION, reviewerSpec);
			List<ObjectReferenceType> refList = expressionHelper
					.evaluateRefExpressionChecked(reviewerExpression, variables, "reviewer expression", task, result);
			cloneAndMerge(reviewers, refList);
		}
		resolveRoleReviewers(reviewers, task, result);
        if (reviewers.isEmpty()) {
            cloneAndMerge(reviewers, reviewerSpec.getDefaultReviewerRef());
        }
        cloneAndMerge(reviewers, reviewerSpec.getAdditionalReviewerRef());
		resolveRoleReviewers(reviewers, task, result);

		return reviewers;
    }

	private void resolveRoleReviewers(List<ObjectReferenceType> reviewers, Task task, OperationResult result)
			throws SchemaException {
    	List<ObjectReferenceType> resolved = new ArrayList<>();
		for (Iterator<ObjectReferenceType> iterator = reviewers.iterator(); iterator.hasNext(); ) {
			ObjectReferenceType reviewer = iterator.next();
			if (QNameUtil.match(reviewer.getType(), RoleType.COMPLEX_TYPE) ||
					QNameUtil.match(reviewer.getType(), OrgType.COMPLEX_TYPE) ||
					QNameUtil.match(reviewer.getType(), ServiceType.COMPLEX_TYPE)) {
				iterator.remove();
				resolved.addAll(getMembers(reviewer, task, result));
			}
		}
		for (ObjectReferenceType ref : resolved) {
			if (!containsOid(reviewers, ref.getOid())) {
				reviewers.add(ref);
			}
		}
	}

	private List<ObjectReferenceType> getMembers(ObjectReferenceType abstractRoleRef, Task task, OperationResult result)
			throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UserType.F_ROLE_MEMBERSHIP_REF).ref(abstractRoleRef.getOid())
				.build();
		return repositoryService.searchObjects(UserType.class, query, null, result).stream()
				.map(obj -> ObjectTypeUtil.createObjectRef(obj))
				.collect(Collectors.toList());
	}

	private void cloneAndMerge(List<ObjectReferenceType> reviewers, Collection<ObjectReferenceType> newReviewers) {
        if (newReviewers == null) {
            return;
        }
        for (ObjectReferenceType newReviewer : newReviewers) {
            if (!containsOid(reviewers, newReviewer.getOid())) {
                reviewers.add(newReviewer.clone());
            }
        }
    }

    private boolean containsOid(List<ObjectReferenceType> reviewers, String oid) {
        for (ObjectReferenceType reviewer : reviewers) {
            if (reviewer.getOid().equals(oid)) {
                return true;
            }
        }
        return false;
    }

    private Collection<ObjectReferenceType> getObjectManagers(AccessCertificationCaseType _case, ManagerSearchType managerSearch, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            ObjectReferenceType objectRef = _case.getObjectRef();
            ObjectType object = resolveReference(objectRef, ObjectType.class, result);

            String orgType = managerSearch.getOrgType();
            boolean allowSelf = Boolean.TRUE.equals(managerSearch.isAllowSelf());
            Collection<UserType> managers;
            if (object instanceof UserType) {
                managers = orgStructFunctions.getManagers((UserType) object, orgType, allowSelf, true);
            } else if (object instanceof OrgType) {
                // TODO more elaborate behavior; eliminate unneeded resolveReference above
                managers = orgStructFunctions.getManagersOfOrg(object.getOid(), true);
            } else if (object instanceof RoleType || object instanceof ServiceType) {
                // TODO implement
                managers = new HashSet<>();
            } else {
                // TODO warning?
                managers = new HashSet<>();
            }
            List<ObjectReferenceType> retval = new ArrayList<>(managers.size());
            for (UserType manager : managers) {
                retval.add(ObjectTypeUtil.createObjectRef(manager));
            }
            return retval;
        } catch (SecurityViolationException e) {
            // never occurs, as preAuthorized is TRUE above
            throw new IllegalStateException("Impossible has happened: " + e.getMessage(), e);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    protected List<ObjectReferenceType> getTargetObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = resolveReference(_case.getTargetRef(), ObjectType.class, result);
        if (target instanceof AbstractRoleType) {
			return getAssignees((AbstractRoleType) target, SchemaConstants.ORG_OWNER, task, result);
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getOwnerRef((ResourceType) target);
        } else {
            return null;
        }
    }

	private List<ObjectReferenceType> getAssignees(AbstractRoleType role, QName relation, Task task, OperationResult result)
			throws SchemaException {
    	List<ObjectReferenceType> rv = new ArrayList<>();
		if (SchemaConstants.ORG_OWNER.equals(relation)) {
			CollectionUtils.addIgnoreNull(rv, role.getOwnerRef());
		} else if (SchemaConstants.ORG_APPROVER.equals(relation)) {
			rv.addAll(role.getApproverRef());
		} else {
			throw new AssertionError(relation);
		}
		// TODO in theory, we could look for approvers/owners of UserType, right?
		PrismReferenceValue ref = new PrismReferenceValue(role.getOid());
		ref.setRelation(relation);
		ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(ref)
				.build();
		List<PrismObject<FocusType>> assignees = repositoryService.searchObjects(FocusType.class, query, null, result);
		LOGGER.trace("Looking for '{}' of {} using {}: found: {}", relation.getLocalPart(), role, query, assignees);
		assignees.forEach(o -> rv.add(ObjectTypeUtil.createObjectRef(o)));
		return rv;
	}

	protected List<ObjectReferenceType> getObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = resolveReference(_case.getObjectRef(), ObjectType.class, result);
        if (object instanceof AbstractRoleType) {
			return getAssignees((AbstractRoleType) object, SchemaConstants.ORG_OWNER, task, result);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getTargetObjectApprovers(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = resolveReference(_case.getTargetRef(), ObjectType.class, result);
        if (target instanceof AbstractRoleType) {
			return getAssignees((AbstractRoleType) target, SchemaConstants.ORG_APPROVER, task, result);
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getApproverRef((ResourceType) target);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getObjectApprovers(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = resolveReference(_case.getObjectRef(), ObjectType.class, result);
        if (object instanceof AbstractRoleType) {
			return getAssignees((AbstractRoleType) object, SchemaConstants.ORG_APPROVER, task, result);
        } else {
            return null;
        }
    }

    private ObjectType resolveReference(ObjectReferenceType objectRef, Class<? extends ObjectType> defaultObjectTypeClass, OperationResult result) throws SchemaException, ObjectNotFoundException {
        final Class<? extends ObjectType> objectTypeClass;
        if (objectRef.getType() != null) {
            objectTypeClass = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectRef.getType());
            if (objectTypeClass == null) {
                throw new SchemaException("No object class found for " + objectRef.getType());
            }
        } else {
            objectTypeClass = defaultObjectTypeClass;
        }
        PrismObject<? extends ObjectType> object = repositoryService.getObject(objectTypeClass, objectRef.getOid(), null, result);
        return object.asObjectable();
    }


}
