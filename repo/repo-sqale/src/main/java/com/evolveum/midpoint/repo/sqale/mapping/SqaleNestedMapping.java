/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Sqale implementation for nested mapping with support for sqale specific types.
 * This allows for fluent calls of methods like {@link #addRefMapping}
 * which depend on sqale-specific types like {@link QReferenceMapping} in this example.
 * This extends from sqlbase {@link QueryModelMapping} because this is NOT whole table mapping
 * so it can't extend from {@link SqaleTableMapping}.
 *
 * @param <S> schema type for the nested container
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class SqaleNestedMapping<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<S, Q, R>
        implements SqaleMappingMixin<S, Q, R> {

    protected SqaleNestedMapping(@NotNull Class<S> schemaType, @NotNull Class<Q> queryType) {
        super(schemaType, queryType);
    }

    @Override
    public SqaleNestedMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper<S, Q, R> itemMapper) {
        super.addItemMapping(itemName, itemMapper);
        return this;
    }
}
