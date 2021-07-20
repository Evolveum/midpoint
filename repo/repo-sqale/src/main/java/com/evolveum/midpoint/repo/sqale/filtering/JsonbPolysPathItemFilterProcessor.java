/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import static com.querydsl.core.types.dsl.Expressions.booleanTemplate;

import static com.evolveum.midpoint.repo.sqale.jsonb.Jsonb.JSONB_POLY_NORM_KEY;
import static com.evolveum.midpoint.repo.sqale.jsonb.Jsonb.JSONB_POLY_ORIG_KEY;
import static com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor.*;

import java.util.List;
import java.util.function.Function;

import com.google.common.base.Strings;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for multi-value property stored as JSONB array.
 * These paths support only value equality (of any value), which is "contains" in DB terminology.
 * Our filter "contains" (meaning substring) is *not* supported.
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
        String matchingRule = filter.getMatchingRule() != null
                ? filter.getMatchingRule().getLocalPart() : null;

        if (!(filter instanceof EqualFilter) || STRICT_IGNORE_CASE.equals(matchingRule)
                || ORIG_IGNORE_CASE.equals(matchingRule) || NORM_IGNORE_CASE.equals(matchingRule)) {
            throw new QueryException("Can't translate filter '" + filter + "' to operation."
                    + " JSONB stored poly strings support only equals with no IC matching rule.");
        }

        List<?> filterValues = filter.getValues();
        if (filterValues == null || filterValues.isEmpty()) {
            return path.isNull();
        }

        ValueFilterValues<?, ?> values = ValueFilterValues.from(filter);
        if (Strings.isNullOrEmpty(matchingRule) || DEFAULT.equals(matchingRule)
                || STRICT.equals(matchingRule)) {
            // The value here should be poly-string, otherwise it never matches both orig and norm.
            return processPolyStringBoth(values);
        } else if (ORIG.equals(matchingRule)) {
            return processPolyStringComponent(
                    convertPolyValuesToString(values, filter, p -> p.getOrig()),
                    JSONB_POLY_ORIG_KEY);
        } else if (NORM.equals(matchingRule)) {
            return processPolyStringComponent(
                    convertPolyValuesToString(values, filter, p -> p.getNorm()),
                    JSONB_POLY_NORM_KEY);
        } else {
            throw new QueryException("Unknown matching rule '" + matchingRule + "'.");
        }
    }

    @NotNull
    private ValueFilterValues<?, ?> convertPolyValuesToString(ValueFilterValues<?, ?> values,
            PropertyValueFilter<?> filter, Function<PolyString, String> extractor) {
        // In case it is string already we don't need to do anything.
        if (values.singleValueRaw() instanceof String) {
            return values;
        }

        //noinspection unchecked
        return ValueFilterValues.from((PropertyValueFilter<PolyString>) filter, extractor);
    }

    private Predicate processPolyStringBoth(ValueFilterValues<?, ?> values) throws QueryException {
        Object value = values.singleValueRaw();
        assert value != null; // empty values treated in main process()
        if (!(value instanceof PolyString)) {
            throw new QueryException(
                    "PolyString value must be provided to match both orig and norm values, was: "
                            + value + " (type " + value.getClass() + ")");
        }

        PolyString poly = (PolyString) value; // must be Poly here
        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format("[{\"" + JSONB_POLY_ORIG_KEY + "\":\"%s\",\""
                                + JSONB_POLY_NORM_KEY + "\":\"%s\"}]",
                        poly.getOrig(), poly.getNorm())));
    }

    private Predicate processPolyStringComponent(ValueFilterValues<?, ?> values, String subKey)
            throws QueryException {
        // Here the values are converted to Strings already
        return predicateWithNotTreated(path, booleanTemplate("{0} @> {1}::jsonb", path,
                String.format("[{\"%s\":\"%s\"}]", subKey, values.singleValue())));
    }
}
