/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class OutlierItemResultPanel extends BasePanel<IModel<OutlierItemModel>> {

    private static final String ID_ITEM_BOX = "item-box";
    private static final String ID_VALUE = "value";
    private static final String ID_VALUE_DESCRIPTION = "value-description";
    private static final String ID_ICON = "icon";
    private static final String ID_LINK = "link";

    OutlierItemModel model;

    public OutlierItemResultPanel(String id, OutlierItemModel model) {
        super(id);
        this.model = model;
        initLayout();
    }

    private void initLayout() {

        add(AttributeModifier.append("class", getInitialCssClass()));

        WebMarkupContainer itemBox = new WebMarkupContainer(ID_ITEM_BOX);
        itemBox.setOutputMarkupId(true);
        itemBox.add(AttributeModifier.append("style", getItemBoxCssStyle()));
        itemBox.add(AttributeModifier.append("class", getItemBocCssClass()));
        add(itemBox);

        Label value = new Label(ID_VALUE, getValue());
        value.setOutputMarkupId(true);
        itemBox.add(value);

        Label valueDescription = new Label(ID_VALUE_DESCRIPTION, getValueDescription());
        valueDescription.setOutputMarkupId(true);
        itemBox.add(valueDescription);

        WebMarkupContainer iconContainer = createIconContainer(ID_ICON);
        iconContainer.setOutputMarkupId(true);
        itemBox.add(iconContainer);

        WebMarkupContainer link = new WebMarkupContainer(ID_LINK);
        link.setOutputMarkupId(true);
        link.add(AttributeModifier.replace("class", getLinkCssClass()));
        itemBox.add(link);
    }

    private @NotNull WebMarkupContainer createIconContainer(String componentId) {
        WebMarkupContainer iconContainer = new WebMarkupContainer(componentId);
        iconContainer.add(AttributeModifier.replace("class", getIcon()));
        return iconContainer;
    }

    public String getValue() {

        String value = model.getValue();
        if (value != null) {
            return value;
        }

        return "Not defined";
    }

    public String getValueDescription() {
        String valueDescription = model.getValueDescription();
        return Objects.requireNonNullElse(valueDescription, "Not defined");
    }

    public String getIcon() {
        String icon = model.getIcon();
        if (icon != null) {
            return icon;
        }
        return "fa fa-question";
    }

    protected String getItemBoxCssStyle() {
        return null;
    }

    protected String getItemBocCssClass() {
        return "small-box bg-primary";
    }

    protected String getLinkCssClass() {
        return "small-box-footer";
    }

    protected String getInitialCssClass() {
        return "col-xl-6";
    }
}
