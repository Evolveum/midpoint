/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.ClauseProcessingContextDescription;
import com.evolveum.midpoint.schema.selector.eval.FilterCollector;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.util.exception.*;

class SelectorFilterEvaluation<T>
        extends SelectorEvaluation {

    /** The object/value type we are searching for. */
    @NotNull private final Class<T> searchType;

    @Nullable private final ObjectFilter originalFilter;

    private final String selectorLabel;

    /** The result */
    @NotNull private final FilterCollector filterCollector;

    SelectorFilterEvaluation(
            @NotNull String id,
            @NotNull SelectorWithItems extendedSelector,
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter originalFilter,
            @NotNull String desc,
            String selectorLabel,
            @NotNull AuthorizationEvaluation authorizationEvaluation,
            @NotNull OperationResult result) {
        super(id, extendedSelector.getSelector(), null, desc, authorizationEvaluation, result);
        this.searchType = filterType;
        this.originalFilter = originalFilter;
        this.selectorLabel = selectorLabel;
        this.filterCollector = FilterCollector.defaultOne();
    }

    boolean processFilter()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        FilteringContext ctx = new FilteringContext(
                searchType,
                selector.getTypeClass(searchType),
                originalFilter,
                authorizationEvaluation.authorization.maySkipOnSearch(),
                filterCollector,
                createFilterEvaluator(),
                authorizationEvaluation.op.tracer,
                b.repositoryService,
                b.repositoryService.isNative(),
                this,
                getOwnerResolver(),
                this::resolveReference,
                ClauseProcessingContextDescription.defaultOne(id, desc),
                DelegatorSelection.NO_DELEGATOR);

        return selector.toFilter(ctx);
    }

    ObjectFilter getSecurityFilter() {
        return filterCollector.getFilter();
    }

    ObjectFilterExpressionEvaluator createFilterEvaluator() {
        return authorizationEvaluation.createFilterEvaluator(selectorLabel);
    }
}
