/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;

/**
 * Filter processor that resolves item path and then constructs an SQL condition for it.
 * This covers the needs of {@link ValueFilter} subtypes.
 *
 * Despite the class name it does not directly contain code that creates conditions based on values.
 * This is still a structural processor and the name "value filter" merely reflects the type of
 * a filter which it processes.
 * During multi-part item path it creates necessary JOINs or subqueries creating new
 * {@link #context} and {@link #mapping} instances in the process.
 * Finally, it then delegates to the right {@link ItemValueFilterProcessor} to process the values
 * and construct SQL conditions.
 */
public class ValueFilterProcessor implements FilterProcessor<ValueFilter<?, ?>> {

    /** Query context and mapping is not final as it can change during complex path resolution. */
    private final SqlQueryContext<?, ?, ?> context;
    private final QueryModelMapping<?, ?, ?> mapping;

    public ValueFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this(context, context.mapping());
    }

    private ValueFilterProcessor(
            SqlQueryContext<?, ?, ?> context, QueryModelMapping<?, ?, ?> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
        return process(filter.getPath(), filter);
    }

    private Predicate process(ItemPath path, ValueFilter<?, ?> filter) throws RepositoryException {
        if (filter.getRightHandSidePath() != null) {
            // TODO implement
            throw new QueryException(
                    "Filter with right-hand-side path is not supported YET: " + path);
        }

        if (path.isSingleName()) {
            QName itemName = path.asSingleName();
            ItemValueFilterProcessor<ValueFilter<?, ?>> filterProcessor =
                    mapping.itemMapper(itemName)
                            .createFilterProcessor(context);
            if (filterProcessor == null) {
                throw new QueryException("Filtering on " + path + " is not supported.");
                // this should not even happen, we can't even create a Query that would cause this
            }

            return filterProcessor.process(filter);
        } else {
            // we know nothing about context and resolver types, so we have to ignore it
            //noinspection rawtypes
            ItemRelationResolver resolver = mapping.relationResolver(path.firstName());
            //noinspection unchecked
            ItemRelationResolver.ResolutionResult resolution = resolver.resolve(context);
            SqlQueryContext<?, ?, ?> subcontext = resolution.context;
            ValueFilterProcessor nestedProcessor =
                    new ValueFilterProcessor(subcontext, resolution.mapping);
            Predicate predicate = nestedProcessor.process(path.rest(), filter);
            if (resolution.subquery) {
                subcontext.sqlQuery().where(predicate);
                return subcontext.sqlQuery().exists();
            } else {
                return predicate;
            }
        }
    }
}
