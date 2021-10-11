/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class EnableBehaviour extends VisibleEnableBehaviour {

    private static final long serialVersionUID = 1L;

    @NotNull private final SerializableSupplier<Boolean> producer;

    public EnableBehaviour(@NotNull SerializableSupplier<Boolean> producer) {
        this.producer = producer;
    }

    @Override
    public boolean isEnabled() {
        return producer.get();
    }
}
