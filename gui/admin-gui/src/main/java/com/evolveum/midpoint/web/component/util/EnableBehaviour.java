/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import org.jetbrains.annotations.NotNull;

public class EnableBehaviour extends VisibleEnableBehaviour {

    private static final long serialVersionUID = 1L;

    public EnableBehaviour(@NotNull SerializableSupplier<Boolean> enabled) {
        super(() -> true, enabled);
    }
}
