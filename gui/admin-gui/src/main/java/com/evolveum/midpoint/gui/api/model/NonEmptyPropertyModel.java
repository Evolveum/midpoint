/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.model;


import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public class NonEmptyPropertyModel<T> extends PropertyModel<T> implements NonEmptyModel<T> {

    public NonEmptyPropertyModel(Object modelObject, String expression) {
        super(modelObject, expression);
    }

    @Override
    @NotNull
    public T getObject() {
        return super.getObject();
    }

    @Override
    public void setObject(@NotNull T object) {
        super.setObject(object);
    }
}
