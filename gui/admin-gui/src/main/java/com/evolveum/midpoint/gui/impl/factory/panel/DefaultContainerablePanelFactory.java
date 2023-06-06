/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.panel.DefaultContainerablePanel;
import com.evolveum.midpoint.prism.Containerable;

@Component
public class DefaultContainerablePanelFactory<C extends Containerable>
        implements GuiComponentFactory<PrismContainerPanelContext<C>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return wrapper instanceof PrismContainerWrapper && !wrapper.isMetadata();
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismContainerPanelContext<C> panelCtx) {
//        if (FormPanelType.VERTICAL.equals(panelCtx.getFormType())) {
//
//        }
        return new DefaultContainerablePanel<>(panelCtx.getComponentId(), panelCtx.getValueWrapper(), panelCtx.getSettings());
    }

//    @Override TODO unnecessary?
//    public void configure(PrismContainerPanelContext<C> panelCtx, org.apache.wicket.Component component) {
//        if (!(component instanceof DefaultContainerablePanel)) {
//            return;
//        }
//        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(component));
//    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }
}
