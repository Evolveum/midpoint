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
import com.evolveum.midpoint.repo.sqale.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.NestedMappingResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Mapping superclass with common functions for {@link QObject} and non-objects (e.g. containers).
 *
 * @see QueryTableMapping
 */
public abstract class SqaleTableMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryTableMapping<S, Q, R> {

    protected SqaleTableMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);
    }

    /** Nested mapping adaptation for repo-sqale. */
    @Override
    public <N> SqaleNestedMapping<N, Q, R> nestedMapping(
            @NotNull ItemName itemName, @NotNull Class<N> nestedSchemaType) {
        SqaleNestedMapping<N, Q, R> nestedMapping =
                new SqaleNestedMapping<>(nestedSchemaType, queryType());
        addRelationResolver(itemName, new NestedMappingResolver<>(nestedMapping));
        return nestedMapping;
    }

    // TODO will the version for RefItemFilterProcessor be useful too?
    //  Yes, if it needs relation mapping too!
    public final void addRefMapping(
            @NotNull QName itemName, @NotNull QReferenceMapping qReferenceMapping) {
        ((QueryModelMapping<?, ?, ?>) this).addItemMapping(itemName,
                RefTableItemFilterProcessor.mapper(qReferenceMapping));
        // TODO add relation mapping too
    }
}
