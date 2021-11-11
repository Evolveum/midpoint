/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DateTimePath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor for an attribute path (Prism item) of a timestamp type.
 * Should support conversion of filter value types {@link XMLGregorianCalendar}
 * (what else do we want?) to paths of {@link Instant}, {@link Timestamp} and {@link Long}.
 */
public class TimestampItemFilterProcessor<T extends Comparable<T>>
        extends SinglePathItemFilterProcessor<Object, DateTimePath<T>> {

    public <Q extends FlexibleRelationalPathBase<R>, R> TimestampItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, DateTimePath<T>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<Object> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, this::convertToPathType));
    }

    @Override
    public Predicate process(PropertyValueFilter<Object> filter, RightHandProcessor rightPath)
            throws RepositoryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, rightPath.rightHand(filter)));
    }

    // Used <T> instead of Object to conform to unknown type of path above
    private T convertToPathType(@NotNull Object value) {
        return QuerydslUtils.convertTimestampToPathType(value, path);
    }
}
