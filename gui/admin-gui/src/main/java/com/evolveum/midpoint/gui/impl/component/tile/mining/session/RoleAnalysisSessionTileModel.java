/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.session;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.reductionBasedColor;

public class RoleAnalysisSessionTileModel<T extends Serializable> extends Tile<T> {

    String icon;
    String oid;
    private String name;
    private String description;
    private double progressBarValue;
    private String processedObjectCount;
    private String clusterCount;
    RoleAnalysisProcessModeType processMode;
    RoleAnalysisCategoryType category;
    RoleAnalysisProcedureType procedureType;
    private RoleAnalysisOperationStatusType status;
    ObjectReferenceType taskRef;
    String stateString;
    String progressBarTitle;
    String progressBarColor;

    public RoleAnalysisSessionTileModel(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisSessionTileModel(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase) {

        this.icon = IconAndStylesUtil.createDefaultColoredIcon(session.asPrismObject().getValue().getTypeName());
        this.name = String.valueOf(session.getName());
        this.oid = session.getOid();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption != null) {
            this.processMode = analysisOption.getProcessMode();
            this.category = analysisOption.getAnalysisCategory();
            this.procedureType = analysisOption.getAnalysisProcedureType();
        }

        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();
        if (sessionStatistic != null) {
            Double meanDensity = sessionStatistic.getMeanDensity();
            if (meanDensity != null) {
                BigDecimal bd = new BigDecimal(Double.toString(meanDensity));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                this.progressBarValue = bd.doubleValue();
            } else {
                this.progressBarValue = 0.00;
            }

            Integer newProcessedObjectCount = sessionStatistic.getProcessedObjectCount();
            if (newProcessedObjectCount != null) {
                this.processedObjectCount = newProcessedObjectCount.toString();
            }

            Integer newClusterCount = sessionStatistic.getClusterCount();
            if (newClusterCount != null) {
                this.clusterCount = newClusterCount.toString();
            }
        } else {
            this.progressBarValue = 0.00;
            this.processedObjectCount = "0";
            this.clusterCount = "0";
        }
        this.description = session.getDescription();
        if (this.description == null) {
            this.description = "...";
        }
        this.status = session.getOperationStatus();

        resolveStatus(pageBase);

        calculatePossibleReduction(session, pageBase);

        if (procedureType != null && !procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            this.progressBarTitle = pageBase.createStringResource("RoleAnalysisSessionTile.possible.reduction")
                    .getString();
            this.progressBarColor = reductionBasedColor(progressBarValue);
        } else {
            this.progressBarTitle = pageBase.createStringResource("RoleAnalysisSessionTile.density")
                    .getString();
            this.progressBarColor = densityBasedColor(progressBarValue);
        }

    }

    private void calculatePossibleReduction(@NotNull RoleAnalysisSessionType session, @NotNull PageBase pageBase) {
        if (procedureType != RoleAnalysisProcedureType.OUTLIER_DETECTION) {
            RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
            Task task = pageBase.createSimpleTask("calculatePossibleAssignmentReduction");
            OperationResult result = task.getResult();
            this.progressBarValue = roleAnalysisService.calculatePossibleAssignmentReduction(session, task, result);
        }
    }

    private void resolveStatus(@NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("OP_UPDATE_STATUS");

        OperationResult result = task.getResult();

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService.getSessionTypeObject(oid, task, result);

        if (sessionTypeObject == null) {
            return;
        }

        this.stateString = roleAnalysisService.recomputeAndResolveSessionOpStatus(
                sessionTypeObject,
                result, task);

        ObjectReferenceType resultTaskRef = null;
        RoleAnalysisOperationStatusType operationStatus = sessionTypeObject.asObjectable().getOperationStatus();
        if (operationStatus != null) {
            resultTaskRef = operationStatus.getTaskRef();
            if (resultTaskRef == null || resultTaskRef.getOid() == null) {
                resultTaskRef = null;
            } else {
                PrismObject<TaskType> object = roleAnalysisService
                        .getObject(TaskType.class, resultTaskRef.getOid(), task, result);
                if (object == null) {
                    resultTaskRef = null;
                }
            }
        }
        this.taskRef = resultTaskRef;

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

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public Double getProgressBarValue() {
        return progressBarValue;
    }

    public String getProcessedObjectCount() {
        return processedObjectCount;
    }

    public String getClusterCount() {
        return clusterCount;
    }

    public RoleAnalysisOperationStatusType getStatus() {
        return status;
    }

    public void setStatus(RoleAnalysisOperationStatusType status) {
        this.status = status;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

    public RoleAnalysisCategoryType getCategory() {
        return category;
    }

    public void setCategory(RoleAnalysisCategoryType category) {
        this.category = category;
    }

    public ObjectReferenceType getTaskRef() {
        return taskRef;
    }

    public void setTaskRef(ObjectReferenceType taskRef) {
        this.taskRef = taskRef;
    }

    public String getStateString() {
        return stateString;
    }

    public void setStateString(String stateString) {
        this.stateString = stateString;
    }

    public String getProgressBarTitle() {
        return progressBarTitle;
    }

    public String getProgressBarColor() {
        return progressBarColor;
    }

    public RoleAnalysisProcedureType getProcedureType() {
        return procedureType;
    }

}
