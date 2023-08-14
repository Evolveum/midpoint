/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.MainPageMining;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.importRoleAnalysisClusterObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.importRoleAnalysisSessionObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.endTimer;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.startTimer;

public class ClusteringAction {

    private Clusterable clusterable;

    public ClusteringAction(RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            this.clusterable = new UserBasedClustering();
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            this.clusterable = new RoleBasedClustering();
        }
    }

    public void execute(@NotNull PageBase pageBase, @NotNull ClusterOptions clusterOptions,
            @NotNull OperationResult operationResult, @NotNull Task task) {

        System.out.println(clusterOptions.getSimilarity());
        clusterOptions.setSimilarity(clusterOptions.getSimilarity() / 100);
        RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption = getRoleAnalysisSessionFilterOption(clusterOptions);

        RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption = getRoleAnalysisSessionDetectionOption(clusterOptions);

        List<PrismObject<RoleAnalysisClusterType>> clusterObjects = clusterable.executeClustering(clusterOptions,
                operationResult, pageBase);

        importObjects(clusterObjects, clusterOptions,
                roleAnalysisSessionClusterOption, roleAnalysisSessionDetectionOption, pageBase, operationResult, task);

        pageBase.setResponsePage(MainPageMining.class);

    }

    @NotNull
    private RoleAnalysisDetectionOptionType getRoleAnalysisSessionDetectionOption(ClusterOptions clusterOptions) {
        RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption = new RoleAnalysisDetectionOptionType();
//        roleAnalysisSessionDetectionOption.setDetectionMode(clusterOptions.getSearchMode());
        roleAnalysisSessionDetectionOption.setMinFrequencyThreshold(clusterOptions.getDefaultMinFrequency());
        roleAnalysisSessionDetectionOption.setMaxFrequencyThreshold(clusterOptions.getDefaultMaxFrequency());
        roleAnalysisSessionDetectionOption.setMinMembersOccupancy(clusterOptions.getDefaultOccupancySearch());
        roleAnalysisSessionDetectionOption.setMinPropertiesOccupancy(clusterOptions.getDefaultIntersectionSearch());
        roleAnalysisSessionDetectionOption.setDetectionProcessMode(clusterOptions.getDetect());

        return roleAnalysisSessionDetectionOption;
    }

    @NotNull
    private RoleAnalysisSessionOptionType getRoleAnalysisSessionFilterOption(ClusterOptions clusterOptions) {
        RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption = new RoleAnalysisSessionOptionType();
        if (clusterOptions.getQuery() != null) {
            roleAnalysisSessionClusterOption.setFilter(clusterOptions.getQuery().toString());
        }

        roleAnalysisSessionClusterOption.setProcessMode(clusterOptions.getMode());
        roleAnalysisSessionClusterOption.setSimilarityThreshold(clusterOptions.getSimilarity());
        roleAnalysisSessionClusterOption.setMinUniqueMembersCount(clusterOptions.getMinGroupSize());
        roleAnalysisSessionClusterOption.setMinMembersCount(clusterOptions.getMinMembers());
        roleAnalysisSessionClusterOption.setMinPropertiesCount(clusterOptions.getMinProperties());
        roleAnalysisSessionClusterOption.setMaxPropertiesCount(clusterOptions.getMaxProperties());
        roleAnalysisSessionClusterOption.setMinPropertiesOverlap(clusterOptions.getMinIntersections());
        return roleAnalysisSessionClusterOption;
    }

    private void importObjects(List<PrismObject<RoleAnalysisClusterType>> clusters, ClusterOptions clusterOptions,
            RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption,
            RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption,
            PageBase pageBase, OperationResult result, Task task) {

        List<ObjectReferenceType> roleAnalysisClusterRef = new ArrayList<>();

        int processedObjectCount = 0;

        QName complexType;
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            complexType = RoleType.COMPLEX_TYPE;
        } else {complexType = UserType.COMPLEX_TYPE;}

        double meanDensity = 0;
        for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
            RoleAnalysisClusterStatisticType clusterStatistic = clusterTypePrismObject.asObjectable().getClusterStatistic();
            meanDensity += clusterStatistic.getPropertiesDensity();
            processedObjectCount += clusterStatistic.getMemberCount();

            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setOid(clusterTypePrismObject.getOid());
            objectReferenceType.setType(complexType);
            roleAnalysisClusterRef.add(objectReferenceType);
        }

        meanDensity = meanDensity / clusters.size();

        RoleAnalysisSessionStatisticType roleAnalysisSessionStatisticType = new RoleAnalysisSessionStatisticType();
        roleAnalysisSessionStatisticType.setProcessedObjectCount(processedObjectCount);
        roleAnalysisSessionStatisticType.setMeanDensity(meanDensity);

        System.out.println("IMPORT SESSION");

        ObjectReferenceType parentRef = importRoleAnalysisSessionObject(result, pageBase, roleAnalysisSessionClusterOption,
                roleAnalysisSessionStatisticType, roleAnalysisClusterRef, clusterOptions.getName());
        System.out.println("SESSION IMPORTED");
        try {
            int counter = 1;
            for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
                System.out.println("IMPORT CLUSTER: " + counter + "/" + clusters.size());
                importRoleAnalysisClusterObject(result, task, pageBase, clusterTypePrismObject, parentRef,
                        roleAnalysisSessionDetectionOption);
                System.out.println("END IMPORTING CLUSTER: " + counter + "/" + clusters.size());
                counter++;

            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Import RoleAnalysisCluster object failed" + e);
        }
    }
}
