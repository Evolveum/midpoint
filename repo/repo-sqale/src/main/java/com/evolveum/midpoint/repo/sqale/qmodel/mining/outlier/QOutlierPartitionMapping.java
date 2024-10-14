/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
        var row  = initRowObject(outlierPartition, ownerRow);

        setReference(outlierPartition.getClusterRef(),
                o -> row.clusterRefOid = o,
                t -> row.clusterRefTargetType = t,
                r -> row.clusterRefRelationId = r);

        insert(row, jdbcSession);

        return row;
    }
}
