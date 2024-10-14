/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

public class RoleAnalysisOutlierTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    RoleAnalysisOutlierType outlier;
    RoleAnalysisOutlierPartitionType partition;
    ObjectReferenceType clusterRef;
    ObjectReferenceType sessionRef;
    String status = "TBD";
    PageBase pageBase;

    public RoleAnalysisOutlierTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisOutlierTileModel(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull PageBase pageBase) {
        this.partition = partition;
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.outlier = outlier;
        this.name = outlier.getName().getOrig();
        this.pageBase = pageBase;
        this.clusterRef = partition.getClusterRef();
        this.sessionRef = partition.getTargetSessionRef();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
        OutlierSpecificCategoryType outlierSpecificCategory = outlierCategory.getOutlierSpecificCategory();
        this.status = outlierSpecificCategory.value();

        tmpNameRefResolver(clusterRef, sessionRef, pageBase);

    }

    //TODO Temporary solution (remove later) but why is targetName null? it is stored in fullObject.
    private static void tmpNameRefResolver(
            @NotNull ObjectReferenceType clusterRef,
            @NotNull ObjectReferenceType sessionRef,
            @NotNull PageBase pageBase) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Load target name");
        OperationResult result = task.getResult();
        if (clusterRef.getTargetName() == null) {
            PrismObject<RoleAnalysisClusterType> prismCluster = roleAnalysisService.getClusterTypeObject(
                    clusterRef.getOid(), task, result);
            if (prismCluster != null) {
                PolyStringType name = prismCluster.asObjectable().getName();
                clusterRef.setTargetName(name);
            }
        }

        if (sessionRef.getTargetName() == null) {
            PrismObject<RoleAnalysisSessionType> prismSession = roleAnalysisService.getSessionTypeObject(
                    sessionRef.getOid(), task, result);
            if (prismSession != null) {
                PolyStringType name = prismSession.asObjectable().getName();
                sessionRef.setTargetName(name);
            }
        }
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectReferenceType getClusterRef() {
        return clusterRef;
    }

    public ObjectReferenceType getSessionRef() {
        return sessionRef;
    }

    public RoleAnalysisOutlierType getOutlier() {
        return outlier;
    }

    public void setOutlier(RoleAnalysisOutlierType outlier) {
        this.outlier = outlier;
    }

    public RoleAnalysisOutlierPartitionType getPartition() {
        return partition;
    }

    public String getStatus() {
        return status;
    }
}
