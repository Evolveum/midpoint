/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getSessionOptionType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.startTimer;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public class RoleBasedClustering implements Mining {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(@NotNull RoleAnalysisSessionType session,
            OperationResult result, PageBase pageBase) {

        RoleAnalysisSessionOptionType sessionOptionType = session.getRoleModeOptions();
        long start = startTimer(" prepare clustering object");

        int minUserOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxUserOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();

        ListMultimap<List<String>, String> chunkMap = loadData(result, pageBase,
                minUserOccupancy, maxUserOccupancy, sessionOptionType.getQuery());

        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        start = startTimer("clustering");

        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);

        if (similarityDifference == 0.00) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, result, dataPoints, session);
        }

        int minUsersOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minRolesCount = sessionOptionType.getMinMembersCount();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minUsersOverlap);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(
                similarityDifference, minRolesCount, distanceMeasure);

        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);

        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, result, dataPoints, clusters, session);

    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, @NotNull PageBase pageBase,
            int minProperties, int maxProperties, SearchFilterType userQuery) {

        Set<String> existingRolesOidsSet = MiningDataUtils.getExistingRolesOidsSet(result, pageBase);

        //role //user
        ListMultimap<String, String> roleToUserMap = MiningDataUtils.getRoleBasedRoleToUserMap(result, pageBase, userQuery,
                existingRolesOidsSet);

        //user //role
        return MiningDataUtils.getRoleBasedUserToRoleMap(minProperties, maxProperties, roleToUserMap);
    }

}
