/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.audit.api.AuditEventStage.EXECUTION;
import static com.evolveum.midpoint.audit.api.AuditEventType.WORKFLOW_PROCESS_INSTANCE;

/**
 *  Deals with preparation and recording of audit events.
 */
@Component("wfAuditHelper")
public class AuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AuditHelper.class);

    @Autowired private AuditService auditService;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private PrismContext prismContext;
    @Autowired private MiscHelper miscHelper;
    @Autowired private SchemaService schemaService;
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;   // todo
    @Autowired private com.evolveum.midpoint.model.impl.util.AuditHelper modelAuditHelper;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    //region Recoding of audit events
    public void prepareProcessStartRecord(EngineInvocationContext ctx, OperationResult result) {
        prepareProcessStartEndRecord(ctx, AuditEventStage.REQUEST, result);
    }

    public void prepareProcessEndRecord(EngineInvocationContext ctx, OperationResult result) {
        prepareProcessStartEndRecord(ctx, AuditEventStage.EXECUTION, result);
    }

    private void prepareProcessStartEndRecord(EngineInvocationContext ctx, AuditEventStage stage, OperationResult result) {
        AuditEventRecord auditEventRecord = primaryChangeProcessor.prepareProcessInstanceAuditRecord(ctx.getCurrentCase(), stage, ctx.getWfContext(), result);
        ctx.addAuditRecord(auditEventRecord);
    }

    public void auditPreparedRecords(EngineInvocationContext ctx, OperationResult result) {
        for (AuditEventRecord record : ctx.pendingAuditRecords) {
            modelAuditHelper.audit(record, null, ctx.getTask(), result);
        }
    }
    //endregion


    //region Preparation of audit events
    public AuditEventRecord prepareProcessInstanceAuditRecord(CaseType aCase, AuditEventStage stage, OperationResult result) {

        ApprovalContextType wfc = aCase.getApprovalContext();

        AuditEventRecord record = new AuditEventRecord();
        record.setEventType(WORKFLOW_PROCESS_INSTANCE);
        record.setEventStage(stage);
        record.setInitiator(miscHelper.getRequesterIfExists(aCase, result), prismContext);           // set real principal in case of explicitly requested process termination (MID-4263)

        // TODO we could be more strict and allow non-existence of object only in case of "object add" delta. But we are not.
        ObjectReferenceType objectRef = resolveIfNeeded(aCase.getObjectRef(), true, result);
        record.setTargetRef(objectRef.asReferenceValue());

        record.setOutcome(OperationResultStatus.SUCCESS);

        record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_TARGET, resolveIfNeeded(aCase.getTargetRef(), false, result));
        if (stage == EXECUTION) {
            String stageInfo = miscHelper.getCompleteStageInfo(aCase);
            record.setParameter(stageInfo);
            String answer = miscHelper.getAnswerNice(aCase);
            record.setResult(answer);
            record.setMessage(stageInfo != null ? stageInfo + " : " + answer : answer);

            record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NUMBER, aCase.getStageNumber());
            record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_COUNT, ApprovalContextUtil.getStageCount(wfc));
            record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
            record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
        }
        record.addPropertyValue(WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID, aCase.getOid());
        OperationBusinessContextType businessContext = ApprovalContextUtil.getBusinessContext(aCase);
        String requesterComment = businessContext != null ? businessContext.getComment() : null;
        if (requesterComment != null) {
            record.addPropertyValue(WorkflowConstants.AUDIT_REQUESTER_COMMENT, requesterComment);
        }
        return record;
    }

    private ObjectReferenceType resolveIfNeeded(ObjectReferenceType ref, boolean optional, OperationResult result) {
        if (ref == null || ref.getOid() == null || ref.asReferenceValue().getObject() != null || ref.getType() == null) {
            return ref;
        }
        ObjectTypes types = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
        if (types == null) {
            return ref;
        }
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                    .allowNotFound(optional)
                    .build();
            return ObjectTypeUtil.createObjectRef(
                    repositoryService.getObject(types.getClassDefinition(), ref.getOid(), options, result), prismContext);
        } catch (ObjectNotFoundException e) {
            if (optional) {
                LOGGER.trace("Couldn't resolve {} but it's perhaps expected", ref, e);
            } else {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve {}", e, ref);
            }
            return ref;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve {}", e, ref);
            return ref;
        }
    }

    private List<ObjectReferenceType> resolveIfNeeded(List<ObjectReferenceType> refs, boolean optional, OperationResult result) {
        return refs.stream()
                .map(ref -> resolveIfNeeded(ref, optional, result))
                .collect(Collectors.toList());
    }

    private AuditEventRecord prepareWorkItemAuditRecordCommon(CaseWorkItemType workItem, CaseType aCase, AuditEventStage stage,
            OperationResult result) {

        ApprovalContextType wfc = aCase.getApprovalContext();

        AuditEventRecord record = new AuditEventRecord();
        record.setEventType(AuditEventType.WORK_ITEM);
        record.setEventStage(stage);

        // TODO we could be more strict and allow non-existence of object only in case of "object add" delta. But we are not.
        ObjectReferenceType objectRef = resolveIfNeeded(aCase.getObjectRef(), true, result);
        record.setTargetRef(objectRef.asReferenceValue());

        record.setOutcome(OperationResultStatus.SUCCESS);
        record.setParameter(miscHelper.getCompleteStageInfo(aCase));

        record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_TARGET, resolveIfNeeded(aCase.getTargetRef(), false, result));
        record.addReferenceValueIgnoreNull(WorkflowConstants.AUDIT_ORIGINAL_ASSIGNEE, resolveIfNeeded(workItem.getOriginalAssigneeRef(), false, result));
        record.addReferenceValues(WorkflowConstants.AUDIT_CURRENT_ASSIGNEE, resolveIfNeeded(workItem.getAssigneeRef(), false, result));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NUMBER, workItem.getStageNumber());
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_COUNT, ApprovalContextUtil.getStageCount(wfc));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_NUMBER, ApprovalContextUtil.getEscalationLevelNumber(workItem));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_NAME, ApprovalContextUtil.getEscalationLevelName(workItem));
        record.addPropertyValueIgnoreNull(WorkflowConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME, ApprovalContextUtil.getEscalationLevelDisplayName(workItem));
        record.addPropertyValue(WorkflowConstants.AUDIT_WORK_ITEM_ID, WorkItemId.create(aCase.getOid(), workItem.getId()).asString());
        return record;
    }

    // workItem contains taskRef, assignee, originalAssignee, candidates resolved (if possible)
    public AuditEventRecord prepareWorkItemCreatedAuditRecord(CaseWorkItemType workItem, CaseType aCase, OperationResult result) {

        AuditEventRecord record = prepareWorkItemAuditRecordCommon(workItem, aCase, AuditEventStage.REQUEST, result);
        record.setInitiator(miscHelper.getRequesterIfExists(aCase, result), prismContext);
        record.setMessage(miscHelper.getCompleteStageInfo(aCase));
        return record;
    }

    // workItem contains taskRef, assignee, candidates resolved (if possible)
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(CaseWorkItemType workItem, WorkItemEventCauseInformationType cause,
            CaseType aCase, OperationResult result) {

        AuditEventRecord record = prepareWorkItemAuditRecordCommon(workItem, aCase, AuditEventStage.EXECUTION, result);
        setInitiatorAndAttorneyFromPrincipal(record);

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
        String stageInfo = miscHelper.getCompleteStageInfo(aCase);
        if (stageInfo != null) {
            message.append(stageInfo).append(" : ");
        }
        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output != null) {
            String answer =
                    CaseTypeUtil.isApprovalCase(aCase) ?
                            ApprovalUtils.makeNiceFromUri(output.getOutcome()) : output.getOutcome();
            record.setResult(answer);
            message.append(answer);
            if (output.getComment() != null) {
                message.append(" : ").append(output.getComment());
                record.addPropertyValue(WorkflowConstants.AUDIT_COMMENT, output.getComment());
            }
        } else {
            message.append("(no decision)");        // TODO
        }
        record.setMessage(message.toString());
        return record;
    }

    private void setInitiatorAndAttorneyFromPrincipal(AuditEventRecord record) {
        try {
            MidPointPrincipal principal = securityContextManager.getPrincipal();
            record.setInitiator(principal.getFocus().asPrismObject(), prismContext);
            if (principal.getAttorney() != null) {
                record.setAttorney(principal.getAttorney().asPrismObject(), prismContext);
            }
        } catch (SecurityViolationException e) {
            record.setInitiator(null, prismContext);
            LOGGER.warn("No initiator known for auditing work item event: " + e.getMessage(), e);
        }
    }
    //endregion

}
