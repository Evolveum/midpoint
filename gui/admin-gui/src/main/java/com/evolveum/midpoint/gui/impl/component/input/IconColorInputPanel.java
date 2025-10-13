/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/***
 * Panel for Icon color with insight. We need define abstract method createPanel for InputPanel.
 */
public abstract class IconColorInputPanel extends IconInputPanel {

    public IconColorInputPanel(String componentId, IModel<String> valueModel) {
        super(componentId, valueModel);
    }

    @Override
    protected void customProcessOfInsight(WebMarkupContainer insight) {
        insight.add(AttributeModifier.replace("style", () -> "color: " + getIconColor() + ";"));
    }

    private String getIconColor() {
        String color = getValueModel().getObject();
        if (color == null) {
            return "";
        }
        return GuiDisplayTypeUtil.removeStringAfterSemicolon(color);
    }

    protected String getCssIconClass() {
        return "fa fa-square";
    }
}
