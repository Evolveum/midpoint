/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperModel<C extends Containerable, T> extends ItemWrapperModel<C, PrismPropertyWrapper<T>>{

    private static final long serialVersionUID = 1L;

    PrismPropertyWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerWrapper) {
        super(parent, path, fromContainerWrapper);
    }

    public static <C extends Containerable, T> PrismPropertyWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemPath path) {
        return new PrismPropertyWrapperModel<C,T>(parent, path, false);
    }

    public static <C extends Containerable, T> PrismPropertyWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemName path) {
        return new PrismPropertyWrapperModel<C,T>(parent, ItemPath.create(path), false);
    }

    public static <C extends Containerable, T> PrismPropertyWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemPath path) {
        return new PrismPropertyWrapperModel<>(parent, path, true);
    }

    public static <C extends Containerable, T> PrismPropertyWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemName path) {
        return new PrismPropertyWrapperModel<>(parent, ItemPath.create(path), true);
    }




    @Override
    public PrismPropertyWrapper<T> getObject() {
        PrismPropertyWrapper<T> ret = getItemWrapper(PrismPropertyWrapper.class);
        return ret;
    }

}
