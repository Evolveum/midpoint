/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.extensions;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import java.util.Collection;

/**
 * Provides functionality that the case engine calls when dealing with specific case archetypes (like approval cases, etc).
 *
 * TODO better name
 */
public interface EngineExtension {

    /**
     * Returns the case archetype OID(s) this extension handles.
     */
    @NotNull Collection<String> getArchetypeOids();

    /**
     * Called to finish case closing procedure. E.g. for approvals we may submit execution task here.
     *
     * When called, the case should be in `closing` state. This may happen e.g. when approval execution task is submitted.
     * After return, the case may be still in this state, or it may be `closed`.
     */
    void finishCaseClosing(@NotNull CaseEngineOperation operation, @NotNull OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException, SecurityViolationException;

    /**
     * Returns the number of stages the case is expected to go through. E.g. for approval cases, it is determined
     * from the approval schema. Must return 1 if {@link #doesUseStages()} is false.
     */
    int getExpectedNumberOfStages(@NotNull CaseEngineOperation operation);

    /**
     * Does this extension use stages at all? If not, there is only a single stage (numbered 1).
     */
    boolean doesUseStages();

    /**
     * May provide new work items and/or pre-computed stage result.
     */
    @NotNull StageOpeningResult processStageOpening(CaseEngineOperation operation, OperationResult result)
            throws SchemaException;

    /**
     * Does the specific stage closing activities (including determination of the case processing continuation).
     */
    @NotNull StageClosingResult processStageClosing(CaseEngineOperation operation, OperationResult result);

    /**
     * Processes work item completion. May update the case!
     *
     * Note: Work item is already updated (output written, closed).
     */
    @NotNull WorkItemCompletionResult processWorkItemCompletion(
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) throws SchemaException;

    /**
     * Returns an object that helps with audit records creation.
     */
    @NotNull AuditingExtension getAuditingExtension();
}
