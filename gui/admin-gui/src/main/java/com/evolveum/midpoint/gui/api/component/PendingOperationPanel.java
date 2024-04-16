/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PendingOperationPanel extends BasePanel<List<PendingOperationType>> {

    private static final String ID_LABEL = "label";
    private static final String ID_OPERATION = "operation";
    private static final String ID_TEXT = "text";

    public PendingOperationPanel(String id, IModel<List<PendingOperationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        List<Badge> badgeList;
        if (getModelObject() != null) {
            badgeList = getModelObject()
                    .stream()
                    .map(op -> new Badge(createBadgeClass(op), null, createLabelText(op), createTooltip(op)))
                    .toList();
        } else {
            badgeList = new ArrayList<>();
        }
        add(new BadgeListPanel(
                ID_OPERATION,
                Model.ofList(badgeList)));
    }

    private String createBadgeClass(PendingOperationType op) {
        OperationResultStatusType rStatus = op.getResultStatus();
        PendingOperationExecutionStatusType eStatus = op.getExecutionStatus();

        if (rStatus == OperationResultStatusType.FATAL_ERROR
                || rStatus == OperationResultStatusType.PARTIAL_ERROR) {
            return Badge.State.DANGER.getCss();
        }

        if (rStatus == OperationResultStatusType.UNKNOWN
                || rStatus == OperationResultStatusType.WARNING) {
            return Badge.State.WARNING.getCss();
        }

        if (rStatus == OperationResultStatusType.SUCCESS
                || eStatus == PendingOperationExecutionStatusType.COMPLETED) {
            return Badge.State.SUCCESS.getCss();
        }

        if (rStatus == OperationResultStatusType.IN_PROGRESS
                || rStatus == OperationResultStatusType.NOT_APPLICABLE
                || rStatus == OperationResultStatusType.HANDLED_ERROR) {
            return Badge.State.INFO.getCss();
        }

        return Badge.State.PRIMARY.getCss();
    }

    private String createTooltip(PendingOperationType op) {
        StringBuilder sb = new StringBuilder();

        buildStringItem(sb, "PendingOperationPanel.resultStatus", op.getResultStatus());
        buildStringItem(sb, "PendingOperationPanel.executionStatus", op.getExecutionStatus());
        buildStringItem(sb, "PendingOperationPanel.operationReference", op.getAsynchronousOperationReference());
        buildStringItem(sb, "PendingOperationPanel.attempt", op.getAttemptNumber());
        buildStringItem(sb, "PendingOperationPanel.pendingOperationType", op.getType());
        buildStringItem(sb, "PendingOperationPanel.lastAttemptTimestamp", WebComponentUtil.formatDate(op.getLastAttemptTimestamp()));
        buildStringItem(sb, "PendingOperationPanel.completionTimestamp", WebComponentUtil.formatDate(op.getCompletionTimestamp()));

        return sb.toString();
    }

    private void buildStringItem(StringBuilder sb, String key, Object obj) {
        if (obj == null) {
            return;
        }

        sb.append(getString(key)).append(" ");

        String value = obj instanceof Enum ? getString((Enum<?>) obj) : obj.toString();

        sb.append(value);
        sb.append('\n');
    }

    private String createLabelText(PendingOperationType op) {
        OperationResultStatusType rStatus = op.getResultStatus();
        PendingOperationExecutionStatusType eStatus = op.getExecutionStatus();

        if (rStatus == null) {
            return getString(eStatus);
        }

        return getString(rStatus);
    }
}
