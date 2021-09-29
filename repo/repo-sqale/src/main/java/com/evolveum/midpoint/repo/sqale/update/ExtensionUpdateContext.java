/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import static com.querydsl.core.types.ExpressionUtils.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.mapping.ExtensionMapping;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Update context for extension/attributes JSONB column.
 *
 * @param <Q> entity query type that holds the data for the mapped attributes
 * @param <R> row type related to the {@link Q}
 */
public class ExtensionUpdateContext<Q extends FlexibleRelationalPathBase<R>, R>
        extends SqaleUpdateContext<Containerable, Q, R> {

    private final ExtensionMapping<Q, R> mapping;
    private final JsonbPath jsonbPath;

    private final Map<String, Object> changedItems = new HashMap<>();
    private final List<String> deletedItems = new ArrayList<>();

    public ExtensionUpdateContext(SqaleUpdateContext<?, ?, ?> parentContext,
            ExtensionMapping<Q, R> mapping, JsonbPath jsonbPath) {
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
    public ExtensionMapping<Q, R> mapping() {
        return mapping;
    }

    @Override
    public <P extends Path<T>, T> void set(P path, T value) {
        throw new UnsupportedOperationException("not needed, not supported");
    }

    @Override
    public <P extends Path<T>, T> void set(P path, Expression<T> value) {
        // set is called directly on the parent in finishExecutionOwn()
        throw new UnsupportedOperationException("Not available for extension update context");
    }

    @Override
    public <P extends Path<T>, T> void setNull(P path) {
        throw new UnsupportedOperationException("not needed, not supported");
    }

    public void setChangedItem(String extItemId, Object value) {
        changedItems.put(extItemId, value);
    }

    public void deleteItem(String extItemId) {
        deletedItems.add(extItemId);
    }

    @Override
    protected void finishExecutionOwn() throws SchemaException, RepositoryException {
        if (deletedItems.isEmpty() && changedItems.isEmpty()) {
            return; // we don't have to do anything in the DB
        }

        // We need to avoid NULL otherwise the operations lower return NULL too.
        Expression<Jsonb> resultJsonb =
                template(Jsonb.class, "coalesce({0}, '{}')::jsonb", jsonbPath);
        if (!deletedItems.isEmpty()) {
            for (String deletedItem : deletedItems) {
                resultJsonb = template(Jsonb.class, "{0} - '{1s}'", resultJsonb, deletedItem);
            }
        }
        if (!changedItems.isEmpty()) {
            resultJsonb = template(Jsonb.class, "{0} || {1}::jsonb",
                    resultJsonb, Jsonb.fromMap(changedItems));
        }

        parentContext.set(jsonbPath, resultJsonb);
    }
}
