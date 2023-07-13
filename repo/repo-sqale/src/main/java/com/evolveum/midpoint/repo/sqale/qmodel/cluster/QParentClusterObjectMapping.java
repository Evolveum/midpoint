/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cluster;


import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSession;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSession.*;

public class QParentClusterObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisSession, QParentClusterData, MParentClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisSession";

    public static QParentClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QParentClusterObjectMapping(repositoryContext);
    }

    private QParentClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QParentClusterData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisSession.class, QParentClusterData.class, repositoryContext);

        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));
        addItemMapping(F_ELEMENT_CONSIST, stringMapper(q -> q.meanDensity));
        addItemMapping(F_ROLE_ANALYSIS_CLUSTER_REF, multiStringMapper(q -> q.roleAnalysisClusterRef));
        addItemMapping(F_OPTIONS, stringMapper(q -> q.options));
        addItemMapping(F_ELEMENT_CONSIST, integerMapper(q -> q.elementConsist));
        addItemMapping(F_PROCESS_MODE, stringMapper(q -> q.processMode));

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
            RoleAnalysisSession clusterObject, JdbcSession jdbcSession) {
        MParentClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        row.riskLevel = clusterObject.getRiskLevel();
        row.meanDensity = clusterObject.getMeanDensity();
        row.roleAnalysisClusterRef = stringsToArray(clusterObject.getRoleAnalysisClusterRef());
        row.options = clusterObject.getOptions();
        row.elementConsist = clusterObject.getElementConsist();
        row.processMode = clusterObject.getProcessMode();

        return row;
    }
}
