/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.google.common.base.Strings;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Filter processor for a polystring attribute path (Prism item).
 * This creates conditions to either {@code *_orig} or {@code *_norm} column depending on
 * matching conditions.
 * Sorting is always executed by {@code *_orig} column.
 *
 * @param <T> type of values in filter - PolyString, PolyStringType and String is supported
 */
public class PolyStringItemFilterProcessor<T>
        extends ItemValueFilterProcessor<PropertyValueFilter<T>> {

    public static final String STRICT = PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME.getLocalPart();
    public static final String ORIG = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME.getLocalPart();
    public static final String NORM = PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME.getLocalPart();
    public static final String DEFAULT = PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart();

    // special IC cases of match rules, these are not defined in PrismConstants
    public static final String STRICT_IGNORE_CASE = "strictIgnoreCase";
    public static final String ORIG_IGNORE_CASE = "origIgnoreCase";
    public static final String NORM_IGNORE_CASE = "normIgnoreCase";

    private final StringPath origPath;
    private final StringPath normPath;

    public <Q extends FlexibleRelationalPathBase<R>, R> PolyStringItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, StringPath> origMapping,
            Function<Q, StringPath> normMapping) {
        super(context);
        this.origPath = origMapping.apply(context.path());
        this.normPath = normMapping.apply(context.path());
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter) throws QueryException {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            return ExpressionUtils.and(
                    createBinaryCondition(filter, normPath,
                            ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractNorm)),
                    createBinaryCondition(filter, origPath,
                            ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractOrig)));
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, origPath,
                    ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractOrig));
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, normPath,
                    ValueFilterValues.from(filter, PolyStringItemFilterProcessor::extractNorm));
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'.");
        }
    }

    @Override
    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        return isIgnoreCasePolyStringFilter(filter);
    }

    /**
     * Extracted for other poly-string filters in other parts of the filter processor hierarchy.
     */
    public static boolean isIgnoreCasePolyStringFilter(ValueFilter<?, ?> filter) {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        return STRICT_IGNORE_CASE.equals(matchingRule)
                || ORIG_IGNORE_CASE.equals(matchingRule)
                || NORM_IGNORE_CASE.equals(matchingRule);
    }

    /**
     * Method extracting normalized value from (potentially poly-)string.
     * May require adapter method to provide {@link PrismContext} for normalization, see usages.
     */
    public static String extractNorm(Object value) {
        if (value instanceof String) {
            // we normalize the provided String value to ignore casing, etc.
            return PrismContext.get().getDefaultPolyStringNormalizer().normalize((String) value);
        } else if (value instanceof PolyString) {
            return ((PolyString) value).getNorm();
        } else if (value instanceof PolyStringType) {
            return ((PolyStringType) value).getNorm();
        } else {
            throw new IllegalArgumentException(
                    "Value [" + value + "] is neither String nor PolyString(Type).");
        }
    }

    public static String extractOrig(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof PolyString) {
            return ((PolyString) value).getOrig();
        } else if (value instanceof PolyStringType) {
            return ((PolyStringType) value).getOrig();
        } else {
            throw new IllegalArgumentException(
                    "Value [" + value + "] is neither String nor PolyString(Type).");
        }
    }

    @Override
    public Expression<?> rightHand(ValueFilter<?, ?> filter) {
        return origPath;
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter, RightHandProcessor rightPath) throws RepositoryException {
        FilterOperation operation = operation(filter);
        if (rightPath instanceof PolyStringItemFilterProcessor) {
            return processPoly(filter, (PolyStringItemFilterProcessor<?>) rightPath);
        }
        return singleValuePredicateWithNotTreated(this.normPath, operation, rightPath.rightHand(filter));
    }

    private Predicate processPoly(PropertyValueFilter<T> filter, PolyStringItemFilterProcessor<?> rightPath) throws QueryException {
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            return ExpressionUtils.and(
                    createBinaryCondition(filter, normPath,
                            ValueFilterValues.from(filter, rightPath.normPath)),
                    createBinaryCondition(filter, origPath,
                            ValueFilterValues.from(filter, rightPath.origPath)));
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, origPath,
                    ValueFilterValues.from(filter, rightPath.origPath));
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            return createBinaryCondition(filter, normPath,
                    ValueFilterValues.from(filter, this.normPath));
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'.");
        }
    }
}
