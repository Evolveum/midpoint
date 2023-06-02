/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class VerticalFormPrismPropertyPanel<T> extends PrismPropertyPanel<T> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(VerticalFormPrismPropertyPanel.class);

    private static final String ID_HEADER = "header";

    private boolean isRequiredTagVisibleInHeaderPanel = false;

    public VerticalFormPrismPropertyPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createHeaderPanel() {
        VerticalFormPrismPropertyHeaderPanel<T> header = new VerticalFormPrismPropertyHeaderPanel<T>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPrismPropertyPanel.this);
            }
        };
        header.setRequiredTagVisibleInHeaderPanel(this.isRequiredTagVisibleInHeaderPanel);
        return header;
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<T>> item) {
        PrismPropertyValuePanel<T> panel = new VerticalFormPrismPropertyValuePanel<T>(ID_VALUE, item.getModel(), getSettings()) {

            @Override
            protected void remove(PrismPropertyValueWrapper<T> valueToRemove, AjaxRequestTarget target) throws SchemaException {
                removeValue(valueToRemove, target);
            }
        };
        item.add(panel);
        return panel;
    }

    protected String getCssClassForValueContainer() {
        return "";
    }

    public void setRequiredTagVisibleInHeaderPanel(boolean requiredTagVisibleInHeaderPanel) {
        isRequiredTagVisibleInHeaderPanel = requiredTagVisibleInHeaderPanel;
    }
}
