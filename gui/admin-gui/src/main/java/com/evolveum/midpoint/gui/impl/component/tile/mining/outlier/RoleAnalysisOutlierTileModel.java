/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel.generateAssignmentOutlierResultModel;

public class RoleAnalysisOutlierTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String name;
    DetectedAnomalyResult descriptionType;
    String processMode;
    RoleAnalysisOutlierType outlierParent;
    OutlierObjectModel outlierObjectModel;
    RoleAnalysisOutlierPartitionType partition;

    public RoleAnalysisOutlierTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisOutlierTileModel(
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull DetectedAnomalyResult descriptionType,
            @NotNull String name,
            @NotNull String processMode,
            @NotNull RoleAnalysisOutlierType outlierParent,
            @NotNull PageBase pageBase) {
        this.partition = partition;
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.name = name;
        this.descriptionType = descriptionType;
        this.processMode = processMode;
        this.outlierParent = outlierParent;

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Load object");
        OperationResult result = task.getResult();

        ObjectReferenceType ref = descriptionType.getTargetObjectRef();
        QName type = ref.getType();

        if (type.equals(UserType.COMPLEX_TYPE)) {
            //TODO
            return;
        }

        ObjectReferenceType targetObjectRef = outlierParent.getTargetObjectRef();
        PrismObject<UserType> userTypeObject = WebModelServiceUtils.loadObject(UserType.class, targetObjectRef.getOid(), pageBase, task, result);
//        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(
//                targetObjectRef.getOid(), task, task.getResult());

        if (userTypeObject == null) {
            //TODO show result ? it userTypeObject is null then probably there was a error during loading
            return;
        }

        this.outlierObjectModel = generateAssignmentOutlierResultModel(
                roleAnalysisService, descriptionType,partition, task, task.getResult(), userTypeObject, outlierParent);
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

    public String getProcessMode() {
        return processMode;
    }

    public void setProcessMode(String processMode) {
        this.processMode = processMode;
    }

    public DetectedAnomalyResult getDescriptionType() {
        return descriptionType;
    }

    public void setDescriptionType(DetectedAnomalyResult descriptionType) {
        this.descriptionType = descriptionType;
    }

    public RoleAnalysisOutlierType getOutlierParent() {
        return outlierParent;
    }

    public void setOutlierParent(RoleAnalysisOutlierType outlierParent) {
        this.outlierParent = outlierParent;
    }

    public OutlierObjectModel getOutlierObjectModel() {
        return outlierObjectModel;
    }

    public RoleAnalysisOutlierPartitionType getPartition() {
        return partition;
    }
}
