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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
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
        if (caseExpressionList != null) {
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
}
