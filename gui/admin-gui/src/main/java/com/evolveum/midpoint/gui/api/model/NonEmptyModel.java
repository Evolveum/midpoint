/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
