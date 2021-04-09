/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.mapping.ObjectRefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.NestedMappingResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Sqale implementation for nested mapping with support for sqale specific types.
 * This allows for fluent calls of methods like {@link #addRefMapping(QName, QObjectReferenceMapping)}
 * which depend on sqale-specific types like {@link QReferenceMapping} in this example.
 */
public class SqaleNestedMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<S, Q, R> {

    protected SqaleNestedMapping(@NotNull Class<S> schemaType, @NotNull Class<Q> queryType) {
        super(schemaType, queryType);
    }

    @Override
    public SqaleNestedMapping<S, Q, R> addItemMapping(
            @NotNull QName itemName, @NotNull ItemSqlMapper itemMapper) {
        super.addItemMapping(itemName, itemMapper);
        return this;
    }

    // TODO will the version for RefItemFilterProcessor be useful too? Yes, if it needs relation mapping too!
    public final SqaleNestedMapping<S, Q, R> addRefMapping(
            @NotNull QName itemName, @NotNull QObjectReferenceMapping qReferenceMapping) {
        ((QueryModelMapping<?, ?, ?>) this).addItemMapping(itemName,
                ObjectRefTableItemFilterProcessor.mapper(qReferenceMapping));
        // TODO add relation mapping too
        return this;
    }

    /** Nested mapping adaptation for repo-sqale. */
    @Override
    public <N> SqaleNestedMapping<N, Q, R> addNestedMapping(
            @NotNull ItemName itemName, @NotNull Class<N> nestedSchemaType) {
        SqaleNestedMapping<N, Q, R> nestedMapping =
                new SqaleNestedMapping<>(nestedSchemaType, queryType());
        addRelationResolver(itemName, new NestedMappingResolver<>(nestedMapping));
        return nestedMapping;
    }
}
