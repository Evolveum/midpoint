/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

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
        ListView<PendingOperationType> operation = new ListView<PendingOperationType>(ID_OPERATION, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<PendingOperationType> item) {
                item.setRenderBodyOnly(true);

                WebMarkupContainer label = new WebMarkupContainer(ID_LABEL);
                item.add(label);

                Label text = new Label(ID_TEXT, createLabelText(item.getModel()));
                text.setRenderBodyOnly(true);
                label.add(text);

                label.add(AttributeAppender.append("class", createTextClass(item.getModel())));

                label.add(AttributeModifier.replace("title", createTextTooltipModel(item.getModel())));
                label.add(new InfoTooltipBehavior() {

                    @Override
                    public String getCssClass() {
                        return null;
                    }
                });
            }
        };
        add(operation);
    }

    private IModel<String> createTextClass(IModel<PendingOperationType> model) {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                PendingOperationType op = model.getObject();
                OperationResultStatusType rStatus = op.getResultStatus();
                PendingOperationExecutionStatusType eStatus = op.getExecutionStatus();

                if (rStatus == OperationResultStatusType.FATAL_ERROR
                        || rStatus == OperationResultStatusType.PARTIAL_ERROR) {
                    return "label-danger";
                }

                if (rStatus == OperationResultStatusType.UNKNOWN
                        || rStatus == OperationResultStatusType.WARNING) {
                    return "label-warning";
                }

                if (rStatus == OperationResultStatusType.SUCCESS
                        || eStatus == PendingOperationExecutionStatusType.COMPLETED) {
                    return "label-success";
                }

                if (rStatus == OperationResultStatusType.IN_PROGRESS
                        || rStatus == OperationResultStatusType.NOT_APPLICABLE
                        || rStatus == OperationResultStatusType.HANDLED_ERROR) {
                    return "label-info";
                }

                return "label-default";
            }
        };
    }

    private IModel<String> createTextTooltipModel(IModel<PendingOperationType> model) {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                PendingOperationType op = model.getObject();

                buildStringItem(sb, "PendingOperationPanel.resultStatus", op.getResultStatus());
                buildStringItem(sb, "PendingOperationPanel.executionStatus", op.getExecutionStatus());
                buildStringItem(sb, "PendingOperationPanel.operationReference", op.getAsynchronousOperationReference());
                buildStringItem(sb, "PendingOperationPanel.attempt", op.getAttemptNumber());
                buildStringItem(sb, "PendingOperationPanel.pendingOperationType", op.getType());
                buildStringItem(sb, "PendingOperationPanel.lastAttemptTimestamp", WebComponentUtil.formatDate(op.getLastAttemptTimestamp()));
                buildStringItem(sb, "PendingOperationPanel.completionTimestamp", WebComponentUtil.formatDate(op.getCompletionTimestamp()));

                return sb.toString();
            }
        };
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

    private IModel<String> createLabelText(IModel<PendingOperationType> model) {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                PendingOperationType op = model.getObject();
                OperationResultStatusType rStatus = op.getResultStatus();
                PendingOperationExecutionStatusType eStatus = op.getExecutionStatus();

                if (rStatus == null) {
                    return getString(eStatus);
                }

                return getString(rStatus);
            }
        };
    }
}
