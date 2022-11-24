/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.update;

import java.util.Map;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.delta.item.UriItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Update context manages state information related to the currently executed modify operation.
 * Contexts can be nested for any non-trivial updates where each instance matches the changed item
 * and its mapping.
 * {@link RootUpdateContext} is the top level parent context that holds the main object.
 *
 * For example, given a path `assignment/1/metadata/channel`:
 *
 * * {@link RootUpdateContext} is created for the object, imagine it at the root of the path.
 * * {@link ContainerTableUpdateContext} is created for `assignment/1` part under the root context.
 * * {@link NestedContainerUpdateContext} is created for `metadata` part.
 * * One of {@link ItemDeltaValueProcessor} is then created for `channel` based on the type of
 * the property (in this case {@link UriItemDeltaProcessor}) which processes actual values and
 * uses the context above to do so.
 *
 * In this example channel is single-valued property and will be changed adding a `SET` clause
 * to the `UPDATE` held by the container table update context.
 * This `UPDATE` will use proper `WHERE` clause negotiated between this context and its parent,
 * which in this case is the root context, using also the container ID provided in the item path.
 * Nested container context merely "focuses" from assignment container to its metadata, but the
 * table is still the same.
 *
 * This class also coordinates final update execution for the whole context tree implemented
 * as a template method {@link #finishExecution()} called from the {@link RootUpdateContext}.
 * Subclasses implement their specific logic in {@link #finishExecutionOwn()}.
 *
 * @param <S> schema type of the mapped object (potentially in nested mapping)
 * @param <Q> query entity type
 * @param <R> row type related to the {@link Q}
 */
public abstract class SqaleUpdateContext<S, Q extends FlexibleRelationalPathBase<R>, R> {

    protected final Trace logger = TraceManager.getTrace(getClass());

    private final SqaleRepoContext repositoryContext;
    protected final JdbcSession jdbcSession;
    protected final R row;

    // Fields for managing update context tree
    /**
     * Parent is typically used to do some work for the child like when nested container
     * uses the parent's UPDATE clause.
     */
    protected final SqaleUpdateContext<?, ?, ?> parentContext;

    /**
     * Map of subcontext for known {@link ItemPath} sub-paths relative to this context.
     * {@link ItemName} is not enough to represent multi-value container paths like `assignment/1`.
     *
     * PathKeyedMap is used to ignore rare, but possible, namespace discrepancies (MID-8258).
     * Ignoring namespaces here is acceptable, because it is only used for known built-in containers.
     */
    protected final Map<ItemPath, SqaleUpdateContext<?, ?, ?>> subcontexts = new PathKeyedMap<>();

    public SqaleUpdateContext(
            SqaleRepoContext repositoryContext,
            JdbcSession jdbcSession,
            R row) {
        this.repositoryContext = repositoryContext;
        this.jdbcSession = jdbcSession;
        this.row = row;

        parentContext = null; // this is the root context without any parent
    }

    public SqaleUpdateContext(
            SqaleUpdateContext<?, ?, ?> parentContext,
            R row) {
        this.parentContext = parentContext;
        // registering this with parent context must happen outside of constructor!
        this.repositoryContext = parentContext.repositoryContext;
        this.jdbcSession = parentContext.jdbcSession();
        this.row = row;
    }

    public SqaleRepoContext repositoryContext() {
        return repositoryContext;
    }

    public JdbcSession jdbcSession() {
        return jdbcSession;
    }

    public R row() {
        return row;
    }

    /**
     * Returns entity path (table) for the context.
     * This is NOT the path for item column, which can be actually multiple columns (ref, poly)
     * or just part of the value in the column (JSONB).
     */
    public abstract Q entityPath();

    public abstract QueryModelMapping<S, Q, R> mapping();

    public abstract <P extends Path<T>, T> void set(P path, T value);

    public abstract <P extends Path<T>, T> void set(P path, Expression<T> value);

    public abstract <P extends Path<T>, T> void setNull(P path);

    @SuppressWarnings("UnusedReturnValue")
    public <TS, TR> TR insertOwnedRow(QOwnedByMapping<TS, TR, R> mapping, TS schemaObject) throws SchemaException {
        return mapping.insert(schemaObject, row, jdbcSession);
    }

    public SqaleUpdateContext<?, ?, ?> getSubcontext(ItemPath itemPath) {
        return subcontexts.get(itemPath);
    }

    public void addSubcontext(ItemPath itemPath, SqaleUpdateContext<?, ?, ?> subcontext) {
        if (subcontext == null) {
            throw new IllegalArgumentException("Null update subcontext for item path: " + itemPath);
        }
        if (subcontexts.containsKey(itemPath)) {
            // This should not happen if code above is written properly, but prevents losing
            // updates when multiple modifications use the same container path segment.
            throw new IllegalStateException(
                    "Trying to overwrite existing subcontext for item path: " + itemPath);
        }
        subcontexts.put(itemPath, subcontext);
    }

    /**
     * Executes collected updates if applicable including all subcontexts.
     * Implement the logic for one context in {@link #finishExecutionOwn()}.
     * Updates for subtree are executed first.
     *
     * Insert and delete clauses were all executed by {@link ItemDeltaValueProcessor}s already.
     */
    protected final void finishExecution() throws SchemaException, RepositoryException {
        for (SqaleUpdateContext<?, ?, ?> sqaleUpdateContext : subcontexts.values()) {
            sqaleUpdateContext.finishExecution();
        }
        finishExecutionOwn();
    }

    protected abstract void finishExecutionOwn() throws SchemaException, RepositoryException;

    public <O> O findValueOrItem(@NotNull ItemPath path) {
        if (parentContext == null) {
            throw new UnsupportedOperationException(
                    "findItem() is unsupported on non-root update context");
        }
        return parentContext.findValueOrItem(path);
    }

    public boolean isOverwrittenId(Long id) {
        if (parentContext == null) {
            throw new UnsupportedOperationException(
                    "findItem() is unsupported on non-root update context");
        }
        return parentContext.isOverwrittenId(id);
    }
}
