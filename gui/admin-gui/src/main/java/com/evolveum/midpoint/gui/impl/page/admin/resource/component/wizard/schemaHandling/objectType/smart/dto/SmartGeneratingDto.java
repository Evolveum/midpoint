/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * DTO backing the SmartGeneratingPanel.
 * Holds start time, current suggestion statuses, and generates display rows.
 */
public class SmartGeneratingDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> elapsedTimeModel;
    private final IModel<List<StatusRow>> statusRowsModel;
    private final IModel<OperationResultStatus> operationResultStatusModel;

    public SmartGeneratingDto(
            IModel<String> elapsedTimeModel,
            IModel<List<StatusRow>> statusRows,
            IModel<OperationResultStatus> operationResultStatusModel) {
        this.elapsedTimeModel = elapsedTimeModel;
        this.statusRowsModel = statusRows;
        this.operationResultStatusModel = operationResultStatusModel;
    }

    /**
     * Elapsed time as a human-readable string, e.g., "12s elapsed".
     */
    public String getTimeElapsed() {
        return elapsedTimeModel.getObject() != null
                ? elapsedTimeModel.getObject()
                : "0s elapsed";
    }

    /**
     * Builds a list of status rows for display in the UI.
     * Each row has a label and a done/in-progress flag.
     */
    public List<StatusRow> getStatusRows() {
        return statusRowsModel.getObject();
    }

    /**
     * Simple inner DTO for rendering one status line.
     */
    public record StatusRow(String text, boolean done) implements Serializable {
    }

    public OperationResultStatus getOperationResultStatus() {
        return operationResultStatusModel.getObject();
    }

    public boolean isFinished() {
        if (getOperationResultStatus() == null) {
            return false;
        }
        return getOperationResultStatus() == OperationResultStatus.SUCCESS
                || getOperationResultStatus() == OperationResultStatus.FATAL_ERROR;
    }
}
