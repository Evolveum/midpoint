/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

public class QClusterObjectMapping
        extends QAssignmentHolderMapping<ClusterType, QClusterData, MClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "cluster";

    public static QClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QClusterObjectMapping(repositoryContext);
    }

    private QClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QClusterData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ClusterType.class, QClusterData.class, repositoryContext);

        addItemMapping(F_IDENTIFIER, stringMapper(q -> q.identifier));
        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));
//        addItemMapping(F_POINTS, multiStringMapper(q -> q.points));
        addItemMapping(F_ELEMENTS, multiStringMapper(q -> q.elements));
        addItemMapping(F_POINT_COUNT, integerMapper(q -> q.pointCount));
        addItemMapping(F_ELEMENT_COUNT, integerMapper(q -> q.elementCount));
        addItemMapping(F_MEAN, stringMapper(q -> q.mean));
        addItemMapping(F_MIN_OCCUPATION, integerMapper(q -> q.minOccupation));
        addItemMapping(F_MAX_OCCUPATION, integerMapper(q -> q.maxOccupation));
        addItemMapping(F_DENSITY, stringMapper(q -> q.density));
        addItemMapping(F_DENSITY, stringMapper(q -> q.parentRef));
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
            ClusterType clusterObject, JdbcSession jdbcSession) {
        MClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        row.identifier = clusterObject.getIdentifier();
        row.riskLevel = clusterObject.getRiskLevel();
        row.pointCount = clusterObject.getPointCount();
        row.elementCount = clusterObject.getElementCount();
        row.mean = clusterObject.getMean();
        row.density = clusterObject.getDensity();
        row.minOccupation = clusterObject.getMinOccupation();
        row.maxOccupation = clusterObject.getMaxOccupation();
//        row.points = stringsToArray(clusterObject.getPoints());
        row.elements = stringsToArray(clusterObject.getElements());
        row.parentRef = clusterObject.getParentRef();
        row.defaultDetection = stringsToArray(clusterObject.getDefaultDetection());

        return row;
    }
}
