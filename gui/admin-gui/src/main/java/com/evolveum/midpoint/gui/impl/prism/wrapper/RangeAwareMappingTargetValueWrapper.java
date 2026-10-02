/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeOption;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

/** Applies cardinality-based defaults to missing, all, or provenance ranges, preserving custom ranges. */
public class RangeAwareMappingTargetValueWrapper
        extends PrismPropertyValueWrapper<VariableBindingDefinitionType> {

    @Serial private static final long serialVersionUID = 1L;

    public RangeAwareMappingTargetValueWrapper(
            PrismPropertyWrapper<VariableBindingDefinitionType> parent,
            PrismPropertyValue<VariableBindingDefinitionType> value, ValueStatus status) {
        super(parent, value, status);
        VariableBindingDefinitionType target = getRealValue();
        if (shouldApplyDefaultRange(target)) {
            target.setSet(defaultRange(target));
        }

    }

    @Override
    public void setRealValue(VariableBindingDefinitionType target) {
        if (shouldApplyDefaultRange(target)) {
            target = target.clone();
            target.setSet(defaultRange(target));
        }
        super.setRealValue(target);
    }

    private boolean shouldApplyDefaultRange(VariableBindingDefinitionType target) {
        if (target == null) {
            return false;
        }
        if (target.getSet() == null) {
            return true;
        }

        MappingRangeOption option = MappingRangeOption.of(target.getSet());
        return option == MappingRangeOption.ALL || option == MappingRangeOption.MATCHING_PROVENANCE;
    }

    @SuppressWarnings("unchecked")
    private ValueSetDefinitionType defaultRange(VariableBindingDefinitionType target) {
        PrismPropertyWrapper<VariableBindingDefinitionType> property = getParent();
        return MappingRangeUtils.defaultRange(
                (PrismContainerValueWrapper<MappingType>) property.getParent(), target);
    }

}
