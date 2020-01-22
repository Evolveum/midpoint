/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.model;


import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
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
