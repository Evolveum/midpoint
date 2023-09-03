/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;

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

        // OWNER_OID does not need to be mapped, it is handled by InOidFilterProcessor
        addItemMapping(PrismConstants.T_ID, longMapper(p -> p.cid));
    }

    /**
     * Implemented for searchable containers that do not use fullObject for their recreation.
     */
    @Override
    public S toSchemaObject(R row) throws SchemaException {
        throw new UnsupportedOperationException(
                "Container search not supported for schema type " + schemaType());
    }

    @Override
    public S toSchemaObject(@NotNull Tuple tuple, @NotNull Q entityPath, @NotNull JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
        var ret =  super.toSchemaObject(tuple, entityPath, jdbcSession, options);
        attachOwnerOid(ret, tuple, entityPath);
        return ret;
    }

    /**
     * Attaches ownerOid (UUID) to user data of container, since this is required for iterativer search for proper continuation.
     *
     * @param ret Parsed Container Result
     * @param tuple Tuple representing container
     * @param entityPath path
     */
    protected void attachOwnerOid(S ret, Tuple tuple, Q entityPath) {
        var row = tuple.get(entityPath);
        var ownerOid = row.ownerOid;
        if (ownerOid == null) {
           ownerOid = tuple.get(entityPath.ownerOid);
        }
        if (ownerOid != null) {
            ret.asPrismContainerValue().setUserData(SqaleUtils.OWNER_OID,  ownerOid.toString());
        }
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
    public R insert(S schemaObject, OR ownerRow, JdbcSession jdbcSession) throws SchemaException {
        throw new UnsupportedOperationException("Missing insert() implementation in " + getClass());
    }

    public Predicate containerIdentityPredicate(Q entityPath, S container) {
        return entityPath.cid.eq(container.asPrismContainerValue().getId());
    }
}
