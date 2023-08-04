/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.impl.engine.events;

import static com.evolveum.midpoint.audit.api.AuditEventStage.EXECUTION;
import static com.evolveum.midpoint.audit.api.AuditEventType.WORKFLOW_PROCESS_INSTANCE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.AuditingConstants;

import com.evolveum.midpoint.cases.api.extensions.AuditingExtension;
import com.evolveum.midpoint.cases.impl.engine.CaseBeans;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;

import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Maintains prepared audit records for given {@link CaseEngineOperationImpl}.
 *
 * They are sent to the real audit service after successful commit, via the {@link #flush(OperationResult)} method.
 *
 * TODO clean up the code!
 * TODO free from approval-specific things
 */
public class PendingAuditRecords implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(PendingAuditRecords.class);

    /** Current operation carrying the whole context. */
    @NotNull private final CaseEngineOperationImpl operation;

    /** Approvals / provisioning / correlation - specific functionality. */
    @NotNull private final AuditingExtension extension;

    /** Commonly useful beans. */
    @NotNull private final CaseBeans beans;

    /** Audit records to be written after commit (see {@link #flush(OperationResult)} method). */
    @NotNull private final List<AuditEventRecord> records = new ArrayList<>();

    public PendingAuditRecords(@NotNull CaseEngineOperationImpl operation) {
        this.operation = operation;
        this.extension = operation.getEngineExtension().getAuditingExtension();
        this.beans = operation.getBeans();
    }

    //region Case level
    public void addCaseOpening(OperationResult result) {
        addCaseLevelRecord(AuditEventStage.REQUEST, result);
    }

    public void addCaseClosing(OperationResult result) {
        addCaseLevelRecord(AuditEventStage.EXECUTION, result);
    }

    private void addCaseLevelRecord(AuditEventStage stage, OperationResult result) {
        AuditEventRecord auditRecord = createCaseLevelRecord(stage, result);

        extension.enrichCaseRecord(auditRecord, operation, result);

        records.add(auditRecord);
    }

    private AuditEventRecord createCaseLevelRecord(@NotNull AuditEventStage stage, @NotNull OperationResult result) {

        CaseType aCase = operation.getCurrentCase();

        AuditEventRecord record = new AuditEventRecord();
        record.setEventType(WORKFLOW_PROCESS_INSTANCE);
        record.setEventStage(stage);
        record.setOutcome(OperationResultStatus.SUCCESS);

        // TODO we should set real principal in case of explicitly requested process termination (MID-4263)
        //  (here we use the case requester)
        record.setInitiator(
                beans.miscHelper.getRequesterIfExists(aCase, result));

        // TODO we could be more strict and allow non-existence of object only in case of "object add" delta. But we are not.
        ObjectReferenceType objectRef = resolveIfNeeded(aCase.getObjectRef(), true, result);
        ObjectReferenceType targetRef = resolveIfNeeded(aCase.getTargetRef(), false, result);

        record.setTargetRef(objectRef.asReferenceValue());

        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_TARGET, targetRef);

        if (stage == EXECUTION) {
            String stageInfo;
            if (operation.doesUseStages()) {
                stageInfo = ApprovalContextUtil.getCompleteStageInfo(aCase);
                record.setParameter(stageInfo);
            } else {
                stageInfo = null;
            }

            String answer = ApprovalUtils.getAnswerNice(aCase); // FIXME
            record.setResult(answer);
            record.setMessage(stageInfo != null ? stageInfo + " : " + answer : answer);

            if (operation.doesUseStages()) {
                record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NUMBER, aCase.getStageNumber());
                record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_COUNT, operation.getExpectedNumberOfStages());
                record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
                record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
            }
        }
        record.addPropertyValue(AuditingConstants.AUDIT_CASE_OID, aCase.getOid());
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_REQUESTER_COMMENT, ApprovalContextUtil.getRequesterComment(aCase));
        return record;
    }
    //endregion

    //region Work item level
    public void addWorkItemCreation(
            @NotNull CaseWorkItemType workItem,
            @NotNull OperationResult result) {
        // workItem contains taskRef, assignee, originalAssignee, candidates resolved (if possible)
        CaseType aCase = operation.getCurrentCase();

        AuditEventRecord record = prepareWorkItemAuditRecordCommon(workItem, AuditEventStage.REQUEST, result);
        record.setInitiator(
                beans.miscHelper.getRequesterIfExists(aCase, result));
        if (operation.doesUseStages()) {
            record.setMessage(ApprovalContextUtil.getCompleteStageInfo(aCase));
        }

        extension.enrichWorkItemCreatedAuditRecord(record, workItem, operation, result);

        records.add(record);
    }

    public void addWorkItemClosure(
            @NotNull CaseWorkItemType workItem,
            @Nullable WorkItemEventCauseInformationType cause,
            @NotNull OperationResult result) {
        // workItem contains taskRef, assignee, candidates resolved (if possible)

        // We don't pass userRef (initiator) to the audit method. It does need the whole object (not only the reference),
        // so it fetches it directly from the security enforcer (logged-in user). This could change in the future.

        CaseType aCase = operation.getCurrentCase();

        AuditEventRecord record = prepareWorkItemAuditRecordCommon(workItem, AuditEventStage.EXECUTION, result);
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
        String stageInfo = operation.doesUseStages() ?
                ApprovalContextUtil.getCompleteStageInfo(aCase) : null;
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

        extension.enrichWorkItemDeletedAuditRecord(record, workItem, operation, result);

        records.add(record);
    }

    private AuditEventRecord prepareWorkItemAuditRecordCommon(
            @NotNull CaseWorkItemType workItem,
            @NotNull AuditEventStage stage,
            @NotNull OperationResult result) {

        CaseType aCase = operation.getCurrentCase();

        ApprovalContextType wfc = aCase.getApprovalContext(); // FIXME!!!

        AuditEventRecord record = new AuditEventRecord();
        record.setEventType(AuditEventType.WORK_ITEM);
        record.setEventStage(stage);

        // TODO we could be more strict and allow non-existence of object only in case of "object add" delta. But we are not.
        ObjectReferenceType objectRef = resolveIfNeeded(aCase.getObjectRef(), true, result);
        record.setTargetRef(objectRef.asReferenceValue());

        record.setOutcome(OperationResultStatus.SUCCESS);
        if (operation.doesUseStages()) {
            record.setParameter(ApprovalContextUtil.getCompleteStageInfo(aCase));
        }

        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_OBJECT, objectRef);
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_TARGET, resolveIfNeeded(aCase.getTargetRef(), false, result));
        record.addReferenceValueIgnoreNull(AuditingConstants.AUDIT_ORIGINAL_ASSIGNEE, resolveIfNeeded(workItem.getOriginalAssigneeRef(), false, result));
        record.addReferenceValues(AuditingConstants.AUDIT_CURRENT_ASSIGNEE, resolveIfNeeded(workItem.getAssigneeRef(), false, result));
        if (operation.doesUseStages()) {
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NUMBER, workItem.getStageNumber());
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_COUNT, ApprovalContextUtil.getStageCount(wfc));
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_NAME, ApprovalContextUtil.getStageName(aCase));
            record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_STAGE_DISPLAY_NAME, ApprovalContextUtil.getStageDisplayName(aCase));
        }
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_NUMBER, WorkItemTypeUtil.getEscalationLevelNumber(workItem));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_NAME, WorkItemTypeUtil.getEscalationLevelName(workItem));
        record.addPropertyValueIgnoreNull(AuditingConstants.AUDIT_ESCALATION_LEVEL_DISPLAY_NAME, WorkItemTypeUtil.getEscalationLevelDisplayName(workItem));
        record.addPropertyValue(AuditingConstants.AUDIT_WORK_ITEM_ID, WorkItemId.create(aCase.getOid(), workItem.getId()).asString());
        return record;
    }
    //endregion

    //region Common
    private void setInitiatorAndAttorneyFromPrincipal(AuditEventRecord record) {
        MidPointPrincipal principal = operation.getPrincipal();
        record.setInitiator(
                principal.getFocusPrismObject());
        record.setAttorney(
                principal.getAttorneyPrismObject());
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
            Collection<SelectorOptions<GetOperationOptions>> options = SchemaService.get().getOperationOptionsBuilder()
                    .allowNotFound(optional)
                    .build();
            return ObjectTypeUtil.createObjectRef(
                    beans.repositoryService.getObject(types.getClassDefinition(), ref.getOid(), options, result));
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
    //endregion

    //region Misc
    /**
     * Writes prepared records to the audit.
     */
    public void flush(OperationResult result) {
        for (AuditEventRecord record : records) {
            beans.modelAuditHelper.audit(record, null, operation.getTask(), result);
        }
    }

    @Override
    public String debugDump(int indent) {
        // Temporary implementation
        return DebugUtil.debugDump(records, indent);
    }

    public int size() {
        return records.size();
    }
    //endregion
}
