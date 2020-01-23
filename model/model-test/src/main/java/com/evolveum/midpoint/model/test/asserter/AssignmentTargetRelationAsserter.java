/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author semancik
 *
 */
public class AssignmentTargetRelationAsserter<RA> extends AbstractAsserter<RA> {

    private final AssignmentObjectRelation assignmentTargetRelation;

    public AssignmentTargetRelationAsserter(AssignmentObjectRelation assignmentTargetRelation, RA returnAsserter, String desc) {
        super(returnAsserter, desc);
        this.assignmentTargetRelation = assignmentTargetRelation;
    }

    public AssignmentTargetRelationAsserter<RA> assertDescription(String expected) {
        assertEquals("Wrong description in "+desc(), expected, assignmentTargetRelation.getDescription());
        return this;
    }

    public AssignmentTargetRelationAsserter<RA> assertObjectType(QName expected) {
        List<QName> objectTypes = assignmentTargetRelation.getObjectTypes();
        if (objectTypes == null || objectTypes.isEmpty()) {
            fail("No object types in "+desc());
        }
        if (objectTypes.size() > 1) {
            fail("Too many object types in "+desc());
        }
        assertEquals("Wrong object type in "+desc(), expected, objectTypes.get(0));
        return this;
    }

    public AssignmentTargetRelationAsserter<RA> assertNoArchetype() {
        assertNotNull("Unexpected archetype references in "+desc()+": "+assignmentTargetRelation.getArchetypeRefs(), assignmentTargetRelation.getArchetypeRefs());
        return this;
    }

    public AssignmentTargetRelationAsserter<RA> assertArchetypeOid(String expected) {
        List<ObjectReferenceType> archetypeRefs = assignmentTargetRelation.getArchetypeRefs();
        if (archetypeRefs == null || archetypeRefs.isEmpty()) {
            fail("No archetype refs in "+desc());
        }
        if (archetypeRefs.size() > 1) {
            fail("Too many archetype refs in "+desc());
        }
        assertEquals("Wrong archetype ref in "+desc(), expected, archetypeRefs.get(0).getOid());
        return this;
    }
    // TODO

    @Override
    protected String desc() {
        return descWithDetails(assignmentTargetRelation);
    }

}
