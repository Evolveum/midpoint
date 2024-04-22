/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.mining;


import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.AnalysisAttributeSelectorPanel;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class AnalysisAttributeFactory extends AbstractInputGuiComponentFactory<ClusteringAttributeSettingType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<ClusteringAttributeSettingType> panelCtx) {
        AnalysisAttributeSelectorPanel clusteringAttributeSelectorPanel = new AnalysisAttributeSelectorPanel(panelCtx.getComponentId(),
                new PropertyModel<>(panelCtx.getItemWrapperModel(), "value"), getProcessMode(panelCtx));
        clusteringAttributeSelectorPanel.setOutputMarkupId(true);
        return clusteringAttributeSelectorPanel;
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    @Override
    public void configure(PrismPropertyPanelContext<ClusteringAttributeSettingType> panelCtx, org.apache.wicket.Component
            component) {
        component.setEnabled(panelCtx.getVisibleEnableBehavior().isEnabled());
    }

    public RoleAnalysisProcessModeType getProcessMode
            (@NotNull PrismPropertyPanelContext<ClusteringAttributeSettingType> panelCtx) {
        IModel<PrismPropertyWrapper<ClusteringAttributeSettingType>> itemWrapperModel = panelCtx.getItemWrapperModel();

        if (itemWrapperModel != null) {
            PrismPropertyWrapper<ClusteringAttributeSettingType> object = itemWrapperModel.getObject();
            if (object != null) {
                PrismContainerValueWrapper<?> parent = object.getParent();
                if (parent != null) {
                    if (parent.getParent() != null) {
                        if (parent.getParent().getParent() != null) {
                            Object realValue = parent.getParent().getParent().getRealValue();
                            if (realValue instanceof RoleAnalysisSessionType session) {
                                RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
                                return analysisOption.getProcessMode();
                            }
                        }
                    }
                }
            }
        }

        return RoleAnalysisProcessModeType.USER;
    }
}
