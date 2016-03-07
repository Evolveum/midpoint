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

package com.evolveum.midpoint.wf.impl.activiti.dao;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.FormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class WorkItemManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private MiscDataUtil miscDataUtil;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItemWithDetails";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_CLASS + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_CLASS + "releaseWorkItem";

    // choiceDecision - contains the name of the button ([B]xxxx) that was pressed
    // approvalDecision - contains true or false (approved / rejected)
    //
    // exactly one of choiceDecision and approvalDecision must be set
    //
    // todo error reporting
    public void completeWorkItemWithDetails(String taskId, String comment, String decision, OperationResult parentResult) {

        MidPointPrincipal principal;
		try {
			principal = securityEnforcer.getPrincipal();
		} catch (SecurityViolationException e) {
			LOGGER.error("Security violation: {}", e.getMessage(), e);
            return;
		}

        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEM);
        result.addParam("taskId", taskId);
        result.addParam("comment", comment);
        result.addParam("decision", decision);
        result.addContext("user", principal.getUser());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Completing work item " + taskId);
            LOGGER.trace("Decision: " + decision);
            LOGGER.trace("Comment: " + comment);
            LOGGER.trace("User: " + principal.getUser());
        }

        FormService formService = activitiEngine.getFormService();
        TaskFormData data = activitiEngine.getFormService().getTaskFormData(taskId);

        String assigneeOid = data.getTask().getAssignee();
        if (!miscDataUtil.isAuthorizedToSubmit(taskId, assigneeOid)) {
            result.recordFatalError("You are not authorized to complete the selected work item.");
            LOGGER.error("Authorization failure: task.assigneeOid = {}, principal = {}", assigneeOid, principal);
            return;
        }

        Map<String,String> propertiesToSubmit = new HashMap<String,String>();

        propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_DECISION, decision);
        if (comment != null) {
            propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_COMMENT, comment);
        }

        // we also fill-in the corresponding 'button' property (if there's one that corresponds to the decision)
        for (FormProperty formProperty : data.getFormProperties()) {
            if (formProperty.getId().startsWith(CommonProcessVariableNames.FORM_BUTTON_PREFIX)) {
                boolean value = formProperty.getId().equals(CommonProcessVariableNames.FORM_BUTTON_PREFIX + decision);
                LOGGER.trace("Setting the value of {} to writable property {}", value, formProperty.getId());
                propertiesToSubmit.put(formProperty.getId(), Boolean.toString(value));
            }
        }

//        if (specific != null) {
//
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("# of form properties: " + data.getFormProperties().size());
//            }
//
//            for (FormProperty formProperty : data.getFormProperties()) {
//
//                if (LOGGER.isTraceEnabled()) {
//                    LOGGER.trace("Processing property " + formProperty.getId() + ":" + formProperty.getName());
//                }
//
//                if (formProperty.isWritable()) {
//
//                    Object value;
//
//                    if (!CommonProcessVariableNames.FORM_FIELD_DECISION.equals(formProperty.getId()) &&
//                            !formProperty.getId().startsWith(CommonProcessVariableNames.FORM_BUTTON_PREFIX)) {
//
//                        // todo strip [flags] section
//                        QName propertyName = new QName(SchemaConstants.NS_WFCF, formProperty.getId());
//                        value = specific.getPropertyRealValue(propertyName, Object.class);
//
//                        if (LOGGER.isTraceEnabled()) {
//                            LOGGER.trace("Writable property " + formProperty.getId() + " has a value of " + value);
//                        }
//
//                        propertiesToSubmit.put(formProperty.getId(), value == null ? "" : value.toString());
//                    }
//                }
//            }
//        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Submitting " + propertiesToSubmit.size() + " properties");
        }

        formService.submitTaskFormData(taskId, propertiesToSubmit);

        result.recordSuccessIfUnknown();
    }


    public void claimWorkItem(String taskId, OperationResult parentResult) {
        MidPointPrincipal principal;
        try {
            principal = securityEnforcer.getPrincipal();
        } catch (SecurityViolationException e) {
            LOGGER.error("Security violation: {}", e.getMessage(), e);
            return;
        }

        OperationResult result = parentResult.createSubresult(OPERATION_CLAIM_WORK_ITEM);
        result.addParam("taskId", taskId);
        result.addContext("user", principal.getUser());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Claiming work item " + taskId);
            LOGGER.trace("User: " + principal.getUser());
        }

        try {
            TaskService taskService = activitiEngine.getTaskService();
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                result.recordFatalError("Couldn't claim work item " + taskId + ", because it does not exist");
                return;
            }
            if (task.getAssignee() != null) {
                String desc = task.getAssignee().equals(principal.getOid()) ?
                        "the current" : "another";
                result.recordFatalError("Couldn't claim work item " + taskId + ", because it is already assigned to "+desc+" user");
                return;
            }
            if (!miscDataUtil.isAuthorizedToClaim(task.getId())) {
                result.recordFatalError("Current user is not authorized to claim the selected work item.");
                return;
            }
            taskService.claim(taskId, principal.getOid());
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't claim work item " + taskId + ": " + e.getMessage(), e);
        }
    }

    public void releaseWorkItem(String taskId, OperationResult parentResult) {
        MidPointPrincipal principal;
        try {
            principal = securityEnforcer.getPrincipal();
        } catch (SecurityViolationException e) {
            LOGGER.error("Security violation: {}", e.getMessage(), e);
            parentResult.recordFatalError("Security violation: " + e.getMessage(), e);
            return;
        }

        OperationResult result = parentResult.createSubresult(OPERATION_RELEASE_WORK_ITEM);
        result.addParam("taskId", taskId);
        result.addContext("user", principal.getUser());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Releasing work item " + taskId);
            LOGGER.trace("User: " + principal.getUser());
        }

        try {
            TaskService taskService = activitiEngine.getTaskService();
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                result.recordFatalError("Couldn't release work item " + taskId + ", because it does not exist");
                return;
            }
            if (task.getAssignee() == null) {
                result.recordFatalError("Couldn't release work item " + taskId + ", because it is not assigned to a user");
                return;
            }
            if (!task.getAssignee().equals(principal.getOid())) {
                result.recordFatalError("Couldn't release work item " + taskId + ", because it is not assigned to the current user");
                return;
            }
            boolean candidateFound = false;
            for (IdentityLink link : taskService.getIdentityLinksForTask(taskId)) {
                if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
                    candidateFound = true;
                    break;
                }
            }
            if (!candidateFound) {
                result.recordFatalError("Couldn't release work item " + taskId + ", because it has no candidates to be offered to");
                return;
            }
            taskService.unclaim(taskId);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't release work item " + taskId + ": " + e.getMessage(), e);
        }
    }
}
