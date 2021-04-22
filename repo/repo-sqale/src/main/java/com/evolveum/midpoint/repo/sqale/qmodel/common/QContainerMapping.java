/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ContainerSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

/**
 * Mapping between {@link QContainer} and {@link Containerable}.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 * @param <OR> type of the owner row
 */
public class QContainerMapping<S extends Containerable, Q extends QContainer<R, OR>, R extends MContainer, OR>
        extends SqaleTableMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "c";

    public static final
    QContainerMapping<Containerable, QContainer<MContainer, Object>, MContainer, Object> INSTANCE =
            new QContainerMapping<>(QContainer.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    Containerable.class, QContainer.CLASS);

    protected QContainerMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        // TODO how CID is mapped?
//        addItemMapping(PrismConstants.T_ID, uuidMapper(q -> q.oid));
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QContainer<>(MContainer.class, alias);
    }

    @Override
    public ContainerSqlTransformer<S, Q, R, OR> createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new ContainerSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MContainer();
    }
}
