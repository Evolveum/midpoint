/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.factory;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

public abstract class AbstractGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    @Autowired
    private GuiComponentRegistry registry;

    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    @Override
    public Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        Panel panel = getPanel(panelCtx);
        return panel;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    protected abstract Panel getPanel(PrismPropertyPanelContext<T> panelCtx);
}
