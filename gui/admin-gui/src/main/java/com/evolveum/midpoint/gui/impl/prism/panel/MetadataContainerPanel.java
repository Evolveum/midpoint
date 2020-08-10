/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.model.PropertyModel;

/**
 * @author katka
 *
 */
public class MetadataContainerPanel<C extends Containerable> extends PrismContainerPanel<C> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_PANEL = "panel";

    private static final String ID_NON_CONTAINERS = "nonContainers";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    /**
     * @param id
     * @param model
     */
    public MetadataContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

    }

    @Override
    protected Component createHeaderPanel() {
        Label button = new Label(ID_HEADER, new PropertyModel<>(getModel(), "displayName"));
        button.setOutputMarkupId(true);
        return button;
    }

    @Override
    protected boolean getHeaderVisibility() {
        return false;
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        ValueMetadataPanel<C, PrismContainerValueWrapper<C>> panel = new ValueMetadataPanel<>("value", item.getModel(), settings);
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;
    }


    //    @Override
//    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
//
//        WebMarkupContainer valuePanel = new WebMarkupContainer(ID_PANEL);
//        item.add(valuePanel);
//        ListView<ItemWrapper> nonContainers = new ListView<ItemWrapper>(ID_NON_CONTAINERS, new PropertyModel<>(item.getModel(), "nonContainers")) {
//
//            @Override
//            protected void populateItem(ListItem<ItemWrapper> listItem) {
//                listItem.add(new Label(ID_LABEL, new PropertyModel<>(listItem.getModel(), "displayName")));
//
//                ListView<PrismValueWrapper> values = new ListView<PrismValueWrapper>(ID_VALUES, new PropertyModel<>(listItem.getModel(), "values")) {
//
//                    @Override
//                    protected void populateItem(ListItem<PrismValueWrapper> listItem) {
//                        Label value = new Label(ID_VALUE, new ReadOnlyModel<>(() -> {
//
//                            return listItem.getModelObject().toShortString();
//                        }));
//                        listItem.add(value);
//                    }
//                };
//
//                values.add(new VisibleBehaviour(() -> listItem.getModelObject() != null && !listItem.getModelObject().isEmpty()));
//                listItem.add(values);
//            }
//        };
//        valuePanel.add(nonContainers);
//
//        ListView<PrismContainerWrapper<?>> containers = new ListView<PrismContainerWrapper<?>>(ID_CONTAINERS, new PropertyModel<>(item.getModel(), "containers")) {
//
//            @Override
//            protected void populateItem(ListItem<PrismContainerWrapper<?>> listItem) {
//                ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
//                listItem.add(new MetadataContainerPanel(ID_CONTAINER, listItem.getModel(), settings));
//            }
//        };
//        valuePanel.add(containers);
//
//        return valuePanel;
//
//    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismContainerWrapper<C> itemWrapper) {
        return (PV) itemWrapper.getItem().createNewValue();
    }
}
