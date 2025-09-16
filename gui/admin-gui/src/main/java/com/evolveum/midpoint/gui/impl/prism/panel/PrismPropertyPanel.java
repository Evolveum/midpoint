/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisitor;

/**
 * @author katkav
 */
public class  PrismPropertyPanel<T> extends ItemPanel<PrismPropertyValueWrapper<T>, PrismPropertyWrapper<T>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);

    protected static final String ID_HEADER = "header";
    protected static final String ID_VALUE = "value";


    /**
     * @param id
     * @param model
     */
    public PrismPropertyPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ItemHeaderPanel header = getHeader();
        if (header == null || header.getLabelComponent() == null) {
            return;
        }
        visitChildren(
                FormComponent.class,
                (IVisitor<FormComponent, Void>) (component, visit) -> component.add(
                        AttributeAppender.append(
                                "aria-labelledby",
                                getHeader().getLabelComponent().getMarkupId())));
    }

    private ItemHeaderPanel getHeader() {
        return (ItemHeaderPanel) get(ID_HEADER);
    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        return new PrismPropertyHeaderPanel<T>(ID_HEADER, getModel()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(PrismPropertyPanel.this);
            }

            @Override
            protected boolean isRequired() {
                ItemMandatoryHandler handler = (getSettings() != null) ? getSettings().getMandatoryHandler() : null;
                if (handler != null) {
                    return handler.isMandatory(getModelObject());
                }

                return super.isRequired();
            }
        };
    }


    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<T>> item) {
        PrismPropertyValuePanel<T> panel = new PrismPropertyValuePanel<T>(ID_VALUE, item.getModel(), getSettings()) {

            @Override
            protected void remove(PrismPropertyValueWrapper<T> valueToRemove, AjaxRequestTarget target) throws SchemaException {
                removeValue(valueToRemove, target);
            }
        };

        if (getModelObject().isMultiValue()
                && getModelObject().getValues().size() > 1
                && item.getIndex() + 1 != getModelObject().getValues().size()) {
            item.add(AttributeAppender.append("class", "pb-2"));
        }
        item.add(panel);
        return panel;
    }


    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismPropertyWrapper<T> itemWrapper) {
        return (PV) getPrismContext().itemFactory().createPropertyValue();
    }
}
