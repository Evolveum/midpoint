/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseProcessingContextDescription;
import com.evolveum.midpoint.schema.selector.eval.FilterCollector;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.selector.spec.SelfClause;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

class SelectorFilterEvaluation<T>
        extends SelectorEvaluation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    /** The object/value type we are searching for. */
    @NotNull private final Class<T> searchType;

    @Nullable private final ObjectFilter originalFilter;

    private final String selectorLabel;

    /** The result */
    @NotNull private final FilterCollector filterCollector;

    SelectorFilterEvaluation(
            @NotNull String id,
            @NotNull Specification specification,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter originalFilter,
            @NotNull String desc,
            String selectorLabel,
            @NotNull AuthorizationEvaluation authorizationEvaluation,
            @NotNull OperationResult result) throws SchemaException, ConfigurationException {
        super(id, specification.getSelector(), null, desc, authorizationEvaluation, result);
        this.searchType = filterType;
        this.originalFilter = originalFilter;
        this.selectorLabel = selectorLabel;
        this.filterCollector = FilterCollector.defaultOne();
    }

    boolean processFilter(boolean includeSpecial)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        ClauseFilteringContext ctx = new ClauseFilteringContext(
                searchType,
                selector.getTypeClass(searchType),
                originalFilter,
                authorizationEvaluation.authorization.maySkipOnSearch(),
                (clause, ctx1) -> {
                    if (!includeSpecial && clause instanceof SelfClause) {
                        ctx1.traceClauseNotApplicable(clause, "'self' clause should be skipped");
                        return false;
                    } else {
                        return true;
                    }
                },
                filterCollector,
                createFilterEvaluator(),
                new LoggingTracer(),
                b.repositoryService,
                this,
                getOwnerResolver(),
                this,
                ClauseProcessingContextDescription.defaultOne(),
                DelegatorSelection.NO_DELEGATOR);

        return selector.applyFilters(ctx);
    }

    ObjectFilter getSecurityFilter() {
        return filterCollector.getFilter();
    }

    ObjectFilterExpressionEvaluator createFilterEvaluator() {
        return authorizationEvaluation.createFilterEvaluator(selectorLabel);
    }
}
