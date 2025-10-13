/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class ArchetypeRefAsserter<R> extends ObjectReferenceAsserter<ArchetypeType, R> {

    public ArchetypeRefAsserter(PrismReferenceValue refVal) {
        super(refVal, ArchetypeType.class);
    }

    public ArchetypeRefAsserter(PrismReferenceValue refVal, String detail) {
        super(refVal, ArchetypeType.class, detail);
    }

    public ArchetypeRefAsserter(PrismReferenceValue refVal, PrismObject<? extends ArchetypeType> resolvedTarget, R returnAsserter, String detail) {
        super(refVal, ArchetypeType.class, resolvedTarget, returnAsserter, detail);
    }

    @Override
    public ArchetypeRefAsserter<R> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public ArchetypeRefAsserter<R> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public ArchetypeRefAsserter<R> assertOidDifferentThan(String expected) {
        super.assertOidDifferentThan(expected);
        return this;
    }

    @Override
    public FocusAsserter<ArchetypeType, ArchetypeRefAsserter<R>> target()
            throws ObjectNotFoundException, SchemaException {
        return new FocusAsserter<>(getResolvedTarget(), this, "archetype resolved from " + desc());
    }

    @Override
    public FocusAsserter<ArchetypeType, ArchetypeRefAsserter<R>> resolveTarget()
            throws ObjectNotFoundException, SchemaException {
        PrismObject<ArchetypeType> object = resolveTargetObject();
        return new FocusAsserter<>(object, this, "archetype resolved from " + desc());
    }
}
