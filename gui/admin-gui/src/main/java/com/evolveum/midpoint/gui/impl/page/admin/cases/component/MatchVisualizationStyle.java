/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;

import org.jetbrains.annotations.NotNull;

public enum MatchVisualizationStyle {

    NOT_APPLICABLE("bg-info disabled color-palette"),
    NONE("bg-danger disabled color-palette"),
    PARTIAL("bg-warning disabled color-palette"),
    FULL("bg-success disabled color-palette");

    private final String css;

    MatchVisualizationStyle(String css) {
        this.css = css;
    }

    static MatchVisualizationStyle forMatch(@NotNull CorrelationCaseDescription.Match match) {
        return switch (match) {
            case NOT_APPLICABLE -> NOT_APPLICABLE;
            case NONE -> NONE;
            case PARTIAL -> PARTIAL;
            case FULL -> FULL;
        };
    }

    public String getCss() {
        return css;
    }
}
