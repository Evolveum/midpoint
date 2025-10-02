package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;

import com.evolveum.midpoint.smart.api.info.StatusInfo;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Simple inner DTO for rendering one statusInfo line.
 */
public record StatusRowRecord(IModel<String> text, ActivityProgressInformation.RealizationState done,
                              StatusInfo<?> statusInfo) implements Serializable {

    public StatusInfo<?> getStatusInfo() {
        return statusInfo;
    }

    public boolean isFailed() {
        return statusInfo != null && statusInfo.getStatus() == OperationResultStatusType.FATAL_ERROR;
    }

    public boolean isExecuting() {
        return statusInfo != null && statusInfo.isExecuting();
    }

    @Contract(pure = true)
    public @NotNull String getIconCss() {
        OperationResultStatusType statusResult = statusInfo.getStatus();
        boolean isFatalError = statusResult == OperationResultStatusType.FATAL_ERROR;
        boolean executing = statusInfo.isExecuting();

        if (isFatalError) {
            return "fa fa-exclamation-triangle text-danger";
        }
        if (done == null) {
            return "fa fa fa-pause";
        } else if (done == ActivityProgressInformation.RealizationState.UNKNOWN) {
            return "fa fa-question-circle";
        } else if (done == ActivityProgressInformation.RealizationState.COMPLETE) {
            return "fa fa-check text-success";
        } else {
            assert done == ActivityProgressInformation.RealizationState.IN_PROGRESS;
            if (executing) {
                return "fa fa-spinner fa-spin";
            } else {
                return GuiStyleConstants.CLASS_TASK_SUSPENDED_ICON + " text-info";
            }
        }
    }
}
