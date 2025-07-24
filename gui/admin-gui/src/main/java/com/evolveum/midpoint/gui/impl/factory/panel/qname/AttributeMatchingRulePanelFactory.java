/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;

@Component
public class AttributeMatchingRulePanelFactory extends AbstractInputGuiComponentFactory<QName> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return ItemRefinedDefinitionType.F_MATCHING_RULE.equals(wrapper.getItemName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        List<QName> matchingRuleList = WebComponentUtil.getAttributeApplicableMatchingRuleList();

        DropDownChoicePanel<QName> matchingRulePanel = new DropDownChoicePanel<QName>(panelCtx.getComponentId(), panelCtx.getRealValueModel(),
                Model.ofList(matchingRuleList), new QNameIChoiceRenderer(), true);
        matchingRulePanel.setOutputMarkupId(true);
        matchingRulePanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        return matchingRulePanel;
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

}
