package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.sql.Timestamp;
import java.time.Instant;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Filter processor for a an attribute path (Prism item) of a timestamp type.
 * Should support conversion of filter value types {@link XMLGregorianCalendar}
 * (what else do we want?) to paths of {@link Instant}, {@link Timestamp} and {@link Long}.
 */
public class TimestampItemFilterProcessor implements FilterProcessor<PropertyValueFilter<String>> {

    private final Path<?> path;

    public TimestampItemFilterProcessor(Path<?> path) {
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
