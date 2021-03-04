/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;

/**
 * Mapping between {@link QContainer} and {@link Containerable}.
 */
public class QContainerMapping<S extends Containerable, Q extends QContainer<R>, R extends MContainer>
        extends SqaleModelMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "c";

    public static final QContainerMapping<Containerable, QContainer<MContainer>, MContainer> INSTANCE =
            new QContainerMapping<>(QContainer.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    Containerable.class, QContainer.CLASS);

    protected QContainerMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        // TODO how CID is mapped?
//        addItemMapping(PrismConstants.T_ID, uuidMapper(path(q -> q.oid)));
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QContainer<>(MContainer.class, alias);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MContainer();
    }
}
