/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType.*;

public class QOutlierPartitionMapping extends QContainerMapping<RoleAnalysisOutlierPartitionType,
        QOutlierPartition, MOutlierPartition, MOutlier> {

    private static QOutlierPartitionMapping instance;

    @NotNull
    public static QOutlierPartitionMapping initMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QOutlierPartitionMapping(repositoryContext);
        }
        return get();
    }

    public static @NotNull QOutlierPartitionMapping get() {
        return Objects.requireNonNull(instance);
    }

    protected QOutlierPartitionMapping(SqaleRepoContext repositoryContext) {
        super(QOutlierPartition.TABLE_NAME, QOutlierPartition.ALIAS, RoleAnalysisOutlierPartitionType.class, QOutlierPartition.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QAssignmentHolderMapping::getAssignmentHolderMapping,
                        // Adding and(q.ownerType.eq(p.objectType) doesn't help the planner.
                        (q, p) -> q.ownerOid.eq(p.oid)));
        addRefMapping(F_CLUSTER_REF,
                q -> q.clusterRefOid,
                q -> q.clusterRefTargetType,
                q -> q.clusterRefRelationId,
                QClusterObjectMapping::getInstance);

        addNestedMapping(F_PARTITION_ANALYSIS, RoleAnalysisPartitionAnalysisType.class)
                .addItemMapping(RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE, doubleMapper(q -> q.overallConfidence));
    }

    @Override
    protected QOutlierPartition newAliasInstance(String alias) {
        return new QOutlierPartition(alias);
    }

    @Override
    public MOutlierPartition newRowObject() {
        return new MOutlierPartition();
    }

    @Override
    public MOutlierPartition newRowObject(MOutlier ownerRow) {
        var ret = newRowObject();
        ret.ownerOid = ownerRow.oid;
        return ret;
    }

    @Override
    public MOutlierPartition insert(RoleAnalysisOutlierPartitionType outlierPartition, MOutlier ownerRow, JdbcSession jdbcSession) throws SchemaException {
        var row = initRowObject(outlierPartition, ownerRow);

        RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
        if (partitionAnalysis != null) {
            row.overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
        }

        setReference(outlierPartition.getClusterRef(),
                o -> row.clusterRefOid = o,
                t -> row.clusterRefTargetType = t,
                r -> row.clusterRefRelationId = r);

        insert(row, jdbcSession);

        return row;
    }

    @Override
    public RoleAnalysisOutlierPartitionType toSchemaObject(MOutlierPartition row) throws SchemaException {
        return new RoleAnalysisOutlierPartitionType().id(row.cid);
    }

    @Override
    public ResultListRowTransformer<RoleAnalysisOutlierPartitionType, QOutlierPartition, MOutlierPartition> createRowTransformer(
            SqlQueryContext<RoleAnalysisOutlierPartitionType, QOutlierPartition, MOutlierPartition> sqlQueryContext,
            JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {
        Map<UUID, PrismObject<RoleAnalysisOutlierType>> casesCache = new HashMap<>();

        return (tuple, entityPath) -> {
            MOutlierPartition row = Objects.requireNonNull(tuple.get(entityPath));
            UUID caseOid = row.ownerOid;
            PrismObject<RoleAnalysisOutlierType> aCase = casesCache.get(caseOid);
            if (aCase == null) {
                aCase = ((SqaleQueryContext<?, ?, ?>) sqlQueryContext)
                        .loadObject(jdbcSession, RoleAnalysisOutlierType.class, caseOid, options);
                casesCache.put(caseOid, aCase);
            }

            PrismContainer<RoleAnalysisOutlierPartitionType> workItemContainer = aCase.findContainer(RoleAnalysisOutlierType.F_PARTITION);
            if (workItemContainer == null) {
                throw new SystemException("Outlier " + aCase + " has no partition even if it should have " + tuple);
            }
            PrismContainerValue<RoleAnalysisOutlierPartitionType> workItemPcv = workItemContainer.findValue(row.cid);
            if (workItemPcv == null) {
                throw new SystemException("Outlier " + aCase + " has no partition with ID " + row.cid);
            }
            var containerable = workItemPcv.asContainerable();
            attachContainerIdPath(containerable, tuple, entityPath);
            return containerable;
        };
    }
}
