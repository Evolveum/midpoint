/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RangeSliderPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;

@Component
public class ValueSelectorSliderPanelFactory extends AbstractInputGuiComponentFactory<Double> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Double> panelCtx) {
        RangeSliderPanel rangeSliderPanel = new RangeSliderPanel(panelCtx.getComponentId(),panelCtx.getRealValueModel());
        rangeSliderPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }


}
