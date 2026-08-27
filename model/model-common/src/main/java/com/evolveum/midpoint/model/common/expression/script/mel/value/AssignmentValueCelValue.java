/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import dev.cel.common.types.CelType;

import com.evolveum.midpoint.model.common.expression.script.mel.DynType;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author Radovan Semancik
 */
public class AssignmentValueCelValue extends ContainerValueCelValue<AssignmentType>  {

    public static final String CEL_TYPE_NAME = AssignmentType.class.getName();
    public static final CelType CEL_TYPE = new DynType(CEL_TYPE_NAME);

    AssignmentValueCelValue(PrismContainerValue<AssignmentType> containerValue) {
        super(containerValue);
    }

//    public static <C extends Containerable> AssignmentValueCelValue create(PrismContainerValue<AssignmentType> containerValue) {
//        return new AssignmentValueCelValue(containerValue);
//    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    @Override
    public PrismContainerValue<AssignmentType> getJavaValue() {
        return getContainerValue();
    }
}
