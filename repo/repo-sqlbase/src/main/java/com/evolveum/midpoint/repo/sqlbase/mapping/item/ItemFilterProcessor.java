/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import static com.evolveum.midpoint.prism.PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;

import com.querydsl.core.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;

/**
 * Type of {@link FilterProcessor} for a single Prism item (not necessarily one SQL column).
 * These are executed as "leaves" of filter processing tree returning terminal predicates.
 * These are used in {@link QueryModelMapping} objects.
 * This superclass contains support methods for determining operator from filter,
 * getting single value and other typical operations needed by item filter processors.
 */
public abstract class ItemFilterProcessor<O extends ObjectFilter>
        implements FilterProcessor<O> {

    protected final SqlQueryContext<?, ?, ?> context;

    protected ItemFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    /**
     * Returns the single "real" value from the property filter (or null).
     */
    @Nullable
    protected <T> T getSingleValue(PropertyValueFilter<T> filter) {
        PrismValue val = filter.getSingleValue();
        return val != null ? val.getRealValue() : null;
    }

    protected Ops operation(ValueFilter<?, ?> filter) throws QueryException {
        if (filter instanceof EqualFilter) {
            return isIgnoreCaseFilter(filter) ? Ops.EQ_IGNORE_CASE : Ops.EQ;
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter<?> gf = (GreaterFilter<?>) filter;
            return gf.isEquals() ? Ops.GOE : Ops.GT;
        } else if (filter instanceof LessFilter) {
            LessFilter<?> lf = (LessFilter<?>) filter;
            return lf.isEquals() ? Ops.LOE : Ops.LT;
        } else if (filter instanceof SubstringFilter) {
            SubstringFilter<?> substring = (SubstringFilter<?>) filter;
            if (substring.isAnchorEnd()) {
                return isIgnoreCaseFilter(filter) ? Ops.ENDS_WITH_IC : Ops.ENDS_WITH;
            } else if (substring.isAnchorStart()) {
                return isIgnoreCaseFilter(filter) ? Ops.STARTS_WITH_IC : Ops.STARTS_WITH;
            } else {
                return isIgnoreCaseFilter(filter) ? Ops.STRING_CONTAINS_IC : Ops.STRING_CONTAINS;
            }
        }

        throw new QueryException("Can't translate filter '" + filter + "' to operation.");
    }

    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        return filter.getMatchingRule() != null
                && filter.getMatchingRule().equals(STRING_IGNORE_CASE_MATCHING_RULE_NAME);
    }

    @NotNull
    protected Predicate createBinaryCondition(
            ValueFilter<?, ?> filter, Path<?> path, ValueFilterValues<?> values)
            throws QueryException {
        Ops operator = operation(filter);
        if (values.isEmpty()) {
            if (operator == Ops.EQ || operator == Ops.EQ_IGNORE_CASE) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }
        if (values.isMultiValue()) {
            // TODO do we want Ops.EQ_IGNORE_CASE too? For one value, Querydsl takes care of
            //  ignore-case. For IN, we would have to do it ourselves.
            if (operator == Ops.EQ) {
                return ExpressionUtils.predicate(Ops.IN, path,
                        ConstantImpl.create(values.allValues()));
            } else {
                throw new QueryException("Multi-value for other than EQUAL filter: " + filter);
            }
        }

        return singleValuePredicate(path, operator, values.singleValue());
    }

    protected Predicate singleValuePredicate(Path<?> path, Ops operator, Object value) {
        Predicate predicate = ExpressionUtils.predicate(operator, path, ConstantImpl.create(value));
        return predicateWithNotTreated(path, predicate);
    }

    /**
     * Returns the predicate or (predicate AND path IS NOT NULL) if NOT is used somewhere above.
     * This makes NOT truly complementary to non-NOT result.
     */
    protected Predicate predicateWithNotTreated(Path<?> path, Predicate predicate) {
        return context.isNotFilterUsed()
                ? ExpressionUtils.and(predicate, ExpressionUtils.predicate(Ops.IS_NOT_NULL, path))
                : predicate;
    }
}
