/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.range;

import java.io.Serial;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

/**
 * Range of a mapping, which the schema keeps in the set of the mapping target.
 *
 *
 * @author jjarabinec
 */
public class MappingRangeModel implements IModel<ValueSetDefinitionType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MappingRangeModel.class);

    private final IModel<PrismContainerValueWrapper<MappingType>> mappingModel;

    public MappingRangeModel(IModel<PrismContainerValueWrapper<MappingType>> mappingModel) {
        this.mappingModel = mappingModel;
    }

    @Override
    public ValueSetDefinitionType getObject() {
        VariableBindingDefinitionType target = getTarget();
        return target != null ? target.getSet() : null;
    }

    @Override
    public void setObject(ValueSetDefinitionType set) {
        PrismValueWrapper<VariableBindingDefinitionType> value = findTargetValue();
        if (value == null) {
            LOGGER.error("Target of the mapping {} not found, there is nothing to store the range into", getMapping());
            return;
        }

        VariableBindingDefinitionType current = value.getRealValue();
        VariableBindingDefinitionType updated =
                current != null ? current.clone() : new VariableBindingDefinitionType();

        updated.setSet(isEmpty(set) ? null : set);
        value.setRealValue(updated);
    }

    private static boolean isEmpty(ValueSetDefinitionType set) {
        return set == null
                || (set.getPredefined() == null
                && ExpressionUtil.isEmpty(set.getCondition())
                && ExpressionUtil.isEmpty(set.getYieldCondition())
                && set.getAdditionalMappingSpecification().isEmpty());
    }

    private VariableBindingDefinitionType getTarget() {
        MappingType mapping = getMapping();
        return mapping != null ? mapping.getTarget() : null;
    }

    private MappingType getMapping() {
        PrismContainerValueWrapper<MappingType> mappingValue = mappingModel.getObject();
        return mappingValue != null ? mappingValue.getRealValue() : null;
    }

    private PrismValueWrapper<VariableBindingDefinitionType> findTargetValue() {
        PrismContainerValueWrapper<MappingType> mappingValue = mappingModel.getObject();
        if (mappingValue == null) {
            return null;
        }

        try {
            PrismPropertyWrapper<VariableBindingDefinitionType> wrapper =
                    mappingValue.findProperty(MappingType.F_TARGET);
            return wrapper != null ? wrapper.getValue() : null;
        } catch (SchemaException ex) {
            LOGGER.warn("Couldn't find the target of the mapping {}: {}", getMapping(), ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void detach() {
        mappingModel.detach();
    }
}
