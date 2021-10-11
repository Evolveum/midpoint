/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory;

import java.io.Serializable;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.web.component.input.QueryTextAreaPanel;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@Component
public class QueryTextAreaPanelFactory extends AbstractGuiComponentFactory<QueryType> {

    private static final long serialVersionUID = 1L;

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return QueryType.COMPLEX_TYPE.equals(wrapper.getTypeName()); // || CleanupPoliciesType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<QueryType> panelCtx) {
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
