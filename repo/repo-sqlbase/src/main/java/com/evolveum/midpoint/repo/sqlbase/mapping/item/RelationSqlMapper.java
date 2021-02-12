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

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Mapper describing how to traverse between DB entities when resolving complex item paths.
 * It provides enough information to add needed {@code JOIN} to the current query.
 *
 * @param <Q> type of source entity path
 * @param <DQ> type of target entity path
 * @param <DR> row type related to the target entity path {@link DQ}
 */
public class RelationSqlMapper<
        Q extends FlexibleRelationalPathBase<?>,
        DQ extends FlexibleRelationalPathBase<DR>,
        DR> {

    private Class<DQ> targetQueryType;
    private BiFunction<Q, DQ, Predicate> joinPredicate;

    public RelationSqlMapper(
            @NotNull Class<DQ> targetQueryType,
            @NotNull BiFunction<Q, DQ, Predicate> joinPredicate) {
        this.targetQueryType = targetQueryType;
        this.joinPredicate = joinPredicate;
    }


}
