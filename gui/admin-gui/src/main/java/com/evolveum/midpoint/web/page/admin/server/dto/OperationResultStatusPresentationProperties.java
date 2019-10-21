/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 *  @author shood
 * */
public enum OperationResultStatusPresentationProperties {

    UNKNOWN("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_UNKNOWN_COLORED, "OperationResultStatus.UNKNOWN"),
    SUCCESS("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED, "OperationResultStatus.SUCCESS"),
    WARNING("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_WARNING_COLORED, "OperationResultStatus.WARNING"),
    PARTIAL_ERROR("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_PARTIAL_ERROR_COLORED, "OperationResultStatus.PARTIAL_ERROR"),
    FATAL_ERROR("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED, "OperationResultStatus.FATAL_ERROR"),
    HANDLED_ERROR("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_HANDLED_ERROR_COLORED, "OperationResultStatus.HANDLED_ERROR"),
    NOT_APPLICABLE("fa-fw " + GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_NOT_APPLICABLE_COLORED, "OperationResultStatus.NOT_APPLICABLE"),
    IN_PROGRESS("fa-fw " + GuiStyleConstants.CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED, "OperationResultStatus.IN_PROGRESS");

    private String icon;
    private String statusLabelKey;

    private OperationResultStatusPresentationProperties(String icon, String statusLabelKey) {
        this.icon = icon;
        this.statusLabelKey = statusLabelKey;
    }

    public String getIcon() {
        return icon;
    }

    public String getStatusLabelKey() {
        return statusLabelKey;
    }

    public static OperationResultStatusPresentationProperties parseOperationalResultStatus(OperationResultStatusType statusType){
        if (statusType == null) {
            return UNKNOWN;
        }

        switch (statusType) {
            case FATAL_ERROR:
                return FATAL_ERROR;
            case PARTIAL_ERROR:
                return PARTIAL_ERROR;
            case HANDLED_ERROR:
                return HANDLED_ERROR;
            case SUCCESS:
                return SUCCESS;
            case WARNING:
                return WARNING;
            case NOT_APPLICABLE:
                return NOT_APPLICABLE;
            case IN_PROGRESS:
                return IN_PROGRESS;
            default:
                return UNKNOWN;
        }
    }

    public static OperationResultStatusPresentationProperties parseOperationalResultStatus(OperationResultStatus statusType){
        if (statusType == null) {
            return UNKNOWN;
        }

        switch (statusType) {
            case FATAL_ERROR:
                return FATAL_ERROR;
            case PARTIAL_ERROR:
                return PARTIAL_ERROR;
            case HANDLED_ERROR:
                return HANDLED_ERROR;
            case SUCCESS:
                return SUCCESS;
            case WARNING:
                return WARNING;
            case NOT_APPLICABLE:
                return NOT_APPLICABLE;
            case IN_PROGRESS:
                return IN_PROGRESS;
            default:
                return UNKNOWN;
        }
    }
}
