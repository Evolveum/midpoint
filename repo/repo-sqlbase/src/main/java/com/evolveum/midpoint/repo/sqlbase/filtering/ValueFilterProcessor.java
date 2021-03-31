/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemRelationResolver;

/**
 * Filter processor that resolves item path and then constructs an SQL condition for it.
 * This covers needs of {@link RefFilter} and {@link PropertyValueFilter}.
 */
public class ValueFilterProcessor implements FilterProcessor<ValueFilter<?, ?>> {

    /** Query context here is not final as it can change during complex path resolution. */
    private SqlQueryContext<?, ?, ?> context;
    private QueryModelMapping<?, ?, ?> mapping;

    public ValueFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
        this.mapping = context.mapping();
    }

    @Override
    public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
        if (filter.getRightHandSidePath() != null) {
            throw new QueryException(
                    "Filter with right-hand-side path is not supported YET: " + filter.getPath());
        }

        // TODO: needed only for Any filter?
//        ItemDefinition definition = filter.getDefinition();

        QName itemName = resolvePath(filter.getPath());
        return mapping.itemMapper(itemName)
                .createFilterProcessor(context)
                .process(filter);
    }

    /**
     * Resolves potentially complex path and returns {@link ItemName} of its last component.
     * Initial elements (all-but-last) may add new JOINs to the query (or find matching ones),
     * but not necessarily, e.g. for containers embedded in a table (like {@code metadata}).
     */
    private QName resolvePath(ItemPath path) throws QueryException {
        // TODO do we want to cache it? where to keep the cache? sqlQueryContext probably...
        //  Also: when we want to reuse the resolution and when we want to add new JOINs?
        //  Embedded mapping is probably cacheable, but what if it is behind JOIN?

        while (!path.isSingleName()) {
            ItemName firstName = path.firstName();
            path = path.rest();

            ItemRelationResolver resolver = mapping.relationResolver(firstName);
            ItemRelationResolver.ResolutionResult resolution = resolver.resolve(context);
            context = resolution.context;
            mapping = resolution.mapping;
        }
        return path.asSingleName();
    }
}
