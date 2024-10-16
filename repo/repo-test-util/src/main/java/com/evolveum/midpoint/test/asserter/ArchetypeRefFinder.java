/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Note: considered to align this with ParentOrgRefFinder into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code.
 *
 */
public class ArchetypeRefFinder<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> {

    private final ArchetypeRefsAsserter<F,FA,RA> refsAsserter;
    private String targetOid;
    private QName targetType;
    private QName relation;

    public ArchetypeRefFinder(ArchetypeRefsAsserter<F,FA,RA> refsAsserter) {
        this.refsAsserter = refsAsserter;
    }

    public ArchetypeRefFinder<F,FA,RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public ArchetypeRefFinder<F,FA,RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public ArchetypeRefFinder<F,FA,RA> relation(QName relation) {
        this.relation = relation;
        return this;
    }

    public ArchetypeRefAsserter<ArchetypeRefsAsserter<F, FA, RA>> find() throws ObjectNotFoundException, SchemaException {
        PrismReferenceValue found = null;
        for (PrismReferenceValue ref: refsAsserter.getArchetypeRefs()) {
            if (matches(ref)) {
                if (found == null) {
                    found = ref;
                } else {
                    fail("Found more than one archetypeRefs that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no archetypeRefs that matches search criteria");
        }
        return refsAsserter.forRef(found, null);
    }

    public ArchetypeRefsAsserter<F,FA,RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (PrismReferenceValue ref: refsAsserter.getArchetypeRefs()) {
            if (matches(ref)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of archetypeRefs for specified criteria in "+refsAsserter.desc(), expectedCount, foundCount);
        return refsAsserter;
    }

    public ArchetypeRefsAsserter<F,FA,RA> assertNone() throws ObjectNotFoundException, SchemaException {
        for (PrismReferenceValue ref: refsAsserter.getArchetypeRefs()) {
            if (matches(ref)) {
                fail("Found assignment archetypeRefs while not expecting it: "+ref);
            }
        }
        return refsAsserter;
    }

    public ArchetypeRefsAsserter<F,FA,RA> assertAll() throws ObjectNotFoundException, SchemaException {
        for (PrismReferenceValue ref: refsAsserter.getArchetypeRefs()) {
            if (!matches(ref)) {
                fail("Found assignment archetypeRefs that does not match search criteria: "+ref);
            }
        }
        return refsAsserter;
    }

    private boolean matches(PrismReferenceValue refVal) throws ObjectNotFoundException, SchemaException {

        if (targetOid != null) {
            if (!targetOid.equals(refVal.getOid())) {
                return false;
            }
        }

        if (relation != null) {
            if (!QNameUtil.match(relation, refVal.getRelation())) {
                return false;
            }
        }

        if (targetType != null) {
            if (!QNameUtil.match(targetType, refVal.getTargetType())) {
                return false;
            }
        }

        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
