/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.clustering;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.ClusteringUtils.*;

/**
 * Implements the outlier category clustering operation for role analysis.
 * This class is responsible for executing the clustering operation.
 */
public class OutlierClustering implements Clusterable {
    @Override
    public @NotNull List<PrismObject<RoleAnalysisClusterType>> executeClustering(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull ModelService modelService,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisProgressIncrement handler,
            @NotNull Task task,
            @NotNull OperationResult result) {

        AdvancedClustering advancedClustering = new AdvancedClustering() {
            @Override
            public @NotNull ListMultimap<List<String>, String> loadUserModeData(
                    @NotNull ModelService modelService,
                    @NotNull Boolean isIndirect,
                    int minRolesOccupancy,
                    int maxRolesOccupancy,
                    @Nullable SearchFilterType sessionOptionType,
                    @NotNull Task task,
                    @NotNull OperationResult result) {
                return super.loadUserModeData(modelService, isIndirect, minRolesOccupancy, maxRolesOccupancy,
                        sessionOptionType, task, result);
            }

            @Override
            public @NotNull ListMultimap<List<String>, String> loadRoleModeData(
                    @NotNull ModelService modelService,
                    Boolean isIndirect,
                    int minUserOccupancy,
                    int maxUserOccupancy,
                    @Nullable SearchFilterType sessionOptionType,
                    @NotNull Task task,
                    @NotNull OperationResult result) {
                return super.loadRoleModeData(modelService, isIndirect, minUserOccupancy, maxUserOccupancy,
                        sessionOptionType, task, result);
            }
        };

        return advancedClustering.executeClustering(roleAnalysisService, modelService, session, handler, task, result);
//
//        RoleAnalysisProcessModeType processMode = session.getAnalysisOption().getProcessMode();
//
//        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
//            RoleBasedClustering roleBasedClustering = new RoleBasedClustering() {
//                @Override
//                public @NotNull ListMultimap<List<String>, String> loadData(
//                        @NotNull ModelService modelService,
//                        @NotNull Task task,
//                        @NotNull OperationResult result,
//                        int minUserOccupancy,
//                        int maxUserOccupancy,
//                        @NotNull RoleAnalysisSessionOptionType sessionOptionType) {
//                    return loadRoleBasedMembershipMultimapData(
//                            modelService, minUserOccupancy, maxUserOccupancy, sessionOptionType.getQuery(), task, result);
//                }
//            };
//
//            return roleBasedClustering.executeClustering(roleAnalysisService, modelService, session, handler, task, result);
//        } else {
//            UserBasedClustering userBasedClustering = new UserBasedClustering() {
//                @Override
//                public @NotNull ListMultimap<List<String>, String> loadData(
//                        @NotNull ModelService modelService,
//                        @NotNull Task task,
//                        @NotNull OperationResult result,
//                        int minRolesOccupancy,
//                        int maxRolesOccupancy,
//                        @NotNull UserAnalysisSessionOptionType sessionOptionType) {
//                    return loadUserBasedMembershipMultimapData(modelService, minRolesOccupancy,
//                            maxRolesOccupancy, sessionOptionType.getQuery(), task, result);
//                }
//            };
//            return userBasedClustering
//                    .executeClustering(roleAnalysisService, modelService, session, handler, task, result);
//        }

    }
}
