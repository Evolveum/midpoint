/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType.F_CLUSTER_STATISTICS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisClusterStatisticType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public class QClusterObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisClusterType, QClusterData, MClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisCluster";
    private static final int DEFAULT_DECIMALS = 1000;

    public static QClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QClusterObjectMapping(repositoryContext);
    }

    private QClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QClusterData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisClusterType.class, QClusterData.class, repositoryContext);

        addRefMapping(F_ROLE_ANALYSIS_SESSION_REF,
                q -> q.parentRefTargetOid,
                q -> q.parentRefTargetType,
                q -> q.parentRefRelationId,
                QObjectMapping::getObjectMapping);

        addNestedMapping(F_CLUSTER_STATISTICS, AnalysisClusterStatisticType.class)
                .addItemMapping(F_DETECTED_REDUCTION_METRIC,
                        longMapper(q -> q.detectedReductionMetric))
                .addItemMapping(AnalysisClusterStatisticType.F_MEMBERSHIP_DENSITY,
                        longMapper(q -> q.membershipDensity))
                .addItemMapping(AnalysisClusterStatisticType.F_MEMBERSHIP_MEAN,
                        longMapper(q -> q.membershipMean))
                .addItemMapping(AnalysisClusterStatisticType.F_USERS_COUNT,
                        integerMapper(q -> q.usersCount))
                .addItemMapping(AnalysisClusterStatisticType.F_ROLES_COUNT,
                        integerMapper(q -> q.rolesCount));

    }

    @Override
    protected QClusterData newAliasInstance(String alias) {
        return new QClusterData(alias);
    }

    @Override
    public MClusterObject newRowObject() {
        return new MClusterObject();
    }

    @Override
    public @NotNull MClusterObject toRowObjectWithoutFullObject(
            RoleAnalysisClusterType clusterObject, JdbcSession jdbcSession) {
        MClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        setReference(clusterObject.getRoleAnalysisSessionRef(),
                o -> row.parentRefTargetOid = o,
                t -> row.parentRefTargetType = t,
                r -> row.parentRefRelationId = r);

        AnalysisClusterStatisticType clusterStatistics = clusterObject.getClusterStatistics();
        if (clusterStatistics != null) {
            row.membershipMean = (long) (clusterStatistics.getMembershipMean() * DEFAULT_DECIMALS);
            row.membershipDensity = (long) (clusterStatistics.getMembershipDensity() * DEFAULT_DECIMALS);
            row.detectedReductionMetric = (long) (clusterStatistics.getDetectedReductionMetric() * DEFAULT_DECIMALS);

            row.usersCount = clusterObject.getClusterStatistics().getUsersCount();
            row.rolesCount = clusterObject.getClusterStatistics().getRolesCount();

            row.riskLevel = clusterStatistics.getRiskLevel();

        }

        return row;
    }
}
