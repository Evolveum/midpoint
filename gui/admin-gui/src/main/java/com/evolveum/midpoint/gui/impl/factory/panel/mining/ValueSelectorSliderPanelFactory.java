/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.mining;

import java.io.Serializable;

import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RangeSliderPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;

//TODO check serializable
@Component
public class ValueSelectorSliderPanelFactory extends AbstractInputGuiComponentFactory<Double> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD.equals(wrapper.getItemName())
                || RoleAnalysisDetectionOptionType.F_SENSITIVITY.equals(wrapper.getItemName())
                || RoleAnalysisDetectionOptionType.F_FREQUENCY_THRESHOLD.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Double> panelCtx) {
        RangeSliderPanel rangeSliderPanel = new RangeSliderPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
