/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.ExtensionItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;

/**
 * TODO: this much "lazier" than normal item mapper, we'll get to ext definitions only here, not sooner.
 *
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public class ExtensionItemSqlMapper<Q extends FlexibleRelationalPathBase<R>, R>
        implements UpdatableItemSqlMapper<Q, R> {

    private final Function<Q, JsonbPath> rootToExtensionPath;
    private final QName itemName;
    private final MExtItemHolderType holderType;

    public ExtensionItemSqlMapper(
            @NotNull Function<Q, JsonbPath> rootToExtensionPath,
            @NotNull QName itemName,
            @NotNull MExtItemHolderType holderType) {
        this.rootToExtensionPath = rootToExtensionPath;
        this.itemName = itemName;
        this.holderType = holderType;
    }

    @Override
    public @Nullable Path<?> itemPrimaryPath(Q entityPath) {
        return rootToExtensionPath.apply(entityPath);
    }

    @Override
    public @Nullable <T extends ObjectFilter> ItemFilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        //noinspection unchecked
        return (ItemFilterProcessor<T>) new ExtensionItemFilterProcessor(
                (SqlQueryContext<?, Q, R>) sqlQueryContext, rootToExtensionPath, holderType);
    }

    @Override
    public <T> ItemDeltaValueProcessor<T> createItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> sqlUpdateContext) {
        // TODO
        return null;
    }
}
