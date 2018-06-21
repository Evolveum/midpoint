/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

/**
 * @see TestCompare
 * @author semancik
 */
public class TestEquals extends AbstractPrismTest {

	@Test
    public void testContainsEquivalentValue01() throws Exception {
		final String TEST_NAME="testContainsEquivalentValue01";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, getPrismContext(), createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null));
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
		addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
		display("user", user);
		
		PrismContainer<AssignmentType> assignmentConatiner = user.findContainer(UserType.F_ASSIGNMENT);

		// WHEN, THEN
		displayWhen(TEST_NAME);
		assertTrue(ASSIGNMENT_PATLAMA_ID+":null", assignmentConatiner.containsEquivalentValue(createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null)));
		assertTrue("null:"+ASSIGNMENT_PATLAMA_DESCRIPTION, assignmentConatiner.containsEquivalentValue(createAssignmentValue(null, ASSIGNMENT_PATLAMA_DESCRIPTION)));
		assertFalse("364576:null", assignmentConatiner.containsEquivalentValue(createAssignmentValue(364576L, null)));
		assertFalse("null:never ever never", assignmentConatiner.containsEquivalentValue(createAssignmentValue(null, "never ever never")));
	}

	@Test(enabled = false)				// normalization no longer removes empty values
	public void testEqualsBrokenAssignmentActivation() throws Exception {
		final String TEST_NAME="testEqualsBrokenAssignmentActivation";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismObjectDefinition<UserType> userDef = PrismInternalTestUtil.getUserTypeDefinition();
		PrismContainerDefinition<AssignmentType> assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> goodAssignment = assignmentDef.instantiate(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> brokenAssignment = goodAssignment.clone();
		assertEquals("Not equals after clone", goodAssignment, brokenAssignment);
		// lets break one of these ...
		PrismContainerValue<AssignmentType> emptyValue = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
		brokenAssignment.add(emptyValue);

		// WHEN
		assertFalse("Unexpected equals", goodAssignment.equals(brokenAssignment));

		brokenAssignment.normalize();
		assertEquals("Not equals after normalize(bad)", goodAssignment, brokenAssignment);

		goodAssignment.normalize();
		assertEquals("Not equals after normalize(good)", goodAssignment, brokenAssignment);

	}
}
