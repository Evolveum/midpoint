/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperModel<C extends Containerable, IW extends ItemWrapper> implements IModel<IW> {

    private static final Trace LOGGER  = TraceManager.getTrace(ItemWrapperModel.class);


    private IModel<?> parent;
    protected ItemPath path;
    private boolean fromContainerValue;


    ItemWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerValue) {
        this.parent = parent;
        this.path = path;
        this.fromContainerValue = fromContainerValue;
    }

    <W extends ItemWrapper> W getItemWrapper(Class<W> type) {
        try {

            if (fromContainerValue) {
                return findItemWrapperInContainerValue(type, (PrismContainerValueWrapper<C>)parent.getObject(), path);
            }

            return findItemWrapperInContainer(type, (PrismContainerWrapper<C>)parent.getObject(), path);
        } catch (SchemaException e) {
            LOGGER.error("Cannot get {} with path {} from parent {}\nReason: {}", type, path, parent, e.getMessage(), e);
            return null;
        }
    }

    private <W extends ItemWrapper> W findItemWrapperInContainerValue(Class<W> type, PrismContainerValueWrapper containerValue,
            ItemPath path)
            throws SchemaException {
        LOGGER.trace("Finding {} with path {} in {}", type.getSimpleName(), path, containerValue);
        return (W) containerValue.findItem(path, type);
    }

    private <W extends ItemWrapper> W findItemWrapperInContainer(Class<W> type, PrismContainerWrapper container, ItemPath path)
            throws SchemaException {
        LOGGER.trace("Finding {} with path {} in {}", type.getSimpleName(), path, container);
        return (W)container.findItem(path, type);
    }

    <ID extends ItemDefinition> ItemWrapper getItemWrapperForHeader(Class<ID> type, PageBase pageBase) {
        if(path.isEmpty()) {
            return null;
        }

        if (fromContainerValue) {
            return null;
        }

        try {
            PrismContainerDefinition container = (PrismContainerDefinition) this.parent.getObject();
            ItemDefinition<?> def = container.findItemDefinition(path, type);
            if (!type.isAssignableFrom(def.getClass())) {
                return null;
            }

            return createItemWrapper(def.instantiate(), pageBase);
        } catch (SchemaException e) {
            LOGGER.error("Cannot get {} with path {} from parent {}\nReason: {}", ItemWrapper.class, path,
                    this.parent.getObject(), e.getMessage(), e);
            return null;
        }
    }

    private ItemWrapper createItemWrapper(Item i, PageBase pageBase) throws SchemaException {
        Task task = pageBase.createSimpleTask("Create wrapper for column header");
        return pageBase.createItemWrapper(i, ItemStatus.NOT_CHANGED, new WrapperContext(task, task.getResult()));
    }

}
