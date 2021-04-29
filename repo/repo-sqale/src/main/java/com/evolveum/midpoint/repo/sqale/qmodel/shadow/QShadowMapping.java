/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Mapping between {@link QShadow} and {@link ShadowType}.
 */
public class QShadowMapping
        extends QObjectMapping<ShadowType, QShadow, MShadow> {

    public static final String DEFAULT_ALIAS_NAME = "sh";

    public static final QShadowMapping INSTANCE = new QShadowMapping();

    private QShadowMapping() {
        super(QShadow.TABLE_NAME, DEFAULT_ALIAS_NAME, ShadowType.class, QShadow.class);

        addItemMapping(ShadowType.F_OBJECT_CLASS, uriMapper(q -> q.objectClassId));
        addItemMapping(F_RESOURCE_REF, refMapper(
                q -> q.resourceRefTargetOid,
                q -> q.resourceRefTargetType,
                q -> q.resourceRefRelationId));
        addItemMapping(F_INTENT, stringMapper(q -> q.intent));
        addItemMapping(F_KIND, enumMapper(q -> q.kind));
        // TODO attemptNumber?
        addItemMapping(F_DEAD, booleanMapper(q -> q.dead));
        addItemMapping(F_EXISTS, booleanMapper(q -> q.exist));
        addItemMapping(F_FULL_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.fullSynchronizationTimestamp));
        // TODO size filter? how?
//        addItemMapping(F_PENDING_OPERATION, integerMapper(q -> q.pendingOperationCount));
        addItemMapping(F_PRIMARY_IDENTIFIER_VALUE, stringMapper(q -> q.primaryIdentifierValue));
        addItemMapping(F_SYNCHRONIZATION_SITUATION, enumMapper(q -> q.synchronizationSituation));
        addItemMapping(F_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(q -> q.synchronizationTimestamp));
    }

    @Override
    protected QShadow newAliasInstance(String alias) {
        return new QShadow(alias);
    }

    @Override
    public ShadowSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new ShadowSqlTransformer(transformerSupport, this);
    }

    @Override
    public MShadow newRowObject() {
        return new MShadow();
    }
}
