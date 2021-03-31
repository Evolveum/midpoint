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

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DateTimePath;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Filter processor for a an attribute path (Prism item) of a timestamp type.
 * Should support conversion of filter value types {@link XMLGregorianCalendar}
 * (what else do we want?) to paths of {@link Instant}, {@link Timestamp} and {@link Long}.
 */
public class TimestampItemFilterProcessor
        extends SinglePathItemFilterProcessor<PropertyValueFilter<?>, DateTimePath<Instant>> {

    /**
     * Returns the mapper function creating the timestamp filter processor from context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, DateTimePath<Instant>> rootToQueryItem) {
        return new ItemSqlMapper(context ->
                new TimestampItemFilterProcessor(context, rootToQueryItem), rootToQueryItem);
    }

    private TimestampItemFilterProcessor(SqlQueryContext<?, ?, ?> context,
            Function<EntityPath<?>, DateTimePath<Instant>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<?> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, this::convertToPathType));
    }

    // Used <T> instead of Object to conform to unknown type of path above
    @SuppressWarnings("unchecked")
    private <T> T convertToPathType(@NotNull Object value) {
        if (value.getClass() == path.getType()) {
            return (T) value;
        }

        long timestamp;
        if (value instanceof XMLGregorianCalendar) {
            timestamp = MiscUtil.asLong((XMLGregorianCalendar) value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported temporal type " + value.getClass() + " for value: " + value);
        }
        Class<?> pathType = path.getType();
        if (Long.class.isAssignableFrom(pathType)) {
            value = timestamp;
        } else if (Instant.class.isAssignableFrom(pathType)) {
            value = Instant.ofEpochMilli(timestamp);
        } else if (Timestamp.class.isAssignableFrom(pathType)) {
            value = new Timestamp(timestamp);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported temporal type " + pathType + " for path: " + path);
        }
        return (T) value;
    }
}
