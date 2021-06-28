/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.mapping.ExtensionMapping;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Update context for extension/attributes JSONB column.
 *
 * @param <S> schema type for the extension/attributes container
 * @param <Q> entity query type that holds the data for the mapped attributes
 * @param <R> row type related to the {@link Q}
 */
public class ExtensionUpdateContext<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqaleUpdateContext<S, Q, R> {

    private final ExtensionMapping<S, Q, R> mapping;
    private final JsonbPath jsonbPath;

    private final Map<String, Object> changedItems = new HashMap<>();
    private final List<String> deletedItems = new ArrayList<>();

    public ExtensionUpdateContext(SqaleUpdateContext<?, ?, ?> parentContext,
            ExtensionMapping<S, Q, R> mapping, JsonbPath jsonbPath) {
        super(parentContext, null);

        this.mapping = mapping;
        this.jsonbPath = jsonbPath;
    }

    @Override
    public Q entityPath() {
        //noinspection unchecked
        return (Q) parentContext.entityPath();
    }

    @Override
    public QueryModelMapping<S, Q, R> mapping() {
        return mapping;
    }

    @Override
    public <P extends Path<T>, T> void set(P path, T value) {
        // set is called directly on the parent in finishExecutionOwn()
        throw new UnsupportedOperationException("Not available for extension update context");
    }

    public void setChangedItem(String extItemId, Object value) {
        changedItems.put(extItemId, value);
    }

    public void deleteItem(String extItemId) {
        deletedItems.add(extItemId);
    }

    @Override
    protected void finishExecutionOwn() throws SchemaException, RepositoryException {
        try {
            // TODO finish, now it just overwrites, no delete, etc.
            parentContext.set(jsonbPath, Jsonb.from(changedItems));
        } catch (IOException e) {
            throw new RepositoryException("Unexpected problem with JSONB construction", e);
        }
    }
}
