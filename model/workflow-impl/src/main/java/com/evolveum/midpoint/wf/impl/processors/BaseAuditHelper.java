/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.jobs.Job;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class BaseAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAuditHelper.class);

    @Autowired
    private MiscDataUtil miscDataUtil;

    @Autowired
    private WorkItemProvider workItemProvider;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    public AuditEventRecord prepareProcessInstanceAuditRecord(Map<String, Object> variables, Job job, AuditEventStage stage, OperationResult result) {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        auditEventRecord.setEventStage(stage);

        String requesterOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        if (requesterOid != null) {
            try {
                auditEventRecord.setInitiator(miscDataUtil.getRequester(variables, result));
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            }
        }

        PrismObject<GenericObjectType> processInstanceObject = new PrismObject<>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        processInstanceObject.asObjectable().setName(new PolyStringType((String) variables.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME)));
        processInstanceObject.asObjectable().setOid(job.getActivitiId());
        auditEventRecord.setTarget(processInstanceObject);

        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);

        return auditEventRecord;
    }

    public AuditEventRecord prepareWorkItemAuditRecord(TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORK_ITEM);
        auditEventRecord.setEventStage(stage);

        Map<String, Object> variables = taskEvent.getVariables();
        String requesterOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        if (requesterOid != null) {
            try {
                auditEventRecord.setInitiator(miscDataUtil.getRequester(variables, result));
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve the workflow process instance requester information", e);
            }
        }

        WorkItemType workItemType = workItemProvider.taskEventToWorkItem(taskEvent, false, false, result);

        // temporary hacking (work items in audit are not supported)
        PrismObject<GenericObjectType> targetObject = new PrismObject<>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        targetObject.asObjectable().setName(workItemType.getName());
        targetObject.asObjectable().setOid(workItemType.getWorkItemId());
        auditEventRecord.setTarget(targetObject);
//      auditEventRecord.setTarget(workItemType.asPrismObject());

        String assigneeOid = taskEvent.getAssigneeOid();
        if (stage == AuditEventStage.REQUEST) {
            if (assigneeOid != null) {
                try {
                    auditEventRecord.setTargetOwner(repositoryService.getObject(UserType.class, assigneeOid, null, result));
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't retrieve the work item assignee information", e);
                }
            }
        } else {
            MidPointPrincipal principal;
			try {
				principal = securityEnforcer.getPrincipal();
				auditEventRecord.setTargetOwner(principal.getUser().asPrismObject());
			} catch (SecurityViolationException e) {
				LOGGER.warn("Not recording user in the audit record because there is no user: "+e.getMessage(), e);
			}
        }

        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);
        if (stage == AuditEventStage.EXECUTION) {
            auditEventRecord.setResult((String) variables.get(CommonProcessVariableNames.FORM_FIELD_DECISION));
            auditEventRecord.setMessage((String) variables.get(CommonProcessVariableNames.FORM_FIELD_COMMENT));
        }

        return auditEventRecord;
    }
}