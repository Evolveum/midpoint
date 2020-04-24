/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismObjectValueWrapper;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectValuePanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE = "value";
    private static final String ID_VIRTUAL_CONTAINERS = "virtualContainers";

    private ItemPanelSettings settings;

    public PrismObjectValuePanel(String id, IModel<PrismObjectWrapper<O>> model, ItemPanelSettings settings) {
        super(id, model);
        this.settings = settings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        createValuePanel(ID_VALUE, new PropertyModel<>(getModel(), "value"));
    }

    protected void createValuePanel(String panelId, IModel<PrismObjectValueWrapper<O>> valueModel) {

        PrismContainerValuePanel<O, PrismObjectValueWrapper<O>> valueWrapper = new PrismContainerValuePanel<>(panelId, valueModel,
                settings);
        valueWrapper.setOutputMarkupId(true);
        add(valueWrapper);

    }
}
