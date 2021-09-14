/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.model;

import com.evolveum.midpoint.util.Producer;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Loadable model whose object is always not null.
 * Used to reduce checks of the 'model.getObject() != null' kind.
 *
 * TODO remove redundant checks after annotations are checked at runtime (needs to be done in maven build)
 */
public abstract class NonEmptyLoadableModel<T> extends LoadableModel<T> implements NonEmptyModel<T> {

    public NonEmptyLoadableModel(boolean alwaysReload) {
        super(alwaysReload);
    }

    public static <T> NonEmptyLoadableModel<T> create(Producer<T> producer, boolean alwaysReload) {
        return new NonEmptyLoadableModel<T>(alwaysReload) {
            @Override
            protected @NotNull T load() {
                return Objects.requireNonNull(
                        producer.run(),
                        "model object is null");
            }
        };
    }

    @NotNull
    public T getObject() {
        T object = super.getObject();
        if (object == null) {
            throw new IllegalStateException("Model object is null");
        }
        return object;
    }

    public void setObject(@NotNull T object) {
        Validate.notNull(object, "Model object is to be set to null");
        super.setObject(object);
    }

    @NotNull
    abstract protected T load();
}
