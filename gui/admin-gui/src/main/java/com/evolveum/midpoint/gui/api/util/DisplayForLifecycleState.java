/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.component.Badge;

import org.apache.commons.lang3.StringUtils;

public enum DisplayForLifecycleState {

    ACTIVE("SimulationModePanel.option.active", "colored-form-success"),
    DRAFT("SimulationModePanel.option.draft", "colored-form-secondary"),
    SUSPENDED(null, "colored-form-secondary"),
    FAILED(null, "colored-form-danger"),
    PROPOSED("SimulationModePanel.option.proposed", "colored-form-warning"),
    DEFAULT(null, "colored-form-info");

    private final String label;
    private final String cssClass;

    DisplayForLifecycleState(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public static DisplayForLifecycleState valueOfOrDefault(String name) {
        if (StringUtils.isEmpty(name)) {
            return DisplayForLifecycleState.DEFAULT;
        }

        DisplayForLifecycleState value;
        try {
            value = valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DisplayForLifecycleState.DEFAULT;
        }

        if (value == null) {
            return DisplayForLifecycleState.DEFAULT;
        }

        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }
}
