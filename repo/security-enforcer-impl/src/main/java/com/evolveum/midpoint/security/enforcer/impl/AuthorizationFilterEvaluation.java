/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.SecurityEnforcerImpl.FILTER_TRACE_ENABLED;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Evaluation of given {@link Authorization} aimed to determine a "security filter", i.e. additional filter to be
 * applied in searching/filtering situation.
 *
 * It is a part of {@link EnforcerFilterOperation}.
 */
class AuthorizationFilterEvaluation<T> extends AuthorizationEvaluation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final Class<T> filterType;
    @Nullable private final ObjectFilter originalFilter;
    @NotNull private final List<ValueSelector> objectSelectors;
    @NotNull private final String selectorLabel;
    private final boolean includeSpecial;
    private ObjectFilter autzFilter = null;
    private boolean applicable;

    AuthorizationFilterEvaluation(
            @NotNull String id,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter originalFilter,
            @NotNull Authorization authorization,
            @NotNull List<ValueSelector> objectSelectors,
            @NotNull String selectorLabel,
            boolean includeSpecial,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        super(id, authorization, op, result);
        this.filterType = filterType;
        this.originalFilter = originalFilter;
        this.includeSpecial = includeSpecial;
        this.objectSelectors = objectSelectors;
        this.selectorLabel = selectorLabel;
    }

    boolean computeFilter()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (objectSelectors.isEmpty()) {

            // FIXME logging
            LOGGER.trace("      No {} specification in authorization (authorization is universally applicable)", selectorLabel);
            autzFilter = FilterCreationUtil.createAll();
            applicable = true;

        } else {
            int i = 0;
            for (var objectSelector : objectSelectors) {
                processSelector(i++, objectSelector);
            }
        }
        traceAutzProcessingEnd();
        return applicable;
    }

    private void processSelector(int i, ValueSelector selector)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        Specification base = Specification.of(
                selector, authorization.getItems(), authorization.getExceptItems(), TracingUtil.getHumanReadableDesc(selector));
        Specification adjusted = base.adjust(filterType);
        if (adjusted == null) {
            // TODO log
        } else {
            var evaluation = new SelectorFilterEvaluation<>(
                    String.valueOf(i), adjusted, filterType, originalFilter, adjusted.getDescription(),
                    selectorLabel, AuthorizationFilterEvaluation.this, result);
            if (evaluation.processFilter(includeSpecial)) {
                autzFilter = ObjectQueryUtil.filterOr(autzFilter, evaluation.getSecurityFilter());
                applicable = true; // At least one selector is applicable => the whole authorization is applicable
            }
        }
    }

    ObjectFilter getAutzFilter() {
        return autzFilter;
    }

    private void traceAutzProcessingEnd() {
        if (FILTER_TRACE_ENABLED && op.traceEnabled) {
            LOGGER.trace("{} Finished deriving filter from {} (applicable: {}):\n{}",
                    end(), TracingUtil.describe(authorization), applicable,
                    DebugUtil.debugDump(autzFilter, 1));
        }
    }
}
