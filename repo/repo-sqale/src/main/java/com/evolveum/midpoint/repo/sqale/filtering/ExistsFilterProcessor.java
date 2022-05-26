/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.mapping.CountMappingResolver;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor that resolves {@link ExistsFilter}.
 *
 * @param <Q> query type of the original context
 * @param <R> row type related to {@link Q}
 */
public class ExistsFilterProcessor<Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<ExistsFilter> {

    private final SqaleQueryContext<?, Q, R> context;
    private final QueryModelMapping<?, Q, R> mapping;

    public ExistsFilterProcessor(SqaleQueryContext<?, Q, R> context) {
        this.context = context;
        this.mapping = context.mapping();
    }

    private ExistsFilterProcessor(
            SqaleQueryContext<?, Q, R> context, QueryModelMapping<?, Q, R> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public Predicate process(ExistsFilter filter) throws RepositoryException {
        return process(filter.getFullPath(), filter);
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR> Predicate process(
            ItemPath path, ExistsFilter filter) throws RepositoryException {
        if (path.isEmpty()) {
            ObjectFilter innerFilter = filter.getFilter();

            // empty filter means EXISTS, NOT can be added before the filter
            return innerFilter != null
                    ? context.process(innerFilter)
                    : null;
        }

        ItemRelationResolver<Q, R, TQ, TR> resolver = mapping.relationResolver(path);
        // Instead of cleaner solution that would follow the code lower we resolve it here and now.
        // "Clean" solution would require more classes/code and would be more confusing.
        if (resolver instanceof CountMappingResolver<?, ?>) {
            return ((CountMappingResolver<Q, R>) resolver).createExistsPredicate(context);
        }

        ItemRelationResolver.ResolutionResult<TQ, TR> resolution = resolver.resolve(context);
        //noinspection unchecked
        SqaleQueryContext<?, TQ, TR> subcontext = (SqaleQueryContext<?, TQ, TR>) resolution.context;
        // TODO this check should only apply for the last component of EXISTS, see SqaleRepoSearchTest.test422SearchObjectBySingleValueReferenceTargetUsingExists
        //if (!(resolution.mapping instanceof QueryTableMapping)) {
        //    throw new QueryException("Repository supports exists only for multi-value containers or refs");
        //}

        ExistsFilterProcessor<TQ, TR> nestedProcessor =
                new ExistsFilterProcessor<>(subcontext, resolution.mapping);
        Predicate predicate = nestedProcessor.process(path.rest(), filter);
        if (resolution.subquery) {
            return subcontext.sqlQuery().where(predicate).exists();
        } else {
            return predicate;
        }
    }
}
