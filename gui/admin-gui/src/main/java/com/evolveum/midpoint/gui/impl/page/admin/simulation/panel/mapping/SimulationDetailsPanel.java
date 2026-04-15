/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.util.LocalizableMessage;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class SimulationDetailsPanel extends BasePanel<OperationResult> {

    private static final String ID_BOX = "box";
    private static final String ID_ICON = "icon";
    private static final String ID_MESSAGE = "message";
    private static final String ID_LINK = "link";

    /**
     * Panel showing a compact warning/error summary for simulation details.
     *
     * <p>The panel is visible only for result states that should be presented to the user,
     * such as warning, partial error, fatal error, unknown, or not applicable.</p>
     */
    public SimulationDetailsPanel(String id, IModel<OperationResult> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        OperationResult result = getModelObject();

        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        box.setOutputMarkupPlaceholderTag(true);
        add(box);

        if (result == null || !hasVisibleDetails(result)) {
            box.setVisible(false);
            box.add(new WebMarkupContainer(ID_ICON));
            box.add(new Label(ID_MESSAGE, Model.of("")));
            box.add(new Link<Void>(ID_LINK) {
                @Override
                public void onClick() {
                }
            }.setVisible(false));
            return;
        }

        box.add(AttributeModifier.replace("class", resolveBoxCss(result)));

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.replace("class", resolveIconCss(result)));
        box.add(icon);

        box.add(new Label(ID_MESSAGE, Model.of(resolveMessage(result))));

        box.add(new AjaxLink<Void>(ID_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                onShowDetails(result,ajaxRequestTarget);
            }
        });
    }

    protected void onShowDetails(OperationResult result, AjaxRequestTarget ajaxRequestTarget) {
        // override where used, or open popup here later
    }

    private boolean hasVisibleDetails(@NotNull OperationResult result) {
        OperationResultStatus status = result.getStatus();
        return status == OperationResultStatus.WARNING
                || status == OperationResultStatus.PARTIAL_ERROR
                || status == OperationResultStatus.FATAL_ERROR
                || status == OperationResultStatus.UNKNOWN
                || status == OperationResultStatus.NOT_APPLICABLE;
    }

    private @NotNull String resolveBoxCss(@NotNull OperationResult result) {
        OperationResultStatus status = result.getStatus();
        if (status == OperationResultStatus.WARNING
                || status == OperationResultStatus.NOT_APPLICABLE) {
            return "alert warning-light-alert d-flex align-items-center gap-3 mb-0";
        }
        return "alert danger-light-alert d-flex align-items-center gap-3 mb-0";
    }

    private @NotNull String resolveIconCss(@NotNull OperationResult result) {
        OperationResultStatus status = result.getStatus();
        if (status == OperationResultStatus.WARNING
                || status == OperationResultStatus.NOT_APPLICABLE) {
            return "fa fa-exclamation-circle text-warning";
        }
        return "fa fa-exclamation-triangle text-danger";
    }

    private String resolveMessage(@NotNull OperationResult result) {
        if (result.getUserFriendlyMessage() != null) {
            LocalizableMessage userFriendlyMessage = result.getUserFriendlyMessage();
            return LocalizationUtil.translateMessage(userFriendlyMessage);
        }

        if (result.getMessage() != null) {
            return result.getMessage();
        }

        if (result.getStatus() == OperationResultStatus.NOT_APPLICABLE) {
            return createStringResource(
                    "SimulationDetailsPanel.message.notApplicable").getString();
        }

        if (result.getStatus() == OperationResultStatus.WARNING) {
            return createStringResource(
                    "SimulationDetailsPanel.message.warning").getString();
        }

        return createStringResource(
                "SimulationDetailsPanel.message.error").getString();
    }
}
