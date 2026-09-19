/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

/**
 * Severity of one {@link OperationLogEntry}, with prescribed style (icon + css pairing per level)
 */
public enum OperationLogLevel {

    TRACE("fa fa-circle-info text-secondary", "badge text-bg-secondary"),
    DEBUG("fa fa-bug text-info", "badge text-bg-info"),
    INFO("fa fa-circle-info text-primary", "badge text-bg-primary"),
    WARN("fa fa-exclamation-triangle text-warning", "badge text-bg-warning"),
    ERROR("fa fa-exclamation-circle text-danger", "badge text-bg-danger");

    private final String icon;
    private final String css;

    OperationLogLevel(String icon, String css) {
        this.icon = icon;
        this.css = css;
    }

    public String getIcon() {
        return icon;
    }

    public String getCss() {
        return css;
    }
}
