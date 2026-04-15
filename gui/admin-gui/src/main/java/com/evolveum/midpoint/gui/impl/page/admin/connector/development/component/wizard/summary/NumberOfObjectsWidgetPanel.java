/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class NumberOfObjectsWidgetPanel extends BasePanel<NumberOfObjectsWidgetDto>{

    private static final String ID_HEADER_ICON = "headerIcon";
    private static final String ID_HEADER_LABEL = "headerLabel";

    private static final String ID_NUMBER = "number";

    private static final String ID_LIST_OF_OBJECTS_BUTTON = "listOfObjectsButton";
    private static final String ID_LIST_OF_OBJECTS_LABEL = "listOfObjectsLabel";

    public NumberOfObjectsWidgetPanel(String id, IModel<NumberOfObjectsWidgetDto> model) {
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

        Label statusLabel = new Label(ID_NUMBER, getModelObject().getNumber());
        statusLabel.setOutputMarkupId(true);
        add(statusLabel);

        AjaxLink<?> listOfObjectsButton = new AjaxLink<>(ID_LIST_OF_OBJECTS_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NumberOfObjectsWidgetPanel.this.getModelObject().openListOfObjects(target);
            }
        };
        listOfObjectsButton.setOutputMarkupId(true);
        add(listOfObjectsButton);

        Label listOfObjectsLabel = new Label(ID_LIST_OF_OBJECTS_LABEL, getModelObject().getListButtonLabel());
        listOfObjectsLabel.setOutputMarkupId(true);
        listOfObjectsButton.add(listOfObjectsLabel);
    }
}
