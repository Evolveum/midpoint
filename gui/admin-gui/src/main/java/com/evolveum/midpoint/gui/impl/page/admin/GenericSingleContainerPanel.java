/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.GuiPanelConfiguration;
import com.evolveum.midpoint.web.application.PanelDescription;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

@PanelDescription(identifier = "genericSingleValue")
public class GenericSingleContainerPanel<C extends Containerable, O extends ObjectType> extends AbstractObjectMainPanel<PrismObjectWrapper<O>> {

    private static final String ID_DETAILS = "details";
    private ContainerPanelConfigurationType config;

    public GenericSingleContainerPanel(String id, LoadableModel<PrismObjectWrapper<O>> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel<C> panel = new SingleContainerPanel<>(ID_DETAILS, createContainerModel(), getType());
        add(panel);

    }

    private <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), getContainerPath());
    }

    private ItemPath getContainerPath() {
        if (config.getPath() == null) {
            return null;
        }
        return config.getPath().getItemPath();
    }

    private QName getType() {
        return config.getType();
    }

}
