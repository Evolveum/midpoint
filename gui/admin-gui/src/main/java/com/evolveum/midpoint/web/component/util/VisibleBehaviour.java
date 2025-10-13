/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import org.jetbrains.annotations.NotNull;

public class VisibleBehaviour extends VisibleEnableBehaviour {

    private static final long serialVersionUID = 1L;

    public VisibleBehaviour(@NotNull SerializableSupplier<Boolean> visibility) {
        super(visibility);
    }
}
