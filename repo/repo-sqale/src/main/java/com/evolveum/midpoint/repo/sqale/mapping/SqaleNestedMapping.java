/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

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
public class SqaleNestedMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<S, Q, R>
        implements SqaleMappingMixin<S, Q, R> {

    protected SqaleNestedMapping(@NotNull Class<S> schemaType, @NotNull Class<Q> queryType) {
        super(schemaType, queryType);
    }

    @Override
    public SqaleNestedMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper<Q, R> itemMapper) {
        super.addItemMapping(itemName, itemMapper);
        return this;
    }

    @Override
    public <TQ extends QReference<TR, R>, TR extends MReference> SqaleNestedMapping<S, Q, R>
    addRefMapping(@NotNull QName itemName, @NotNull QReferenceMapping<TQ, TR, Q, R> referenceMapping) {
        SqaleMappingMixin.super.addRefMapping(itemName, referenceMapping);
        return this;
    }

    @Override
    public <TS, TQ extends QObject<TR>, TR extends MObject> SqaleNestedMapping<S, Q, R> addRefMapping(
            @NotNull QName itemName,
            @NotNull Function<Q, UuidPath> rootToOidPath,
            @Nullable Function<Q, EnumPath<MObjectType>> rootToTypePath,
            @Nullable Function<Q, NumberPath<Integer>> rootToRelationIdPath,
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier) {
        SqaleMappingMixin.super.addRefMapping(itemName,
                rootToOidPath, rootToTypePath, rootToRelationIdPath, targetMappingSupplier);
        return this;
    }
}
