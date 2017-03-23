/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class DirectAssignmentCertificationHandler extends BaseCertificationHandler {

    public static final String URI = AccessCertificationApiConstants.DIRECT_ASSIGNMENT_HANDLER_URI;

    private static final transient Trace LOGGER = TraceManager.getTrace(DirectAssignmentCertificationHandler.class);

    @PostConstruct
    public void init() {
        certificationManager.registerHandler(URI, this);
    }

    @Override
    public QName getDefaultObjectType() {
        return UserType.COMPLEX_TYPE;
    }

    // converts assignments to cases
    @Override
    public Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<ObjectType> objectPrism, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

        AccessCertificationAssignmentReviewScopeType assignmentScope = null;
        if (campaign.getScopeDefinition() instanceof AccessCertificationAssignmentReviewScopeType) {
            assignmentScope = (AccessCertificationAssignmentReviewScopeType) campaign.getScopeDefinition();
        }
        // TODO what if AccessCertificationObjectBasedScopeType?

        FocusType focus = castToFocus(objectPrism);

        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        if (isIncludeAssignments(assignmentScope)) {
            for (AssignmentType assignment : focus.getAssignment()) {
                processAssignment(assignment, false, assignmentScope, campaign, focus, caseList, task, parentResult);
            }
        }
        if (focus instanceof AbstractRoleType && isIncludeInducements(assignmentScope)) {
            for (AssignmentType assignment : ((AbstractRoleType) focus).getInducement()) {
                processAssignment(assignment, true, assignmentScope, campaign, focus, caseList, task, parentResult);
            }
        }
        return caseList;
    }

    private void processAssignment(AssignmentType assignment, boolean isInducement, AccessCertificationAssignmentReviewScopeType scope,
                                   AccessCertificationCampaignType campaign, ObjectType object, List<AccessCertificationCaseType> caseList, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        AccessCertificationAssignmentCaseType assignmentCase = new AccessCertificationAssignmentCaseType(prismContext);
        assignmentCase.setAssignment(assignment.clone());
        assignmentCase.setIsInducement(isInducement);
        assignmentCase.setObjectRef(ObjectTypeUtil.createObjectRef(object));
        assignmentCase.setTenantRef(assignment.getTenantRef());
        assignmentCase.setOrgRef(assignment.getOrgRef());
        assignmentCase.setActivation(assignment.getActivation());
        boolean valid;
        if (assignment.getTargetRef() != null) {
            assignmentCase.setTargetRef(assignment.getTargetRef());
            if (RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeRoles(scope);
            } else if (OrgType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeOrgs(scope);
            } else if (ServiceType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeServices(scope);
            } else if (UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                valid = isIncludeUsers(scope);
            } else {
                throw new IllegalStateException("Unexpected targetRef type: " + assignment.getTargetRef().getType() + " in " + ObjectTypeUtil.toShortString(assignment));
            }
            valid = valid && relationMatches(assignment.getTargetRef().getRelation(), scope.getRelation());
        } else if (assignment.getConstruction() != null) {
            assignmentCase.setTargetRef(assignment.getConstruction().getResourceRef());
            valid = isIncludeResources(scope);
        } else {
            valid = false;      // neither role/org/service nor resource assignment; ignored for now
        }
        valid = valid && (!isEnabledItemsOnly(scope) || ActivationUtil.isAdministrativeEnabledOrNull(assignment.getActivation()));
        valid = valid && itemSelectionExpressionAccepts(assignment, isInducement, object, campaign, task, result);
        if (valid) {
            caseList.add(assignmentCase);
        }
    }

    private boolean relationMatches(QName assignmentRelation, List<QName> scopeRelations) {
        return (!scopeRelations.isEmpty() ? scopeRelations : Collections.singletonList(SchemaConstants.ORG_DEFAULT))
                .stream().anyMatch(r -> ObjectTypeUtil.relationMatches(r, assignmentRelation));
    }

    private boolean itemSelectionExpressionAccepts(AssignmentType assignment, boolean isInducement, ObjectType object, AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        AccessCertificationObjectBasedScopeType scope = null;
        if (campaign.getScopeDefinition() instanceof AccessCertificationObjectBasedScopeType) {
            scope = (AccessCertificationObjectBasedScopeType) (campaign.getScopeDefinition());
        }
        if (scope == null || scope.getItemSelectionExpression() == null) {
            return true;        // no expression, no rejections
        }
        ExpressionType selectionExpression = scope.getItemSelectionExpression();
        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignment);
        if (object instanceof FocusType) {
            variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, object);
        }
        if (object instanceof UserType) {
            variables.addVariableDefinition(ExpressionConstants.VAR_USER, object);
        }
        return expressionHelper.evaluateBooleanExpression(selectionExpression, variables,
                "item selection for assignment " + ObjectTypeUtil.toShortString(assignment), task, result);
    }

    private boolean isIncludeAssignments(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeAssignments());
    }

    private boolean isIncludeInducements(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeInducements());
    }

    private boolean isIncludeResources(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeResources());
    }

    private boolean isIncludeRoles(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeRoles());
    }

    private boolean isIncludeOrgs(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeOrgs());
    }

    private boolean isIncludeServices(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeServices());
    }

    private boolean isIncludeUsers(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isIncludeUsers());
    }

    private boolean isEnabledItemsOnly(AccessCertificationAssignmentReviewScopeType scope) {
        return scope == null || !Boolean.FALSE.equals(scope.isEnabledItemsOnly());
    }

    @Override
    public void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (!(aCase instanceof AccessCertificationAssignmentCaseType)) {
            throw new IllegalStateException("Expected " + AccessCertificationAssignmentCaseType.class + ", got " + aCase.getClass() + " instead");
        }
        revokeAssignmentCase((AccessCertificationAssignmentCaseType) aCase, campaign, caseResult, task);
    }
}
