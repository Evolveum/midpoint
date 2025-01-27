/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainPartition;

public class RoleAnalysisOutlierPartitionTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    RoleAnalysisOutlierType outlierParent;
    RoleAnalysisOutlierPartitionType partition;
    Model<String> explanationTranslatedModel;
    boolean isMostImpactful = false;

    public RoleAnalysisOutlierPartitionTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisOutlierPartitionTileModel(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull String name,
            @NotNull RoleAnalysisOutlierType outlierParent,
            @NotNull PageBase pageBase) {
        this.partition = partition;
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.name = name;
        this.outlierParent = outlierParent;

        Task task = pageBase.createSimpleTask("Build partition model");
        OperationResult result = task.getResult();

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        this.explanationTranslatedModel = explainPartition(roleAnalysisService, partition, false,task, result);

        ObjectReferenceType targetObjectRef = outlierParent.getObjectRef();
        PrismObject<UserType> userPrismObject = WebModelServiceUtils.loadObject(
                UserType.class, targetObjectRef.getOid(), pageBase, task, result);

        if (userPrismObject == null) {
            //TODO show result ? it userTypeObject is null then probably there was a error during loading
            return;
        }

        resolveIfTopImpacted(partition, outlierParent);

    }

    private void resolveIfTopImpacted(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull RoleAnalysisOutlierType outlierParent) {
        RoleAnalysisOutlierPartitionType topPartition = partition;
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierParent.getPartition();
        for (RoleAnalysisOutlierPartitionType nextPartition : outlierPartitions) {
            topPartition = partition;
            if (topPartition.getPartitionAnalysis().getOverallConfidence()
                    < nextPartition.getPartitionAnalysis().getOverallConfidence()) {
                topPartition = nextPartition;
            }
        }

        String oid = partition.getTargetSessionRef().getOid();
        String topPartitionSessionOid = topPartition.getTargetSessionRef().getOid();
        if (oid.equals(topPartitionSessionOid)) {
            isMostImpactful = true;
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

    public RoleAnalysisOutlierType getOutlierParent() {
        return outlierParent;
    }

    public void setOutlierParent(RoleAnalysisOutlierType outlierParent) {
        this.outlierParent = outlierParent;
    }

    public RoleAnalysisOutlierPartitionType getPartition() {
        return partition;
    }

    public boolean isMostImpactful() {
        return isMostImpactful;
    }

    public Model<String> getExplanationTranslatedModel() {
        return explanationTranslatedModel;
    }
}
