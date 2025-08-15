/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class CompositedIconWithLabelPanel extends CompositedIconPanel {

    private static final String ID_LABEL = "label";

    private IModel<DisplayType> labelDisplayModel;

    public CompositedIconWithLabelPanel(String id, IModel<CompositedIcon> compositedIcon, IModel<DisplayType> labelDisplayModel) {
        super(id, compositedIcon);
        this.labelDisplayModel = labelDisplayModel;
    }

    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_LABEL, getLabel());
        if (StringUtils.isNotEmpty(getLabelColor())) {
            label.add(AttributeAppender.append("style", "color: " + getLabelColor()));
        }
        label.add(AttributeAppender.append("class", getLabelCssClass()));
        label.setOutputMarkupId(true);
        add(label);
    }

    private String getLabel() {
        return labelDisplayModel != null && labelDisplayModel.getObject() != null && labelDisplayModel.getObject().getLabel() != null ?
                WebComponentUtil.getTranslatedPolyString(labelDisplayModel.getObject().getLabel()) : "";
    }

    private String getLabelColor() {
        return labelDisplayModel != null && labelDisplayModel.getObject() != null && labelDisplayModel.getObject().getColor() != null ?
                GuiDisplayTypeUtil.removeStringAfterSemicolon(labelDisplayModel.getObject().getColor()) : "";
    }

    private IModel<String> getLabelCssClass() {
        return () -> {
            if (labelDisplayModel != null && labelDisplayModel.getObject() != null) {
                return labelDisplayModel.getObject().getCssClass();
            }
            return null;
        };
    }
}
