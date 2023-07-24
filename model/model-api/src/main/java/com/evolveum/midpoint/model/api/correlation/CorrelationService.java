/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateCorrelationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

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
     * Correlates the provided (partial) focus object to a set of candidate matches.
     *
     * TODO finish the method signature
     */
    @NotNull CompleteCorrelationResult correlate(
            @NotNull FocusType preFocus,
            @NotNull ObjectTemplateType objectTemplate, //todo should be removed, archetype is to be here instead
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

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
     *
     * @param caseCloser Makes the case definitely closed. (This functionality must be provided by the caller.)
     */
    void completeCorrelationCase(
            @NotNull CaseType currentCase,
            @NotNull CaseCloser caseCloser,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Instantiates a correlator
     */
//    CorrelatorConfiguration determineCorrelatorConfiguration(@NotNull ObjectTemplateType objectTemplate,
//            SystemConfigurationType systemConfiguration);

    PathSet determineCorrelatorConfiguration(@NotNull CorrelatorDiscriminator discriminator, String archetypeOid, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException;


    @FunctionalInterface
    interface CaseCloser {
        /** Does everything needed to (definitely) close the case. */
        void closeCase(OperationResult result) throws ObjectNotFoundException;
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
