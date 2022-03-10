/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @deprecated This helper model was created because currently there's no simple way of creating PrismContainerWrapper automatically if ItemPath
 * has more than one item. PrismObjectWrapper creates containers only up to first level. In our case for SystemConfigurationType it creates
 * PrismContainerWrapper for messageTransportConfiguration container but if there's no existing configuration, this container contains zero items.
 *
 * This should be fixed somewhere in PrismContainerWrapperModel or ItemWrapperModel, but it was not an easy fix - it's a mess
 * there (between model, wrapper and real prism items). To complicate the change even further, we also don't want to create
 * recursively all container wrappers for all non-existing containers at all times.
 *
 * Created by Viliam Repan (lazyman).
 */
@Deprecated
public class MessageTransportContainerModel<T extends Containerable> implements IModel<PrismContainerWrapper<T>> {

    private BasePanel panel;

    private LoadableModel<PrismObjectWrapper<SystemConfigurationType>> model;

    private QName containerName;

    public MessageTransportContainerModel(BasePanel panel, LoadableModel<PrismObjectWrapper<SystemConfigurationType>> model, QName containerName) {

        this.panel = panel;
        this.model = model;
        this.containerName = containerName;
    }

    @Override
    public PrismContainerWrapper<T> getObject() {
        PrismContainerWrapper container = PrismContainerWrapperModel.fromContainerWrapper(model, ItemPath.create(
                SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION, containerName)).getObject();

        if (container != null) {
            return container;
        }

        container = PrismContainerWrapperModel.fromContainerWrapper(model, ItemPath.create(
                SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)).getObject();

        try {
            PrismContainerValueWrapper value = (PrismContainerValueWrapper) container.getValue();

            PrismContainerDefinition def = container.findContainerDefinition(ItemPath.create(containerName));
            PrismContainerWrapperFactory factory = panel.getPageBase().getRegistry().findContainerWrapperFactory(def);

            Task task = panel.getPageBase().createSimpleTask("Create child containers");
            WrapperContext ctx = new WrapperContext(task, task.getResult());
            ctx.setCreateIfEmpty(true);

            PrismContainerWrapper child = (PrismContainerWrapper) factory.createWrapper(value, def, ctx);
            value.addItem(child);

            return child;
        } catch (SchemaException ex) {
            throw new SystemException("Couldn't create message transport configuration container item for " + containerName, ex);
        }
    }
}
