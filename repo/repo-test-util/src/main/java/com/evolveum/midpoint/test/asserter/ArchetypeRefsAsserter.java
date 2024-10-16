/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class ArchetypeRefsAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {

    private FA focusAsserter;
    private List<PrismReferenceValue> archetypeRefs;

    public ArchetypeRefsAsserter(FA focusAsserter) {
        super();
        this.focusAsserter = focusAsserter;
    }

    public ArchetypeRefsAsserter(FA focusAsserter, String details) {
        super(details);
        this.focusAsserter = focusAsserter;
    }

    public static <F extends FocusType> ArchetypeRefsAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
        return new ArchetypeRefsAsserter<>(FocusAsserter.forFocus(focus));
    }

    List<PrismReferenceValue> getArchetypeRefs() {
        if (archetypeRefs == null) {
            PrismReference linkRef = getFocus().findReference(FocusType.F_ARCHETYPE_REF);
            if (linkRef == null) {
                archetypeRefs = new ArrayList<>();
            } else {
                archetypeRefs = linkRef.getValues();
            }
        }
        return archetypeRefs;
    }

    public ArchetypeRefsAsserter<F, FA, RA> assertArchetypeRefs(int expected) {
        assertEquals("Wrong number of archetypeRefs in " + desc(), expected, getArchetypeRefs().size());
        return this;
    }

    public ArchetypeRefsAsserter<F, FA, RA> assertNone() {
        assertArchetypeRefs(0);
        return this;
    }

    ArchetypeRefAsserter<ArchetypeRefsAsserter<F, FA, RA>> forRef(PrismReferenceValue refVal, PrismObject<? extends ArchetypeType> target) {
        ArchetypeRefAsserter<ArchetypeRefsAsserter<F, FA, RA>> asserter = new ArchetypeRefAsserter<>(refVal, target, this, "archetypeRefs in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ArchetypeRefAsserter<ArchetypeRefsAsserter<F, FA, RA>> single() {
        assertArchetypeRefs(1);
        return forRef(getArchetypeRefs().get(0), null);
    }

    PrismObject<F> getFocus() {
        return focusAsserter.getObject();
    }

    @Override
    public FA end() {
        return focusAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("archetypeRefs of "+getFocus());
    }

    public ArchetypeRefFinder<F,FA,RA> by() {
        return new ArchetypeRefFinder<>(this);
    }

    public ArchetypeRefsAsserter<F,FA,RA> assertArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
            .targetOid(archetypeOid)
            .targetType(ArchetypeType.COMPLEX_TYPE)
            .find();
        return this;
    }

    public ArchetypeRefsAsserter<F,FA,RA> assertNoArchetype(String archetypeOid) throws ObjectNotFoundException, SchemaException {
        by()
                .targetOid(archetypeOid)
                .targetType(ArchetypeType.COMPLEX_TYPE)
                .assertNone();
        return this;
    }

}
