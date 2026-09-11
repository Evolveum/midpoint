/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.range;

import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionPredefinedType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;

/**
 * Range of a mapping as it is offered by the GUI.
 *
 * @author jjarabinec
 */
public enum MappingRangeOption {

    ALL(ValueSetDefinitionPredefinedType.ALL),
    MATCHING_PROVENANCE(ValueSetDefinitionPredefinedType.MATCHING_PROVENANCE),

    CONDITION(null),

    NONE(ValueSetDefinitionPredefinedType.NONE);

    private final ValueSetDefinitionPredefinedType predefined;

    MappingRangeOption(ValueSetDefinitionPredefinedType predefined) {
        this.predefined = predefined;
    }

    public ValueSetDefinitionPredefinedType getPredefined() {
        return predefined;
    }

    /**
     * Reads the option out of the set of a mapping target.
     *
     * @param set set to read, null when the mapping has no range.
     * @return the option the set stands for, or null when it stands for none of them.
     */
    public static MappingRangeOption of(ValueSetDefinitionType set) {
        if (set == null) {
            return null;
        }

        if (!ExpressionUtil.isEmpty(set.getCondition())) {
            return CONDITION;
        }

        ValueSetDefinitionPredefinedType predefined = set.getPredefined();
        if (predefined == null) {
            return null;
        }

        return switch (predefined) {
            case ALL -> ALL;
            case MATCHING_PROVENANCE -> MATCHING_PROVENANCE;
            case NONE -> NONE;
        };
    }
}
