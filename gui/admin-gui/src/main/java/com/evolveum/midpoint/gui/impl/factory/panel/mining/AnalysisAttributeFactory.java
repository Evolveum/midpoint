/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.mining;

import java.util.Collection;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

//TODO probably should be removed
@Component
public class AnalysisAttributeFactory extends AbstractInputGuiComponentFactory<Collection<ItemPathType>> {


    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS, RoleAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING, AnalysisAttributeSettingType.F_PATH)
                .equivalent(wrapper.getPath())
                ||
                ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS, RoleAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING, AnalysisAttributeSettingType.F_PATH)
                        .equivalent(wrapper.getPath());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Collection<ItemPathType>> panelCtx) {
        return new TextPanel<>(panelCtx.getComponentId(), null);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    @Override
    public void configure(PrismPropertyPanelContext<Collection<ItemPathType>> panelCtx, org.apache.wicket.Component
            component) {
        component.setEnabled(panelCtx.getVisibleEnableBehavior().isEnabled());
    }

    public RoleAnalysisProcessModeType getProcessMode
            (@NotNull PrismPropertyPanelContext<AnalysisAttributeSettingType> panelCtx) {
        IModel<PrismPropertyWrapper<AnalysisAttributeSettingType>> itemWrapperModel = panelCtx.getItemWrapperModel();

        if (itemWrapperModel != null) {
            PrismPropertyWrapper<AnalysisAttributeSettingType> object = itemWrapperModel.getObject();
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
