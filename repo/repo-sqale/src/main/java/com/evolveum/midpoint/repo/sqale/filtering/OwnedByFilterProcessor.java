/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.Objects;

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
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

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

    @Override
    public Predicate process(OwnedByFilter filter) throws RepositoryException {
        return process(filter.getType(), filter.getPath(), filter.getFilter());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <TQ extends FlexibleRelationalPathBase<TR>, TR> Predicate process(
            @Nullable ComplexTypeDefinition ownerDefinition, ItemPath path, ObjectFilter innerFilter) throws RepositoryException {

        ItemRelationResolver<Q, R, TQ, TR> itemResolver = mapping.relationResolver(PARENT);
        // Instead of cleaner solution that would follow the code lower we resolve it here and now.
        // "Clean" solution would require more classes/code and would be more confusing.

        // Type is explicitly mentioned, we need to change parent type
        if (!(itemResolver instanceof TableRelationResolver)) {
            throw new QueryException("Repository supports owned by only for multi-value parent containers (for now)");
        }
        var resolver = (TableRelationResolver) itemResolver;

        var parentMapping = resolver.mapping();
        // User provided more specific definition of type
        if (ownerDefinition != null) {
            var typeClass = Objects.requireNonNull(ownerDefinition.getCompileTimeClass());
            QueryException.check(parentMapping.schemaType().isAssignableFrom(typeClass),
                    "Requested type %s is not subtype of %s", typeClass, parentMapping.schemaType());
            parentMapping = context.repositoryContext().getMappingBySchemaType(typeClass);
            // Resolver is updated with more specific type, so we search only concrete type and not abstract type
            resolver = resolver.replaceTable(parentMapping);
        }
        QueryException.check(parentMapping instanceof QueryTableMapping,
                "Repository supports owned by only for multi-value parent containers (for now)");
        resolver = resolver.withSubquery();
        // User provided path - different paths may have different correlation queries,
        // so we need to do walk from parent to child in order to obtain correct correlation
        // query, e.g. inducement vs assignment.
        if (path != null) {
            @NotNull
            ItemRelationResolver<TQ, TR, FlexibleRelationalPathBase<Object>, Object> actualMapping = parentMapping.relationResolver(path);
            if (actualMapping instanceof TableRelationResolver) {
                // We reverse found mapping from owner -> container to container -> owner
                resolver = ((TableRelationResolver) actualMapping).reverse(parentMapping);
            }
        }
        // Finally, we have correctly configured resolver so we can proceed with construction of query.
        var resolution = resolver.resolve(context);
        var parent = (SqaleQueryContext<?, TQ, TR>) resolution.context;
        parent.processFilter(innerFilter);
        return parent.sqlQuery().exists();
    }
}
