/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.mapping.CountMappingResolver;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor that resolves {@link ExistsFilter}.
 *
 * @param <Q> query type of the original context
 * @param <R> row type related to {@link Q}
 */
public class OwnedByFilterProcessor<Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<OwnedByFilter> {

    private static final ItemPath PARENT = ItemPath.create(new ParentPathSegment());
    private final SqaleQueryContext<?, Q, R> context;
    private final QueryModelMapping<?, Q, R> mapping;

    public OwnedByFilterProcessor(SqaleQueryContext<?, Q, R> context) {
        this.context = context;
        this.mapping = context.mapping();
    }

    private OwnedByFilterProcessor(
            SqaleQueryContext<?, Q, R> context, QueryModelMapping<?, Q, R> mapping) {
        this.context = context;
        this.mapping = mapping;
    }

    @Override
    public Predicate process(OwnedByFilter filter) throws RepositoryException {
        return process(filter.getType(), filter.getPath(), filter.getFilter());
    }

    private <TQ extends FlexibleRelationalPathBase<TR>, TR>  Predicate process(
            @Nullable ComplexTypeDefinition ownerDefinition, ItemPath path, ObjectFilter innerFilter) throws RepositoryException {


        ItemRelationResolver<Q, R, TQ, TR> resolver = mapping.relationResolver(PARENT);
        // Instead of cleaner solution that would follow the code lower we resolve it here and now.
        // "Clean" solution would require more classes/code and would be more confusing.

        // Type is explicitly mentioned, we need to change parent type
        if (!(resolver instanceof TableRelationResolver)) {
            throw new QueryException("Repository supports owned by only for multi-value parent containers (for now)");
        }

        resolver = ((TableRelationResolver) resolver).forceSubquery();
        ItemRelationResolver.ResolutionResult<TQ, TR> resolution = resolver.resolve(context);
        //noinspection unchecked
        SqaleQueryContext<?, TQ, TR> parent = (SqaleQueryContext<?, TQ, TR>) resolution.context;


        if (ownerDefinition != null) {
            var typeClass = ownerDefinition.getCompileTimeClass();
            var actualMapping = context.repositoryContext().getMappingBySchemaType(ownerDefinition.getCompileTimeClass());


            resolver = ((TableRelationResolver) resolver).replaceTable(actualMapping);
        }

        // The resolver should not be join but subquery

        if (!(resolution.mapping instanceof QueryTableMapping)) {
            throw new QueryException("Repository supports owned by only for multi-value parent containers (for now)");
        }

        if (path != null) {
            @NotNull
            ItemRelationResolver<TQ, TR, FlexibleRelationalPathBase<Object>, Object> actualMapping = parent.mapping().relationResolver(path);
            if (actualMapping instanceof TableRelationResolver) {
                // We reverse found mapping from owner -> container to container -> owner
                resolver = ((TableRelationResolver) actualMapping).reverse((QueryTableMapping) resolution.mapping);
            }
        }
        // We resolve context again, since we may need to add distinguisher from forwards mapping (owner to container)
        resolution = resolver.resolve(context);
        parent = (SqaleQueryContext<?, TQ, TR>) resolution.context;
        parent.processFilter(innerFilter);
        if (resolution.subquery) {
            return parent.sqlQuery().exists();
        }
        return QuerydslUtils.EXPRESSION_TRUE;
    }
}
