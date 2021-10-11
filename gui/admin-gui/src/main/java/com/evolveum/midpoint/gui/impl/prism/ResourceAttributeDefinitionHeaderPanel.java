/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author skublik
 *
 */
public class ResourceAttributeDefinitionHeaderPanel<T> extends PrismPropertyHeaderPanel<T>{

    private static final long serialVersionUID = 1L;
    private static final String ID_OUTBOUND = "outbound";

    /**
     * @param id
     * @param model
     */
    public ResourceAttributeDefinitionHeaderPanel(String id, IModel<ResourceAttributeWrapper<T>> model) {
        super(id, (IModel) model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer hasOutbound = new WebMarkupContainer(ID_OUTBOUND);
        hasOutbound.add(new VisibleBehaviour(() -> hasOutboundMapping()));
        getLabelContainer().add(hasOutbound);
    }

    private IModel<ResourceAttributeWrapper<T>> getResourceAttributeDefinitionModel(){
        return (IModel)getModel();
    }

    private boolean hasOutboundMapping() {
        IModel<ResourceAttributeWrapper<T>> model = getResourceAttributeDefinitionModel();
        if (model == null) {
            return false;
        }

        ResourceAttributeWrapper<T> modelObject = model.getObject();
        if (modelObject == null) {
            return false;
        }

        return modelObject.hasOutboundMapping();
    }

}
