/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.Collections;

public class PrismPropertyValuePanel<T> extends PrismValuePanel<T, PrismPropertyWrapper<T>, PrismPropertyValueWrapper<T>> {


    public PrismPropertyValuePanel(String id, IModel<PrismPropertyValueWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected <PC extends ItemPanelContext> PC createPanelCtx(IModel<PrismPropertyWrapper<T>> wrapper) {
        PrismPropertyPanelContext<T> panelCtx = new PrismPropertyPanelContext<>(wrapper);
        return (PC) panelCtx;
    }

    @Override
    protected Component createDefaultPanel(String id) {
            if (getApplication().usesDevelopmentConfig()) {
                return new ErrorPanel(id, createStringResource("Cannot create component for: " + getModelObject().getParent().getItem()));
            } else {
                Label noComponent = new Label(id);
                noComponent.setVisible(false);
                return noComponent;
            }
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismPropertyWrapper<T> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createPropertyValue();
    }

    @Override
    protected void remove(PrismPropertyValueWrapper<T> valueToRemove, AjaxRequestTarget target) throws SchemaException {
        throw new UnsupportedOperationException("Must be implemented in calling panel");
    }

    protected String getCssClassForValueContainer() {
        if (getSettings() != null && getSettings().isDisplayedInColumn()) {
            if (getModelObject().getParent() != null
                    && getModelObject().getParent().isMultiValue()
                    && getModelObject().getParent().getValues().size() > 1
                    && !getModelObject().getParent().getValues().get(getModelObject().getParent().getValues().size() - 1).equals(getModelObject())) {
                return "w-100 pb-2";
            }
            return "w-100";
        }
        return "col-10";
    }
}
