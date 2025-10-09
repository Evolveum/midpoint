/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.model;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * use pure lambda eg. not new ReadOnlyModel(() -> xxx) but () -> xxx
 * @param <T>
 */
@Deprecated
public class ReadOnlyModel<T> implements IModel<T> {

    @NotNull private final SerializableSupplier<T> objectSupplier;

    public ReadOnlyModel(@NotNull SerializableSupplier<T> objectSupplier) {
        this.objectSupplier = objectSupplier;
    }

    @Override
    public T getObject() {
        return objectSupplier.get();
    }
}
