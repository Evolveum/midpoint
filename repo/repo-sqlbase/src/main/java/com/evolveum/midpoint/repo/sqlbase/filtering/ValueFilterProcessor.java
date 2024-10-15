/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
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
 * filter which it processes.
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
        this(context, context.queryMapping());
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

    @Override
    public Predicate process(ValueFilter<?, ?> filter, RightHandProcessor rightPath) throws RepositoryException {
        return process(filter.getPath(), filter, rightPath);
    }

    private Predicate process(
            ItemPath path, ValueFilter<?, ?> filter) throws RepositoryException {
        final RightHandProcessor right;
        if (filter.getRightHandSidePath() != null) {
            right = processRight(filter.getRightHandSidePath());
        } else {
            right = null;
        }
        return process(path, filter, right);
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR> Predicate process(
            ItemPath path, ValueFilter<?, ?> filter, RightHandProcessor right) throws RepositoryException {
        // isSingleName/asSingleName or firstName don't work for T_ID (OID)
        if (path.size() <= 1) {
            QName itemName = path.isEmpty() ? PrismConstants.T_SELF : path.firstToQName();

            // Special case for iterative ref search additional conditions for ownedBy/ref filter.
            // If mapper for self-path is not found, we will try parent context, but insert the predicate to this one.
            // Tested by test130SearchIterativeWithCustomOrderingByOwnerItemDesc.
            SqlQueryContext<?, ?, ?> parentContext = context.parentContext();
            if (path.isEmpty() && parentContext != null && mapping.getItemMapper(itemName) == null) {
                try {
                    return parentContext.process(filter);
                } catch (RepositoryException e) {
                    throw new QueryException(
                            "Special case self-path processing on parent context failed for filter: " + filter, e);
                }
            }

            ItemValueFilterProcessor<ValueFilter<?, ?>> filterProcessor =
                    mapping.itemMapper(itemName)
                            .createFilterProcessor(context);
            if (filterProcessor == null) {
                throw new QueryException("Filtering on " + path + " is not supported.");
                // this should not even happen, we can't even create a Query that would cause this
            }
            if (right != null) {
                return filterProcessor.process(filter, right);
            }
            return filterProcessor.process(filter);
        } else {
            boolean skipJoinIfPossible = !path.isEmpty() && PrismConstants.T_PARENT.equals(path.first());

            //noinspection DuplicatedCode
            ItemRelationResolver.ResolutionResult<TQ, TR> resolution = mapping.<TQ, TR>relationResolver(path).resolve(context, skipJoinIfPossible);
            SqlQueryContext<?, TQ, TR> subcontext = resolution.context;

            ValueFilterProcessor<TQ, TR> nestedProcessor = new ValueFilterProcessor<>(subcontext, resolution.mapping);
            Predicate predicate = nestedProcessor.process(path.rest(), filter, right);

            if (resolution.subquery) {
                return subcontext.sqlQuery()
                        .where(predicate)
                        .exists();
            } else {
                return predicate;
            }
        }
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR> RightHandProcessor processRight(ItemPath path)
            throws RepositoryException {
        if (path.size() == 1) {
            QName itemName = path.firstToQName();
            RightHandProcessor filterProcessor =
                    mapping.itemMapper(itemName)
                            .createRightHandProcessor(context);
            if (filterProcessor == null) {
                throw new QueryException("Filtering on " + path + " is not supported.");
                // this should not even happen, we can't even create a Query that would cause this
            }
            return filterProcessor;
        } else {
            //noinspection DuplicatedCode
            ItemRelationResolver.ResolutionResult<TQ, TR> resolution =
                    mapping.<TQ, TR>relationResolver(path).resolve(context);
            SqlQueryContext<?, TQ, TR> subcontext = resolution.context;
            ValueFilterProcessor<TQ, TR> nestedProcessor =
                    new ValueFilterProcessor<>(subcontext, resolution.mapping);
            if (resolution.subquery) {
                throw new RepositoryException("Right path " + path + "is not single value");
            }
            return nestedProcessor.processRight(path.rest());
        }
    }
}
