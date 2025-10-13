/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.tabs;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Tab that contains a singleton panel.
 *
 * @author semancik
 */
public abstract class PanelTab extends AbstractTab {

    private static final long serialVersionUID = 1L;

    private VisibleEnableBehaviour visible;
    private WebMarkupContainer panel;

    public PanelTab(IModel<String> title) {
        super(title);
    }

    public PanelTab(IModel<String> title, VisibleEnableBehaviour visible) {
        super(title);
        this.visible = visible;
    }

    @Override
    public WebMarkupContainer getPanel(String panelId) {
        if (panel == null) {
            panel = createPanel(panelId);
        }

        panel.setOutputMarkupId(true);
        panel.setOutputMarkupPlaceholderTag(true);
        return panel;
    }

    public WebMarkupContainer getPanel(){
        return panel;
    }

    public abstract WebMarkupContainer createPanel(String panelId);

    @Override
    public boolean isVisible() {
        if (visible == null) {
            return true;
        }

        return visible.isVisible();
    }

    public void resetPanel(){
        panel = null;
    }
}
