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
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * This acts like a container mapping for extension/attributes containers.
 * Compared to other subclasses of {@link QueryModelMapping} this does NOT use the item mapping
 * and resolver maps, but instead returns the same stateless mapper which then delegates
 * all the real work to item filter/delta processors (see {@link ExtensionItemSqlMapper}).
 * This allows for dynamic mapping which is needed especially for shadow attributes.
 *
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class ExtensionMapping<Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryModelMapping<Containerable, Q, R> {

    private final MExtItemHolderType holderType;
    private final ExtensionItemSqlMapper<Q, R> itemMapper;

    protected ExtensionMapping(
            @NotNull MExtItemHolderType holderType,
            @NotNull Class<Q> queryType,
            @NotNull Function<Q, JsonbPath> rootToExtensionPath,
            SqaleRepoContext context) {
        super(Containerable.class, queryType);

        this.holderType = holderType;
        this.itemMapper = new ExtensionItemSqlMapper<>(rootToExtensionPath, holderType, context);
    }

    @Override
    public @Nullable ItemSqlMapper<Q, R> getItemMapper(QName itemName) {
        return itemMapper;
    }

    public MExtItemHolderType holderType() {
        return holderType;
    }
}
