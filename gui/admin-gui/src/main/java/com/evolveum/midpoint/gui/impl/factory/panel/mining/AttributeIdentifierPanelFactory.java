/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.mining;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.AttributeIdentifierDropDownPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

//TODO check serializable
@Component
public class AttributeIdentifierPanelFactory extends AbstractInputGuiComponentFactory<String> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisMatchingRuleType.F_ATTRIBUTE_IDENTIFIER.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        RoleAnalysisProcessModeType processMode = getProcessMode(panelCtx);
        AttributeIdentifierDropDownPanel rangeSliderPanel = new AttributeIdentifierDropDownPanel(
                panelCtx.getComponentId(), panelCtx.getRealValueModel(), processMode, getContainer(panelCtx));
        rangeSliderPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    public RoleAnalysisProcessModeType getProcessMode(@NotNull PrismPropertyPanelContext<String> panelCtx) {
        IModel<PrismPropertyWrapper<String>> itemWrapperModel = panelCtx.getItemWrapperModel();

        if (itemWrapperModel != null) {
            PrismPropertyWrapper<String> object = itemWrapperModel.getObject();
            if (object != null) {
                PrismContainerValueWrapper<?> parent = object.getParent();
                if (parent != null) {
                    if (parent.getParent() != null) {
                        if (parent.getParent().getParent() != null) {
                            Object realValue = parent.getParent().getParent().getRealValue();
                            if (realValue != null && realValue instanceof RoleAnalysisSessionType session) {
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

    public PrismContainerValueWrapper<?> getContainer(@NotNull PrismPropertyPanelContext<String> panelCtx) {
        IModel<PrismPropertyWrapper<String>> itemWrapperModel = panelCtx.getItemWrapperModel();
        return itemWrapperModel.getObject().getParent();
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
