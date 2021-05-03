/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

/**
 * Mapping between {@link QContainer} and {@link Containerable}.
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 * @param <OR> type of the owner row
 */
public class QContainerMapping<S extends Containerable, Q extends QContainer<R, OR>, R extends MContainer, OR>
        extends SqaleTableMapping<S, Q, R>
        implements QOwnedByMapping<S, R, OR> {

    public static final String DEFAULT_ALIAS_NAME = "c";

    public static QContainerMapping<?, ?, ?, ?> initContainerMapping(@NotNull SqaleRepoContext repositoryContext) {
        return new QContainerMapping<>(
                QContainer.TABLE_NAME, DEFAULT_ALIAS_NAME,
                Containerable.class, QContainer.CLASS,
                repositoryContext);
    }

    protected QContainerMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);

        // CID is not mapped directly, it is used by path resolver elsewhere
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QContainer<>(MContainer.class, alias);
    }

    @Override
    public R newRowObject(OR ownerRow) {
        throw new UnsupportedOperationException(
                "Container bean creation for owner row called on abstract container mapping");
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MContainer();
    }

    /**
     * This creates the right type of object and fills in the base {@link MContainer} attributes.
     */
    public R initRowObject(S schemaObject, OR ownerRow) {
        R row = newRowObject(ownerRow);
        row.cid = schemaObject.asPrismContainerValue().getId();
        // containerType is generated in DB, must be left null!
        return row;
    }

    @Override
    public R insert(S schemaObject, OR ownerRow, JdbcSession jdbcSession) {
        throw new UnsupportedOperationException("insert not implemented in the subclass");
    }
}
