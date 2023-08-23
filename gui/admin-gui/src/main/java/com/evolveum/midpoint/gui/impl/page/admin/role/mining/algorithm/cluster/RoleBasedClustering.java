/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class RoleBasedClustering implements Clusterable {

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> executeClustering(@NotNull RoleAnalysisSessionType session,
            OperationResult result, PageBase pageBase, Handler handler) {

        RoleAnalysisSessionOptionType sessionOptionType = session.getRoleModeOptions();

        int minUserOccupancy = sessionOptionType.getPropertiesRange().getMin().intValue();
        int maxUserOccupancy = sessionOptionType.getPropertiesRange().getMax().intValue();

        handler.setSubTitle("Load Data");
        handler.setOperationCountToProcess(1);
        ListMultimap<List<String>, String> chunkMap = loadData(result, pageBase,
                minUserOccupancy, maxUserOccupancy, sessionOptionType.getQuery());
        handler.iterateActualStatus();

        handler.setSubTitle("Prepare Data");
        handler.setOperationCountToProcess(1);
        List<DataPoint> dataPoints = ClusterAlgorithmUtils.prepareDataPoints(chunkMap);
        handler.iterateActualStatus();

        double similarityThreshold = sessionOptionType.getSimilarityThreshold();
        double similarityDifference = 1 - (similarityThreshold / 100);

        if (similarityDifference == 0.00) {
            return new ClusterAlgorithmUtils().processExactMatch(pageBase, result, dataPoints, session, handler);
        }

        int minUsersOverlap = sessionOptionType.getMinPropertiesOverlap();
        int minRolesCount = sessionOptionType.getMinMembersCount();

        DistanceMeasure distanceMeasure = new JaccardDistancesMeasure(minUsersOverlap);
        DensityBasedClustering<DataPoint> dbscan = new DensityBasedClustering<>(
                similarityDifference, minRolesCount, distanceMeasure);


        List<Cluster<DataPoint>> clusters = dbscan.cluster(dataPoints, handler);

        return new ClusterAlgorithmUtils().processClusters(pageBase, result, dataPoints, clusters, session, handler);

    }

    private ListMultimap<List<String>, String> loadData(OperationResult result, @NotNull PageBase pageBase,
            int minProperties, int maxProperties, SearchFilterType userQuery) {

        Set<String> existingRolesOidsSet = ClusterDataLoaderUtils.getExistingRolesOidsSet(result, pageBase);

        //role //user
        ListMultimap<String, String> roleToUserMap = ClusterDataLoaderUtils.getRoleBasedRoleToUserMap(result, pageBase, userQuery,
                existingRolesOidsSet);

        //user //role
        return ClusterDataLoaderUtils.getRoleBasedUserToRoleMap(minProperties, maxProperties, roleToUserMap);
    }

}
