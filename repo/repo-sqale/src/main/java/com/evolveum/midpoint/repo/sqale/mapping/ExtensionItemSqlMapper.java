/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import static com.evolveum.midpoint.repo.sqale.filtering.ExtensionItemFilterProcessor.*;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.Expression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor.ExtItemInfo;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.ExtensionItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.ExtensionItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Extension item mapper that is much lazier than {@link SqaleItemSqlMapper} for typical column.
 * Normally the mapper knows how to get from the query/update contextual information to the columns.
 * This mapper only knows the way to the JSONB column and lets extension item filter/delta
 * processors to do the rest of the work based on the context and item information contained in
 * the filter/modification.
 *
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public class ExtensionItemSqlMapper<Q extends FlexibleRelationalPathBase<R>, R>
        implements UpdatableItemSqlMapper<Q, R> {

    private final Function<Q, JsonbPath> rootToExtensionPath;
    private final MExtItemHolderType holderType;
    private final SqaleRepoContext repositoryContext;

    public ExtensionItemSqlMapper(
            @NotNull Function<Q, JsonbPath> rootToExtensionPath,
            @NotNull MExtItemHolderType holderType,
            @NotNull SqaleRepoContext context) {
        this.rootToExtensionPath = rootToExtensionPath;
        this.holderType = holderType;
        this.repositoryContext = context;
    }

    @Override
    public @Nullable Expression<?> primaryPath(Q entityPath, ItemDefinition<?> definition)
            throws QueryException {
        Objects.requireNonNull(definition, "Null definition provided for extension/attributes item.");
        JsonbPath path = rootToExtensionPath.apply(entityPath);
        ExtItemInfo info = new ExtensionProcessor(repositoryContext)
                .findExtensionItem(definition, holderType);
        if (info == null) {
            throw new QueryException("Extension property " + definition
                    + " is not indexed and cannot be used for ordering.");
        }

        MExtItem extItem = info.item;
        if (extItem.valueType.equals(STRING_TYPE)
                || ExtUtils.isEnumDefinition((PrismPropertyDefinition<?>) definition)
                || extItem.valueType.equals(DATETIME_TYPE)) {
            return stringTemplate("{0}->>'{1s}'",
                    path, info.getId());
        } else if (extItem.valueType.equals(POLY_STRING_TYPE)) {
            return stringTemplate("{0}->'{1s}'->>'" + JsonbUtils.JSONB_POLY_ORIG_KEY + "'",
                    path, info.getId());
        } else {
            return stringTemplate("{0}->'{1s}'", path, info.getId());
        }
    }

    @Override
    public @Nullable <T extends ValueFilter<?, ?>> ItemValueFilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        //noinspection unchecked
        return (ItemValueFilterProcessor<T>) new ExtensionItemFilterProcessor(
                sqlQueryContext,
                (Function<FlexibleRelationalPathBase<?>, JsonbPath>) rootToExtensionPath,
                holderType);
    }

    @Override
    public @Nullable RightHandProcessor createRightHandProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        return createFilterProcessor(sqlQueryContext);
    }

    @Override
    public ItemDeltaProcessor createItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> sqlUpdateContext) {
        return new ExtensionItemDeltaProcessor(sqlUpdateContext, holderType);
    }
}
