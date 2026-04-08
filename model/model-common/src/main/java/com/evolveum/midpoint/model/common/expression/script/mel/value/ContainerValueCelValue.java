/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.model.common.expression.script.mel.DynType;
import com.evolveum.midpoint.prism.*;

import dev.cel.common.types.CelType;

/**
 * @author Radovan Semancik
 */
public class ContainerValueCelValue<C extends Containerable> extends AbstractContainerValueCelValue<C> implements MidPointValueProducer<PrismContainerValue<C>> {

    public static final String CEL_TYPE_NAME = PrismContainerValue.class.getName();
    public static final CelType CEL_TYPE = new DynType(CEL_TYPE_NAME);

    ContainerValueCelValue(PrismContainerValue<C> containerValue) {
        super(containerValue);
    }

    public static <C extends Containerable> ContainerValueCelValue<C> create(PrismContainerValue<C> containerValue) {
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
}
