/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CandidateDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Contains correlation-related methods that should be accessible from the outside of `model` module.
 */
public interface CorrelationService {

    /**
     * Describes the provided correlation case by providing {@link CorrelationCaseDescription} object.
     *
     * Currently, it
     *
     * . takes the shadow stored in the correlation case (i.e. does NOT fetch it anew),
     * . recomputes inbound mappings (i.e. ignores stored pre-focus),
     * . and processes candidate owners stored in the correlation case (i.e. does NOT search for them again).
     *
     * The {@link CorrelationCaseDescriptionOptions} parameter signals if the client wishes to provide also
     * the correlation explanation, or not. (In the future, we may provide options also for behavior in points 1-3
     * mentioned above.)
     */
    @NotNull CorrelationCaseDescription<?> describeCorrelationCase(
            @NotNull CaseType aCase,
            @Nullable CorrelationCaseDescriptionOptions options,
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

    @FunctionalInterface
    interface CaseCloser {
        /** Closes the case in repository. */
        void closeCaseInRepository(OperationResult result) throws ObjectNotFoundException;
    }

    /**
     * Options for {@link #describeCorrelationCase(CaseType, CorrelationCaseDescriptionOptions, Task, OperationResult)} method.
     *
     * TODO Make also other parts of the method behavior configurable.
     */
    class CorrelationCaseDescriptionOptions implements Serializable {

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

        public static boolean isExplain(CorrelationCaseDescriptionOptions options) {
            return options != null && options.explain;
        }

        @Override
        public String toString() {
            return "CorrelationCaseDescriptionOptions{" +
                    "explain=" + explain +
                    '}';
        }
    }
}
