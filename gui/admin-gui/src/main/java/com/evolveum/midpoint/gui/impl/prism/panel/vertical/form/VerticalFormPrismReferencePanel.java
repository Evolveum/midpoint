/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.prism.panel.*;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author lskublik
 */
public class VerticalFormPrismReferencePanel<R extends Referencable>
        extends PrismReferencePanel<R> {

    public VerticalFormPrismReferencePanel(
            String id,
            IModel<PrismReferenceWrapper<R>> model,
            ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismReferenceValueWrapperImpl<R>> item) {
        VerticalFormPrismReferenceValuePanel<R> valuePanel
                = new VerticalFormPrismReferenceValuePanel<R>(ID_VALUE, item.getModel(), getSettings()) {
            @Override
            protected void remove(
                    PrismReferenceValueWrapperImpl<R> valueToRemove, AjaxRequestTarget target)
                    throws SchemaException {
                VerticalFormPrismReferencePanel.this.removeValue(valueToRemove, target);
            }
        };
        item.add(valuePanel);
        return valuePanel;
    }

    @Override
    protected Component createHeaderPanel() {
        return new VerticalFormPrismReferenceHeaderPanel<R>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPrismReferencePanel.this);
            }

            @Override
            protected void createRequired(String id) {
                WebMarkupContainer required = new WebMarkupContainer(id);
                required.add(new VisibleBehaviour(() -> false));
                add(required);
            }
        };
    }
}
