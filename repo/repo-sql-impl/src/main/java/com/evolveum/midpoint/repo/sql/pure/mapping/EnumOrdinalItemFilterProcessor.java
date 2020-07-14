package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Filter processor for a an attribute path (Prism item) of enum type that is mapped
 * to SQL as ordinal value.
 *
 * @param <E> type of enum on the enum contained in object filter, this is optionally mapped
 * to final type used for ordinal. Can be {@code null} if no mapping is needed.
 */
public class EnumOrdinalItemFilterProcessor<E extends Enum<E>>
        extends SinglePathItemFilterProcessor<PropertyValueFilter<E>> {

    @Nullable
    private final Function<E, Enum<?>> valueFunction;

    /**
     * Returns the mapper creating the enum filter processor from context.
     * With no value mapping function the filter value must contain enum whose ordinal
     * numbers are used in the repository.
     */
    public static ItemSqlMapper mapper(
            @NotNull Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return mapper(rootToQueryItem, null);
    }

    /**
     * Returns the mapper creating the enum filter processor from context
     * with enum value mapping function.
     */
    public static <E extends Enum<E>> ItemSqlMapper mapper(
            @NotNull Function<EntityPath<?>, Path<?>> rootToQueryItem,
            @Nullable Function<E, Enum<?>> valueFunction) {
        return new ItemSqlMapper(ctx ->
                new EnumOrdinalItemFilterProcessor<>(ctx, rootToQueryItem, valueFunction),
                rootToQueryItem);
    }

    private EnumOrdinalItemFilterProcessor(
            SqlPathContext<?, ?> context,
            Function<EntityPath<?>, Path<?>> rootToQueryItem,
            @Nullable Function<E, Enum<?>> valueFunction) {
        super(context, rootToQueryItem);
        this.valueFunction = valueFunction;
    }

    @Override
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        E value = getSingleValue(filter);
        Enum<?> finalValue = valueFunction != null && value != null
                ? valueFunction.apply(value)
                : value;
        return createBinaryCondition(filter, path,
                finalValue != null ? finalValue.ordinal() : null);
    }
}
