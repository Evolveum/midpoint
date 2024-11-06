/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author katka
 */
public class PrismContainerPanel<C extends Containerable, PCW extends PrismContainerWrapper<C>> extends ItemPanel<PrismContainerValueWrapper<C>, PCW> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";

    public PrismContainerPanel(String id, IModel<PCW> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeModifier.append("class", () -> {

            if (getModelObject() != null && getModelObject().isMultiValue()) {
                return " multivalue-container";
            }
            return "";
        }));

    }

    @Override
    protected Component createValuesPanel() {
        Component valueContainer = super.createValuesPanel();
        valueContainer.add(AttributeAppender.append(
                "aria-label",
                () -> {
                    if (getModelObject() != null && getModelObject().isMultiValue()) {
                        //TODO check it
                        if (getHeader() == null) {
                            return getParentPage().createStringResource(
                                    "PrismContainerPanel.container");
                        }
                        return getParentPage().createStringResource(
                                        "PrismContainerPanel.container", getHeader().createLabelModel().getObject())
                                .getString();
                    }
                    return null;
                }));
//        valueContainer.add(AttributeAppender.append(
//                "tabindex",
//                () -> getModelObject() != null && getModelObject().isMultiValue() ? "0" : null));
        return valueContainer;
    }

    private PrismContainerHeaderPanel getHeader() {
        return (PrismContainerHeaderPanel) get(ID_HEADER);
    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        PrismContainerHeaderPanel<C, PCW> header = new PrismContainerHeaderPanel(ID_HEADER, getModel()) {
            @Override
            protected void onExpandClick(AjaxRequestTarget target) {
                PrismContainerWrapper<C> wrapper = PrismContainerPanel.this.getModelObject();
                wrapper.setExpanded(!wrapper.isExpanded());
                target.add(PrismContainerPanel.this);
            }

            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(PrismContainerPanel.this);
            }
        };

        header.add(AttributeAppender.append(
                "aria-label",
                () -> getParentPage().createStringResource(
                        "PrismContainerPanel.header",
                        getHeader().createLabelModel().getObject())));
        return header;
    }

    @Override
    protected boolean getHeaderVisibility() {
        if (!super.getHeaderVisibility()) {
            return false;
        }
        return getModelObject() != null && getModelObject().isMultiValue();
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> panel = new PrismContainerValuePanel<C, PrismContainerValueWrapper<C>>("value", item.getModel(), settings) {

            @Override
            protected void remove(PrismContainerValueWrapper<C> valueToRemove, AjaxRequestTarget target) throws SchemaException {
                PrismContainerPanel.this.removeValue(valueToRemove, target);
            }
        };
        panel.add(AttributeAppender.replace("style", getModelObject().isMultiValue() && !getModelObject().isExpanded() ? "display:none" : ""));
        item.add(panel);

        return panel;
    }

    protected String getCssClassForValueContainer() {
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <PV extends PrismValue> PV createNewValue(PCW itemWrapper) {
        return (PV) itemWrapper.getItem().createNewValue();
    }
}
