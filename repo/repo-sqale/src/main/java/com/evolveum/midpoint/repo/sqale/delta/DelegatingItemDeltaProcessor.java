/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.mapping.ContainerTableRelationResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemRelationResolver;
import com.evolveum.midpoint.repo.sqale.mapping.UpdatableItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.update.NestedContainerUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * This is default item delta processor that decides what to do with the modification.
 * If the modification has multi-part name then it resolves it to the last component first.
 *
 * This component is for delta processing what {@link ValueFilterProcessor} is for filters.
 * Notable difference is that context and mapping are always related here, even for nested
 * mappings on the same table we create new context which contains the right mapping, so we
 * just track context changes here.
 */
public class DelegatingItemDeltaProcessor implements ItemDeltaProcessor {

    /** Query context is not final as it can change during complex path resolution. */
    private SqaleUpdateContext<?, ?, ?> context;

    public DelegatingItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public ProcessingHint process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException {
        QName itemName = resolvePath(modification);
        if (itemName == null) {
            // This may indicate forgotten mapping, but normally it means that the item is simply
            // not externalized and there is nothing to, update is only in fullObject.
            return DEFAULT_PROCESSING;
        }

        QueryModelMapping<?, ?, ?> mapping = context.mapping();
        ItemSqlMapper<?, ?> itemSqlMapper = mapping.getItemMapper(itemName);
        if (itemSqlMapper instanceof UpdatableItemSqlMapper) {
            return ((UpdatableItemSqlMapper<?, ?>) itemSqlMapper)
                    .createItemDeltaProcessor(context)
                    .process(modification);
        } else if (itemSqlMapper != null) {
            // This should not happen, all the Sqale mappings should use SqaleItemSqlMapper.
            throw new IllegalArgumentException("No delta processor available for " + itemName
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }

        // If the mapper is null, the item is not indexed ("externalized") attribute, no action.
        // It's a similar case like the fast return after resolving the path.
        return DEFAULT_PROCESSING;
    }

    private QName resolvePath(ItemDelta<?, ?> modification) throws RepositoryException {
        ItemPath path = modification.getPath();
        while (!path.isSingleName()) {
            ItemName firstName = path.firstName();
            path = path.rest();

            QueryModelMapping<?, ?, ?> mapping = context.mapping();
            ItemRelationResolver<?, ?, ?, ?> relationResolver = mapping.getRelationResolver(firstName);
            if (relationResolver == null) {
                return null; // unmapped, not persisted, nothing to do
            }

            if (!(relationResolver instanceof SqaleItemRelationResolver)) {
                // Again, programmers fault.
                throw new IllegalArgumentException("Relation resolver for " + firstName
                        + " in mapping " + mapping + " does not support delta modifications. "
                        + "Used modification: " + modification);
            }

            ItemPath subcontextPath = firstName;
            if (relationResolver instanceof ContainerTableRelationResolver) {
                Object cid = path.first();
                path = path.rest();
                subcontextPath = ItemPath.create(firstName, cid);
            }

            // We want to use the same subcontext for the same item path to use one UPDATE.
            SqaleUpdateContext<?, ?, ?> subcontext = context.getSubcontext(subcontextPath);
            if (subcontext == null) {
                // we know nothing about context and resolver types, so we have to ignore it
                //noinspection unchecked,rawtypes
                subcontext = ((SqaleItemRelationResolver) relationResolver)
                        .resolve(this.context, subcontextPath);
                if (subcontext == null) {
                    return null; // this means "ignore"
                }
                context.addSubcontext(subcontextPath, subcontext);
            }
            context = subcontext;
            if (path.firstToIdOrNull() != null && context instanceof NestedContainerUpdateContext<?,?,?>) {
                // Skip id (We are nesting inside virtual multivalue container
                path = path.rest();
            }
        }
        return path.asSingleName();
    }
}
