/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

public class QOutlierObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisOutlierType, QOutlierData, MOutlierObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisOutlier";

    public static QOutlierObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QOutlierObjectMapping(repositoryContext);
    }

    private QOutlierObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QOutlierData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisOutlierType.class, QOutlierData.class, repositoryContext);

        addRefMapping(RoleAnalysisOutlierType.F_OBJECT_REF,
                q -> q.targetObjectRefTargetOid,
                q -> q.targetObjectRefTargetType,
                q -> q.targetObjectRefRelationId,
                QObjectMapping::getObjectMapping);
//
//        addRefMapping(RoleAnalysisOutlierType.F_TARGET_CLUSTER_REF,
//                q -> q.targetClusterRefTargetOid,
//                q -> q.targetClusterRefTargetType,
//                q -> q.targetClusterRefRelationId,
//                QObjectMapping::getObjectMapping);

    }

    @Override
    protected QOutlierData newAliasInstance(String alias) {
        return new QOutlierData(alias);
    }

    @Override
    public MOutlierObject newRowObject() {
        return new MOutlierObject();
    }

    @Override
    public @NotNull MOutlierObject toRowObjectWithoutFullObject(
            RoleAnalysisOutlierType outlierObject, JdbcSession jdbcSession) {
        MOutlierObject row = super.toRowObjectWithoutFullObject(outlierObject, jdbcSession);

        setReference(outlierObject.getObjectRef(),
                o -> row.targetObjectRefTargetOid = o,
                t -> row.targetObjectRefTargetType = t,
                r -> row.targetObjectRefRelationId = r);
//
//        //TODO: session not cluster
//        setReference(outlierObject.getTargetSessionRef(),
//                o -> row.targetClusterRefTargetOid = o,
//                t -> row.targetClusterRefTargetType = t,
//                r -> row.targetClusterRefRelationId = r);

        return row;
    }
}
