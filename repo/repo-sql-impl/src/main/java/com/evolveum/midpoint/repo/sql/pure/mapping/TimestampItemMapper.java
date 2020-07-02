package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Mapping for a timestamp attribute path (Prism item).
 * Should support {@link Instant}, {@link ZonedDateTime} and {@link XMLGregorianCalendar}.
 */
public class TimestampItemMapper implements FilterProcessor<PropertyValueFilter<String>> {

    private final Path<?> path;

    public TimestampItemMapper(Path<?> path) {
        this.path = path;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        Object value = filter.getSingleValue().getRealValue();
        Ops operator = operation(filter);
        if (value.getClass() != path.getType()) {
            value = convertToPathType(value);
        }

        return ExpressionUtils.predicate(operator, path, ConstantImpl.create(value));
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
