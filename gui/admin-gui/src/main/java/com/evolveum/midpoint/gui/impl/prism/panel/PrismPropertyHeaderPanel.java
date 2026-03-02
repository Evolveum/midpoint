/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

import java.io.Serial;

/**
 * @author katka
 *
 */
public class PrismPropertyHeaderPanel<T> extends ItemHeaderPanel<PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyDefinition<T>, PrismPropertyWrapper<T>>{

    @Serial private static final long serialVersionUID = 1L;

    public PrismPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings itemPanelSettings) {
        super(id, model, itemPanelSettings);
    }

    //TODO check support for itemPanelSettings (label)
    public PrismPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<T>> model) {
        super(id, model, null);
    }

    @Override
    protected Component createTitle(IModel<String> label) {
        Label displayName = new Label(ID_LABEL, label);
        displayName.setOutputMarkupId(true);
        return displayName;

    }

    @Override
    protected PrismPropertyValue<T> createNewValue(PrismPropertyWrapper<T> parent) {
        return getPrismContext().itemFactory().createPropertyValue();
    }

    @Override
    protected void refreshPanel(AjaxRequestTarget target) {

    }

    @Override
    protected IModel<String> getTitleForAddButton() {
        return getParentPage().createStringResource("PrismPropertyHeaderPanel.addButtonTitle", createLabelModel().getObject());
    }

    @Override
    protected IModel<String> getTitleForRemoveAllButton() {
        return getParentPage().createStringResource("PrismPropertyHeaderPanel.removeAllButtonTitle", createLabelModel().getObject());
    }
}
