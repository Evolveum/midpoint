/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.cases.api.extensions.*;

import com.evolveum.midpoint.cases.impl.engine.CaseBeans;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCaseSchemaType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Engine extension for "other" case archetypes. Currently used for manual provisioning and correlation cases,
 * but they will be eventually separated from it.
 */
public class DefaultEngineExtension implements EngineExtension {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultEngineExtension.class);

    @NotNull private final CaseBeans beans;

    public DefaultEngineExtension(@NotNull CaseBeans beans) {
        this.beans = beans;
    }

    @Override
    public @NotNull Collection<String> getArchetypeOids() {
        return List.of(); // This extension is applied "manually" after no suitable extension is found.
    }

    @Override
    public void finishCaseClosing(@NotNull CaseEngineOperation operation, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException {
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
        return new DefaultStageOpeningResult(
                beans.simpleStageOpeningHelper.createWorkItems(
                        getCaseSchema(operation),
                        operation,
                        result));
    }

    protected SimpleCaseSchemaType getCaseSchema(CaseEngineOperation operation) {
        return null;
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

        List<String> outcomesFromEarliest = operation.getCurrentCase().getWorkItem().stream()
                .filter(wi -> wi.getCloseTimestamp() != null)
                .sorted(Comparator.comparing(wi -> XmlTypeConverter.toMillis(wi.getCloseTimestamp())))
                .map(AbstractWorkItemType::getOutput)
                .filter(Objects::nonNull)
                .map(AbstractWorkItemOutputType::getOutcome)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> uniqueOutcomes = new HashSet<>(outcomesFromEarliest);
        if (uniqueOutcomes.size() > 1) {
            LOGGER.warn("Different outcomes for {}: {}", operation, uniqueOutcomes);
        }

        return new DefaultStageClosingResult(
                selectOutcomeUri(outcomesFromEarliest, uniqueOutcomes));
    }

    protected String selectOutcomeUri(List<String> outcomesFromEarliest, Set<String> uniqueOutcomes) {
        if (uniqueOutcomes.size() == 1) {
            return uniqueOutcomes.iterator().next();
        } else if (outcomesFromEarliest.isEmpty()) {
            return null;
        } else {
            return outcomesFromEarliest.get(0);
        }
    }

    @Override
    public @NotNull AuditingExtension getAuditingExtension() {
        return new DefaultAuditingExtension();
    }
}
