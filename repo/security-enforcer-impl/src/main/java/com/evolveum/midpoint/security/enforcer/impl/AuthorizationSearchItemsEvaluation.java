/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

/**
 * Evaluation of given {@link Authorization} aimed to determine the items that can be used
 * in searching/filtering situation.
 *
 * It is a part of {@link EnforcerFilterOperation}.
 *
 * @see AuthorizationFilterEvaluation
 */
class AuthorizationSearchItemsEvaluation<O extends ObjectType> extends AuthorizationEvaluation {

    @NotNull private final Class<O> objectType;
    @NotNull private final List<ValueSelector> objectSelectors;

    AuthorizationSearchItemsEvaluation(
            int id,
            @NotNull Class<O> objectType,
            @NotNull Authorization authorization,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) throws ConfigurationException {
        super(id, authorization, op, result);
        this.objectType = objectType;
        this.objectSelectors = authorization.getParsedObjectSelectors();
    }

    @Nullable AuthorizedSearchItems getAuthorizedSearchItems() throws ConfigurationException {

        if (objectSelectors.isEmpty()) {
            traceEndApplied("applied (no object selectors)");
            return new AuthorizedSearchItems(authorization.getItems(), authorization.getExceptItems());
        }

        for (var objectSelector : objectSelectors) {
            var resultingSearchItems = processAuthorizedSearchItemsSelector(objectSelector);
            if (resultingSearchItems != null) {
                return resultingSearchItems;
            }
        }
        traceEndNotApplicable("not applicable (no selector applies)");
        return null;
    }

    private AuthorizedSearchItems processAuthorizedSearchItemsSelector(ValueSelector selector)
            throws ConfigurationException {

        String selectorDesc = TracingUtil.getHumanReadableDesc(selector);
        var baseSelector = SelectorWithItems.of(selector, authorization.getItems(), authorization.getExceptItems(), selectorDesc);
        var tieredSelectors = baseSelector.asTieredSelectors(objectType);
        if (tieredSelectors == null || !tieredSelectors.hasOverlapWith(objectType)) {
            traceAutzProcessingNote("selector %s cannot be matched to %s", selectorDesc, objectType.getSimpleName());
            return null;
        } else {
            traceEndApplied("applied; selector %s matches %s", selectorDesc, objectType.getSimpleName());
            return new AuthorizedSearchItems(tieredSelectors.getPositives(), tieredSelectors.getNegatives());
        }
    }

    record AuthorizedSearchItems(
            @NotNull PathSet positives, @NotNull PathSet negatives) {
    }
}
