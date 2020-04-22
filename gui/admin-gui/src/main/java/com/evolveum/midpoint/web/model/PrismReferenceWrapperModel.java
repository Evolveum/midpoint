/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author katka
 *
 */
public class PrismReferenceWrapperModel<C extends Containerable, R extends Referencable> extends ItemWrapperModel<C, PrismReferenceWrapper<R>>{

    private static final long serialVersionUID = 1L;

    PrismReferenceWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerWrapper) {
        super(parent, path, fromContainerWrapper);
    }

    public static <C extends Containerable, R extends Referencable> PrismReferenceWrapperModel<C, R> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemPath path) {
        return new PrismReferenceWrapperModel<C,R>(parent, path, false);
    }

    public static <C extends Containerable, R extends Referencable> PrismReferenceWrapperModel<C, R> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemName path) {
        return new PrismReferenceWrapperModel<C,R>(parent, ItemPath.create(path), false);
    }

    public static <C extends Containerable, R extends Referencable> PrismReferenceWrapperModel<C, R> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemPath path) {
        return new PrismReferenceWrapperModel<>(parent, path, true);
    }

    public static <C extends Containerable, R extends Referencable> PrismReferenceWrapperModel<C, R> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemName path) {
        return new PrismReferenceWrapperModel<>(parent, ItemPath.create(path), true);
    }



    @Override
    public PrismReferenceWrapper<R> getObject() {
        return getItemWrapper(PrismReferenceWrapper.class);
    }



}
