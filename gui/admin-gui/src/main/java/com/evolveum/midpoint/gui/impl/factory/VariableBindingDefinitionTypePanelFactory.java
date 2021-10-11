/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.VariableBindingDefinitionTypePanel;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class VariableBindingDefinitionTypePanelFactory extends AbstractGuiComponentFactory<VariableBindingDefinitionType> {


    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {
        return new VariableBindingDefinitionTypePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName());
    }
}
