/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCluster.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCluster;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

public class QClusterObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisCluster, QClusterData, MClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisCluster";

    public static QClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QClusterObjectMapping(repositoryContext);
    }

    private QClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QClusterData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisCluster.class, QClusterData.class, repositoryContext);

        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));
        addItemMapping(F_PARENT_REF, stringMapper(q -> q.parentRef));
        addItemMapping(F_ELEMENTS, multiStringMapper(q -> q.elements));
        addItemMapping(F_POINTS_COUNT, integerMapper(q -> q.pointsCount));
        addItemMapping(F_ELEMENTS_COUNT, integerMapper(q -> q.elementsCount));
        addItemMapping(F_POINTS_MEAN, stringMapper(q -> q.pointsMean));
        addItemMapping(F_POINTS_MIN_OCCUPATION, integerMapper(q -> q.pointsMinOccupation));
        addItemMapping(F_POINTS_MAX_OCCUPATION, integerMapper(q -> q.pointsMaxOccupation));
        addItemMapping(F_POINTS_DENSITY, stringMapper(q -> q.pointsDensity));
        addItemMapping(F_PARENT_REF, stringMapper(q -> q.parentRef));
        addItemMapping(F_DEFAULT_DETECTION, multiStringMapper(q -> q.defaultDetection));

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
            RoleAnalysisCluster clusterObject, JdbcSession jdbcSession) {
        MClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        row.riskLevel = clusterObject.getRiskLevel();
        row.pointsCount = clusterObject.getPointsCount();
        row.elementsCount = clusterObject.getElementsCount();
        row.pointsMean = clusterObject.getPointsMean();
        row.pointsDensity = clusterObject.getPointsDensity();
        row.pointsMinOccupation = clusterObject.getPointsMinOccupation();
        row.pointsMaxOccupation = clusterObject.getPointsMaxOccupation();
        row.elements = stringsToArray(clusterObject.getElements());
        row.parentRef = clusterObject.getParentRef();
        row.defaultDetection = stringsToArray(clusterObject.getDefaultDetection());

        return row;
    }
}
