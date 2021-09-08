/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for a an attribute path (Prism item) of enum type that is mapped
 * to SQL as ordinal value.
 *
 * @param <E> type of enum on the enum contained in object filter, this is optionally mapped
 * to final type used for ordinal. Can be {@code null} if no mapping is needed.
 */
public class EnumOrdinalItemFilterProcessor<E extends Enum<E>>
        extends SinglePathItemFilterProcessor<E, Path<Integer>> {

    @Nullable
    private final Function<E, Integer> conversionFunction;

    /**
     * Returns the mapper creating the enum filter processor from context.
     * With no value conversion function the filter value must contain enum whose ordinal
     * numbers are used in the repository.
     *
     * @param <Q> entity query type of the mapping
     * @param <R> row type related to the {@link Q}
     */
    public static <Q extends FlexibleRelationalPathBase<R>, R>
    ItemSqlMapper<Q, R> mapper(
            @NotNull Function<Q, Path<Integer>> rootToQueryItem) {
        return mapper(rootToQueryItem, null);
    }

    /**
     * Returns the mapper creating the enum filter processor from context
     * with enum value conversion function.
     *
     * @param <Q> entity query type of the mapping
     * @param <R> row type related to the {@link Q}
     * @param <E> see class javadoc
     */
    public static <Q extends FlexibleRelationalPathBase<R>, R, E extends Enum<E>>
    ItemSqlMapper<Q, R> mapper(
            @NotNull Function<Q, Path<Integer>> rootToQueryItem,
            @Nullable Function<E, Enum<?>> conversionFunction) {
        return new DefaultItemSqlMapper<>(
                ctx -> new EnumOrdinalItemFilterProcessor<>(
                        ctx, rootToQueryItem, conversionFunction),
                rootToQueryItem);
    }

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    private <Q extends FlexibleRelationalPathBase<R>, R> EnumOrdinalItemFilterProcessor(
            @NotNull SqlQueryContext<?, Q, R> context,
            @NotNull Function<Q, Path<Integer>> rootToQueryItem,
            @Nullable Function<E, Enum<?>> conversionFunction) {
        super(context, rootToQueryItem);
        this.conversionFunction = conversionFunction != null
                ? conversionFunction.andThen(Enum::ordinal)
                : Enum::ordinal;
    }

    @Override
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, conversionFunction));
    }


    @Override
    public Predicate process(PropertyValueFilter<E> filter, RightHandProcessor rightPath)
            throws RepositoryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, rightPath.rightHand(filter)));
    }
}
