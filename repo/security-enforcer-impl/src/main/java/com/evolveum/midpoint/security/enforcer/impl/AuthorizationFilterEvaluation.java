/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.impl.SecurityTraceEvent.AuthorizationFilterProcessingFinished;
import com.evolveum.midpoint.util.exception.*;

/**
 * Evaluation of given {@link Authorization} aimed to determine a "security filter", i.e. additional filter to be
 * applied in searching/filtering situation.
 *
 * It is a part of {@link EnforcerFilterOperation}.
 */
class AuthorizationFilterEvaluation<T> extends AuthorizationEvaluation {

    @NotNull private final Class<T> filterType;
    @Nullable private final ObjectFilter originalFilter;
    @NotNull private final List<ValueSelector> objectSelectors;
    @NotNull private final String selectorLabel;
    private ObjectFilter autzFilter = null;
    private boolean applicable;

    AuthorizationFilterEvaluation(
            int id,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter originalFilter,
            @NotNull Authorization authorization,
            @NotNull List<ValueSelector> objectSelectors,
            @NotNull String selectorLabel,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        super(id, authorization, op, result);
        this.filterType = filterType;
        this.originalFilter = originalFilter;
        this.objectSelectors = objectSelectors;
        this.selectorLabel = selectorLabel;
    }

    boolean computeFilter()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (objectSelectors.isEmpty()) {
            autzFilter = FilterCreationUtil.createAll();
            applicable = true;
            traceAutzProcessingEnd("no %s specification (authorization is universally applicable)", selectorLabel);
        } else {
            int i = 0;
            for (var objectSelector : objectSelectors) {
                processSelector(i++, objectSelector);
            }
            traceAutzProcessingEnd("%d selector(s) processed", i);
        }
        return applicable;
    }

    private void processSelector(int i, ValueSelector selector)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        String selectorDesc = TracingUtil.getHumanReadableDesc(selector);
        SelectorWithItems baseSelector =
                SelectorWithItems.of(selector, authorization.getItems(), authorization.getExceptItems(), selectorDesc);
        SelectorWithItems adjustedSelector = baseSelector.adjustToSubObjectFilter(filterType);
        if (adjustedSelector == null) {
            traceAutzProcessingNote("No adjustment for selector exists: %s", selectorDesc);
        } else {
            var evaluation = new SelectorFilterEvaluation<>(
                    selectorId(i), adjustedSelector, filterType, originalFilter, adjustedSelector.getDescription(),
                    selectorLabel, AuthorizationFilterEvaluation.this, result);
            if (evaluation.processFilter()) {
                autzFilter = ObjectQueryUtil.filterOr(autzFilter, evaluation.getSecurityFilter());
                applicable = true; // At least one selector is applicable => the whole authorization is applicable
            }
        }
    }

    public boolean isApplicable() {
        return applicable;
    }

    ObjectFilter getAutzFilter() {
        return autzFilter;
    }

    private void traceAutzProcessingEnd(String message, Object... arguments) {
        if (op.tracer.isEnabled()) {
            op.tracer.trace(
                    new AuthorizationFilterProcessingFinished(this, message, arguments));
        }
    }
}
