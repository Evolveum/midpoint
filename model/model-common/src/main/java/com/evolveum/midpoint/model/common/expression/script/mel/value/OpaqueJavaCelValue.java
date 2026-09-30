/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import dev.cel.common.types.OpaqueType;
import dev.cel.common.values.OpaqueValue;

/**
 * Safely carries an otherwise unsupported Java value through MEL without exposing its members.
 */
public class OpaqueJavaCelValue extends OpaqueValue implements MidPointValueProducer<Object> {

    public static final OpaqueType CEL_TYPE = OpaqueType.create(OpaqueJavaCelValue.class.getName());

    private final Object javaValue;

    private OpaqueJavaCelValue(Object javaValue) {
        this.javaValue = javaValue;
    }

    public static OpaqueJavaCelValue create(Object javaValue) {
        return new OpaqueJavaCelValue(javaValue);
    }

    @Override
    public OpaqueJavaCelValue value() {
        return this;
    }

    @Override
    public OpaqueType celType() {
        return CEL_TYPE;
    }

    @Override
    public Object getJavaValue() {
        return javaValue;
    }
}
