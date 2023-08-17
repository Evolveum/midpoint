/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.web.component.input.QueryTextAreaPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@Component
public class QueryTextAreaPanelFactory extends AbstractInputGuiComponentFactory<QueryType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QueryType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    //FIXME should be TextAreaPanel with custom model
    //TODO cleanup
    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QueryType> panelCtx) {
        int size = 10;
        if (FocusType.F_DESCRIPTION.equals(panelCtx.getDefinitionName())) {
            size = 2;
        }
        return new QueryTextAreaPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), size);
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 2;
    }
}
