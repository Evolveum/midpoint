/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.StringItemFilterProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 */
public abstract class QObjectMapping<S extends ObjectType, Q extends QObject<R>, R extends MObject>
        extends SqaleModelMapping<S, Q, R> {

    protected QObjectMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        addItemMapping(PrismConstants.T_ID, StringItemFilterProcessor.mapper(path(q -> q.oid)));
        addItemMapping(ObjectType.F_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.nameOrig), path(q -> q.nameNorm)));

        addItemMapping(ObjectType.F_METADATA,
                // TODO nested-mapping
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.nameOrig), path(q -> q.nameNorm)));
        // TODO mappings
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        return new Path[] { entity.oid, entity.fullObject };
    }
}
