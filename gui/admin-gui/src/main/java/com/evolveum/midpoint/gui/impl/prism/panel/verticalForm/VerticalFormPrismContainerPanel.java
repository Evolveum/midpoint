/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.verticalForm;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

/**
 * @author katka
 *
 */
public class VerticalFormPrismContainerPanel<C extends Containerable> extends PrismContainerPanel<C, PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";

    public VerticalFormPrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createHeaderPanel() {
        return new VerticalFormContainerHeaderPanel(ID_HEADER, getTitleModel()) {
            @Override
            protected String getIcon() {
                return VerticalFormPrismContainerPanel.this.getIcon();
            }
        };
    }

    protected IModel<String> getTitleModel() {
        return getPageBase().createStringResource(getModelObject().getDisplayName());
    }

    protected String getIcon() {
        return "";
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        VerticalFormDefaultContainerablePanel<C> panel = new VerticalFormDefaultContainerablePanel<C>("value", item.getModel(), settings);
        item.add(panel);
        return panel;
    }

    protected boolean getHeaderVisibility() {
        return true;
    }
}
