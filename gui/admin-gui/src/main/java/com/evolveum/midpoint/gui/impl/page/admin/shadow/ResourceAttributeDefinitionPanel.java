/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.prism.wrapper.ResourceAttributeWrapper;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.Serial;

/**
 * @author katkav
 */
public class ResourceAttributeDefinitionPanel<T> extends PrismPropertyPanel<T> {

    @Serial private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributeDefinitionPanel.class);

    private static final String ID_HEADER = "header";


    /**
     * @param id
     * @param model
     */
    public ResourceAttributeDefinitionPanel(String id, IModel<ResourceAttributeWrapper<T>> model, ItemPanelSettings settings) {
        super(id, (IModel)model, settings);
    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        return new ResourceAttributeDefinitionHeaderPanel<>(ID_HEADER, getResourceAttributeDefinitionModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(ResourceAttributeDefinitionPanel.this);
            }
        };
    }

    private IModel<ResourceAttributeWrapper<T>> getResourceAttributeDefinitionModel(){
        return (IModel)getModel();
    }

}
