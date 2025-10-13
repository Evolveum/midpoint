/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.ClusteringAttributeSelectorPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class VerticalFormClusteringAttributesPanel extends VerticalFormPrismContainerPanel<ClusteringAttributeSettingType> {

    private static final String ID_ATTRIBUTES = "attributes";

    public VerticalFormClusteringAttributesPanel(String id, IModel<PrismContainerWrapper<ClusteringAttributeSettingType>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createValuesPanel() {
        ClusteringAttributeSelectorPanel panel = new ClusteringAttributeSelectorPanel(ID_ATTRIBUTES, getModel(), getAnalysisOption().getProcessMode()) {
            @Override
            public boolean isEditable() {
                if (getSettings() == null || getSettings().getEditabilityHandler() == null) {
                    return true;
                }
                return getSettings().getEditabilityHandler().isEditable(getModelObject());
            }
        };
        panel.setOutputMarkupId(true);
        return panel;
    }

    @Override
    protected boolean isHelpTextVisible() {
        return true;
    }

    @Override
    protected boolean isExpandedButtonVisible() {
        return false;
    }

    public @Nullable RoleAnalysisOptionType getAnalysisOption() {
        PrismContainerWrapper<ClusteringAttributeSettingType> clusteringSettings = getModelObject();
        RoleAnalysisSessionType session = (RoleAnalysisSessionType) clusteringSettings.findObjectWrapper().getObject().asObjectable();

        return session.getAnalysisOption(); //TODO this might not work in all cases. rather navigate through the wrapper structure
    }

    @Override
    protected String getCssForHeader() {
        return "pb-0 pt-2 pl-3 pr-3 text-gray font-weight-bold text-center w-100";
    }
}
