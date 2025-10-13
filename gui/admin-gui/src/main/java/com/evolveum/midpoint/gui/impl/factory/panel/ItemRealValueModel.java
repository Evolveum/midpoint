/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

public class ItemRealValueModel<T> extends PropertyModel<T>{

    private static final long serialVersionUID = 1L;


    public ItemRealValueModel(IModel<? extends PrismValueWrapper<T>> modelObject) {
        super(modelObject, "realValue");
    }

    @Override
    public void setObject(T object) {
        super.setObject(object);
    }
}
