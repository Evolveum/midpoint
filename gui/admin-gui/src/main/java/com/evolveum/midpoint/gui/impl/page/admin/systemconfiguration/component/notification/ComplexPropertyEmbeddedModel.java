/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import java.io.Serializable;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ComplexPropertyEmbeddedModel<O extends Serializable, T extends Serializable> implements IModel<T> {

    private IModel<O> model;

    private SerializableFunction<O, T> get;

    private SerializableBiConsumer<O, T> set;

    public ComplexPropertyEmbeddedModel(@NotNull IModel<O> model, @NotNull SerializableFunction<O, T> get, @NotNull SerializableBiConsumer<O, T> set) {
        this.model = model;
        this.get = get;
        this.set = set;
    }

    @Override
    public T getObject() {
        O object = model.getObject();
        if (object == null) {
            return null;
        }

        return get.apply(object);
    }

    @Override
    public void setObject(T object) {
        O parent = model.getObject();

        if (object == null && parent == null) {
            return;
        }

        if (parent == null) {
            parent = createEmptyParentObject();
            model.setObject(parent);
        }

        if (parent == null) {
            return;
        }

        set.accept(parent, object);

        if (isParentModelObjectEmpty(parent)) {
            model.setObject(null);
        }
    }

    protected O createEmptyParentObject() {
        return null;
    }

    protected boolean isParentModelObjectEmpty(O object) {
        return false;
    }
}
