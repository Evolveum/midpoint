/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeWrapper;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katkav
 */
public class ResourceAttributeDefinitionPanel<T> extends PrismPropertyPanel<T> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributeDefinitionPanel.class);

    private static final String ID_HEADER = "header";

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_VALUE_CONTAINER = "valueContainer";

    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";


    /**
     * @param id
     * @param model
     */
    public ResourceAttributeDefinitionPanel(String id, IModel<ResourceAttributeWrapper<T>> model, ItemPanelSettings settings) {
        super(id, (IModel)model, settings);
    }

    @Override
    protected Panel createHeaderPanel() {
        return new ResourceAttributeDefinitionHeaderPanel<>(ID_HEADER, getResourceAttributeDefinitionModel());
    }

    private IModel<ResourceAttributeWrapper<T>> getResourceAttributeDefinitionModel(){
        return (IModel)getModel();
    }

}
