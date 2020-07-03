package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.*;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Filter processor for a an attribute path (Prism item) of enum type that is mapped
 * to SQL as ordinal value.
 *
 * @param <E> type of enum on the enum contained in object filter, this is optionally mapped
 * to final type used for ordinal. Can be {@code null} if no mapping is needed.
 */
public class EnumOrdinalItemFilterProcessor<E extends Enum<E>>
        implements FilterProcessor<PropertyValueFilter<E>> {

    private final Path<Integer> path;

    @Nullable
    private final Function<E, Enum<?>> valueFunction;

    /**
     * Creates enum filter processor for concrete attribute path.
     * This has no mapping function so filter must contain enum whose ordinal numbers are used in repo.
     */
    public EnumOrdinalItemFilterProcessor(Path<Integer> path) {
        this(path, null);
    }

    /**
     * Creates enum filter processor for concrete attribute path.
     * This uses value function for enum conversion before calling ordinal.
     */
    public EnumOrdinalItemFilterProcessor(Path<Integer> path,
            @Nullable Function<E, Enum<?>> valueFunction) {
        this.path = path;
        this.valueFunction = valueFunction;
    }

    /**
     * Allows shorter creation of path->handler mapping function for enum-ordinal paths with
     * enum value mapping function. See the single line lambda it may replace.
     */
    public static <E extends Enum<E>> Function<Path<Integer>, FilterProcessor<?>> withValueFunction(
            @Nullable Function<E, Enum<?>> valueFunction) {
        return p -> new EnumOrdinalItemFilterProcessor<>(p, valueFunction);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        E value = filter.getSingleValue().getRealValue();
        Enum<?> finalValue = valueFunction != null
                ? valueFunction.apply(value)
                : value;
        Ops operator = operation(filter);
        return ExpressionUtils.predicate(operator, path,
                ConstantImpl.create(finalValue.ordinal()));
    }
}
