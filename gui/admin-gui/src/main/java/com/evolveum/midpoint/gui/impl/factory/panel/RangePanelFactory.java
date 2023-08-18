/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.PropertyModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RangeSimplePanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisClusterStatisticType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

@Component
public class RangePanelFactory extends AbstractInputGuiComponentFactory<RangeType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE.equals(wrapper.getItemName())
                || AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE.equals(wrapper.getItemName())
        || AnalysisClusterStatisticType.F_MEMBERSHIP_RANGE.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<RangeType> panelCtx) {
        ItemName itemName = panelCtx.unwrapWrapperModel().getItemName();

        double max;
        if (RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE.equals(itemName)) {
            max = 100.0;
        } else {
            max = 1000.0;
        }

        RangeSimplePanel rangeSliderPanel = new RangeSimplePanel(panelCtx.getComponentId(),
                new PropertyModel<>(panelCtx.getItemWrapperModel(), "value"), max);
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

    @Override
    public void configure(PrismPropertyPanelContext<RangeType> panelCtx, org.apache.wicket.Component component) {
    }
}
