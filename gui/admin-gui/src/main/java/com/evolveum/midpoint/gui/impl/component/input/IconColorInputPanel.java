/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;

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
