/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CandidateDescription;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Supports correlation.
 */
@Experimental
public interface CorrelationService {

    /**
     * TODO
     */
    @NotNull CorrelationCaseDescription<?> describeCorrelationCase(
            @NotNull CaseType aCase,
            @Nullable CorrelationCaseDescriptionOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    /**
     * Instantiates the correlator for given correlation case. TODO remove this method - hide the logic in the service impl
     */
    Correlator instantiateCorrelator(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    /**
     * Completes given correlation case.
     *
     * Preconditions:
     *
     * - case is freshly fetched,
     * - case is a correlation one
     */
    void completeCorrelationCase(
            @NotNull CaseType currentCase,
            @NotNull CaseCloser closeCaseInRepository,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Creates the root correlator context for given configuration.
     */
    @NotNull CorrelatorContext<?> createRootCorrelatorContext(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration) throws ConfigurationException, SchemaException;

    /**
     * Clears the correlation state of a shadow.
     *
     * Does not do unlinking (if the shadow is linked)!
     */
    void clearCorrelationState(@NotNull String shadowOid, @NotNull OperationResult result) throws ObjectNotFoundException;

    /**
     * Executes the correlation in the standard way.
     */
    @NotNull CompleteCorrelationResult correlate(
            @NotNull CorrelatorContext<?> rootCorrelatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Executes the correlation for a given shadow + pre-focus.
     *
     * This is _not_ the standard use of the correlation, though.
     * (Note that it lacks e.g. the resource object delta information.)
     */
    @VisibleForTesting
    @NotNull CompleteCorrelationResult correlate(
            @NotNull ShadowType shadow,
            @Nullable FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Executes the correlation for a given shadow. (By computing pre-focus first.)
     *
     * This is _not_ the standard use. It lacks e.g. the resource object delta information. It is used in special cases like
     * {@link MidpointFunctions#findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)}.
     */
    @NotNull CompleteCorrelationResult correlate(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull Class<? extends FocusType> focusType,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Checks whether the supplied candidate owner would be the correlation result (if real correlation would take place).
     * Used for opportunistic synchronization.
     *
     * Why not doing the actual correlation? Because the owner may not exist in repository yet.
     */
    boolean checkCandidateOwner(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull FocusType candidateOwner,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** TEMPORARY!!! */
    ObjectTemplateType determineObjectTemplate(
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull FocusType preFocus,
            @NotNull OperationResult result)
                    throws SchemaException, ConfigurationException, ObjectNotFoundException;

    /** TODO Maybe temporary. Maybe visible for testing? */
    @NotNull <F extends FocusType> F computePreFocus(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull SynchronizationPolicy synchronizationPolicy,
            @NotNull Class<F> focusClass,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    @FunctionalInterface
    interface CaseCloser {
        /** Closes the case in repository. */
        void closeCaseInRepository(OperationResult result) throws ObjectNotFoundException;
    }

    class CorrelationCaseDescriptionOptions {
        /** Whether to explain the correlation. See {@link CandidateDescription#explanation}. */
        private boolean explain;

        public boolean isExplain() {
            return explain;
        }

        public void setExplain(boolean explain) {
            this.explain = explain;
        }

        public CorrelationCaseDescriptionOptions explain(boolean value) {
            setExplain(value);
            return this;
        }

        // TODO: whether to do the correlation anew

        public static boolean isExplain(CorrelationCaseDescriptionOptions options) {
            return options != null && options.explain;
        }
    }
}
