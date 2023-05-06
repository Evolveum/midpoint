/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Evaluates "filter" object selector clause.
 *
 * Note that applicability checking si provided by the repository service.
 */
public class Filter extends AbstractSelectorClauseEvaluation {

    @NotNull private final SearchFilterType filter;

    public Filter(@NotNull SearchFilterType filter, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.filter = filter;
    }

    public void applyFilter()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ObjectFilter increment = parseAndEvaluateFilter();
        if (increment != null) {
            ObjectQueryUtil.assertNotRaw(
                    increment,
                    "Filter in authorization object has undefined items."
                            + " Maybe a 'type' specification is missing in the authorization?");
            ObjectQueryUtil.assertPropertyOnly(
                    increment, "Filter in authorization object is not property-only filter");
        }
        LOGGER.trace("      applying property filter {}", increment);
        fCtx.addConjunction(increment);
    }

    private ObjectFilter parseAndEvaluateFilter()
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ObjectFilter parsedFilter =
                PrismContext.get().getQueryConverter().createObjectFilter(fCtx.getObjectDefinition(), filter);
        if (parsedFilter == null) {
            return null; // TODO what to do here?
        } else {
            return fCtx.createFilterEvaluator()
                    .evaluate(parsedFilter);
        }
    }
}
