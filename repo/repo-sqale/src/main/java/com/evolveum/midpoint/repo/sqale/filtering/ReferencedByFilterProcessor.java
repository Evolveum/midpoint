/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ReferencedByFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Filter processor that resolves {@link ExistsFilter}.
 *
 * @param <Q> query type of the original context
 * @param <R> row type related to {@link Q}
 */
public class ReferencedByFilterProcessor<Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<ReferencedByFilter> {

    private final SqaleQueryContext<?, Q, R> context;

    public ReferencedByFilterProcessor(SqaleQueryContext<?, Q, R> context) throws QueryException {
        QueryException.check(context.mapping() instanceof QObjectMapping, "ReferencedBy can only be used on object");
        this.context = context;
    }

    @Override
    public Predicate process(ReferencedByFilter filter) throws RepositoryException {
        return process(filter.getType(), filter.getPath(), filter.getRelation(), filter.getFilter());
    }

    private Predicate process(@NotNull ComplexTypeDefinition ownerDefinition,
            ItemPath path, QName relation, ObjectFilter innerFilter) throws RepositoryException {

        var targetMapping = context.repositoryContext().getMappingBySchemaType(ownerDefinition.getCompileTimeClass());
        var targetContext = context.subquery(targetMapping);
        relation = relation != null ? relation : PrismConstants.Q_ANY;
        // We use our package private RefFilter and logic in existing ref filter implementation
        // to sneak in the OID from parent.
        targetContext.processFilter(internalRefFilter(path, relation, context.path(QObject.class).oid));
        // We add nested filter for referencing object
        targetContext.processFilter(innerFilter);
        return targetContext.sqlQuery().exists();
    }

    private ObjectFilter internalRefFilter(ItemPath path, QName relation, UuidPath oid) {
        return new RefFilterWithRepoPath(path, relation, oid);
    }
}
