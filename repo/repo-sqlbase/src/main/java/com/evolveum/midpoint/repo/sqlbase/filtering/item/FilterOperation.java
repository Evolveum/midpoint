/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.List;
import java.util.stream.Collectors;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.StringExpression;

import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;

/**
 * This represents operation between the path (typically) and value(s).
 * In most cases it's just glorified wrapper around Querydsl {@link Ops} value, but for cases
 * of case-insensitive string comparison (greater/lower than) or IN operations it hides
 * the complexity of adding "normalizing" operation (lowering the casing) to the both sides.
 */
public class FilterOperation {

    public final Ops operator;

    /** True if {@link #operator} does not solve ignore-case implicitly. */
    public final boolean handleIgnoreCase;

    private FilterOperation(Ops operator, boolean handleIgnoreCase) {
        this.operator = operator;
        this.handleIgnoreCase = handleIgnoreCase;
    }

    public static FilterOperation of(Ops ops) {
        return new FilterOperation(ops, false);
    }

    public static FilterOperation of(Ops ops, boolean handleIgnoreCase) {
        return new FilterOperation(ops, handleIgnoreCase);
    }

    /** True if {@link #operator} is EQ. */
    public boolean isEqualOperation() {
        return operator == Ops.EQ;
    }

    /** True if {@link #operator} is EQ or EQ_IGNORE_CASE. */
    public boolean isAnyEqualOperation() {
        return operator == Ops.EQ || operator == Ops.EQ_IGNORE_CASE;
    }

    public Expression<?> treatPath(Expression<?> expression) {
        return handleIgnoreCase && expression instanceof StringExpression
                ? ((StringExpression) expression).lower()
                : expression;
    }

    public Object treatValue(Object value) {
        return handleIgnoreCase && value instanceof String
                ? ((String) value).toLowerCase()
                : value;
    }

    // assumes EQ or EQ_IGNORE_CASE
    public Expression<?> treatPathForIn(Expression<?> expression) {
        return operator == Ops.EQ_IGNORE_CASE && expression instanceof StringExpression
                ? ((StringExpression) expression).lower()
                : expression;
    }

    public List<?> treatValuesForIn(ValueFilterValues<?, ?> values) throws QueryException {
        List<?> allValues = values.allValues();
        return operator == Ops.EQ_IGNORE_CASE && values.singleValue() instanceof String
                ? allValues.stream().map(s -> ((String) s).toLowerCase())
                .collect(Collectors.toList())
                : allValues;
    }
}
