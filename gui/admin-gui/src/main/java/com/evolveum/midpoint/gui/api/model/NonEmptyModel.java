/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public interface NonEmptyModel<T> extends IModel<T> {

    @Override
    @NotNull
    T getObject();

    @Override
    void setObject(@NotNull T object);
}
