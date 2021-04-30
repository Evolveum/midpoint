/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
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

        addItemMapping(F_STATE, stringMapper(q -> q.state));
        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        addItemMapping(F_OBJECT_REF, refMapper(
                q -> q.objectRefTargetOid,
                q -> q.objectRefTargetType,
                q -> q.objectRefRelationId));
        addItemMapping(F_PARENT_REF, refMapper(
                q -> q.parentRefTargetOid,
                q -> q.parentRefTargetType,
                q -> q.parentRefRelationId));
        addItemMapping(F_REQUESTOR_REF, refMapper(
                q -> q.requestorRefTargetOid,
                q -> q.requestorRefTargetType,
                q -> q.requestorRefRelationId));
        addItemMapping(F_TARGET_REF, refMapper(
                q -> q.targetRefTargetOid,
                q -> q.targetRefTargetType,
                q -> q.targetRefRelationId));
    }

    @Override
    protected QCase newAliasInstance(String alias) {
        return new QCase(alias);
    }

    @Override
    public MCase newRowObject() {
        return new MCase();
    }

    // TODO transformation code
}
