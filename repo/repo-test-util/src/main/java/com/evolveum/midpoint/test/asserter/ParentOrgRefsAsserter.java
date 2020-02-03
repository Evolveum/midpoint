/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Note: considered to align this with LinksAsserter into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code.
 *
 * @author semancik
 */
public class ParentOrgRefsAsserter<O extends ObjectType, OA extends PrismObjectAsserter<O, RA>,RA> extends AbstractAsserter<OA> {

    private OA objectAsserter;
    private List<PrismReferenceValue> parentOrgRefs;

    public ParentOrgRefsAsserter(OA objectAsserter) {
        super();
        this.objectAsserter = objectAsserter;
    }

    public ParentOrgRefsAsserter(OA focusAsserter, String details) {
        super(details);
        this.objectAsserter = focusAsserter;
    }

    public static <F extends FocusType> ParentOrgRefsAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
        return new ParentOrgRefsAsserter<>(FocusAsserter.forFocus(focus));
    }

    PrismObject<OrgType> getRefTarget(String oid) throws ObjectNotFoundException, SchemaException {
        return objectAsserter.getCachedObject(OrgType.class, oid);
    }

    List<PrismReferenceValue> getRefs() {
        if (parentOrgRefs == null) {
            PrismReference linkRef = getFocus().findReference(FocusType.F_PARENT_ORG_REF);
            if (linkRef == null) {
                parentOrgRefs = new ArrayList<>();
            } else {
                parentOrgRefs = linkRef.getValues();
            }
        }
        return parentOrgRefs;
    }

    public ParentOrgRefsAsserter<O, OA, RA> assertRefs(int expected) {
        assertEquals("Wrong number of parentOrgRefs in " + desc(), expected, getRefs().size());
        return this;
    }

    public ParentOrgRefsAsserter<O, OA, RA> assertNone() {
        assertRefs(0);
        return this;
    }

    public ParentOrgRefsAsserter<O, OA, RA> assertRefs(String... expectedOids) {
        PrismAsserts.assertEqualsCollectionUnordered("Wrong parentOrgRefs in " + desc(), getOids(), expectedOids);
        return this;
    }

    ParentOrgRefAsserter<ParentOrgRefsAsserter<O, OA, RA>> forRef(PrismReferenceValue refVal, PrismObject<OrgType> target) {
        ParentOrgRefAsserter<ParentOrgRefsAsserter<O, OA, RA>> asserter = new ParentOrgRefAsserter<>(refVal, target, this, "parentOrgRef in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ParentOrgRefAsserter<ParentOrgRefsAsserter<O, OA, RA>> single() {
        assertRefs(1);
        return forRef(getRefs().get(0), null);
    }

    PrismObject<O> getFocus() {
        return objectAsserter.getObject();
    }

    @Override
    public OA end() {
        return objectAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("parentOrgRefs of "+getFocus());
    }

    public ParentOrgRefFinder<O,OA,RA> by() {
        return new ParentOrgRefFinder<>(this);
    }

    public ParentOrgRefsAsserter<O, OA, RA> hasTarget(String targetOid) throws ObjectNotFoundException, SchemaException {
        return by()
            .targetOid(targetOid)
            .find()
            .end();
    }

    public List<String> getOids() {
        List<String> oids = new ArrayList<>();
        for (PrismReferenceValue ref: getRefs()) {
            oids.add(ref.getOid());
        }
        return oids;
    }

}
