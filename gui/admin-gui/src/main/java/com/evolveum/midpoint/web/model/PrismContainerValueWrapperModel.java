/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Model that returns PrismContainerValueWrapperModel base on path.
 *
 * @author katkav
 */
public class PrismContainerValueWrapperModel<T extends Containerable, C extends Containerable> implements IModel<PrismContainerValueWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerValueWrapperModel.class);

    private ItemPath path;
    private IModel<? extends PrismContainerWrapper<T>> parent;
    private IModel<? extends PrismContainerValueWrapper<T>> valueParent;

    PrismContainerValueWrapperModel() {
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerValueWrapperModel<T, C> fromContainerWrapper(
            IModel<? extends PrismContainerWrapper<T>> model, ItemPath path) {
        PrismContainerValueWrapperModel<T, C> retModel = new PrismContainerValueWrapperModel<>();
        retModel.path = path;
        retModel.parent = model;
        Validate.notNull(path, "Item path must not be null.");
        return retModel;
    }

    public static <C extends Containerable, T extends Containerable> PrismContainerValueWrapperModel<T, C> fromContainerValueWrapper(
            IModel<? extends PrismContainerValueWrapper<T>> model, ItemPath path) {
        PrismContainerValueWrapperModel<T, C> retModel = new PrismContainerValueWrapperModel<>();
        retModel.path = path;
        retModel.valueParent = model;
        Validate.notNull(path, "Item path must not be null.");
        return retModel;
    }

    @Override
    public void detach() {
    }

    @Override
    public PrismContainerValueWrapper<C> getObject() {
        PrismContainerValueWrapper<C> containerWrapper = null;
        String messageParent = null;

        try {
            if (valueParent != null) {
                messageParent = "valueParent: " + valueParent.getObject();
                containerWrapper = valueParent.getObject().findContainerValue(path);
            } else {
                messageParent = "parent: " + parent.getObject();
                containerWrapper = parent.getObject().findContainerValue(path);
            }
        } catch (SchemaException e) {
            LOGGER.error("Cannot find container value wrapper, \n" + messageParent + ", \npath: {}", path);
        }
        return containerWrapper;
    }

    @Override
    public void setObject(PrismContainerValueWrapper<C> arg0) {
        throw new UnsupportedOperationException("ContainerWrapperFromObjectWrapperModel.setObject called");

    }

}
