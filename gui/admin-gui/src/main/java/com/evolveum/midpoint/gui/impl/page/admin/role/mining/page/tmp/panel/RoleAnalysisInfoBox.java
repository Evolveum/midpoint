/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class RoleAnalysisInfoBox extends BasePanel<InfoBoxModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_INFO_BOX = "info-box";
    private static final String ID_INFO_BOX_ICON = "info-box-icon";
    private static final String ID_INFO_BOX_TEXT = "info-box-text";
    private static final String ID_INFO_BOX_SUB_TEXT = "info-box-sub-text";
    private static final String ID_INFO_BOX_NUMBER = "info-box-number";
    private static final String ID_INFO_BOX_BAR = "info-box-bar";
    private static final String ID_INFO_BOX_DESCRIPTION = "info-box-description";
    private static final String ID_INFO_BOX_CONTAINER = "info-box-icon-container";

    public RoleAnalysisInfoBox(String id, IModel<InfoBoxModel> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        infoBox.setOutputMarkupId(true);
        infoBox.add(AttributeModifier.append("class", getInfoBoxCssClass()));
        add(infoBox);

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_INFO_BOX_CONTAINER);
        iconContainer.setOutputMarkupId(true);
        iconContainer.add(AttributeModifier.replace("style", getIconBoxContainerCssStyle()));
        infoBox.add(iconContainer);

        WebMarkupContainer icon = new WebMarkupContainer(ID_INFO_BOX_ICON);
        icon.add(AttributeModifier.replace("class", () -> getModelObject().getIconClass()));
        iconContainer.add(icon);

        Label text = new Label(ID_INFO_BOX_TEXT, () -> getModelObject().getText());
        infoBox.add(text);

        Label subText = new Label(ID_INFO_BOX_SUB_TEXT, () -> getModelObject().getSubText());
        infoBox.add(subText);

        Label number = new Label(ID_INFO_BOX_NUMBER, () -> getModelObject().getNumberText());
        infoBox.add(number);

        WebMarkupContainer bar = new WebMarkupContainer(ID_INFO_BOX_BAR);
        bar.add(AttributeModifier.replace("style", () -> "width: " + getModelObject().getNumber() + "%"));
        infoBox.add(bar);

        Label description = new Label(ID_INFO_BOX_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(new TooltipBehavior());
        description.add(AttributeAppender.replace("title", getModelObject().getDescription()));
        infoBox.add(description);
    }

    public String getIconBoxContainerCssStyle() {
        return null;
    }

    protected String getInfoBoxCssClass() {
        return "bg-info";
    }

}
