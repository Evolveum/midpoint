/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import static com.querydsl.core.types.dsl.Expressions.booleanTemplate;
import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils.JSONB_POLY_NORM_KEY;
import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils.JSONB_POLY_ORIG_KEY;
import static com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor.*;

import java.util.function.Function;

import com.google.common.base.Strings;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor for multi-value property stored as JSONB array.
 * Support for contains and other operations was added in 4.6.
 * Multiple values in filter are not supported.
 *
 * @param <T> PolyString or String
 */
public class JsonbPolysPathItemFilterProcessor<T>
        extends SinglePathItemFilterProcessor<T, JsonbPath> {

    public <Q extends FlexibleRelationalPathBase<R>, R> JsonbPolysPathItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            @NotNull Function<Q, JsonbPath> rootToPath) {
        super(context, rootToPath);
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter) throws RepositoryException {
        ValueFilterValues<?, ?> values = ValueFilterValues.from(filter);
        if (values.isMultiValue()) {
            throw new QueryException(
                    "JSONB stored poly strings do not support filter with multiple values: " + filter);
        }

        if (values.isEmpty()) {
            return path.isNull();
        }

        FilterOperation operation = operation(filter);
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;
        if (!operation.isEqualOperation()) {
            // not EQ operation means that all IC matching rules also go here
            return processComplexCases(filter, values, operation, matchingRule);
        }

        // The rest can be matched using @> operator - this is most efficient and indexable:
        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule)) {
            // The value here should be poly-string, otherwise it never matches both orig and norm.
            return processPolyStringStrictEq(values);
        } else if (ORIG.equals(matchingRule)) {
            return processPolyStringComponentEq(JSONB_POLY_ORIG_KEY, extractOrig(values.singleValue())
            );
        } else if (NORM.equals(matchingRule)) {
            return processPolyStringComponentEq(JSONB_POLY_NORM_KEY, extractNorm(values.singleValue())
            );
        } else {
            throw createUnsupportedMatchingRuleException(filter, true);
        }
    }

    @SuppressWarnings("DuplicatedCode") // see ExtensionItemFilterProcessor
    private BooleanExpression processComplexCases(
            PropertyValueFilter<T> filter, ValueFilterValues<?, ?> values, FilterOperation operation, String matchingRule)
            throws QueryException {
        // e.g. for substring: WHERE ... exists (select 1
        //     from jsonb_to_recordset(organizationUnits) as (o text, n text) where n like '%substring%')
        // Optional AND o like '%substring%' is also possible for strict/default matching rule.
        // This can't use index, but it works.
        SQLQuery<?> subselect = new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                .from(stringTemplate("jsonb_to_recordset({0}) as (" + JSONB_POLY_ORIG_KEY
                        + " text, " + JSONB_POLY_NORM_KEY + " text)", path));

        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule) || STRICT_IGNORE_CASE.equals(matchingRule)) {
            // The value here should be poly-string, otherwise it never matches both orig and norm.
            PolyString polyString = values.singleValuePolyString();
            subselect.where(singleValuePredicate(stringTemplate(JSONB_POLY_ORIG_KEY), operation, polyString.getOrig()))
                    .where(singleValuePredicate(stringTemplate(JSONB_POLY_NORM_KEY), operation, polyString.getNorm()));
        } else if (ORIG.equals(matchingRule) || ORIG_IGNORE_CASE.equals(matchingRule)) {
            subselect.where(
                    singleValuePredicate(stringTemplate(JSONB_POLY_ORIG_KEY), operation,
                            extractOrig(values.singleValue())));
        } else if (NORM.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            subselect.where(
                    singleValuePredicate(stringTemplate(JSONB_POLY_NORM_KEY), operation,
                            extractNorm(values.singleValue())));
        } else {
            throw createUnsupportedMatchingRuleException(filter, true);
        }

        return subselect.exists();
    }

    private Predicate processPolyStringStrictEq(ValueFilterValues<?, ?> values) throws QueryException {
        PolyString poly = values.singleValuePolyString();
        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format("[{\"" + JSONB_POLY_ORIG_KEY + "\":\"%s\",\""
                                + JSONB_POLY_NORM_KEY + "\":\"%s\"}]",
                        poly.getOrig(), poly.getNorm())));
    }

    private Predicate processPolyStringComponentEq(String subKey, String value) {
        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format("[{\"%s\":\"%s\"}]", subKey, value)));
    }

    @Override
    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        return isIgnoreCasePolyStringFilter(filter);
    }
}
