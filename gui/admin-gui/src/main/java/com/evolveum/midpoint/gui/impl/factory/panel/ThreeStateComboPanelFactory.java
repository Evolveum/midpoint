/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;

/**
 * @author katka
 *
 */
@Component
public class ThreeStateComboPanelFactory extends AbstractGuiComponentFactory<Boolean> {

    private static final long serialVersionUID = 1L;

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }
    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return DOMUtil.XSD_BOOLEAN.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<Boolean> panelCtx) {
        return new TriStateComboPanel(panelCtx.getComponentId(), (IModel<Boolean>) panelCtx.getRealValueModel());
    }



}
