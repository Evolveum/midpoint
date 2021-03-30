/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for a an attribute path (Prism item) that is stored in detail table.
 * Mapper using this processor defines how to get to the actual column on the detail table
 * and also takes the actual {@link ItemSqlMapper} producing the right type of {@link ItemFilterProcessor}.
 */
public class DetailTableItemFilterProcessor
        <Q extends FlexibleRelationalPathBase<?>, DQ extends FlexibleRelationalPathBase<DR>, DR>
        extends ItemFilterProcessor<PropertyValueFilter<String>> {

    /**
     * Creates composition mapper that defines:
     *
     * 1. how to traverse to the detail table and
     * 2. specifies the actual {@link ItemSqlMapper} used for the column on the detail table.
     *
     * Note that the nested mapper works in the context using the joined path, so any item path
     * mapping is already relative to the query type representing the detail table.
     *
     * @param <Q> query type (entity path) from which we traverse to the detail table
     * @param <DQ> query type for the detail table
     * @param <DR> row type related to the {@link DQ}
     * @param detailQueryType class of the starting query type ({@link DQ})
     * @param joinOnPredicate bi-function producing Querydsl JOIN-ON {@link Predicate} for entity
     * paths of {@link Q} and {@link DQ}
     * @param nestedItemMapper {@link ItemSqlMapper} for the column on the detail table
     * that actually represents the target of the whole composition of mappers for the item path
     */
    public static <Q extends FlexibleRelationalPathBase<?>, DQ extends FlexibleRelationalPathBase<DR>, DR>
    ItemSqlMapper mapper(
            @NotNull Class<DQ> detailQueryType,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicate,
            @NotNull ItemSqlMapper nestedItemMapper) {
        /*
         * Ctx here is the original context that leads to this mapping, not the inner context
         * describing the target detail table (which doesn't even have to have defined mapping).
         * We don't resolve any arguments in the lambda here because we need to throw checked
         * QueryException, so everything is done in process(filter).
         */
        return new ItemSqlMapper(ctx ->
                new DetailTableItemFilterProcessor<>(
                        ctx, detailQueryType, joinOnPredicate, nestedItemMapper));
    }

    private final Class<DQ> detailQueryType;
    private final BiFunction<Q, DQ, Predicate> joinOnPredicate;
    private final ItemSqlMapper nestedItemMapper;

    public DetailTableItemFilterProcessor(
            SqlQueryContext<?, ?, ?> ctx,
            Class<DQ> detailQueryType,
            BiFunction<Q, DQ, Predicate> joinOnPredicate,
            ItemSqlMapper nestedItemMapper) {
        super(ctx);
        this.detailQueryType = detailQueryType;
        this.joinOnPredicate = joinOnPredicate;
        this.nestedItemMapper = nestedItemMapper;
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws RepositoryException {
        //noinspection unchecked
        SqlQueryContext<?, DQ, DR> joinContext =
                ((SqlQueryContext<?, Q, ?>) context).leftJoin(detailQueryType, joinOnPredicate);

        // now we create the actual filter processor on the inner context
        FilterProcessor<ObjectFilter> filterProcessor =
                nestedItemMapper.createFilterProcessor(joinContext);
        return filterProcessor.process(filter);
    }
}
