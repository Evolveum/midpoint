/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;

public interface TileEnum extends Serializable {

    String getIcon();

    default String getDescription() {
        return "";
    }
}
