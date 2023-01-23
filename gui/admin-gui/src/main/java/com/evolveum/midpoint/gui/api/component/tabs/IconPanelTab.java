/*
 * Copyright (c) 2023 Evolveum and contributors
 * <p>
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.tabs;

import com.evolveum.midpoint.gui.api.model.CssIconModelProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.model.IModel;

/**
 * Tab that can display icon in the tab label.
 *
 * @author skublik
 */
public abstract class IconPanelTab extends PanelTab implements CssIconModelProvider {

    private static final long serialVersionUID = 1L;

    public IconPanelTab(IModel<String> title) {
        super(title);
    }

    public IconPanelTab(IModel title, VisibleEnableBehaviour visible) {
        super(title, visible);
    }

    @Override
    public IModel<String> getCssIconModel() {
        return () -> null;
    }
}
