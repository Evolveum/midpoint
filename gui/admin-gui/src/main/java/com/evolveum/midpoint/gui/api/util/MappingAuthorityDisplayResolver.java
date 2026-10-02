/*
 * Copyright (C) 2026 Evolveum and contributors
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeOption;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateEnum;

/** Resolves the authority icon and tooltip from mapping strength and range. */
public final class MappingAuthorityDisplayResolver {

    private static final String PREFIX = "MappingAuthorityDisplayResolver.";

    private MappingAuthorityDisplayResolver() {
    }

    public static DisplayType resolve(MappingType mapping, String additionalCss) {
        MappingStrengthType strength = mapping != null && mapping.getStrength() != null
                ? mapping.getStrength() : MappingStrengthType.NORMAL;
        MappingRangeOption range = MappingRangeOption.of(
                mapping != null && mapping.getTarget() != null ? mapping.getTarget().getSet() : null);

        String icon = "fer fe-circle-dashed";
        String tooltipKey = "notEnforcing";
        // Weak mappings only provide initial values, regardless of their range.
        if (strength == MappingStrengthType.WEAK) {
            icon = "far fa-circle";
            tooltipKey = "initialValue";
        } else if (strength == MappingStrengthType.STRONG) {
            if (range == MappingRangeOption.ALL) {
                icon = "fas fa-circle";
                tooltipKey = "fullyAuthoritative";
            } else if (range == MappingRangeOption.MATCHING_PROVENANCE || range == MappingRangeOption.CONDITION) {
                icon = "fer fe-bullseye";
                tooltipKey = "selectivelyAuthoritative";
            }
        }

        icon += " text-muted";

        String rangeKey = PREFIX + "range." + (range != null ? range.name() : "unspecified");
        return new DisplayType()
                .tooltip(translate(PREFIX + tooltipKey, new Object[] {
                        translateEnum(strength), translate(rangeKey) }))
                .beginIcon()
                .cssClass(icon + (additionalCss != null && !additionalCss.isBlank() ? " " + additionalCss : ""))
                .end();
    }
}
