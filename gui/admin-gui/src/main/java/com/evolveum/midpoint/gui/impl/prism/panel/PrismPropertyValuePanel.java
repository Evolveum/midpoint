/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class PrismPropertyValuePanel<T> extends PrismValuePanel<T, PrismPropertyValue<T>, PrismPropertyValueWrapper<T>> {

    private static final String ID_INPUT = "input";

    public PrismPropertyValuePanel(String id, IModel<PrismPropertyValueWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected PrismPropertyPanelContext<T> createPanelCtx() {
        PrismPropertyPanelContext<T> panelCtx = new PrismPropertyPanelContext<>(getModelObject().getParent());
//        panelCtx.setComponentId(ID_INPUT);
        return panelCtx;
    }

    @Override
    protected <IW extends ItemWrapper<?, ?>> PrismPropertyValue<T> createNewValue(IW itemWrapper) {
        return getPrismContext().itemFactory().createPropertyValue();
    }

    @Override
    protected Component createDefaultPanel(String id) {
            if (getPageBase().getApplication().usesDevelopmentConfig()) {
                return new ErrorPanel(id, createStringResource("Cannot create component for: " + getModelObject().getParent().getItem()));
            } else {
                Label noComponent = new Label(id);
                noComponent.setVisible(false);
                return noComponent;
            }
    }
}
