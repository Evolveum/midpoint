/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
