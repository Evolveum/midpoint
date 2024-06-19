/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serial;
import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

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

        WebMarkupContainer itemBox = new WebMarkupContainer(ID_ITEM_BOX);
        itemBox.setOutputMarkupId(true);
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

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-arrow-right",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton linkButton = new AjaxCompositedIconSubmitButton(ID_LINK,
                iconBuilder.build(), Model.of("More info ")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {

            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        linkButton.titleAsLabel(true);
        linkButton.setOutputMarkupId(true);
        linkButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        itemBox.add(linkButton);
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
}
