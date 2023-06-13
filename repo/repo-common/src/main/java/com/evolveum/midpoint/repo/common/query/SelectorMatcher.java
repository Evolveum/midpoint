/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.query;

import static com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection.NO_DELEGATOR;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseProcessingContextDescription;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.selector.eval.SelectorProcessingTracer;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

/**
 * Modernized version of {@link RepositoryService#selectorMatches(ObjectSelectorType, PrismObject,
 * ObjectFilterExpressionEvaluator, Trace, String)}.
 *
 * Usually a short-lived object, useful for evaluation of applicability of a selector on a given value or a couple of values.
 *
 * @see SelectorToFilterTranslator
 */
@SuppressWarnings("deprecation") // just because of javadoc
public class SelectorMatcher {

    private static final Trace LOGGER = TraceManager.getTrace(SelectorMatcher.class);

    @NotNull private final ValueSelector selector;

    private SelectorProcessingTracer tracer;

    private ObjectFilterExpressionEvaluator filterEvaluator;

    private SelectorMatcher(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    // We may provide analogous method for parsed selector, when necessary.
    public static SelectorMatcher forSelector(@NotNull ObjectSelectorType selectorBean)
            throws ConfigurationException {
        return new SelectorMatcher(
                ValueSelector.parse(selectorBean));
    }

    @SuppressWarnings("unused") // most probably will be used in near future
    public SelectorMatcher withFilterExpressionEvaluator(@NotNull ObjectFilterExpressionEvaluator evaluator) {
        this.filterEvaluator = evaluator;
        return this;
    }

    public SelectorMatcher withLogging(@NotNull Trace logger) {
        return withLogging(logger, "");
    }

    public SelectorMatcher withLogging(@NotNull Trace logger, @NotNull String logPrefix) {
        this.tracer = SelectorProcessingTracer.loggerBased(logger, logPrefix);
        return this;
    }

    public boolean matches(@NotNull PrismObject<?> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return matches(object.getValue());
    }

    public boolean matches(@NotNull PrismValue value)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        return selector.matches(
                value,
                new ClauseMatchingContext(
                        filterEvaluator,
                        Objects.requireNonNullElseGet(tracer, () -> SelectorProcessingTracer.loggerBased(LOGGER)),
                        CommonTaskBeans.get().repositoryService,
                        null,
                        null,
                        null,
                        new ClauseProcessingContextDescription.Default(),
                        NO_DELEGATOR));
    }
}
