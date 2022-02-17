/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.impl.engine.helpers;

import static com.evolveum.midpoint.audit.api.AuditEventStage.EXECUTION;
import static com.evolveum.midpoint.audit.api.AuditEventType.WORKFLOW_PROCESS_INSTANCE;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.AuditingConstants;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;

import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;

import com.evolveum.midpoint.model.common.util.AuditHelper;

import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Deals with preparation and recording of case-related audit events.
 */
@Component
public class CaseAuditHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CaseAuditHelper.class);

    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private PrismContext prismContext;
    @Autowired private CaseMiscHelper miscHelper;
    @Autowired private SchemaService schemaService;
    @Autowired private AuditHelper modelAuditHelper;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    //region Recoding of audit events
    public void auditPendingRecords(CaseEngineOperationImpl ctx, OperationResult result) {
        for (AuditEventRecord record : ctx.pendingAuditRecords) {
            modelAuditHelper.audit(record, null, ctx.getTask(), result);
        }
    }
    //endregion

    //region Preparation of audit events
    public AuditEventRecord prepareCaseRecord(
            @NotNull CaseEngineOperationImpl operation,
            @NotNull AuditEventStage stage,
            @NotNull OperationResult result) {

        CaseType aCase = operation.getCurrentCase();

        AuditEventRecord record = new AuditEventRecord();
        record.setEventType(WORKFLOW_PROCESS_INSTANCE);
        record.setEventStage(stage);
        // set real principal in case of explicitly requested process termination (MID-4263)
        record.setInitiator(miscHelper.getRequesterIfExists(aCase, result), prismContext);

        // TODO we could be more strict and allow non-existence of object only in case of "object add" delta. But we are not.
        ObjectReferenceType objectRef = resolveIfNeeded(aCase.getObjectRef(), true, result);
        record.setTargetRef(objectRef.asReferenceValue());

        record.setOutcome(OperationResultStatus.SUCCESS);

        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_TARGET, resolveIfNeeded(aCase.getTargetRef(), false, result));
        if (stage == EXECUTION) {
            String stageInfo = miscHelper.getCompleteStageInfo(aCase);
            record.setParameter(stageInfo);

            String answer = ApprovalUtils.getAnswerNice(aCase);
            record.setResult(answer);
            record.setMessage(stageInfo != null ? stageInfo + " : " + answer : answer);

            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NUMBER, aCase.getStageNumber());
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_COUNT, operation.getExpectedNumberOfStages());
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
        }
        record.addPropertyValue(AuditingConstants.AUDIT_PROCESS_INSTANCE_ID, aCase.getOid());
        OperationBusinessContextType businessContext = ApprovalContextUtil.getBusinessContext(aCase);
        String requesterComment = businessContext != null ? businessContext.getComment() : null;
        if (requesterComment != null) {
            record.addPropertyValue(AuditingConstants.AUDIT_REQUESTER_COMMENT, requesterComment);
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

    @SuppressWarnings("SameParameterValue")
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

        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_TARGET, resolveIfNeeded(aCase.getTargetRef(), false, result));
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_ORIGINAL_ASSIGNEE, resolveIfNeeded(workItem.getOriginalAssigneeRef(), false, result));
        record.addReferenceValues(AuditingConstants.AUDIT_CURRENT_ASSIGNEE, resolveIfNeeded(workItem.getAssigneeRef(), false, result));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NUMBER, workItem.getStageNumber());
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_COUNT, ApprovalContextUtil.getStageCount(wfc));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_NUMBER, WorkItemTypeUtil.getEscalationLevelNumber(workItem));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_NAME, WorkItemTypeUtil.getEscalationLevelName(workItem));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME, WorkItemTypeUtil.getEscalationLevelDisplayName(workItem));
        record.addPropertyValue(AuditingConstants.AUDIT_WORK_ITEM_ID, WorkItemId.create(aCase.getOid(), workItem.getId()).asString());
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
    public AuditEventRecord prepareWorkItemDeletedAuditRecord(
            CaseWorkItemType workItem,
            WorkItemEventCauseInformationType cause,
            CaseType aCase,
            OperationResult result) {

        AuditEventRecord record = prepareWorkItemAuditRecordCommon(workItem, aCase, AuditEventStage.EXECUTION, result);
        setInitiatorAndAttorneyFromPrincipal(record);

        if (cause != null) {
            if (cause.getType() != null) {
                record.addPropertyValue(AuditingConstants.AUDIT_CAUSE_TYPE, cause.getType().value());
            }
            if (cause.getName() != null) {
                record.addPropertyValue(AuditingConstants.AUDIT_CAUSE_NAME, cause.getName());
            }
            if (cause.getDisplayName() != null) {
                record.addPropertyValue(AuditingConstants.AUDIT_CAUSE_DISPLAY_NAME, cause.getDisplayName());
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
            String answer = ApprovalUtils.makeNiceFromUri(aCase, output); // FIXME
            record.setResult(answer);
            message.append(answer);
            if (output.getComment() != null) {
                message.append(" : ").append(output.getComment());
                record.addPropertyValue(AuditingConstants.AUDIT_COMMENT, output.getComment());
            }
        } else {
            message.append("(no decision)"); // TODO
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
