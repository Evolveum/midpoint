/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
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

        addItemMapping(ShadowType.F_OBJECT_CLASS,
                UriItemFilterProcessor.mapper(path(q -> q.objectClassId)));
        addItemMapping(F_RESOURCE_REF, RefItemFilterProcessor.mapper(
                path(q -> q.resourceRefTargetOid),
                path(q -> q.resourceRefTargetType),
                path(q -> q.resourceRefRelationId)));
        addItemMapping(F_INTENT, stringMapper(path(q -> q.intent)));
        addItemMapping(F_KIND, EnumItemFilterProcessor.mapper(path(q -> q.kind)));
        // TODO attemptNumber?
        addItemMapping(F_DEAD, booleanMapper(path(q -> q.dead)));
        addItemMapping(F_EXISTS, booleanMapper(path(q -> q.exist)));
        addItemMapping(F_FULL_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(path(q -> q.fullSynchronizationTimestamp)));
        // TODO size filter? how?
//        addItemMapping(F_PENDING_OPERATION, integerMapper(path(q -> q.pendingOperationCount)));
        addItemMapping(F_PRIMARY_IDENTIFIER_VALUE,
                stringMapper(path(q -> q.primaryIdentifierValue)));
        addItemMapping(F_SYNCHRONIZATION_SITUATION,
                EnumItemFilterProcessor.mapper(path(q -> q.synchronizationSituation)));
        addItemMapping(F_SYNCHRONIZATION_TIMESTAMP,
                timestampMapper(path(q -> q.synchronizationTimestamp)));
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
