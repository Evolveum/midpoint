/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.cases.api.extensions.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Engine extension for "other" case archetypes. Currently used for manual provisioning and correlation cases,
 * but they will be eventually separated from it.
 */
public class DefaultEngineExtension implements EngineExtension {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultEngineExtension.class);

    @Override
    public @NotNull Collection<String> getArchetypeOids() {
        return List.of(); // This extension is applied "manually" after no suitable extension is found.
    }

    @Override
    public void finishCaseClosing(@NotNull CaseEngineOperation operation, @NotNull OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        // No special action here. Let's just close the case.
        operation.closeCaseInRepository(result);
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

        Set<String> allOutcomes = operation.getCurrentCase().getWorkItem().stream()
                .filter(wi -> wi.getCloseTimestamp() != null)
                .map(AbstractWorkItemType::getOutput)
                .filter(Objects::nonNull)
                .map(AbstractWorkItemOutputType::getOutcome)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // This is relevant for manual cases
        return new DefaultStageClosingResult(
                getOutcomeUri(allOutcomes));
    }

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
    public @NotNull AuditingExtension getAuditingExtension() {
        return new DefaultAuditingExtension();
    }
}
