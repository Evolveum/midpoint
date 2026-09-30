/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.DynType;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import dev.cel.common.types.CelType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Radovan Semancik
 */
public class ContainerValueCelValue<C extends Containerable> extends AbstractContainerValueCelValue<C> implements MidPointValueProducer<PrismContainerValue<C>> {

    public static final String CEL_TYPE_NAME = PrismContainerValue.class.getName();
    public static final CelType CEL_TYPE = new DynType(CEL_TYPE_NAME);

    private static final String F_ID = PrismConstants.T_ID_LOCAL_PART;

    ContainerValueCelValue(PrismContainerValue<C> containerValue) {
        super(containerValue);
    }

    public static <C extends Containerable> ContainerValueCelValue<C> create(PrismContainerValue<C> containerValue) {
        // Ugly, improve later
        if (containerValue.canRepresent(AssignmentType.class)) {
            //noinspection unchecked
            return (ContainerValueCelValue<C>) new AssignmentValueCelValue((PrismContainerValue<AssignmentType>)containerValue);
        }
        return new ContainerValueCelValue<>(containerValue);
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    @Override
    public PrismContainerValue<C> getJavaValue() {
        return getContainerValue();
    }

    @Override
    public Object get(Object key) {
        if (F_ID.equals(key)) {
            return CelTypeMapper.toCelValue(getContainerValue().getId());
        } else {
            return super.get(key);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (F_ID.equals(key)) {
            return true;
        } else {
            return super.containsKey(key);
        }
    }

    @Override
    public @NotNull Set<String> keySet() {
        Set<String> keys = super.keySet();
        keys.add(F_ID);
        return keys;
    }
}
