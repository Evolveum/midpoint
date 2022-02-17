/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;

import com.evolveum.midpoint.cases.api.extensions.WorkItemCompletionResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DefaultEngineExtension implements EngineExtension {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultEngineExtension.class);

    @NotNull private final CaseBeans beans;

    DefaultEngineExtension(@NotNull CaseBeans beans) {
        this.beans = beans;
    }

    @Override
    public void finishCaseClosing(@NotNull CaseEngineOperation operation, @NotNull OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        // No special action here. Let's just close the case.
        beans.miscHelper.closeCaseInRepository(operation.getCurrentCase(), result);
    }

    @Override
    public int getExpectedNumberOfStages(@NotNull CaseEngineOperation operation) {
        return 1;
    }

    @Override
    public boolean doesUseStages() {
        return false;
    }

    @Override
    public @NotNull StageOpeningResult processStageOpening(CaseEngineOperation operation, OperationResult result) {
        // In the future we might return sensible work items here (e.g. for escalation, etc).
        return new DefaultStageOpeningResult();
    }

    @Override
    public @NotNull WorkItemCompletionResult processWorkItemCompletion(
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        return new DefaultWorkItemCompletionResult();
    }

    @Override
    public @NotNull StageClosingResult processStageClosing(CaseEngineOperation operation, OperationResult result) {

        // There should be only a single work item, so the following is maybe an overkill
        Set<String> allOutcomes = operation.getCurrentCase().getWorkItem().stream()
                .filter(wi -> wi.getCloseTimestamp() != null)
                .map(AbstractWorkItemType::getOutput)
                .filter(Objects::nonNull)
                .map(AbstractWorkItemOutputType::getOutcome)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // This is relevant for manual cases -- TODO for correlation cases!
        return new DefaultStageClosingResult(
                getOutcomeUri(allOutcomes));
    }

    // see ManualConnectorInstance.translateOutcome(..) method
    private @NotNull String getOutcomeUri(Set<String> outcomes) {
        if (outcomes.isEmpty()) {
            return OperationResultStatusType.SUCCESS.toString();
        } else if (outcomes.size() == 1) {
            return requireNonNull(outcomes.iterator().next());
        } else {
            LOGGER.warn("Conflicting outcomes: {}", outcomes);
            return OperationResultStatusType.UNKNOWN.toString();
        }
    }

    @Override
    public void enrichCaseAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }
}
