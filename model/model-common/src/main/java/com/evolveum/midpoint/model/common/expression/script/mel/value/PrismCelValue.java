/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.prism.PrismValue;

import dev.cel.common.types.OpaqueType;
import dev.cel.common.values.OpaqueValue;

/**
 * Carries a {@link PrismValue}, currently without exposing its members.
 */
public class PrismCelValue extends OpaqueValue implements MidPointValueProducer<PrismValue> {

    public static final OpaqueType CEL_TYPE = OpaqueType.create(PrismCelValue.class.getName());

    private final PrismValue prismValue;

    private PrismCelValue(PrismValue prismValue) {
        this.prismValue = prismValue;
    }

    public static PrismCelValue create(PrismValue prismValue) {
        return new PrismCelValue(prismValue);
    }

    @Override
    public PrismCelValue value() {
        return this;
    }

    @Override
    public OpaqueType celType() {
        return CEL_TYPE;
    }

    @Override
    public PrismValue getJavaValue() {
        return prismValue;
    }
}
