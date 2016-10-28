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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.activiti.dao.WorkItemProvider;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.evolveum.midpoint.audit.api.AuditEventType.WORKFLOW_PROCESS_INSTANCE;

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

    public AuditEventRecord prepareProcessInstanceAuditRecord(WfTask wfTask, AuditEventStage stage, Map<String, Object> variables, OperationResult result) {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(WORKFLOW_PROCESS_INSTANCE);
        auditEventRecord.setEventStage(stage);
		auditEventRecord.setInitiator(wfTask.getRequesterIfExists(result));

        PrismObject<GenericObjectType> processInstanceObject = new PrismObject<>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        processInstanceObject.asObjectable().setName(new PolyStringType(wfTask.getProcessInstanceName()));
        processInstanceObject.asObjectable().setOid(wfTask.getProcessInstanceId());
        auditEventRecord.setTarget(processInstanceObject);

        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);

        return auditEventRecord;
    }

	// workItem contains taskRef, assignee, candidates resolved (if possible)
    public AuditEventRecord prepareWorkItemAuditRecord(WorkItemType workItem, WfTask wfTask, TaskEvent taskEvent, AuditEventStage stage,
			OperationResult result) throws WorkflowException {

        AuditEventRecord auditEventRecord = new AuditEventRecord();
        auditEventRecord.setEventType(AuditEventType.WORK_ITEM);
        auditEventRecord.setEventStage(stage);

        if (stage == AuditEventStage.REQUEST) {
            auditEventRecord.setInitiator(wfTask.getRequesterIfExists(result));
            auditEventRecord.setTargetOwner((PrismObject<UserType>) ObjectTypeUtil.getPrismObjectFromReference(workItem.getAssigneeRef()));
        } else {
            try {
                @SuppressWarnings("unchecked")
                PrismObject<UserType> principal = securityEnforcer.getPrincipal().getUser().asPrismObject();
                auditEventRecord.setInitiator(principal);
                auditEventRecord.setTargetOwner(principal);
            } catch (SecurityViolationException e) {
                auditEventRecord.setInitiator(null);
                auditEventRecord.setTargetOwner(null);
                LOGGER.warn("No initiator and target owner known for auditing work item completion: " + e.getMessage(), e);
            }
        }

        PrismObject<GenericObjectType> targetObject = new PrismObject<>(GenericObjectType.COMPLEX_TYPE, GenericObjectType.class);
        targetObject.asObjectable().setName(new PolyStringType(workItem.getName()));
        targetObject.asObjectable().setOid(workItem.getWorkItemId());
        auditEventRecord.setTarget(targetObject);

        auditEventRecord.setOutcome(OperationResultStatus.SUCCESS);
        if (stage == AuditEventStage.EXECUTION) {
			DecisionType decision = workItem.getDecision();
			if (decision != null) {
				auditEventRecord.setResult(decision.getResultAsString());
                String comment = decision.getComment() != null ? ": " + decision.getComment() : "";
				auditEventRecord.setMessage(decision.getResultAsString() + comment);
			}
        }

        return auditEventRecord;
    }
}
