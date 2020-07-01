package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;

/**
 * Mapping for a an attribute path (Prism item) of enum type that is mapped to SQL as ordinal value.
 * Actual query-path is determined by the function from a parent path (entity).
 */
public class EnumOrdinalItemMapper implements FilterProcessor<PropertyValueFilter<Enum<?>>> {

    private final Path<?> path;

    /**
     * Creates enum filter processor for concrete attribute path.
     * Not used directly, see {@link #factory(Function)}.
     */
    public EnumOrdinalItemMapper(Path<?> path) {
        this.path = path;
    }

    /**
     * Returns factory function that creates this mapper from entity path.
     *
     * @param entityToPropertyMapping function converting entity path to attribute path
     */
    // TODO: I totally failed to parametrize it, it causes problems either when called or when returned
    @Contract(pure = true)
    public static @NotNull Function<Path, FilterProcessor> factory(
            Function<Path, Path> entityToPropertyMapping) {
        return entityPath -> new EnumOrdinalItemMapper(entityToPropertyMapping.apply(entityPath));
    }

    @Override
    public Predicate process(PropertyValueFilter<Enum<?>> filter) {
        //noinspection ConstantConditions
        Enum<?> singleValue = filter.getSingleValue().getRealValue();
        // TODO now just determine the right operation
        return ExpressionUtils.predicate(Ops.EQ,
                path, ConstantImpl.create(singleValue.ordinal()));
    }
}
