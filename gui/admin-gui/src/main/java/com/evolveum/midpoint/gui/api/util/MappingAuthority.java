/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.api.util;

/** Authority of a mapping as displayed in the GUI. */
public enum MappingAuthority {

    FULLY_AUTHORITATIVE("MappingAuthorityDisplayResolver.fullyAuthoritative",
            "MappingAuthorityDisplayResolver.fullyAuthoritative.label", "fas fa-circle"),
    INITIAL_VALUE("MappingAuthorityDisplayResolver.initialValue",
            "MappingAuthorityDisplayResolver.initialValue.label", "far fa-circle"),
    SELECTIVELY_AUTHORITATIVE("MappingAuthorityDisplayResolver.selectivelyAuthoritative",
            "MappingAuthorityDisplayResolver.selectivelyAuthoritative.label", "fer fe-bullseye"),
    NOT_ENFORCING("MappingAuthorityDisplayResolver.notEnforcing",
            "MappingAuthorityDisplayResolver.notEnforcing.label", "fer fe-circle-dashed");

    private final String tooltipKey;
    private final String labelKey;
    private final String iconCss;

    MappingAuthority(String tooltipKey, String labelKey, String iconCss) {
        this.tooltipKey = tooltipKey;
        this.labelKey = labelKey;
        this.iconCss = iconCss;
    }

    public String getTooltipKey() {
        return tooltipKey;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getIconCss() {
        return iconCss;
    }
}
