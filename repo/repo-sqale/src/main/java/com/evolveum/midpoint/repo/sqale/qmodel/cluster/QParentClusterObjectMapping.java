/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType;

public class QParentClusterObjectMapping
        extends QAssignmentHolderMapping<ParentClusterType, QParentClusterData, MParentClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "parentCluster";

    public static QParentClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QParentClusterObjectMapping(repositoryContext);
    }

    private QParentClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QParentClusterData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ParentClusterType.class, QParentClusterData.class, repositoryContext);

        addItemMapping(F_IDENTIFIER, stringMapper(q -> q.identifier));
        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));
        addItemMapping(F_DENSITY, stringMapper(q -> q.density));
        addItemMapping(F_CLUSTERS_REF, multiStringMapper(q -> q.clustersRef));
        addItemMapping(F_CONSIST, integerMapper(q -> q.consist));

    }

    @Override
    protected QParentClusterData newAliasInstance(String alias) {
        return new QParentClusterData(alias);
    }

    @Override
    public MParentClusterObject newRowObject() {
        return new MParentClusterObject();
    }

    @Override
    public @NotNull MParentClusterObject toRowObjectWithoutFullObject(
            ParentClusterType clusterObject, JdbcSession jdbcSession) {
        MParentClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        row.identifier = clusterObject.getIdentifier();
        row.riskLevel = clusterObject.getRiskLevel();
        row.density = clusterObject.getDensity();
        row.clustersRef = stringsToArray(clusterObject.getClustersRef());
        row.consist = clusterObject.getConsist();

        return row;
    }
}
