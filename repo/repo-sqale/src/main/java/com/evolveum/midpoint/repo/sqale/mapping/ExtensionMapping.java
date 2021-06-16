/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

/**
 * TODO
 *
 * @param <C> schema type for the extension/attributes container
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class ExtensionMapping<C extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<C, Q, R>
        implements SqaleMappingMixin<C, Q, R> {

    private final Function<Q, JsonbPath> rootToExtensionPath;

    protected ExtensionMapping(
            @NotNull Class<C> containerType,
            @NotNull Class<Q> queryType,
            @NotNull Function<Q, JsonbPath> rootToExtensionPath) {
        super(containerType, queryType);
        this.rootToExtensionPath = rootToExtensionPath;
    }

    @Override
    public @Nullable ItemSqlMapper<Q, R> getItemMapper(QName itemName) {
        MExtItemHolderType holderType = schemaType().equals(ExtensionType.class)
                ? MExtItemHolderType.EXTENSION : MExtItemHolderType.ATTRIBUTES;
        return new ExtensionItemSqlMapper<Q, R>(rootToExtensionPath, itemName, holderType);
    }
}
