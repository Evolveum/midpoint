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
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

/**
 * This acts like a container mapping for extension/attributes containers.
 * Compared to other subclasses of {@link QueryModelMapping} this does NOT use the item mapping
 * and resolver maps, but instead creates the mapper on the fly with currently available information
 * and lets the extension mapper/resolver do the real work.
 * This allows for dynamic mapping which is needed especially for shadow attributes.
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
        return new ExtensionItemSqlMapper<>(rootToExtensionPath, itemName, holderType);
    }
}
