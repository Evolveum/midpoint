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
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemRelationResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;

/**
 * This is default item delta processor that decides what to do with the modification.
 * If the modification has multi-part name then it resolves it to the last component first.
 *
 * *Implementation note:*
 * This is a separate component used by {@link SqaleUpdateContext} but while the context is
 * one for all modifications this is instantiated for each modification (or even path resolution
 * step) which allows for state changes that don't affect processing of another modification.
 */
public class DelegatingItemDeltaProcessor implements ItemDeltaProcessor {

    /** Query context and mapping is not final as it can change during complex path resolution. */
    private SqaleUpdateContext<?, ?, ?> context;
    private QueryModelMapping<?, ?, ?> mapping;

    public DelegatingItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context, QueryModelMapping<?, ?, ?> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public void process(ItemDelta<?, ?> modification) throws RepositoryException {
        // TODO will we need various types of SqaleUpdateContext too?
        //  E.g. AccessCertificationWorkItemType is container inside container and to add/delete
        //  it we need to anchor the context in its parent, not in absolute root of update context.
        //  Similar situation is adding multi-value references to containers like assignments.

        QName itemName = resolvePath(modification.getPath());
        if (itemName == null) {
            // This may indicate forgotten mapping, but normally it means that the item is simply
            // not externalized and there is nothing to, update is only in fullObject.
            return;
        }

        ItemSqlMapper itemSqlMapper = mapping.getItemMapper(itemName);
        if (itemSqlMapper instanceof SqaleItemSqlMapper) {
            ((SqaleItemSqlMapper) itemSqlMapper)
                    .createItemDeltaProcessor(context)
                    .process(modification);
        } else if (itemSqlMapper != null) {
            // This should not happen, all the Sqale mappings should use SqaleItemSqlMapper.
            throw new IllegalArgumentException("No delta processor available for " + itemName
                    + " in mapping " + mapping + "! (Only query mapping is available.)");
        }

        // If the mapper is null, the item is not indexed ("externalized") attribute, no action.
        // It's a similar case like the fast return after resolving the path.
    }

    private QName resolvePath(ItemPath path) throws QueryException {
        while (!path.isSingleName()) {
            ItemName firstName = path.firstName();
            path = path.rest();

            ItemRelationResolver relationResolver = mapping.getRelationResolver(firstName);
            if (relationResolver == null) {
                return null; // unmapped, not persisted, nothing to do
            }

            if (!(relationResolver instanceof SqaleItemRelationResolver)) {
                // Again, programmers fault.
                throw new IllegalArgumentException("Relation resolver for " + firstName
                        + " in mapping " + mapping + " does not support delta modifications!");
            }
            SqaleItemRelationResolver resolver = (SqaleItemRelationResolver) relationResolver;
            SqaleItemRelationResolver.UpdateResolutionResult resolution = resolver.resolve(context);
            context = resolution.context;
            mapping = resolution.mapping;
        }
        return path.asSingleName();
    }
}
