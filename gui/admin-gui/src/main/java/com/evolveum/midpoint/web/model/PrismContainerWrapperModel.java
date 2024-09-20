/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import java.util.function.Supplier;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;

/**
 * @author katka
 */
public class PrismContainerWrapperModel<C extends Containerable, T extends Containerable>
        extends ItemWrapperModel<C, PrismContainerWrapper<T>> {

    private Supplier<PageBase> page;

    private static final long serialVersionUID = 1L;

    private String identifier;

    PrismContainerWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerValue) {
        super(parent, path, fromContainerValue);
    }

    PrismContainerWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerValue, Supplier<PageBase> page) {
        super(parent, path, fromContainerValue);
        this.page = page;
    }

    PrismContainerWrapperModel(IModel<?> parent, String identifier) {
        this(parent, identifier, false);
    }

    PrismContainerWrapperModel(IModel<?> parent, String identifier, boolean fromContainerValue) {
        super(parent, null, fromContainerValue);
        this.identifier = identifier;
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemPath path) {
        return new PrismContainerWrapperModel<>(parent, path, false);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(
            IModel<? extends PrismContainerWrapper<C>> parent, ItemPath path, SerializableSupplier<PageBase> page) {
        return new PrismContainerWrapperModel<>(parent, path, false, page);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, String containerIdentifier) {
        return new PrismContainerWrapperModel<>(parent, containerIdentifier);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemName path) {
        return new PrismContainerWrapperModel<>(parent, ItemPath.create(path), false);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemName path, Supplier<PageBase> page) {
        return new PrismContainerWrapperModel<>(parent, ItemPath.create(path), false, page);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemPath path) {
        return new PrismContainerWrapperModel<>(parent, path, true);
    }

    public static <C extends Containerable> PrismContainerWrapperModel<C, C> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent) {
        return new PrismContainerWrapperModel<>(parent, (String) null, true);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemName path) {
        return new PrismContainerWrapperModel<>(parent, ItemPath.create(path), true);
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, String containerIdentifier) {
        return new PrismContainerWrapperModel<>(parent, containerIdentifier, true);
    }

    @Override
    public PrismContainerWrapper<T> getObject() {
        if (identifier != null) {
            if (isFromContainerValue()) {
                PrismContainerValueWrapper<C> parentObject = (PrismContainerValueWrapper) getParent().getObject();
                return parentObject.findContainer(identifier);
            }
            PrismContainerWrapper<?> parentObject = (PrismContainerWrapper) getParent().getObject();
            return parentObject.findContainer(identifier);
        }

        PrismContainerWrapper<T> result = getItemWrapper(PrismContainerWrapper.class);
        if (result != null) {
            return result;
        }

        // if page object is available we'll try to find and create wrapper based on path and underlying PrismContainerWrapper
        PageBase page = this.page != null ? this.page.get() : null;
        if (page == null) {
            return null;
        }

        ItemPath path = this.path;
        if (path == null || path.isEmpty()) {
            return null;
        }

        PrismContainerWrapper<?> parent = (PrismContainerWrapper) getParent().getObject();
        if (parent == null) {
            return null;
        }

        ItemPath item = null;
        while (!path.isEmpty()) {
            try {
                item = path.firstAsPath();
                path = path.rest();

                PrismContainerWrapper containerWrapper = parent.findContainer(item);
                if (containerWrapper != null) {
                    parent = containerWrapper;
                } else {
                    PrismContainerValueWrapper value = parent.getValue();
                    PrismContainerDefinition def = parent.findContainerDefinition(item);

                    PrismContainerWrapperFactory factory = page.getRegistry().findContainerWrapperFactory(def);

                    Task task = page.createSimpleTask("Create child containers");
                    WrapperContext ctx = new WrapperContext(task, task.getResult());
                    ctx.setCreateIfEmpty(true);

                    PrismContainerWrapper child = (PrismContainerWrapper) factory.createWrapper(value, def, ctx);
                    value.addItem(child);

                    parent = child;
                }
            } catch (SchemaException ex) {
                throw new SystemException("Couldn't create container item for " + this.path + "(exact item: " + item + ")", ex);
            }
        }

        return getItemWrapper(PrismContainerWrapper.class);
    }
}
