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

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.wf.api.Constants;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * @author mederly
 */
public class MidPointTaskListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointTaskListener.class);
    private static final String DOT_CLASS = MidPointTaskListener.class.getName() + ".";

    @Override
    public void notify(DelegateTask delegateTask) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("notify called; event name = {}, name = {}", delegateTask.getEventName(), delegateTask.getName());
        }

        OperationResult result = new OperationResult(DOT_CLASS + "notify");

        // auditing & notifications

        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            auditWorkItemEvent(delegateTask, AuditEventStage.REQUEST, result);
            SpringApplicationContextHolder.getProcessInstanceController().notifyWorkItemCreated(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    getWorkItemName(delegateTask),
                    delegateTask.getVariables());
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            auditWorkItemEvent(delegateTask, AuditEventStage.EXECUTION, result);
            SpringApplicationContextHolder.getProcessInstanceController().notifyWorkItemCompleted(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    getWorkItemName(delegateTask),
                    delegateTask.getVariables(),
                    (Boolean) delegateTask.getVariable(CommonProcessVariableNames.FORM_FIELD_DECISION));
        }

    }

    private String getWorkItemName(DelegateTask delegateTask) {
        return (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME);
    }

    private void auditWorkItemEvent(DelegateTask delegateTask, AuditEventStage stage, OperationResult result) {

        Map<String,Object> variables = delegateTask.getVariables();

        MiscDataUtil miscDataUtil = SpringApplicationContextHolder.getMiscDataUtil();
        RepositoryService repositoryService = SpringApplicationContextHolder.getRepositoryService();

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORK_ITEM);
        auditEventRecord.setEventStage(stage);

        String requesterOid = (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        if (requesterOid != null) {
            try {
                auditEventRecord.setInitiator(miscDataUtil.getRequester(delegateTask.getVariables(), result));
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            }
        }

        PrismObject<GenericObjectType> workItemObject = new PrismObject<GenericObjectType>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        workItemObject.asObjectable().setName(new PolyStringType(getWorkItemName(delegateTask)));
        workItemObject.asObjectable().setOid(delegateTask.getId());
        auditEventRecord.setTarget(workItemObject);

        if (stage == AuditEventStage.REQUEST) {
            if (delegateTask.getAssignee() != null) {
                try {
                    auditEventRecord.setTargetOwner(repositoryService.getObject(UserType.class, delegateTask.getAssignee(), result));
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                }
            }
        } else {
            MidPointPrincipal principal = MiscDataUtil.getPrincipalUser();
            if (principal != null && principal.getUser() != null) {
                auditEventRecord.setTargetOwner(principal.getUser().asPrismObject());
            }
        }

        ObjectDelta delta = null;
        try {
            delta = miscDataUtil.getObjectDelta(variables, result);
        } catch (JAXBException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta to be approved", e);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve delta to be approved", e);
        }
        if (delta != null) {
            auditEventRecord.addDelta(new ObjectDeltaOperation(delta));
        }

        if (stage == AuditEventStage.REQUEST) {
            auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);
        } else {
            OperationResult eventResult = new OperationResult(Constants.AUDIT_RESULT_METHOD);
            eventResult.recordSuccess();
            eventResult.addReturn(Constants.AUDIT_RESULT_APPROVAL, variables.get(CommonProcessVariableNames.FORM_FIELD_DECISION));
            eventResult.addReturn(Constants.AUDIT_RESULT_COMMENT, variables.get(CommonProcessVariableNames.FORM_FIELD_COMMENT));
            auditEventRecord.setResult(eventResult);
        }

        Task shadowTask = null;
        try {
            shadowTask = miscDataUtil.getShadowTask(variables, result);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve workflow-related task", e);
        }

        SpringApplicationContextHolder.getAuditService().audit(auditEventRecord, shadowTask);
    }
}
