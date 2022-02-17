/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;
import com.evolveum.midpoint.wf.impl.processors.primary.WorkItemCompletion;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.StageClosing;
import com.evolveum.midpoint.wf.impl.processors.primary.StageOpening;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@Component
public class ApprovalsCaseEngineExtension implements EngineExtension {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalsCaseEngineExtension.class);

    @Autowired private ApprovalBeans beans;
    @Autowired private CaseManager caseManager;
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;

    @PostConstruct
    public void init() {
        caseManager.registerEngineExtension(SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value(), this);
    }

    @Override
    public void finishCaseClosing(@NotNull CaseEngineOperation operation, @NotNull OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {
        primaryChangeProcessor.finishCaseClosing(operation, result);
    }

    @Override
    public int getExpectedNumberOfStages(@NotNull CaseEngineOperation operation) {
        Integer stageCount = ApprovalContextUtil.getStageCount(getApprovalContext(operation));
        if (stageCount == null) {
            LOGGER.error("Couldn't determine stage count from the approval context:\n{}", operation.debugDump(1));
            throw new IllegalStateException("Couldn't determine stage count from the approval context");
        }
        return stageCount;
    }

    public ApprovalContextType getApprovalContext(CaseEngineOperation operation) {
        return operation.getCurrentCase().getApprovalContext();
    }

    @Override
    public boolean doesUseStages() {
        return true;
    }

    @Override
    public @NotNull StageOpeningResult processStageOpening(CaseEngineOperation operation, OperationResult result)
            throws SchemaException {
        return new StageOpening(operation, beans)
                .process(result);
    }

    @Override
    public @NotNull StageClosingResult processStageClosing(CaseEngineOperation operation, OperationResult result) {
        return new StageClosing(operation, beans)
                .process();
    }

    @Override
    public @NotNull WorkItemCompletionResult processWorkItemCompletion(
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) throws SchemaException {
        return new WorkItemCompletion(workItem, operation)
                .process();
    }

    @Override
    public void enrichCaseAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichCaseAuditRecord(auditEventRecord, operation);
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichWorkItemCreatedAuditRecord(auditEventRecord, operation);
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichWorkItemDeletedAuditRecord(auditEventRecord, operation);
    }
}
