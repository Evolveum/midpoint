/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.booleanMapper;
import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType.*;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

/**
 * Mapping between {@link QAbstractRole} and {@link AbstractRoleType}.
 */
public class QAbstractRoleMapping<
        S extends AbstractRoleType, Q extends QAbstractRole<R>, R extends MAbstractRole>
        extends QObjectMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "ar";

    public static final QAbstractRoleMapping<
            AbstractRoleType, QAbstractRole.QAbstractRoleReal, MAbstractRole> INSTANCE =
            new QAbstractRoleMapping<>(QAbstractRole.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    AbstractRoleType.class, QAbstractRole.QAbstractRoleReal.class);

    protected QAbstractRoleMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        // TODO how is approvalProcess mapped? Nothing found in RAbstractRole
        // addItemMapping(AbstractRoleType.F_AUTOASSIGN ...TODO nested mapping AutoassignSpecificationType
        addItemMapping(F_DISPLAY_NAME, PolyStringItemFilterProcessor.mapper(
                path(q -> q.displayNameOrig), path(q -> q.displayNameNorm)));
        addItemMapping(F_IDENTIFIER, stringMapper(path(q -> q.identifier)));
        // TODO how is ownerRef* mapped? Nothing found in RAbstractRole or as F_ constant
        addItemMapping(F_REQUESTABLE, booleanMapper(path(q -> q.requestable)));
        addItemMapping(F_RISK_LEVEL, stringMapper(path(q -> q.riskLevel)));
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        return new Path[] { entity.oid, entity.fullObject };
    }

    // TODO verify that this allows creation of QAbstractRole alias and that it suffices for "generic query"
    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QAbstractRole<>(MAbstractRole.class, alias);
    }

    @Override
    public AbstractRoleSqlTransformer<S, Q, R> createTransformer(
            SqlTransformerContext transformerContext) {
        return new AbstractRoleSqlTransformer<>(transformerContext, this);
    }
}
