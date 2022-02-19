/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.extension;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.model.impl.correlator.CorrelationCaseManager;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CorrelationCaseEngineExtension implements EngineExtension {

    @Autowired private CaseManager caseManager;
    @Autowired private CorrelationCaseManager correlationCaseManager;

    @PostConstruct
    public void init() {
        caseManager.registerEngineExtension(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value(), this);
    }

    @Override
    public void finishCaseClosing(
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException {

        boolean shouldClose =
                correlationCaseManager.completeCorrelationCase(operation.getCurrentCase(), operation.getTask(), result);

        if (shouldClose) {
            operation.closeCaseInRepository(result);
        }
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
        return new CorrelationCaseStageOpeningResult();
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

        if (allOutcomes.size() == 1) {
            return new CorrelationCaseStageClosingResult(allOutcomes.iterator().next());
        } else {
            // TODO we should resolve this somehow
            throw new IllegalStateException("There is not a single outcome: " + allOutcomes);
        }
    }

    @Override
    public @NotNull WorkItemCompletionResult processWorkItemCompletion(
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) throws SchemaException {
        //noinspection Convert2Lambda
        return new WorkItemCompletionResult() {
            @Override
            public boolean shouldCloseOtherWorkItems() {
                // Operators are equivalent: if one completes the item, all items are done.
                // (Moreover, there should be only one item.)
                return true;
            }
        };
    }

    @Override
    public @NotNull AuditingExtension getAuditingExtension() {
        return new CorrelationCaseAuditingExtension();
    }
}
