/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;

/**
 * @author semancik
 * @see TestCompare
 */
public class TestEquals extends AbstractPrismTest {

    @Test
    public void testContainsEquivalentValue01() throws Exception {
        // GIVEN

        ObjectDelta<UserType> userDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(UserType.class, USER_FOO_OID,
                        UserType.F_ASSIGNMENT,
                        createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null));
        display("userDelta", userDelta);

        PrismObject<UserType> user = createUserFoo();
        addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
        addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
        display("user", user);

        PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);

        // WHEN, THEN
        when();
        assertTrue(ASSIGNMENT_PATLAMA_ID + ":null", assignmentContainer.containsEquivalentValue(createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null)));
        assertTrue("null:" + ASSIGNMENT_PATLAMA_DESCRIPTION, assignmentContainer.containsEquivalentValue(createAssignmentValue(null, ASSIGNMENT_PATLAMA_DESCRIPTION)));
        assertFalse("364576:null", assignmentContainer.containsEquivalentValue(createAssignmentValue(364576L, null)));
        assertFalse("null:never ever never", assignmentContainer.containsEquivalentValue(createAssignmentValue(null, "never ever never")));
    }

    @Test(enabled = false)                // normalization no longer removes empty values
    public void testEqualsBrokenAssignmentActivation() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = PrismInternalTestUtil.getUserTypeDefinition();
        PrismContainerDefinition<AssignmentType> assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> goodAssignment = assignmentDef.instantiate(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> brokenAssignment = goodAssignment.clone();
        assertEquals("Not equals after clone", goodAssignment, brokenAssignment);
        // lets break one of these ...
        PrismContainerValue<AssignmentType> emptyValue = getPrismContext().itemFactory().createContainerValue();
        brokenAssignment.add(emptyValue);

        // WHEN
        assertFalse("Unexpected equals", goodAssignment.equals(brokenAssignment));

        brokenAssignment.normalize();
        assertEquals("Not equals after normalize(bad)", goodAssignment, brokenAssignment);

        goodAssignment.normalize();
        assertEquals("Not equals after normalize(good)", goodAssignment, brokenAssignment);

    }
}
