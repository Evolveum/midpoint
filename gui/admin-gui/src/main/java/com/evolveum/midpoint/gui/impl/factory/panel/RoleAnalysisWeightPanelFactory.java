/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisMatchingRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

//TODO check serializable
@Component
public class RoleAnalysisWeightPanelFactory extends AbstractInputGuiComponentFactory<Double> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        ItemPath itemPath = wrapper.getPath().namedSegmentsOnly();
        ItemPath wrapperPath = ItemPath.create(RoleAnalysisSessionType.F_MATCHING_RULE, RoleAnalysisMatchingRuleType.F_WEIGHT);
        return itemPath.equivalent(wrapperPath);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<Double> panelCtx) {
        if (panelCtx.getRealValueModel().getObject() == null) {
            panelCtx.getRealValueModel().setObject(1.0);
        }
        return new TextPanel<>(panelCtx.getComponentId(),
                panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false);
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
