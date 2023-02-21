/*
 * Copyright (C) 2018-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ShadowReferenceAsserter<R> extends ObjectReferenceAsserter<ShadowType, R> {

    public ShadowReferenceAsserter(PrismReferenceValue refVal) {
        super(refVal, ShadowType.class);
    }

    public ShadowReferenceAsserter(PrismReferenceValue refVal, String detail) {
        super(refVal, ShadowType.class, detail);
    }

    public ShadowReferenceAsserter(PrismReferenceValue refVal, PrismObject<ShadowType> resolvedTarget, R returnAsserter, String detail) {
        super(refVal, ShadowType.class, resolvedTarget, returnAsserter, detail);
    }

    @Override
    public ShadowReferenceAsserter<R> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public ShadowReferenceAsserter<R> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public ShadowReferenceAsserter<R> assertOidDifferentThan(String expected) {
        super.assertOidDifferentThan(expected);
        return this;
    }

    public ShadowAsserter<ShadowReferenceAsserter<R>> shadow() {
        ShadowAsserter<ShadowReferenceAsserter<R>> asserter =
                new ShadowAsserter<>(getRefVal().getObject(), this, "shadow in reference " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public ShadowAsserter<ObjectReferenceAsserter<ShadowType, R>> target()
            throws ObjectNotFoundException, SchemaException {
        return new ShadowAsserter<>(getResolvedTarget(), this, "object resolved from " + desc());
    }

    @Override
    public ShadowAsserter<ObjectReferenceAsserter<ShadowType, R>> resolveTarget()
            throws ObjectNotFoundException, SchemaException {
        PrismObject<ShadowType> object = resolveTargetObject();
        return new ShadowAsserter<>(object, this, "object resolved from " + desc());
    }
}
