/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
