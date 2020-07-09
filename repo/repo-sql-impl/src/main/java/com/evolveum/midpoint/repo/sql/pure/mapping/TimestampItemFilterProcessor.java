package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Filter processor for a an attribute path (Prism item) of a timestamp type.
 * Should support conversion of filter value types {@link XMLGregorianCalendar}
 * (what else do we want?) to paths of {@link Instant}, {@link Timestamp} and {@link Long}.
 */
public class TimestampItemFilterProcessor extends ItemFilterProcessor<PropertyValueFilter<String>> {

    private final Path<?> path;

    /**
     * Returns the mapper function creating the timestamp filter processor from context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return new ItemSqlMapper(rootToQueryItem,
                context -> new TimestampItemFilterProcessor(rootToQueryItem.apply(context.path())));
    }

    private TimestampItemFilterProcessor(Path<?> path) {
        this.path = path;
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        Object value = getSingleValue(filter);
        if (value != null && value.getClass() != path.getType()) {
            value = convertToPathType(value);
        }

        return createBinaryCondition(filter, path, value);
    }

    private Object convertToPathType(Object value) throws QueryException {
        long timestamp;
        if (value instanceof XMLGregorianCalendar) {
            timestamp = MiscUtil.asLong((XMLGregorianCalendar) value);
        } else {
            throw new QueryException("Unsupported temporal type " + value.getClass() + " for value: " + value);
        }
        Class<?> pathType = path.getType();
        if (Long.class.isAssignableFrom(pathType)) {
            value = timestamp;
        } else if (Instant.class.isAssignableFrom(pathType)) {
            value = Instant.ofEpochMilli(timestamp);
        } else if (Timestamp.class.isAssignableFrom(pathType)) {
            value = new Timestamp(timestamp);
        } else {
            throw new QueryException("Unsupported temporal type " + pathType + " for path: " + path);
        }
        return value;
    }
}
