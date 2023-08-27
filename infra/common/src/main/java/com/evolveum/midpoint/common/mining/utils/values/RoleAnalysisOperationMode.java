/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils.values;

import java.io.Serializable;

public enum RoleAnalysisOperationMode implements Serializable {

    NEUTRAL("fa fa-plus"),
    ADD("fa fa-minus"),
    REMOVE("fa fa-undo"),
    DISABLE("fa fa-ban");

    private final String displayString;

    RoleAnalysisOperationMode(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }
}
