/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeOption;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateEnum;

/** Resolves the authority, label, icon, and tooltip from mapping strength and range. */
public final class MappingAuthorityDisplayResolver {

    private MappingAuthorityDisplayResolver() {
    }

    public static DisplayType resolveDisplay(MappingType mapping, String additionalCss) {
        MappingStrengthType strength = mapping != null && mapping.getStrength() != null
                ? mapping.getStrength() : MappingStrengthType.NORMAL;
        MappingRangeOption range = MappingRangeOption.of(
                mapping != null && mapping.getTarget() != null ? mapping.getTarget().getSet() : null);

        MappingAuthority authority = resolveAuthority(strength, range);
        String icon = authority.getIconCss() + " text-muted";

        String rangeKey = "MappingAuthorityDisplayResolver.range." + (range != null ? range.name() : "unspecified");
        return new DisplayType()
                .tooltip(translate(authority.getTooltipKey(), new Object[] {
                        translateEnum(strength), translate(rangeKey) }))
                .beginIcon()
                .cssClass(icon + (additionalCss != null && !additionalCss.isBlank() ? " " + additionalCss : ""))
                .end();
    }

    /** Returns the localized authority label for the mapping. */
    public static String resolveLabel(MappingType mapping) {
        return translate(resolveAuthority(mapping).getLabelKey());
    }

    /** Returns the authority of the mapping, defaulting missing strength to normal. */
    public static MappingAuthority resolveAuthority(MappingType mapping) {
        MappingStrengthType strength = mapping != null && mapping.getStrength() != null
                ? mapping.getStrength() : MappingStrengthType.NORMAL;
        MappingRangeOption range = MappingRangeOption.of(
                mapping != null && mapping.getTarget() != null ? mapping.getTarget().getSet() : null);
        return resolveAuthority(strength, range);
    }

    private static MappingAuthority resolveAuthority(MappingStrengthType strength, MappingRangeOption range) {
        if (strength == MappingStrengthType.WEAK) {
            return MappingAuthority.INITIAL_VALUE;
        }
        if (strength == MappingStrengthType.STRONG) {
            if (range == MappingRangeOption.ALL) {
                return MappingAuthority.FULLY_AUTHORITATIVE;
            }
            if (range == MappingRangeOption.MATCHING_PROVENANCE || range == MappingRangeOption.CONDITION) {
                return MappingAuthority.SELECTIVELY_AUTHORITATIVE;
            }
        }
        return MappingAuthority.NOT_ENFORCING;
    }
}
