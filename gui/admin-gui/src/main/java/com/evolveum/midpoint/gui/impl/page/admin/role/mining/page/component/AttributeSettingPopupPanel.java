/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisClusteringAttributeTable;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeSettingType;

public class AttributeSettingPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_TABLE_CLUSTERING_ATTRIBUTES = "clustering-attribute-table";
    private static final String ID_BODY_CONTAINER = "body-container";

    private static final String ID_BUTTON_CLOSE = "closeButton";
    private static final String ID_BUTTON_SAVE = "saveButton";

    PrismContainerWrapperModel<ClusteringAttributeSettingType, ClusteringAttributeRuleType> clusteringAttributeRuleModel;
    IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model;

    public AttributeSettingPopupPanel(
            @NotNull String id,
            @NotNull IModel<String> messageModel,
            IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> selectedObject) {
        super(id, messageModel);
        this.model = selectedObject;

        clusteringAttributeRuleModel = PrismContainerWrapperModel.fromContainerWrapper(model, ClusteringAttributeSettingType.F_CLUSTERING_ATTRIBUTE_RULE);

        initLayout(selectedObject);
    }

    public void initLayout(IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> selectedObject) {
        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CLOSE,
                createStringResource("AttributeSettingPopupPanel.button.cancelButton")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                onClose(ajaxRequestTarget);
            }
        };
        cancelButton.setOutputMarkupId(true);
        add(cancelButton);

        AjaxButton saveButton = new AjaxButton(ID_BUTTON_SAVE,
                createStringResource("AttributeSettingPopupPanel.button.saveButton")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
//                ClusteringAttributeSettingType realValue = null;
//                try {
//                    realValue = model.getObject().getValue().getRealValue();
//                } catch (SchemaException e) {
//                    throw new RuntimeException(e);
//                    //TODO handling
//                }
//                realValue.getClusteringAttributeRule().clear();
//                for (ClusteringAttributeRuleType clusteringAttributeRuleType : clusteringAttributeRuleModel.getObject()) {
//                    realValue.getClusteringAttributeRule().add(clusteringAttributeRuleType.clone());
//                }
//
//                onClose(ajaxRequestTarget);
            }
        };
        saveButton.setOutputMarkupId(true);
        add(saveButton);

        WebMarkupContainer bodyContainer = new WebMarkupContainer(ID_BODY_CONTAINER);
        bodyContainer.setOutputMarkupId(true);
        add(bodyContainer);

        RoleAnalysisClusteringAttributeTable clusteringAttributeTable = new RoleAnalysisClusteringAttributeTable(
                ID_TABLE_CLUSTERING_ATTRIBUTES, clusteringAttributeRuleModel, false);
        clusteringAttributeTable.setOutputMarkupId(true);
        bodyContainer.add(clusteringAttributeTable);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        //TODO
        return null;
    }
}
