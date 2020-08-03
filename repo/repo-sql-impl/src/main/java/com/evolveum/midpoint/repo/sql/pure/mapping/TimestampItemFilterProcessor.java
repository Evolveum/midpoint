package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Filter processor for a an attribute path (Prism item) of a timestamp type.
 * Should support conversion of filter value types {@link XMLGregorianCalendar}
 * (what else do we want?) to paths of {@link Instant}, {@link Timestamp} and {@link Long}.
 */
public class TimestampItemFilterProcessor
        extends SinglePathItemFilterProcessor<PropertyValueFilter<?>> {

    /**
     * Returns the mapper function creating the timestamp filter processor from context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return new ItemSqlMapper(context ->
                new TimestampItemFilterProcessor(context, rootToQueryItem), rootToQueryItem);
    }

    private TimestampItemFilterProcessor(SqlPathContext<?, ?, ?> context,
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<?> filter) throws QueryException {
        ValueFilterValues<?> values = new ValueFilterValues<>(filter, this::convertToPathType);
        return createBinaryCondition(filter, path, values);
    }

    private Object convertToPathType(@NotNull Object value) {
        if (value.getClass() == path.getType()) {
            return value;
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
        return value;
    }
}
