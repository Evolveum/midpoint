/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author skublik
 *
 */
public class PrismContainerColumnHeaderPanel<C extends Containerable> extends ItemHeaderPanel<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    public PrismContainerColumnHeaderPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);
    }


    @Override
    protected Component createTitle(IModel<String> label) {
        Label labelComponent = new Label(ID_LABEL, label) ;
        labelComponent.setOutputMarkupId(true);
        return labelComponent;
    }

    @Override
    protected void refreshPanel(AjaxRequestTarget target) {

    }

    @Override
    protected PrismContainerValue<C> createNewValue(PrismContainerWrapper<C> parent) {
        return null;
    }

    @Override
    protected boolean isAddButtonVisible() {
        return false;
    }

    @Override
    protected boolean isButtonEnabled() {
        return false;
    }
}
