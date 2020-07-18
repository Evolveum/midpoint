/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class QueryPanelFactory extends AbstractGuiComponentFactory<QueryType> {

    @PostConstruct
    public void registerFactory() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<QueryType> panelCtx) {
        return new TextAreaPanel<>(panelCtx.getComponentId(), new QueryTypeModel(panelCtx.getRealValueModel(), panelCtx.getDefinitionName(), panelCtx.getPrismContext()), 10);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return QueryType.COMPLEX_TYPE.equals(wrapper.getTypeName());
    }

    @Override
    public Integer getOrder() {
        return super.getOrder() - 100;
    }
}
