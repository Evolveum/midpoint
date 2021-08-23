/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_OBJECT_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_TARGET_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem.QCaseWorkItemMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Mapping between {@link QCase} and {@link CaseType}.
 */
public class QCaseMapping
        extends QAssignmentHolderMapping<CaseType, QCase, MCase> {

    public static final String DEFAULT_ALIAS_NAME = "cs";
    private static QCaseMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QCaseMapping initCaseMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QCaseMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QCaseMapping getCaseMapping() {
        return Objects.requireNonNull(instance);
    }

    private QCaseMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QCase.TABLE_NAME, DEFAULT_ALIAS_NAME,
                CaseType.class, QCase.class, repositoryContext);

        addItemMapping(F_STATE, stringMapper(q -> q.state));
        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        addRefMapping(F_OBJECT_REF,
                q -> q.objectRefTargetOid,
                q -> q.objectRefTargetType,
                q -> q.objectRefRelationId,
                QObjectMapping::getObjectMapping);
        addRefMapping(F_PARENT_REF,
                q -> q.parentRefTargetOid,
                q -> q.parentRefTargetType,
                q -> q.parentRefRelationId,
                QCaseMapping::getCaseMapping);
        addRefMapping(F_REQUESTOR_REF,
                q -> q.requestorRefTargetOid,
                q -> q.requestorRefTargetType,
                q -> q.requestorRefRelationId,
                QUserMapping::getUserMapping);
        addRefMapping(F_TARGET_REF,
                q -> q.targetRefTargetOid,
                q -> q.targetRefTargetType,
                q -> q.targetRefRelationId,
                QObjectMapping::getObjectMapping);

        addContainerTableMapping(F_WORK_ITEM,
                QCaseWorkItemMapping.initCaseWorkItemMapping(repositoryContext),
                joinOn((o, wi) -> o.oid.eq(wi.ownerOid)));
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
            CaseType schemaObject, JdbcSession jdbcSession) {
        MCase row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.state = schemaObject.getState();
        row.closeTimestamp = MiscUtil.asInstant(schemaObject.getCloseTimestamp());
        setReference(schemaObject.getObjectRef(),
                o -> row.objectRefTargetOid = o,
                t -> row.objectRefTargetType = t,
                r -> row.objectRefRelationId = r);
        setReference(schemaObject.getParentRef(),
                o -> row.parentRefTargetOid = o,
                t -> row.parentRefTargetType = t,
                r -> row.parentRefRelationId = r);
        setReference(schemaObject.getRequestorRef(),
                o -> row.requestorRefTargetOid = o,
                t -> row.requestorRefTargetType = t,
                r -> row.requestorRefRelationId = r);
        setReference(schemaObject.getTargetRef(),
                o -> row.targetRefTargetOid = o,
                t -> row.targetRefTargetType = t,
                r -> row.targetRefRelationId = r);

        return row;
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MCase row, @NotNull CaseType schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        List<CaseWorkItemType> workItems = schemaObject.getWorkItem();
        if (!workItems.isEmpty()) {
            workItems.forEach(t -> QCaseWorkItemMapping.getCaseWorkItemMapping().insert(t, row, jdbcSession));
        }
    }
}
