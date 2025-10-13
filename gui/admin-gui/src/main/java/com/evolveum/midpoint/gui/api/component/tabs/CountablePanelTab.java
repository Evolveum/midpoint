/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.tabs;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.CountModelProvider;
import com.evolveum.midpoint.gui.api.model.CssIconModelProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Tab that can display object count (small bubble with number) in the tab label.
 *
 * @author semancik
 */
public abstract class CountablePanelTab extends PanelTab implements CountModelProvider, CssIconModelProvider {

    private static final long serialVersionUID = 1L;

    public CountablePanelTab(IModel<String> title) {
        super(title);
    }

    public CountablePanelTab(IModel title, VisibleEnableBehaviour visible) {
        super(title, visible);
    }

    @Override
    public IModel<String> getCountModel() {
        // We cannot get the count information from the panel.
        // When we display the tab the panel does not exist yet.
        // The panel is created only when the tab is clicked.

        return () -> getCount();
    }

    @Override
    public IModel<String> getCssIconModel() {
        return () -> null;
    }

    public abstract String getCount();
}
