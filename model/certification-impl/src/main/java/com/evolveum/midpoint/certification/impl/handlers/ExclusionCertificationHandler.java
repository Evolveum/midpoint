/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class ExclusionCertificationHandler extends BaseCertificationHandler {

    public static final String URI = AccessCertificationApiConstants.EXCLUSION_HANDLER_URI;

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
    public <F extends FocusType> Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<F> objectPrism,
            AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) {
        F focus = objectPrism.asObjectable();
        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        for (AssignmentType assignment : focus.getAssignment()) {
            if (assignment.getPolicySituation().contains(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION)) {
                processAssignment(assignment, focus, caseList);
            }
        }
        return caseList;
    }

    private void processAssignment(AssignmentType assignment, ObjectType object, List<AccessCertificationCaseType> caseList) {
        AccessCertificationAssignmentCaseType assignmentCase = new AccessCertificationAssignmentCaseType(prismContext);
        assignmentCase.setAssignment(assignment.clone());
        assignmentCase.setObjectRef(ObjectTypeUtil.createObjectRef(object, prismContext));
        assignmentCase.setTenantRef(assignment.getTenantRef());
        assignmentCase.setOrgRef(assignment.getOrgRef());
        assignmentCase.setActivation(assignment.getActivation());
        if (assignment.getTargetRef() != null) {
            assignmentCase.setTargetRef(assignment.getTargetRef());
        } else {
            // very strange: assignment with no target, but participating in the exclusion?
            // maybe a dynamic target, though
        }
        caseList.add(assignmentCase);
    }


    @Override
    public void doRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult caseResult) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        if (!(aCase instanceof AccessCertificationAssignmentCaseType)) {
            throw new IllegalStateException("Expected " + AccessCertificationAssignmentCaseType.class + ", got " + aCase.getClass() + " instead");
        }
        revokeAssignmentCase((AccessCertificationAssignmentCaseType) aCase, campaign, caseResult, task);
    }

}
