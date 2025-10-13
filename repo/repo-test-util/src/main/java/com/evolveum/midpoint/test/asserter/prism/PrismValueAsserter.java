/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public abstract class PrismValueAsserter<V extends PrismValue, RA> extends AbstractAsserter<RA> {

    private final V prismValue;

    public PrismValueAsserter(V prismValue) {
        super();
        this.prismValue = prismValue;
    }

    public PrismValueAsserter(V prismValue, String detail) {
        super(detail);
        this.prismValue = prismValue;
    }

    public PrismValueAsserter(V prismValue, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.prismValue = prismValue;
    }

    public V getPrismValue() {
        return prismValue;
    }

    public Object getRealValue() {
        return prismValue != null ? prismValue.getRealValue() : null;
    }

    public <T> T getRealValue(Class<T> type) throws SchemaException {
        return MiscUtil.castSafely(
                getRealValue(), type);
    }

    public @NotNull <T> T getRealValueRequired(Class<T> type) throws SchemaException {
        return MiscUtil.requireNonNull(
                getRealValue(type),
                () -> new AssertionError("Expected a value, but none is present in " + desc()));
    }

    // TODO

    protected String desc() {
        return getDetails();
    }

    public PrismValueAsserter<V, RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(prismValue));
        return this;
    }
}
