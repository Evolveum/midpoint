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
import java.text.DecimalFormat;

public class RoleAnalysisSessionTile<T extends Serializable> extends Tile<T> {

    String icon;
    String oid;
    private String name;
    private String description;
    private double density;
    private String processedObjectCount;
    private String clusterCount;
    RoleAnalysisProcessModeType processMode;
    RoleAnalysisCategoryType category;
    private RoleAnalysisOperationStatus status;
    ObjectReferenceType taskRef;
    String stateString;

    public RoleAnalysisSessionTile(String icon, String title) {
        super(icon, title);
    }

    public RoleAnalysisSessionTile(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase) {

        this.icon = IconAndStylesUtil.createDefaultColoredIcon(session.asPrismObject().getValue().getTypeName());
        this.name = String.valueOf(session.getName());
        this.oid = session.getOid();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption != null) {
            this.processMode = analysisOption.getProcessMode();
            this.category = analysisOption.getAnalysisCategory();
        }

        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();
        if (sessionStatistic != null) {

            Double meanDensity = sessionStatistic.getMeanDensity();
            if (meanDensity != null) {
                BigDecimal bd = new BigDecimal(Double.toString(meanDensity));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                this.density = bd.doubleValue();
            } else {
                this.density = 0.00;
            }

            Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
            if (processedObjectCount != null) {
                this.processedObjectCount = processedObjectCount.toString();
            }

            Integer clusterCount = sessionStatistic.getClusterCount();
            if (clusterCount != null) {
                this.clusterCount = clusterCount.toString();
            }
        } else {
            this.density = 0.00;
            this.processedObjectCount = "0";
            this.clusterCount = "0";
        }
        this.description = session.getDescription();
        if (this.description == null) {
            this.description = "...";
        }
        this.status = session.getOperationStatus();

        resolveStatus(pageBase);

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

        ObjectReferenceType taskRef = null;
        RoleAnalysisOperationStatus operationStatus = sessionTypeObject.asObjectable().getOperationStatus();
        if (operationStatus != null) {
            taskRef = operationStatus.getTaskRef();
            if (taskRef == null || taskRef.getOid() == null) {
                taskRef = null;
            } else {
                PrismObject<TaskType> object = roleAnalysisService
                        .getObject(TaskType.class, taskRef.getOid(), task, result);
                if (object == null) {
                    taskRef = null;
                }
            }
        }
        this.taskRef = taskRef;

    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getDensity() {
        return density;
    }

    public void setDensity(Double density) {
        this.density = density;
    }

    public String getProcessedObjectCount() {
        return processedObjectCount;
    }

    public String getClusterCount() {
        return clusterCount;
    }

    public RoleAnalysisOperationStatus getStatus() {
        return status;
    }

    public void setStatus(RoleAnalysisOperationStatus status) {
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

}
