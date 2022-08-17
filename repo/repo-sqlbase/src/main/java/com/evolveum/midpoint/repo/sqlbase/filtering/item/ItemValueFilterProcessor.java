/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import static com.evolveum.midpoint.prism.PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;

import com.querydsl.core.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;

/**
 * Type of {@link FilterProcessor} for a single Prism item (not necessarily one SQL column).
 * These are executed as "leaves" of filter processing tree returning terminal predicates.
 * These are used in {@link QueryTableMapping} objects.
 * This superclass contains support methods for determining operator from filter,
 * getting single value and other typical operations needed by item filter processors.
 *
 * See {@link ValueFilterProcessor} for details how complex paths are resolved to its last part.
 */
public abstract class ItemValueFilterProcessor<O extends ValueFilter<?, ?>>
        implements FilterProcessor<O>, RightHandProcessor {

    protected final SqlQueryContext<?, ?, ?> context;

    protected ItemValueFilterProcessor(SqlQueryContext<?, ?, ?> context) {
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

    protected FilterOperation operation(ValueFilter<?, ?> filter) throws QueryException {
        if (filter instanceof EqualFilter) {
            return FilterOperation.of(isIgnoreCaseFilter(filter) ? Ops.EQ_IGNORE_CASE : Ops.EQ);
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter<?> gf = (GreaterFilter<?>) filter;
            return FilterOperation.of(gf.isEquals() ? Ops.GOE : Ops.GT, isIgnoreCaseFilter(filter));
        } else if (filter instanceof LessFilter) {
            LessFilter<?> lf = (LessFilter<?>) filter;
            return FilterOperation.of(lf.isEquals() ? Ops.LOE : Ops.LT, isIgnoreCaseFilter(filter));
        } else if (filter instanceof SubstringFilter) {
            SubstringFilter<?> substring = (SubstringFilter<?>) filter;
            if (substring.isAnchorEnd()) {
                return FilterOperation.of(
                        isIgnoreCaseFilter(filter) ? Ops.ENDS_WITH_IC : Ops.ENDS_WITH);
            } else if (substring.isAnchorStart()) {
                return FilterOperation.of(
                        isIgnoreCaseFilter(filter) ? Ops.STARTS_WITH_IC : Ops.STARTS_WITH);
            } else {
                return FilterOperation.of(
                        isIgnoreCaseFilter(filter) ? Ops.STRING_CONTAINS_IC : Ops.STRING_CONTAINS);
            }
        }

        throw new QueryException("Can't translate filter '" + filter + "' to operation.");
    }

    private static final String STRING_IGNORE_CASE = STRING_IGNORE_CASE_MATCHING_RULE_NAME.getLocalPart();

    protected boolean isIgnoreCaseFilter(ValueFilter<?, ?> filter) {
        return filter.getMatchingRule() != null
                && filter.getMatchingRule().getLocalPart().equals(STRING_IGNORE_CASE);
    }

    @NotNull
    protected <T> Predicate createBinaryCondition(
            ValueFilter<?, ?> filter, Path<T> path, ValueFilterValues<?, T> values)
            throws QueryException {
        if (filter instanceof FuzzyStringMatchFilter<?>) {
            return fuzzyStringPredicate((FuzzyStringMatchFilter<?>) filter, path, values);
        }

        FilterOperation operation = operation(filter);
        if (values.isEmpty()) {
            if (operation.isAnyEqualOperation()) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }
        if (values.isMultiValue()) {
            if (operation.isAnyEqualOperation()) {
                return ExpressionUtils.predicate(Ops.IN,
                        operation.treatPathForIn(path),
                        ConstantImpl.create(operation.treatValuesForIn(values)));
            } else {
                throw new QueryException("Multi-value for other than EQUAL filter: " + filter);
            }
        }

        return singleValuePredicateWithNotTreated(path, operation, values.singleValue());
    }

    protected Predicate fuzzyStringPredicate(
            FuzzyStringMatchFilter<?> filter, Expression<?> path, ValueFilterValues<?, ?> values)
            throws QueryException {
        return predicateWithNotTreated(path,
                context.processFuzzyFilter(filter, path, values));
    }

    /**
     * Creates predicate for specified path and value using the provided operator.
     * If the value is not Querydsl {@link Expression} it is changed to constant expression,
     * otherwise the expression is passed as-is.
     * Technically, any expression can be used on path side as well.
     */
    protected Predicate singleValuePredicateWithNotTreated(
            Expression<?> path, FilterOperation operation, Object value) {
        Predicate predicate = singleValuePredicate(path, operation, value);
        return predicateWithNotTreated(path, predicate);
    }

    /** Like {@link #singleValuePredicateWithNotTreated} but skips NOT treatment. */
    protected Predicate singleValuePredicate(
            Expression<?> path, FilterOperation operation, Object value) {
        path = operation.treatPath(path);
        if (value instanceof Expression<?>) {
            value = operation.treatPath((Expression<?>) value);
        } else {
            value = operation.treatValue(value);
        }
        return ExpressionUtils.predicate(operation.operator, path,
                value instanceof Expression ? (Expression<?>) value : ConstantImpl.create(value));
    }

    /**
     * Returns the predicate or (predicate AND path IS NOT NULL) if NOT is used somewhere above.
     * This makes NOT truly complementary to non-NOT result.
     */
    protected Predicate predicateWithNotTreated(Expression<?> path, Predicate predicate) {
        return context.isNotFilterUsed()
                ? ExpressionUtils.and(predicate, ExpressionUtils.predicate(Ops.IS_NOT_NULL, path))
                : predicate;
    }

    @Override
    public Expression<?> rightHand(ValueFilter<?, ?> filter) throws RepositoryException {
        throw new RepositoryException("Path " + filter.getRightHandSidePath() + "is not supported as right hand side.");
    }
}
