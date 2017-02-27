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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AccCertReviewersHelper {

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

    public void setupReviewersForCase(AccessCertificationCaseType _case, AccessCertificationCampaignType campaign,
                                      AccessCertificationReviewerSpecificationType reviewerSpec, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        _case.getCurrentReviewerRef().clear();
        if (reviewerSpec == null) {
            return;     // TODO issue a warning here?
        }

        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetOwner())) {
            cloneAndMerge(_case.getCurrentReviewerRef(), getTargetObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetApprover())) {
            cloneAndMerge(_case.getCurrentReviewerRef(), getTargetObjectApprovers(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectOwner())) {
            cloneAndMerge(_case.getCurrentReviewerRef(), getObjectOwners(_case, task, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectApprover())) {
            cloneAndMerge(_case.getCurrentReviewerRef(), getObjectApprovers(_case, task, result));
        }
        if (reviewerSpec.getUseObjectManager() != null) {
            cloneAndMerge(_case.getCurrentReviewerRef(), getObjectManagers(_case, reviewerSpec.getUseObjectManager(), task, result));
        }
        for (ExpressionType reviewerExpression : reviewerSpec.getReviewerExpression()) {
			ExpressionVariables variables = new ExpressionVariables();
			variables.addVariableDefinition(ExpressionConstants.VAR_CERTIFICATION_CASE, _case);
			variables.addVariableDefinition(ExpressionConstants.VAR_CAMPAIGN, campaign);
			variables.addVariableDefinition(ExpressionConstants.VAR_REVIEWER_SPECIFICATION, reviewerSpec);
			List<ObjectReferenceType> refList = expressionHelper
					.evaluateRefExpressionChecked(reviewerExpression, variables, "reviewer expression", task, result);
			cloneAndMerge(_case.getCurrentReviewerRef(), refList);
		}
        if (_case.getCurrentReviewerRef().isEmpty()) {
            cloneAndMerge(_case.getCurrentReviewerRef(), reviewerSpec.getDefaultReviewerRef());
        }
        cloneAndMerge(_case.getCurrentReviewerRef(), reviewerSpec.getAdditionalReviewerRef());
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
            ObjectReferenceType ownerRef = ((AbstractRoleType) target).getOwnerRef();
            if (ownerRef != null) {
                return Arrays.asList(ownerRef);
            } else {
                return null;
            }
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getOwnerRef((ResourceType) target);
        } else {
            return null;
        }
    }

    protected List<ObjectReferenceType> getObjectOwners(AccessCertificationCaseType _case, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = resolveReference(_case.getObjectRef(), ObjectType.class, result);
        if (object instanceof AbstractRoleType) {
            ObjectReferenceType ownerRef = ((AbstractRoleType) object).getOwnerRef();
            if (ownerRef != null) {
                return Arrays.asList(ownerRef);
            } else {
                return null;
            }
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
            return ((AbstractRoleType) target).getApproverRef();
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
            return ((AbstractRoleType) object).getApproverRef();
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
