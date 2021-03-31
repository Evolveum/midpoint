/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Mapping between {@link QCase} and {@link CaseType}.
 */
public class QCaseMapping
        extends QObjectMapping<CaseType, QCase, MCase> {

    public static final String DEFAULT_ALIAS_NAME = "cs";

    public static final QCaseMapping INSTANCE = new QCaseMapping();

    private QCaseMapping() {
        super(QCase.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseType.class, QCase.class);

        addItemMapping(F_STATE, stringMapper(path(q -> q.state)));
        addItemMapping(F_CLOSE_TIMESTAMP,
                TimestampItemFilterProcessor.mapper(path(q -> q.closeTimestamp)));
        addItemMapping(F_OBJECT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.objectRefTargetOid),
                path(q -> q.objectRefTargetType),
                path(q -> q.objectRefRelationId)));
        addItemMapping(F_PARENT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.parentRefTargetOid),
                path(q -> q.parentRefTargetType),
                path(q -> q.parentRefRelationId)));
        addItemMapping(F_REQUESTOR_REF, RefItemFilterProcessor.mapper(
                path(q -> q.requestorRefTargetOid),
                path(q -> q.requestorRefTargetType),
                path(q -> q.requestorRefRelationId)));
        addItemMapping(F_TARGET_REF, RefItemFilterProcessor.mapper(
                path(q -> q.targetRefTargetOid),
                path(q -> q.targetRefTargetType),
                path(q -> q.targetRefRelationId)));
    }

    @Override
    protected QCase newAliasInstance(String alias) {
        return new QCase(alias);
    }

    @Override
    public ObjectSqlTransformer<CaseType, QCase, MCase>
    createTransformer(SqlTransformerSupport transformerSupport) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MCase newRowObject() {
        return new MCase();
    }
}
