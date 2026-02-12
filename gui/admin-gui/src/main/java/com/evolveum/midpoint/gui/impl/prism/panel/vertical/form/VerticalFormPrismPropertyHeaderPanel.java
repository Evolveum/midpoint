/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

/**
 * @author lskublik
 *
 */
public class VerticalFormPrismPropertyHeaderPanel<T> extends PrismPropertyHeaderPanel<T> {

    private static final long serialVersionUID = 1L;

    private boolean isRequiredTagVisibleInHeaderPanel = false;

    /**
     * @param id ID of the component
     * @param model model with PrismPropertyWrapper
     * @param settings settings of the panel
     */
    public VerticalFormPrismPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void createRequired(String id) {
        WebMarkupContainer required = new WebMarkupContainer(id);
        required.add(new VisibleBehaviour(() -> isRequiredTagVisibleInHeaderPanel));
        add(required);
    }

    public void setRequiredTagVisibleInHeaderPanel(boolean requiredTagVisibleInHeaderPanel) {
        isRequiredTagVisibleInHeaderPanel = requiredTagVisibleInHeaderPanel;
    }
}
