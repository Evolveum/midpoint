/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 * @author katka
 *
 */
public class PrismPropertyHeaderPanel<T> extends ItemHeaderPanel<PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyDefinition<T>, PrismPropertyWrapper<T>>{

    private static final long serialVersionUID = 1L;


    /**
     * @param id
     * @param model
     */
    public PrismPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<T>> model) {
        super(id, model);
    }

    @Override
    protected Component createTitle(IModel<String> label) {
        Label displayName = new Label(ID_LABEL, label);

        return displayName;

    }

    @Override
    protected PrismPropertyValue<T> createNewValue(PrismPropertyWrapper<T> parent) {
        return getPrismContext().itemFactory().createPropertyValue();
    }

    @Override
    protected void refreshPanel(AjaxRequestTarget target) {

    }
}
