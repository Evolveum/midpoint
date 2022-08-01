/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author katka
 */
public class PrismReferencePanel<R extends Referencable>
        extends ItemPanel<PrismReferenceValueWrapperImpl<R>, PrismReferenceWrapper<R>> {

    private static final long serialVersionUID = 1L;

    protected static final String ID_HEADER = "header";
    protected static final String ID_VALUE = "value";

    public PrismReferencePanel(String id, IModel<PrismReferenceWrapper<R>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected Component createHeaderPanel() {
        return new PrismReferenceHeaderPanel<R>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(PrismReferencePanel.this);
            }
        };
    }

    @Override
    protected Component createValuePanel(ListItem<PrismReferenceValueWrapperImpl<R>> item) {
        PrismReferenceValuePanel<R> valuePanel = new PrismReferenceValuePanel<R>(ID_VALUE, item.getModel(), getSettings()) {
            @Override
            protected void remove(
                    PrismReferenceValueWrapperImpl<R> valueToRemove, AjaxRequestTarget target)
                    throws SchemaException {
                PrismReferencePanel.this.removeValue(valueToRemove, target);
            }
        };
        item.add(valuePanel);
        return valuePanel;
    }

    @Override
    public boolean isEnabled() {
        if (getEditabilityHandler() != null && !getEditabilityHandler().isEditable(getModelObject())) {
            return false;
        }
        return true;
//        return !getModelObject().isReadOnly() || isLink(getModelObject());
    }

    private boolean isLink(PrismReferenceWrapper<R> iw) {
        boolean isLink = false;
        if (CollectionUtils.isNotEmpty(iw.getValues()) && iw.getValues().size() == 1) {
            isLink = iw.getValues().get(0).isLink();
        }
        return isLink;
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismReferenceWrapper<R> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createReferenceValue();
    }
}
