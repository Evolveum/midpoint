/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.session;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType.*;

public class QSessionObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisSessionType, QSessionData, MSessionObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisSession";
    private static final int DEFAULT_DECIMALS = 1000;

    public static QSessionObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QSessionObjectMapping(repositoryContext);
    }

    private QSessionObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSessionData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisSessionType.class, QSessionData.class, repositoryContext);

//        addNestedMapping(F_SESSION_STATISTIC, RoleAnalysisSessionStatisticType.class)
//                .addItemMapping(RoleAnalysisSessionStatisticType.F_PROCESSED_OBJECT_COUNT,
//                        integerMapper(q -> q.processedObjectCount))
//                .addItemMapping(RoleAnalysisSessionStatisticType.F_CLUSTER_COUNT,
//                        integerMapper(q -> q.clusterCount));
//                .addItemMapping(RoleAnalysisSessionStatisticType.F_MEAN_DENSITY,
//                        longMapper(q -> q.density));

        addNestedMapping(F_USER_MODE_OPTIONS, AbstractAnalysisSessionOptionType.class)
                .addItemMapping(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD,
                        longMapper(q -> q.similarityOption))
                .addItemMapping(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT,
                        integerMapper(q -> q.minMembersOption))
                .addItemMapping(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP,
                        integerMapper(q -> q.overlapOption));

        addNestedMapping(F_ROLE_MODE_OPTIONS, AbstractAnalysisSessionOptionType.class)
                .addItemMapping(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD,
                        longMapper(q -> q.similarityOption))
                .addItemMapping(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT,
                        integerMapper(q -> q.minMembersOption))
                .addItemMapping(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP,
                        integerMapper(q -> q.overlapOption));

    }

    @Override
    protected QSessionData newAliasInstance(String alias) {
        return new QSessionData(alias);
    }

    @Override
    public MSessionObject newRowObject() {
        return new MSessionObject();
    }

    @Override
    public @NotNull MSessionObject toRowObjectWithoutFullObject(
            RoleAnalysisSessionType session, JdbcSession jdbcSession) {
        MSessionObject row = super.toRowObjectWithoutFullObject(session, jdbcSession);

        RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        if (roleModeOptions != null) {
            row.similarityOption = (long) (roleModeOptions.getSimilarityThreshold() * DEFAULT_DECIMALS);
            row.minMembersOption = roleModeOptions.getMinMembersCount();
            row.overlapOption = roleModeOptions.getMinPropertiesOverlap();
        } else if (userModeOptions != null) {
            row.similarityOption = (long) (userModeOptions.getSimilarityThreshold() * DEFAULT_DECIMALS);
            row.minMembersOption = userModeOptions.getMinMembersCount();
            row.overlapOption = userModeOptions.getMinPropertiesOverlap();
        }

        if (session.getSessionStatistic() != null) {
            RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();
//            row.density = (long) (sessionStatistic.getMeanDensity() * DEFAULT_DECIMALS);
//            row.clusterCount = sessionStatistic.getClusterCount();
//            row.processedObjectCount = sessionStatistic.getProcessedObjectCount();
        }

        return row;
    }
}
