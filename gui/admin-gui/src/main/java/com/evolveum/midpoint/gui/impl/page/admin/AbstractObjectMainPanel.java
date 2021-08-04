/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

public abstract class AbstractObjectMainPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private ContainerPanelConfigurationType panelConfiguration;

    public AbstractObjectMainPanel(String id, LoadableModel<PrismObjectWrapper<O>> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.panelConfiguration = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected abstract void initLayout();

    public ContainerPanelConfigurationType getPanelConfiguration() {
        return panelConfiguration;
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), getContainerPath());
    }

    private ItemPath getContainerPath() {
        if (panelConfiguration.getPath() == null) {
            return null;
        }
        return panelConfiguration.getPath().getItemPath();
    }

    public <C extends Containerable> Class<C> getTypeClass() {
        return (Class<C>) WebComponentUtil.qnameToClass(getPrismContext(), getType());
    }

    public QName getType() {
        return getPanelConfiguration().getType();
    }
}
