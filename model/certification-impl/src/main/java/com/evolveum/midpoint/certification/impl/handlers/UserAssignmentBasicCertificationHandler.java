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

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class UserAssignmentBasicCertificationHandler extends BaseCertificationHandler {

    public static final String URI = "http://midpoint.evolveum.com/xml/ns/public/certification/certification-3#user-assignment-basic";        // TODO

    private static final transient Trace LOGGER = TraceManager.getTrace(UserAssignmentBasicCertificationHandler.class);

    @PostConstruct
    public void init() {
        certificationManager.registerHandler(URI, this);
    }

    @Override
    protected QName getDefaultObjectType() {
        return UserType.COMPLEX_TYPE;
    }

    // default behavior for this handler: take all the direct assignments (org, role, resource) and make certification cases from them
    @Override
    protected Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<ObjectType> objectPrism, List<ExpressionType> caseExpressionList, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) {
        if (CollectionUtils.isNotEmpty(caseExpressionList)) {
            return evaluateCaseExpressionList(caseExpressionList, objectPrism, task, parentResult);
        }
        ObjectType object = objectPrism.asObjectable();
        if (!(object instanceof UserType)) {
            throw new IllegalStateException("UserAssignmentBasicCertificationHandler cannot be run against non-user object: " + ObjectTypeUtil.toShortString(object));
        }
        UserType user = (UserType) object;
        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()) {
            AccessCertificationAssignmentCaseType assignmentCase = new AccessCertificationAssignmentCaseType(prismContext);
            assignmentCase.asPrismContainerValue().setConcreteType(AccessCertificationAssignmentCaseType.COMPLEX_TYPE);
            assignmentCase.setAssignment(assignment.clone());
            assignmentCase.setSubjectRef(ObjectTypeUtil.createObjectRef(object));
            boolean valid;
            if (assignment.getTargetRef() != null) {
                assignmentCase.setTargetRef(assignment.getTargetRef());
                valid = true;
            } else if (assignment.getConstruction() != null) {
                assignmentCase.setTargetRef(assignment.getConstruction().getResourceRef());
                valid = true;
            } else {
                valid = false;      // neither role/org nor resource assignment; ignored for now
            }
            if (valid) {
                caseList.add(assignmentCase);
            }
        }
        return caseList;
    }

    @Override
    public void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (!(aCase instanceof AccessCertificationAssignmentCaseType)) {
            throw new IllegalStateException("Expected " + AccessCertificationAssignmentCaseType.class + ", got " + aCase.getClass() + " instead");
        }
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) aCase;
        String subjectOid = assignmentCase.getSubjectRef().getOid();
        Long assignmentId = assignmentCase.getAssignment().getId();
        if (assignmentId == null) {
            throw new IllegalStateException("No ID for an assignment to remove: " + assignmentCase.getAssignment());
        }
        Class clazz = ObjectTypes.getObjectTypeFromTypeQName(assignmentCase.getSubjectRef().getType()).getClassDefinition();
        PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
        cval.setId(assignmentId);

        // quick "solution" - deleting without checking the assignment ID
        ContainerDelta assignmentDelta = ContainerDelta.createModificationDelete(FocusType.F_ASSIGNMENT, clazz, prismContext, cval);
        ObjectDelta objectDelta = ObjectDelta.createModifyDelta(subjectOid, Arrays.asList(assignmentDelta), clazz, prismContext);
        LOGGER.info("Going to execute delta: {}", objectDelta.debugDump());
        modelService.executeChanges((Collection) Arrays.asList(objectDelta), null, task, caseResult);
        LOGGER.info("Case {} in {} (assignment {} of {}) was successfully revoked",
                aCase.asPrismContainerValue().getId(), ObjectTypeUtil.toShortString(campaign),
                assignmentId, subjectOid);
    }
}
