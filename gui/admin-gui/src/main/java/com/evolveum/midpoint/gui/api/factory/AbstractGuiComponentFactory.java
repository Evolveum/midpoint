/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.factory;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient GuiComponentRegistry registry;

    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    @Override
    public Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        Panel panel = getPanel(panelCtx);
//        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel));
        return panel;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    protected abstract Panel getPanel(PrismPropertyPanelContext<T> panelCtx);


}
