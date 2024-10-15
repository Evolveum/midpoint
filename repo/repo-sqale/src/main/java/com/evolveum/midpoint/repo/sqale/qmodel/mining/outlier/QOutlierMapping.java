/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType.*;

public class QOutlierMapping
        extends QAssignmentHolderMapping<RoleAnalysisOutlierType, QOutlier, MOutlier> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisOutlier";

    private static QOutlierMapping instance;

    public static QOutlierMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QOutlierMapping(repositoryContext);
        }
        return get();
    }

    public static @NotNull QOutlierMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QOutlierMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QOutlier.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisOutlierType.class, QOutlier.class, repositoryContext);

        addRefMapping(RoleAnalysisOutlierType.F_OBJECT_REF,
                q -> q.targetObjectRefTargetOid,
                q -> q.targetObjectRefTargetType,
                q -> q.targetObjectRefRelationId,
                QObjectMapping::getObjectMapping);

        addItemMapping(F_OVERALL_CONFIDENCE, doubleMapper(q -> q.overallConfidence));

        // find role outlier where: partition/clusterRef matches (oid = "")
        // find role outlier where: partition/clusterRef/@/sessionRef matches (oid = "")
        addContainerTableMapping(F_PARTITION,
                QOutlierPartitionMapping.initMapping(repositoryContext),
                joinOn((outlier, partition) -> outlier.oid.eq(partition.ownerOid)));


    }

    @Override
    protected QOutlier newAliasInstance(String alias) {
        return new QOutlier(alias);
    }

    @Override
    public MOutlier newRowObject() {
        return new MOutlier();
    }

    @Override
    public @NotNull MOutlier toRowObjectWithoutFullObject(
            RoleAnalysisOutlierType outlierObject, JdbcSession jdbcSession) {
        MOutlier row = super.toRowObjectWithoutFullObject(outlierObject, jdbcSession);

        setReference(outlierObject.getObjectRef(),
                o -> row.targetObjectRefTargetOid = o,
                t -> row.targetObjectRefTargetType = t,
                r -> row.targetObjectRefRelationId = r);
        return row;
    }

    @Override
    public void storeRelatedEntities(@NotNull MOutlier row, @NotNull RoleAnalysisOutlierType schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);
        for (var partition : schemaObject.getPartition()) {
            QOutlierPartitionMapping.get().insert(partition, row, jdbcSession);
        }

    }
}
