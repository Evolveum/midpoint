/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;

/**
 * @author katka
 *
 */
public class MetadataContainerPanel<C extends Containerable> extends PrismContainerPanel<C, PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_VALUE = "value";

    public MetadataContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected boolean getHeaderVisibility() {
        return false;
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        settings.setVisibilityHandler(w -> ItemVisibility.AUTO);
        ValueMetadataPanel<C, PrismContainerValueWrapper<C>> panel = new ValueMetadataPanel<>(ID_VALUE, item.getModel(), settings);
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;
    }
}
