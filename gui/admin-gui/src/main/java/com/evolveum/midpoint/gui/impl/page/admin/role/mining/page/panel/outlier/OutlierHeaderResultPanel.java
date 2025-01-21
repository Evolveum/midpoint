/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class OutlierHeaderResultPanel extends BasePanel<String> {

    private static final String ID_ITEM_BOX = "item-box";
    private static final String ID_VALUE = "value";
    private static final String ID_VALUE_DESCRIPTION = "value-description";
    private static final String ID_PROGRESS_VALUE = "progress-value";
    private static final String ID_PROGRESS_VALUE_LABEL = "progress-label";
    private static final String ID_ICON = "icon";
    private static final String ID_TIMESTAMP = "timestamp";
    private static final String ID_ACTION = "action";

    private final String oid;
    private final String value;
    private final String valueDescription;
    private final String progressValue;
    private final String timestamp;

    public OutlierHeaderResultPanel(String id, String oid, String value, String valueDescription, String progressValue, String timestamp) {
        super(id);
        this.oid = oid;
        this.value = value;
        this.valueDescription = valueDescription;
        this.progressValue = progressValue;
        this.timestamp = timestamp;

        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer itemBox = new WebMarkupContainer(ID_ITEM_BOX);
        itemBox.setOutputMarkupId(true);
        add(itemBox);

        //TODO not in HTML
//        WebMarkupContainer iconContainer = createIconContainer(ID_ICON);
//        iconContainer.setOutputMarkupId(true);
//        itemBox.add(iconContainer);

        if (isViewAnalyzedClusterEnable()) {
            AjaxLinkPanel actionPanel = new AjaxLinkPanel(ID_ACTION, Model.of("View analyzed cluster")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    performOnAction(target);
                }
            };

            actionPanel.setOutputMarkupId(true);
            itemBox.add(actionPanel);
        } else {
            EmptyPanel actionPanel = new EmptyPanel(ID_ACTION);
            itemBox.add(actionPanel);
        }

        AjaxLinkPanel namePanel = new AjaxLinkPanel(ID_VALUE, Model.of(getValue())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetailsPageUtil.dispatchToObjectDetailsPage(RoleAnalysisOutlierType.class, oid, this, true);
            }
        };
        namePanel.setOutputMarkupId(true);
        itemBox.add(namePanel);

        Label timestamp = new Label(ID_TIMESTAMP, Model.of(this.timestamp));
        timestamp.setOutputMarkupId(true);
        itemBox.add(timestamp);

        Label valueDescription = new Label(ID_VALUE_DESCRIPTION, getValueDescription());
        valueDescription.setOutputMarkupId(true);
        itemBox.add(valueDescription);

        WebMarkupContainer progressContainer = createProgressContainer(ID_PROGRESS_VALUE);
        progressContainer.setOutputMarkupId(true);
        itemBox.add(progressContainer);
    }

    public @NotNull WebMarkupContainer createProgressContainer(String componentId) {
        WebMarkupContainer progressContainer = new WebMarkupContainer(componentId);
        progressContainer.add(AttributeModifier.append("style", "width:" + getProgressValue() + "%;"));

        progressContainer.add(AttributeModifier.append("aria-valuenow", getProgressValue()));
        progressContainer.add(AttributeModifier.append("aria-valuemin", "0"));
        progressContainer.add(AttributeModifier.append("aria-valuemax", "100"));

        progressContainer.add(new Label(ID_PROGRESS_VALUE_LABEL, Model.of(this.progressValue + "%")));
        return progressContainer;
    }

    public String getValue() {
        return value;
    }

    public String getValueDescription() {
        return valueDescription;
    }

    public String getProgressValue() {
        double progressValue = Double.parseDouble(this.progressValue.replace(',', '.'));

        return String.valueOf((int) progressValue);
    }

    private @NotNull WebMarkupContainer createIconContainer(String componentId) {
        WebMarkupContainer iconContainer = new WebMarkupContainer(componentId);
        iconContainer.add(AttributeModifier.replace("class", getIcon()));
        return iconContainer;
    }

    protected void performOnClick(AjaxRequestTarget target) {

    }

    protected void performOnAction(AjaxRequestTarget target) {

    }

    protected boolean isViewAnalyzedClusterEnable() {
        return false;
    }

    public String getIcon() {
        return "fa fa-user";
    }
}
