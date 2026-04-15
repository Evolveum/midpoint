/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.jetbrains.annotations.NotNull;

public class ContainerWithStatusWidgetPanel extends BasePanel<ContainerWithStatusWidgetDto>{

    private static final String ID_HEADER_ICON = "headerIcon";
    private static final String ID_HEADER_LABEL = "headerLabel";
    private static final String ID_CONFIGURE_BUTTON = "configureButton";

    private static final String ID_STATUS_CONTAINER = "statusContainer";
    private static final String ID_STATUS_ICON = "statusIcon";
    private static final String ID_STATUS_LABEL = "statusLabel";

    private static final String ID_DETAILS = "details";

    public ContainerWithStatusWidgetPanel(String id, IModel<ContainerWithStatusWidgetDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer headerIcon = new WebMarkupContainer(ID_HEADER_ICON);
        headerIcon.setOutputMarkupId(true);
        headerIcon.add(AttributeAppender.append("class", getModelObject().getTitleCssIcon()));
        add(headerIcon);

        Label headerLabel = new Label(ID_HEADER_LABEL, getModelObject().getTitle());
        headerLabel.setOutputMarkupId(true);
        add(headerLabel);

        AjaxLink<?> configureButton = new AjaxLink<>(ID_CONFIGURE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ContainerWithStatusWidgetPanel.this.getModelObject().editPerformed(target);
            }
        };
        configureButton.setOutputMarkupId(true);
        configureButton.add(new VisibleBehaviour(() -> getModelObject().isEditButtonVisible()));
        add(configureButton);

        WebMarkupContainer statusContainer = new WebMarkupContainer(ID_STATUS_CONTAINER);
        statusContainer.setOutputMarkupId(true);
        statusContainer.add(AttributeAppender.append("class", getModelObject().getStatusCssClasses()));
        add(statusContainer);

        WebMarkupContainer statusIcon = new WebMarkupContainer(ID_STATUS_ICON);
        statusIcon.setOutputMarkupId(true);
        statusIcon.add(AttributeAppender.append("class", getModelObject().getStatusCssIcon()));
        statusIcon.add(new VisibleBehaviour(() -> getModelObject().getStatusCssIcon() != null));
        statusContainer.add(statusIcon);

        Label statusLabel = new Label(ID_STATUS_LABEL, getModelObject().getStatus());
        statusLabel.setOutputMarkupId(true);
        statusLabel.add(AttributeAppender.append("title", getModelObject().getStatus()));
        statusLabel.add(new TooltipBehavior());
        statusContainer.add(statusLabel);

        WebMarkupContainer details = getDetails();
        add(details);
    }

    private @NotNull WebMarkupContainer getDetails() {
        IModel detailsModel = getModelObject().getDetails();
        WebMarkupContainer details;
        if (detailsModel.getObject() instanceof StringValuesWidgetDetailsDto) {
            details = new StringValuesWidgetDetailsPanel(ID_DETAILS, detailsModel);
        } else if (detailsModel.getObject() instanceof NumericWidgetDetailsDto) {
            details = new NumericWidgetDetailsPanel(ID_DETAILS, detailsModel);
        } else {
            details = new WebMarkupContainer(ID_DETAILS);
        }
        details.setOutputMarkupId(true);
        return details;
    }
}
