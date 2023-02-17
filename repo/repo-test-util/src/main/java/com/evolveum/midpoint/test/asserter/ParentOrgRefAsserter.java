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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 */
public class ParentOrgRefAsserter<R> extends ObjectReferenceAsserter<OrgType, R> {

    public ParentOrgRefAsserter(PrismReferenceValue refVal) {
        super(refVal, OrgType.class);
    }

    public ParentOrgRefAsserter(PrismReferenceValue refVal, String detail) {
        super(refVal, OrgType.class, detail);
    }

    public ParentOrgRefAsserter(PrismReferenceValue refVal, PrismObject<OrgType> resolvedTarget, R returnAsserter, String detail) {
        super(refVal, OrgType.class, resolvedTarget, returnAsserter, detail);
    }

    @Override
    public ParentOrgRefAsserter<R> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public ParentOrgRefAsserter<R> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public ParentOrgRefAsserter<R> assertOidDifferentThan(String expected) {
        super.assertOidDifferentThan(expected);
        return this;
    }

    public ShadowAsserter<ParentOrgRefAsserter<R>> shadow() {
        ShadowAsserter<ParentOrgRefAsserter<R>> asserter =
                new ShadowAsserter<>(getRefVal().getObject(), this, "shadow in reference " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public FocusAsserter<OrgType, ObjectReferenceAsserter<OrgType, R>> target()
            throws ObjectNotFoundException, SchemaException {
        return new FocusAsserter<>(getResolvedTarget(), this, "object resolved from " + desc());
    }

    @Override
    public FocusAsserter<OrgType, ObjectReferenceAsserter<OrgType, R>> resolveTarget()
            throws ObjectNotFoundException, SchemaException {
        PrismObject<OrgType> object = resolveTargetObject();
        return new FocusAsserter<>(object, this, "object resolved from " + desc());
    }
}
