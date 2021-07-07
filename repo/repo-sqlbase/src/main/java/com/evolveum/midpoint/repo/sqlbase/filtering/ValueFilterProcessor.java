/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor that resolves item path and then constructs an SQL condition for it.
 * This covers the needs of {@link ValueFilter} subtypes.
 *
 * Despite the class name it does not directly contain code that creates conditions based on values.
 * This is still a structural processor and the name "value filter" merely reflects the type of
 * a filter which it processes.
 *
 * If the path has multiple names it creates new instance of this filter processor for each
 * step to preserve contextual information - e.g. when predicate needs to be applied to the current
 * subcontext and EXISTS subquery needs to be propagated higher.
 *
 * For the last path component it delegates to the right {@link ItemValueFilterProcessor}
 * to process the values and construct SQL conditions.
 */
public class ValueFilterProcessor<Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<ValueFilter<?, ?>> {

    private final SqlQueryContext<?, Q, R> context;
    private final QueryModelMapping<?, Q, R> mapping;

    public ValueFilterProcessor(SqlQueryContext<?, Q, R> context) {
        this(context, context.mapping());
    }

    private ValueFilterProcessor(
            SqlQueryContext<?, Q, R> context, QueryModelMapping<?, Q, R> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
        return process(filter.getPath(), filter);
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR> Predicate process(
            ItemPath path, ValueFilter<?, ?> filter) throws RepositoryException {
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
            QName firstName = path.first() instanceof ParentPathSegment
                    ? PrismConstants.T_PARENT
                    : path.firstName();
            ItemRelationResolver<Q, R, TQ, TR> resolver = mapping.relationResolver(firstName);
            ItemRelationResolver.ResolutionResult<TQ, TR> resolution = resolver.resolve(context);
            SqlQueryContext<?, TQ, TR> subcontext = resolution.context;
            ValueFilterProcessor<TQ, TR> nestedProcessor =
                    new ValueFilterProcessor<>(subcontext, resolution.mapping);
            Predicate predicate = nestedProcessor.process(path.rest(), filter);
            if (resolution.subquery) {
                return subcontext.sqlQuery()
                        .where(predicate)
                        .exists();
            } else {
                return predicate;
            }
        }
    }
}
