/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Resolver supporting dereferencing of embedded references.
 * Internally, it creates its own nested mapping with relation resolver only for @.
 *
 * @param <Q> query type of entity where the reference mapping is declared
 * @param <R> row type of {@link Q}
 */
public class EmbeddedReferenceResolver<Q extends FlexibleRelationalPathBase<R>, R>
        implements ItemRelationResolver<Q, R, Q, R> {

    private final SqaleNestedMapping<Referencable, Q, R> mapping;

    public <TS, TQ extends QObject<TR>, TR extends MObject> EmbeddedReferenceResolver(
            @NotNull Class<Q> queryType,
            @NotNull Function<Q, UuidPath> rootToOidPath,
            @NotNull Supplier<QueryTableMapping<TS, TQ, TR>> targetMappingSupplier) {
        mapping = new SqaleNestedMapping<>(Referencable.class, queryType);
        mapping.addRelationResolver(PrismConstants.T_OBJECT_REFERENCE,
                TableRelationResolver.usingJoin(
                        targetMappingSupplier, (q, t) -> rootToOidPath.apply(q).eq(t.oid)));
    }

    @Override
    public ResolutionResult<Q, R> resolve(SqlQueryContext<?, Q, R> context, boolean parent) {
        return new ResolutionResult<>(context, mapping);
    }
}
