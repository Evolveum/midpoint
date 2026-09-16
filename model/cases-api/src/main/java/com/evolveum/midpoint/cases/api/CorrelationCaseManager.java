/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.cases.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages correlation cases.
 *
 * TODO difference to {@link CaseManager} / {@link CaseEngine} ?
 */
public interface CorrelationCaseManager {

    /**
     * Creates or updates a correlation case for given correlation operation that finished in "uncertain" state.
     *
     * @param resourceObject Shadowed resource object we are correlating. Must have an OID.
     * @param preFocus The result of pre-inbounds application on the resource object.
     */
    void createOrUpdateCase(
            @NotNull ShadowType resourceObject,
            @NotNull ResourceType resource,
            @NotNull ProjectionHolderType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException;

    @Nullable CaseType findCorrelationCase(ShadowType resourceObject, boolean mustBeOpen, OperationResult result)
            throws SchemaException;

    /**
     * Closes a correlation case - if there's any - if it's no longer needed (e.g. because the uncertainty is gone).
     *
     * @param resourceObject Shadowed resource object we correlate. Must have an OID.
     *
     * TODO don't look for cases if not necessary (timestamps?)
     */
    void closeCaseIfStillOpen(
            @NotNull ShadowType resourceObject,
            @NotNull OperationResult result) throws SchemaException;

    /**
     * Preconditions:
     *
     * - case is freshly fetched,
     * - case is a correlation one
     */
    void completeCorrelationCase(
            @NotNull CaseType aCase,
            @NotNull CorrelationService.CaseCloser caseCloser,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Executes retry-safe correlation completion logic before the case is persisted as closing.
     * Throws on failure to keep the case open.
     */
    void prepareCorrelationCaseClosing(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException;

}
