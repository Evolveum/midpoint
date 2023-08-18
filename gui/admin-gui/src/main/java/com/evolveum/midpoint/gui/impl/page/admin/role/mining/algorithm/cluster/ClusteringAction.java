/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

public class ClusteringAction {

    private Clusterable clusterable;

    public ClusteringAction(RoleAnalysisProcessModeType mode) {
        if (mode.equals(RoleAnalysisProcessModeType.USER)) {
            this.clusterable = new UserBasedClustering();
        } else if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            this.clusterable = new RoleBasedClustering();
        }
    }

    public void execute(@NotNull PageBase pageBase, String sessionOid,
            @NotNull OperationResult result, @NotNull Task task) {

        PrismObject<RoleAnalysisSessionType> prismSession = getSessionTypeObject(pageBase, result, sessionOid);
        if (prismSession != null) {
            RoleAnalysisSessionType session = prismSession.asObjectable();
            List<PrismObject<RoleAnalysisClusterType>> clusterObjects = clusterable.executeClustering(session,
                    result, pageBase);

            importObjects(clusterObjects, session, pageBase, result, task);

            pageBase.setResponsePage(PageRoleAnalysis.class);
        }
    }

    private void importObjects(
            List<PrismObject<RoleAnalysisClusterType>> clusters,
            @NotNull RoleAnalysisSessionType session,
            PageBase pageBase,
            OperationResult result,
            Task task
    ) {
        List<ObjectReferenceType> roleAnalysisClusterRef = new ArrayList<>();
        String sessionOid = session.getOid();

        ObjectReferenceType sessionRef = new ObjectReferenceType();
        sessionRef.setOid(sessionOid);
        sessionRef.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        sessionRef.setTargetName(session.getName());

        int processedObjectCount = 0;
        QName complexType = session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)
                ? RoleType.COMPLEX_TYPE
                : UserType.COMPLEX_TYPE;

        double meanDensity = 0;
        for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
            AnalysisClusterStatisticType clusterStatistic = clusterTypePrismObject.asObjectable().getClusterStatistics();
            meanDensity += clusterStatistic.getMembershipDensity();
            if (session.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                processedObjectCount += clusterStatistic.getRolesCount();
            } else {
                processedObjectCount += clusterStatistic.getUsersCount();

            }

            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setOid(clusterTypePrismObject.getOid());
            objectReferenceType.setType(complexType);
            roleAnalysisClusterRef.add(objectReferenceType);

            importRoleAnalysisClusterObject(result,
                    task,
                    pageBase,
                    clusterTypePrismObject,
                    sessionRef,
                    session.getDefaultDetectionOption()
            );
        }

        meanDensity = meanDensity / clusters.size();

        RoleAnalysisSessionStatisticType sessionStatistic = new RoleAnalysisSessionStatisticType();
        sessionStatistic.setProcessedObjectCount(processedObjectCount);
        sessionStatistic.setMeanDensity(meanDensity);
        sessionStatistic.setClusterCount(clusters.size());

        modifySessionAfterClustering(sessionRef,
                sessionStatistic,
                roleAnalysisClusterRef,
                pageBase,
                result
        );
    }

}
