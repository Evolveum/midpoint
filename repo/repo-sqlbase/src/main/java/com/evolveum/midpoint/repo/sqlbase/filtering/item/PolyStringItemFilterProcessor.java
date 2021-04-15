/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.google.common.base.Strings;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;

/**
 * Filter processor for a polystring attribute path (Prism item).
 * This creates conditions to either {@code *_orig} or {@code *_norm} column depending on
 * matching conditions.
 * Sorting is always executed by {@code *_orig} column.
 */
public class PolyStringItemFilterProcessor
        extends ItemFilterProcessor<PropertyValueFilter<PolyString>> {

    private static final String STRICT = PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME.getLocalPart();
    private static final String ORIG = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME.getLocalPart();
    private static final String NORM = PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME.getLocalPart();
    private static final String DEFAULT = PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart();

    // special IC cases of match rules, these are not defined in PrismConstants
    private static final String STRICT_IGNORE_CASE = "strictIgnoreCase";
    private static final String ORIG_IGNORE_CASE = "origIgnoreCase";
    private static final String NORM_IGNORE_CASE = "normIgnoreCase";

    private final StringPath origPath;
    private final StringPath normPath;

    public PolyStringItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, StringPath> origMapping,
            Function<EntityPath<?>, StringPath> normMapping) {
        super(context);
        this.origPath = origMapping.apply(context.path());
        this.normPath = normMapping.apply(context.path());
    }

    @Override
    public Predicate process(PropertyValueFilter<PolyString> filter) throws QueryException {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            return ExpressionUtils.and(
                    createBinaryCondition(filter, normPath,
                            ValueFilterValues.from(filter, p -> p.getNorm())),
                    createBinaryCondition(filter, origPath,
                            ValueFilterValues.from(filter, p -> p.getOrig())));
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, origPath,
                    ValueFilterValues.from(filter, p -> p.getOrig()));
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, normPath,
                    ValueFilterValues.from(filter, p -> p.getNorm()));
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'.");
        }
    }

    @Override
    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        return STRICT_IGNORE_CASE.equals(matchingRule)
                || ORIG_IGNORE_CASE.equals(matchingRule)
                || NORM_IGNORE_CASE.equals(matchingRule);
    }
}
