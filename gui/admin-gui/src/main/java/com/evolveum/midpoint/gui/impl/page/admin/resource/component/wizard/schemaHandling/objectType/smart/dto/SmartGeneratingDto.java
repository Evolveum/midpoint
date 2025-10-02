/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.buildStatusRows;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.formatElapsedTime;

/**
 * DTO backing the SmartGeneratingPanel.
 * Holds start time, current suggestion statuses, and generates display rows.
 */
public class SmartGeneratingDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final LoadableModel<StatusInfo<?>> statusInfo;
    private final IModel<PrismObject<TaskType>> taskModel;

    public SmartGeneratingDto() {
        this(null, null);
    }

    public SmartGeneratingDto(
            LoadableModel<StatusInfo<?>> statusInfo,
            IModel<PrismObject<TaskType>> taskModel) {
        this.statusInfo = statusInfo;
        this.taskModel = taskModel;
    }

    /**
     * Elapsed time as a human-readable string, e.g., "12s elapsed".
     */
    public String getTimeElapsed() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return "-";
        }
        return formatElapsedTime(statusInfo.getObject());
    }

    public LoadableModel<StatusInfo<?>> getStatusInfo() {
        return statusInfo;
    }

    public String getToken() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return null;
        }
        return statusInfo.getObject().getToken();
    }

    /**
     * Builds a list of statusInfo rows for display in the UI.
     * Each row has a label and a done/in-progress flag.
     */
    public List<StatusRowRecord> getStatusRows(PageBase pageBase) {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return List.of();
        }
        return buildStatusRows(pageBase, statusInfo.getObject(), addDefaultRow());
    }

    protected boolean addDefaultRow() {
        return true;
    }

    public boolean isFinished() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        if (isSuspended()) {
            return true;
        }

        return statusInfo.getObject().isComplete();
    }

    public boolean isSuspended() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        TaskExecutionStateType state = getTaskExecutionState();
        if (state == TaskExecutionStateType.SUSPENDED) {
            return true;
        }

        return statusInfo.getObject().isHalted();
    }

    public boolean isFailed() {
        if (statusInfo == null || statusInfo.getObject() == null) {
            return false;
        }

        return statusInfo.getObject().getStatus() == OperationResultStatusType.FATAL_ERROR;
    }

    public TaskType getTaskObject() {
        if (taskModel == null || taskModel.getObject() == null) {
            return null;
        }
        return taskModel.getObject().asObjectable();
    }

    public TaskExecutionStateType getTaskExecutionState() {
        TaskType task = getTaskObject();
        return task != null ? task.getExecutionState() : null;
    }
}
