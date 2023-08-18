/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.simple.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.simple.Tools.startTimer;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserAnalysisSessionOptionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ListMultimap;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.jetbrains.annotations.NotNull;

public class UserBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(@NotNull RoleAnalysisSessionType session,
            OperationResult result, PageBase pageBase) {

        long start = startTimer(" prepare clustering object");

        UserAnalysisSessionOptionType sessionOptionType = session.getUserModeOptions();

        int minRolesOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxRolesOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();

        //roles //users
        ListMultimap<List<String>, String> chunkMap = loadData(result, pageBase,
                minRolesOccupancy, maxRolesOccupancy, sessionOptionType.getQuery());
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        endTimer(start, "prepare clustering object. Objects count: " + dataPoints.size());

        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);

        if (similarityDifference == 0.00) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, result, dataPoints, session);
        }
        start = startTimer("clustering");

        int minRolesOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minUsersCount = sessionOptionType.getMinMembersCount();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minRolesOverlap);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(similarityDifference,
                minUsersCount, distanceMeasure);
        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints);
        endTimer(start, "clustering");

        return new ClusterAlgorithmUtils().processClusters(pageBase, result, dataPoints, clusters, session);
    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, PageBase pageBase,
            int minProperties, int maxProperties, SearchFilterType userQuery) {

        Set<String> existingRolesOidsSet = ClusterDataLoaderUtils.getExistingRolesOidsSet(result, pageBase);

        //role //user
        return ClusterDataLoaderUtils.getUserBasedRoleToUserMap(result, pageBase, minProperties, maxProperties, userQuery, existingRolesOidsSet);
    }

}
