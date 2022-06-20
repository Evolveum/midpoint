/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.jetbrains.annotations.NotNull;

public class VisibleBehaviour extends VisibleEnableBehaviour {

    private static final long serialVersionUID = 1L;

    public VisibleBehaviour(@NotNull SerializableSupplier<Boolean> visibility) {
        super(visibility);
    }
}
