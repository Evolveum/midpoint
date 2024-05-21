/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class RoleAnalysisInfoBox extends BasePanel<InfoBoxModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_INFO_BOX = "info-box";
    private static final String ID_INFO_BOX_ICON = "info-box-icon";
    private static final String ID_INFO_BOX_TEXT = "info-box-text";
    private static final String ID_INFO_BOX_NUMBER = "info-box-number";
    private static final String ID_INFO_BOX_BAR = "info-box-bar";
    private static final String ID_INFO_BOX_DESCRIPTION = "info-box-description";

    public RoleAnalysisInfoBox(String id, IModel<InfoBoxModel> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        infoBox.setOutputMarkupId(true);
        infoBox.add(AttributeModifier.append("class", getInfoBoxCssClass()));
        add(infoBox);

        WebMarkupContainer icon = new WebMarkupContainer(ID_INFO_BOX_ICON);
        icon.add(AttributeModifier.replace("class", () -> getModelObject().getIconClass()));
        infoBox.add(icon);

        Label text = new Label(ID_INFO_BOX_TEXT, () -> getModelObject().getText());
        infoBox.add(text);

        Label number = new Label(ID_INFO_BOX_NUMBER, () -> getModelObject().getNumberText());
        infoBox.add(number);

        WebMarkupContainer bar = new WebMarkupContainer(ID_INFO_BOX_BAR);
        bar.add(AttributeModifier.replace("style", () -> "width: " + getModelObject().getNumber() + "%"));
        infoBox.add(bar);

        Label description = new Label(ID_INFO_BOX_DESCRIPTION, () -> getModelObject().getDescription());
        infoBox.add(description);
    }

    protected String getInfoBoxCssClass() {
        return "bg-info";
    }

}
