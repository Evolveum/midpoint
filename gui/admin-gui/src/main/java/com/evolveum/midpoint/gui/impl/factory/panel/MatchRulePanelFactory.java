/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.MatchRulePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MatchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisMatchingRuleType;

//TODO check serializable
@Component
public class MatchRulePanelFactory extends AbstractGuiComponentFactory<MatchType> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisMatchingRuleType.F_MATCH_RULE.equals(wrapper.getItemName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<MatchType> panelCtx) {
        MatchRulePanel rangeSliderPanel = new MatchRulePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(),false);
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

}
