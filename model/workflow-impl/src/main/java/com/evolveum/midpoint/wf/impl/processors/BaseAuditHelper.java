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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.audit.api.AuditEventStage.EXECUTION;
import static com.evolveum.midpoint.audit.api.AuditEventType.WORKFLOW_PROCESS_INSTANCE;

/**
 * @author mederly
 */
@Component
public class BaseAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseAuditHelper.class);

    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private PrismContext prismContext;

    @Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

    public AuditEventRecord prepareProcessInstanceAuditRecord(WfTask wfTask, AuditEventStage stage, OperationResult result) {

		WfContextType wfc = wfTask.getTask().getWorkflowContext();

		AuditEventRecord record = new AuditEventRecord();
        record.setEventType(WORKFLOW_PROCESS_INSTANCE);
        record.setEventStage(stage);
		record.setInitiator(wfTask.getRequesterIfExists(result));

		ObjectReferenceType objectRef = resolveIfNeeded(wfc.getObjectRef(), result);
		record.setTarget(objectRef.asReferenceValue());

        record.setOutcome(OperationResultStatus.SUCCESS);

		record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_OBJECT, objectRef);
		record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_TARGET, resolveIfNeeded(wfc.getTargetRef(), result));
		if (stage == EXECUTION) {
			String stageInfo = wfTask.getCompleteStageInfo();
			record.setParameter(stageInfo);
			String answer = wfTask.getAnswerNice();
			record.setResult(answer);
			record.setMessage(stageInfo != null ? stageInfo + " : " + answer : answer);

			record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NUMBER, wfc.getStageNumber());
			record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_COUNT, WfContextUtil.getStageCount(wfc));
			record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NAME, WfContextUtil.getStageName(wfc));
			record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_DISPLAY_NAME, WfContextUtil.getStageDisplayName(wfc));
		}
		record.addPropertyValue(WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID, wfc.getProcessInstanceId());
		OperationBusinessContextType businessContext = WfContextUtil.getBusinessContext(wfc);
		String requesterComment = businessContext != null ? businessContext.getComment() : null;
		if (requesterComment != null) {
			record.addPropertyValue(WorkflowConstants.AUDIT_REQUESTER_COMMENT, requesterComment);
		}
		return record;
    }

	private ObjectReferenceType resolveIfNeeded(ObjectReferenceType ref, OperationResult result) {
		if (ref == null || ref.getOid() == null || ref.asReferenceValue().getObject() != null || ref.getType() == null) {
			return ref;
		}
		ObjectTypes types = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
		if (types == null) {
			return ref;
		}
		try {
			return ObjectTypeUtil.createObjectRef(
					repositoryService.getObject(types.getClassDefinition(), ref.getOid(), null, result));
		} catch (ObjectNotFoundException|SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve {}", e, ref);
			return ref;
		}
	}

	private List<ObjectReferenceType> resolveIfNeeded(List<ObjectReferenceType> refs, OperationResult result) {
		return refs.stream()
				.map(ref -> resolveIfNeeded(ref, result))
				.collect(Collectors.toList());
	}

	public AuditEventRecord prepareWorkItemAuditReportCommon(WorkItemType workItem, WfTask wfTask, AuditEventStage stage,
			OperationResult result) throws WorkflowException {

		AuditEventRecord record = new AuditEventRecord();
		record.setEventType(AuditEventType.WORK_ITEM);
		record.setEventStage(stage);

		ObjectReferenceType objectRef = resolveIfNeeded(WfContextUtil.getObjectRef(workItem), result);
		record.setTarget(objectRef.asReferenceValue());

		record.setOutcome(OperationResultStatus.SUCCESS);
		record.setParameter(wfTask.getCompleteStageInfo());

		record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_OBJECT, objectRef);
		record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_TARGET, resolveIfNeeded(WfContextUtil.getTargetRef(workItem), result));
		record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_ORIGINAL_ASSIGNEE, resolveIfNeeded(workItem.getOriginalAssigneeRef(), result));
		record.addReferenceValues(WorkflowConstants.AUDIT_CURRENT_ASSIGNEE, resolveIfNeeded(workItem.getAssigneeRef(), result));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NUMBER, workItem.getStageNumber());
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_COUNT, WfContextUtil.getStageCount(workItem));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NAME, WfContextUtil.getStageName(workItem));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_DISPLAY_NAME, WfContextUtil.getStageDisplayName(workItem));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_NUMBER, WfContextUtil.getEscalationLevelNumber(workItem));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_NAME, WfContextUtil.getEscalationLevelName(workItem));
		record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME, WfContextUtil.getEscalationLevelDisplayName(workItem));
		record.addPropertyValue(WorkflowConstants.AUDIT_WORK_ITEM_ID, workItem.getExternalId());
		record.addPropertyValue(WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID, WfContextUtil.getProcessInstanceId(workItem));
		return record;
	}

	// workItem contains taskRef, assignee, originalAssignee, candidates resolved (if possible)
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(WorkItemType workItem, WfTask wfTask, OperationResult result)
			throws WorkflowException {

        AuditEventRecord record = prepareWorkItemAuditReportCommon(workItem, wfTask, AuditEventStage.REQUEST, result);
		record.setInitiator(wfTask.getRequesterIfExists(result));
		record.setMessage(wfTask.getCompleteStageInfo());
        return record;
    }

	// workItem contains taskRef, assignee, candidates resolved (if possible)
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(WorkItemType workItem, WorkItemEventCauseInformationType cause,
			WfTask wfTask, OperationResult result) throws WorkflowException {

        AuditEventRecord record = prepareWorkItemAuditReportCommon(workItem, wfTask, AuditEventStage.EXECUTION, result);
		setCurrentUserAsInitiator(record);

		if (cause != null) {
			if (cause.getType() != null) {
				record.addPropertyValue(WorkflowConstants.AUDIT_CAUSE_TYPE, cause.getType().value());
			}
			if (cause.getName() != null) {
				record.addPropertyValue(WorkflowConstants.AUDIT_CAUSE_NAME, cause.getName());
			}
			if (cause.getDisplayName() != null) {
				record.addPropertyValue(WorkflowConstants.AUDIT_CAUSE_DISPLAY_NAME, cause.getDisplayName());
			}
		}

		// message + result
		StringBuilder message = new StringBuilder();
		String stageInfo = wfTask.getCompleteStageInfo();
		if (stageInfo != null) {
			message.append(stageInfo).append(" : ");
		}
		AbstractWorkItemOutputType output = workItem.getOutput();
		if (output != null) {
			String answer = ApprovalUtils.makeNiceFromUri(output.getOutcome());
			record.setResult(answer);
			message.append(answer);
			if (output.getComment() != null) {
				message.append(" : ").append(output.getComment());
				record.addPropertyValue(WorkflowConstants.AUDIT_COMMENT, output.getComment());
			}
		} else {
			message.append("(no decision)");		// TODO
		}
		record.setMessage(message.toString());
        return record;
    }

	private void setCurrentUserAsInitiator(AuditEventRecord record) {
		try {
			@SuppressWarnings("unchecked")
			PrismObject<UserType> principal = securityEnforcer.getPrincipal().getUser().asPrismObject();
			record.setInitiator(principal);
		} catch (SecurityViolationException e) {
			record.setInitiator(null);
			LOGGER.warn("No initiator known for auditing work item event: " + e.getMessage(), e);
		}
	}
}
