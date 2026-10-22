/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.math.BigDecimal;
import java.util.Objects;

import dev.cel.common.types.CelType;
import dev.cel.common.types.OpaqueType;
import dev.cel.common.values.CelValue;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;

/**
 * A wrapper for {@link BigDecimal}.
 *
 * Naive implementation; to be improved later.
 */
public class BigDecimalCelValue extends CelValue implements MidPointValueProducer<BigDecimal>, MelComparable {

    public static final OpaqueType CEL_TYPE = OpaqueType.create(BigDecimalCelValue.class.getName());

    private final BigDecimal value;

    BigDecimalCelValue(BigDecimal value) {
        this.value = value;
    }

    public static BigDecimalCelValue create(BigDecimal value) {
        return new BigDecimalCelValue(value);
    }

    @Override
    public BigDecimal getJavaValue() {
        return value;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public boolean isZeroValue() {
        return BigDecimal.ZERO.equals(value);
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BigDecimalCelValue that = (BigDecimalCelValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean melEquals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof BigDecimalCelValue q) {
            return Objects.equals(value, q.value);
        }
        return false;
    }
}
