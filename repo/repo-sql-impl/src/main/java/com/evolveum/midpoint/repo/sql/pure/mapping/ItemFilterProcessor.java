/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.mapping;

import static com.evolveum.midpoint.prism.PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;

import com.querydsl.core.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public abstract class ItemFilterProcessor<O extends ObjectFilter>
        implements FilterProcessor<O> {

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
    protected PredicateOperation createBinaryCondition(
            ValueFilter<?, ?> filter, Path<?> path, Object value) throws QueryException {
        Ops operator = operation(filter);
        if (value == null) {
            if (operator == Ops.EQ || operator == Ops.EQ_IGNORE_CASE) {
                return ExpressionUtils.predicate(Ops.IS_NULL, path);
            } else {
                throw new QueryException("Null value for other than EQUAL filter: " + filter);
            }
        }

        return ExpressionUtils.predicate(operator, path, ConstantImpl.create(value));
    }
}
