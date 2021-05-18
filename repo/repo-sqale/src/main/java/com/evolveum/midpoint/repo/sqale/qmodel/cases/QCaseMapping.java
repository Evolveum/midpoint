/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;

/**
 * Mapping between {@link QCase} and {@link CaseType}.
 */
public class QCaseMapping
        extends QObjectMapping<CaseType, QCase, MCase> {

    public static final String DEFAULT_ALIAS_NAME = "cs";

    public static QCaseMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QCaseMapping(repositoryContext);
    }

    private QCaseMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QCase.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseType.class, QCase.class, repositoryContext);

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

    @Override
    public @NotNull MCase toRowObjectWithoutFullObject(
            CaseType acase, JdbcSession jdbcSession) {
        MCase row = super.toRowObjectWithoutFullObject(acase, jdbcSession);

        row.state = acase.getState();
        row.closeTimestamp = MiscUtil.asInstant(acase.getCloseTimestamp());
        setReference(acase.getObjectRef(),
                o -> row.objectRefTargetOid = o,
                t -> row.objectRefTargetType = t,
                r -> row.objectRefRelationId = r);
        setReference(acase.getParentRef(),
                o -> row.parentRefTargetOid = o,
                t -> row.parentRefTargetType = t,
                r -> row.parentRefRelationId = r);
        setReference(acase.getRequestorRef(),
                o -> row.requestorRefTargetOid = o,
                t -> row.requestorRefTargetType = t,
                r -> row.requestorRefRelationId = r);
        setReference(acase.getTargetRef(),
                o -> row.targetRefTargetOid = o,
                t -> row.targetRefTargetType = t,
                r -> row.targetRefRelationId = r);

        return row;
    }
}
