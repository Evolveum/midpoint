/*
 * Copyright (c) 2014 Evolveum
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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;

import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.display;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

/**
 * Test delta operation on real midpoint schema. Similar to TestDelta in prism, but this is using the
 * real thing and not just testing schema.
 *
 * @author Radovan Semancik
 */
public class TestSchemaDelta extends AbstractSchemaTest {

    @Test
    public void testAssignmentSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testAssignmentSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

		//Delta
    	PrismContainerValue<AssignmentType> assignmentValue = new PrismContainerValue<>(getPrismContext());
    	// The value id is null
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "jamalalicha patlama paprtala", getPrismContext());

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_JACK_OID,
				UserType.F_ASSIGNMENT, getPrismContext(), assignmentValue);

		// WHEN
        userDelta.applyTo(user);

        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_JACK_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 2, assignment.size());
    }

    @Test
    public void testAddInducementConstructionSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testAddInducementConstructionSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
    	PrismContainerValue<AssignmentType> inducementValue = new PrismContainerValue<>(getPrismContext());
    	// The value id is null
    	inducementValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "jamalalicha patlama paprtala", getPrismContext());

		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
				RoleType.F_INDUCEMENT, getPrismContext(), inducementValue);

		// WHEN
        roleDelta.applyTo(role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", assignment);
        assertEquals("Unexpected number of inducement values", 2, assignment.size());
    }

    @Test
    public void testDeleteInducementValidIdSameValueApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdSameValueApplyToObject";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(ROLE_CONSTRUCTION_RESOURCE_OID);
		construction.setResourceRef(resourceRef);
		AssignmentType inducement = new AssignmentType();
		inducement.setConstruction(construction);
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
        		RoleType.F_INDUCEMENT, getPrismContext(), inducement);

		// WHEN
		roleDelta.applyTo(role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }

    @Test
    public void testDeleteInducementValidIdEmptyValueApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdEmptyValueApplyToObject";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		AssignmentType inducement = new AssignmentType();
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
        		RoleType.F_INDUCEMENT, getPrismContext(), inducement);

		// WHEN
		roleDelta.applyTo(role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }

    @Test
    public void testDeleteInducementValidIdEmptyValueApplyToObjectStatic() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdEmptyValueApplyToObjectStatic";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);
		System.out.println("Role before delta application:");
        System.out.println(role.debugDump());

		//Delta
		AssignmentType inducement = new AssignmentType();
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
        		RoleType.F_INDUCEMENT, getPrismContext(), inducement);

		// WHEN
        PropertyDelta.applyTo(roleDelta.getModifications(), role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }

    @Test
    public void testDeleteInducementConstructionSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementConstructionSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(ROLE_CONSTRUCTION_RESOURCE_OID);
        resourceRef.setType(ObjectTypes.RESOURCE.getTypeQName());
		construction.setResourceRef(resourceRef);
		// No container ID
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
        		new ItemPath(
        				new NameItemPathSegment(RoleType.F_INDUCEMENT),
        				new IdItemPathSegment(ROLE_CONSTRUCTION_INDUCEMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
        		getPrismContext(), construction);

		// WHEN
		roleDelta.applyTo(role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> inducementContainer = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", inducementContainer);
        assertEquals("Unexpected number of inducement values", 1, inducementContainer.size());
        PrismContainerValue<AssignmentType> inducementValue = inducementContainer.getValues().iterator().next();
        AssignmentType inducement = inducementValue.asContainerable();
        ConstructionType constructionAfter = inducement.getConstruction();
        // construction should be gone (the error is that it is empty and not gone)
        assertNull("Construction is not gone", constructionAfter);

    }

    @Test
    public void testDeleteInducementActivationSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementActivationSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
		// No container ID
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
        		new ItemPath(
        				new NameItemPathSegment(RoleType.F_INDUCEMENT),
        				new IdItemPathSegment(ROLE_CONSTRUCTION_INDUCEMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		getPrismContext(), activationType);

		// WHEN
		roleDelta.applyTo(role);

        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> inducementContainer = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", inducementContainer);
        assertEquals("Unexpected number of inducement values", 1, inducementContainer.size());
        PrismContainerValue<AssignmentType> inducementValue = inducementContainer.getValues().iterator().next();
        AssignmentType inducement = inducementValue.asContainerable();
        ActivationType activation = inducement.getActivation();
        // activation should be gone (the error is that it is empty and not gone)
        assertNull("Activation is not gone", activation);
    }

    @Test
    public void testDeleteUserAssignmentActivationSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteUserAssignmentActivationSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);

    	// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

		//Delta
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
		// No container ID
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_JACK_OID,
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(USER_JACK_ASSIGNMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		getPrismContext(), activationType);

		// WHEN
		userDelta.applyTo(user);

        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_JACK_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_NAME));
        PrismContainer<AssignmentType> assignmentContainer = user.findContainer(RoleType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignmentContainer);
        assertEquals("Unexpected number of assignment values", 1, assignmentContainer.size());
        PrismContainerValue<AssignmentType> assignmentValue = assignmentContainer.getValues().iterator().next();
        AssignmentType assignment = assignmentValue.asContainerable();
        ActivationType assignmentActivation = assignment.getActivation();
        // activation should be gone (the error is that it is empty and not gone)
        assertNull("Assignment activation is not gone", assignmentActivation);

        ActivationType activation = user.asObjectable().getActivation();
        assertNotNull("Activation missing", activation);
        assertEquals("Wrong activation administrativeStatus", ActivationStatusType.ENABLED, activation.getAdministrativeStatus());
    }

    // subtract of single-valued PCV from multivalued one
	@Test
	public void testSubtractAssignmentFromAddDelta() throws Exception {
		final String TEST_NAME = "testSubtractAssignmentFromAddDelta";
		displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
		ObjectDelta<UserType> addDelta = ObjectDelta.createAddDelta(user);

		// WHEN
		PrismContainerDefinition<AssignmentType> assignmentDef = PrismTestUtil.getSchemaRegistry()
				.findContainerDefinitionByCompileTimeClass(AssignmentType.class).clone();
		assignmentDef.setMaxOccurs(1);
		PrismContainer<AssignmentType> assignmentContainer = assignmentDef.instantiate();

		PrismContainerValue<AssignmentType> assignmentValue =
				ObjectTypeUtil.createAssignmentTo("00000001-d34d-b33f-f00d-000000000002", ObjectTypes.ROLE,
						getPrismContext())
				.asPrismContainerValue();
		assignmentContainer.add(assignmentValue);

		System.out.println("Delta before operation:\n" + addDelta.debugDump() + "\n");
		System.out.println("Assignment to subtract:\n" + assignmentValue.debugDump() + "\n");
		boolean removed = addDelta.subtract(SchemaConstants.PATH_ASSIGNMENT, assignmentValue, false, false);

		// THEN
		System.out.println("Delta after operation:\n" + addDelta.debugDump() + "\n");
		System.out.println("Removed: " + removed + "\n");

		assertTrue("Not removed", removed);
		assertTrue("Remaining delta is not an ADD delta", addDelta.isAdd());
		assertEquals("Wrong # of remaining assignments", 2, addDelta.getObjectToAdd().asObjectable().getAssignment().size());
	}

	@Test
	public void testSubtractAssignmentFromModifyDelta() throws Exception {
		final String TEST_NAME = "testSubtractAssignmentFromModifyDelta";
		displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
		user.asObjectable().getAssignment().get(0).setId(9999L);
		AssignmentType assignment9999 = new AssignmentType();
		assignment9999.setId(9999L);
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, getPrismContext())
				.item(UserType.F_ASSIGNMENT).delete(assignment9999)
				.asObjectDeltaCast(user.getOid());

		// WHEN
		PrismContainerDefinition<AssignmentType> assignmentDef = PrismTestUtil.getSchemaRegistry()
				.findContainerDefinitionByCompileTimeClass(AssignmentType.class).clone();
		assignmentDef.setMaxOccurs(1);
		PrismContainer<AssignmentType> assignmentContainer = assignmentDef.instantiate();

		PrismContainerValue<AssignmentType> assignmentValue =
				ObjectTypeUtil.createAssignmentTo("00000001-d34d-b33f-f00d-000000000002", ObjectTypes.ROLE,
						getPrismContext())
						.asPrismContainerValue();
		assignmentValue.setId(9999L);
		assignmentContainer.add(assignmentValue);

		System.out.println("Delta before operation:\n" + delta.debugDump() + "\n");
		System.out.println("Assignment to subtract:\n" + assignmentValue.debugDump() + "\n");
		boolean removed = delta.subtract(SchemaConstants.PATH_ASSIGNMENT, assignmentValue, true, false);

		// THEN
		System.out.println("Delta after operation:\n" + delta.debugDump() + "\n");
		System.out.println("Removed: " + removed + "\n");

		assertTrue("Not removed", removed);
		assertTrue("Remaining delta is not a MODIFY delta", delta.isModify());
		assertEquals("Wrong # of remaining modifications", 0, delta.getModifications().size());
	}

	// subtract of single-valued PCV from multivalued one
	@Test
	public void testFactorAddDeltaForItem() throws Exception {
		final String TEST_NAME = "testFactorAddDeltaForItem";
		displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
		String OID = "user-oid-1";
		user.setOid(OID);
		ObjectDelta<UserType> addDelta = ObjectDelta.createAddDelta(user);

		// WHEN
		ObjectDelta.FactorOutResultSingle<UserType> out = addDelta.factorOut(singleton(new ItemPath(UserType.F_ASSIGNMENT)), true);

		// THEN
		System.out.println("Delta before factorOut:\n" + addDelta.debugDump() + "\n");
		System.out.println("Delta after factorOut:\n" + out.remainder.debugDump() + "\n");
		System.out.println("Offspring delta:\n" + DebugUtil.debugDump(out.offspring) + "\n");

		assertTrue("Remaining delta is not an ADD delta", out.remainder.isAdd());
		assertEquals("Wrong # of remaining assignments", 0, out.remainder.getObjectToAdd().asObjectable().getAssignment().size());
		assertNotNull("Missing offspring delta", out.offspring);
		assertEquals("Wrong # of modifications in offspring", 1, out.offspring.getModifications().size());
		assertEquals("Wrong # of assignments to add", 3, out.offspring.getModifications().iterator().next().getValuesToAdd().size());

		assertDeltaOid(out.remainder, OID);
		assertDeltaOid(out.offspring, OID);
	}

	private void assertDeltaOid(ObjectDelta<?> delta, String expectedOid) {
		assertEquals("Wrong OID in delta: " + delta, expectedOid, delta.getOid());
	}

	// subtract of single-valued PCV from multivalued one
	@Test
	public void testFactorAddDeltaForItems() throws Exception {
		final String TEST_NAME = "testFactorAddDeltaForItems";
		displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
		String OID = "user-oid-1";
		user.setOid(OID);
		ObjectDelta<UserType> addDelta = ObjectDelta.createAddDelta(user);

		// WHEN
		ObjectDelta.FactorOutResultSingle<UserType> out = addDelta.factorOut(asList(new ItemPath(UserType.F_GIVEN_NAME), new ItemPath(UserType.F_FAMILY_NAME)), true);

		// THEN
		System.out.println("Delta before factorOut:\n" + addDelta.debugDump() + "\n");
		System.out.println("Delta after factorOut:\n" + out.remainder.debugDump() + "\n");
		System.out.println("Offspring delta:\n" + DebugUtil.debugDump(out.offspring) + "\n");

		assertTrue("Remaining delta is not an ADD delta", out.remainder.isAdd());
		assertEquals("Wrong # of remaining assignments", 3, out.remainder.getObjectToAdd().asObjectable().getAssignment().size());
		assertNotNull("Missing offspring delta", out.offspring);
		assertEquals("Wrong # of modifications in offspring", 2, out.offspring.getModifications().size());

		assertDeltaOid(out.remainder, OID);
		assertDeltaOid(out.offspring, OID);
	}

	@Test
	public void testFactorAddDeltaForItemValues() throws Exception {
		final String TEST_NAME = "testFactorAddDeltaForItemValues";
		displayTestTile(TEST_NAME);

		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
		String OID = "user-oid-1";
		user.setOid(OID);
		ObjectDelta<UserType> addDelta = ObjectDelta.createAddDelta(user);

		// WHEN
		ObjectDelta.FactorOutResultMulti<UserType> out = addDelta.factorOutValues(new ItemPath(UserType.F_ASSIGNMENT), true);

		// THEN
		System.out.println("Delta before factorOut:\n" + addDelta.debugDump() + "\n");
		System.out.println("Delta after factorOut:\n" + out.remainder.debugDump() + "\n");
		System.out.println("Offspring deltas:\n" + DebugUtil.debugDump(out.offsprings) + "\n");

		assertTrue("Remaining delta is not an ADD delta", out.remainder.isAdd());
		assertEquals("Wrong # of remaining assignments", 0, out.remainder.getObjectToAdd().asObjectable().getAssignment().size());
		assertEquals("Wrong # of offspring deltas", 3, out.offsprings.size());
		assertDeltaOid(out.remainder, OID);
		for (ObjectDelta<UserType> offspring : out.offsprings) {
			assertEquals("Wrong # of modifications in offspring", 1, offspring.getModifications().size());
			assertEquals("Wrong # of assignments to add", 1, offspring.getModifications().iterator().next().getValuesToAdd().size());
			assertDeltaOid(offspring, OID);
		}
	}

	@Test
	public void testFactorModifyDeltaForItem() throws Exception {
		final String TEST_NAME = "testFactorModifyDeltaForItem";
		displayTestTile(TEST_NAME);

		// GIVEN
		String OID = "oid1";
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, getPrismContext())
				.item(UserType.F_ASSIGNMENT)
						.add(ObjectTypeUtil.createAssignmentTo("oid-r", ObjectTypes.ROLE, getPrismContext()))
						.delete(new AssignmentType().id(101L), new AssignmentType().id(102L))
				.item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_LIFECYCLE_STATE).replace("draft")
				.item(UserType.F_GIVEN_NAME).replace("bill")
				.asObjectDeltaCast(OID);

		// WHEN
		ObjectDelta.FactorOutResultSingle<UserType> out = delta.factorOut(singleton(new ItemPath(UserType.F_ASSIGNMENT)), true);

		// THEN
		System.out.println("Delta before operation:\n" + delta.debugDump() + "\n");
		System.out.println("Delta after factorOut:\n" + out.remainder.debugDump() + "\n");
		System.out.println("Offspring delta:\n" + DebugUtil.debugDump(out.offspring) + "\n");

		assertTrue("Remaining delta is not a MODIFY delta", out.remainder.isModify());
		assertEquals("Wrong # of remaining modifications", 1, out.remainder.getModifications().size());
		assertNotNull("Missing offspring delta", out.offspring);
		assertEquals("Wrong # of modifications in offspring", 2, out.offspring.getModifications().size());

		assertDeltaOid(out.remainder, OID);
		assertDeltaOid(out.offspring, OID);
	}

	@Test
	public void testFactorModifyDeltaForItemValues() throws Exception {
		final String TEST_NAME = "testFactorModifyDeltaForItemValues";
		displayTestTile(TEST_NAME);

		// GIVEN
		String OID = "oid1";
		ObjectDelta<UserType> delta = DeltaBuilder.deltaFor(UserType.class, getPrismContext())
				.item(UserType.F_ASSIGNMENT)
						.add(ObjectTypeUtil.createAssignmentTo("oid-r", ObjectTypes.ROLE, getPrismContext()))
						.delete(new AssignmentType().id(101L), new AssignmentType().id(102L))
				.item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_LIFECYCLE_STATE).replace("draft")
				.item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_DESCRIPTION).replace("descr")
				.item(UserType.F_ASSIGNMENT, 77L, AssignmentType.F_LIFECYCLE_STATE).replace("active")
				.item(UserType.F_GIVEN_NAME).replace("bill")
				.asObjectDeltaCast(OID);

		// WHEN
		ObjectDelta.FactorOutResultMulti<UserType> out = delta.factorOutValues(new ItemPath(UserType.F_ASSIGNMENT), true);

		// THEN
		System.out.println("Delta before operation:\n" + delta.debugDump() + "\n");
		System.out.println("Delta after factorOut:\n" + out.remainder.debugDump() + "\n");
		System.out.println("Offspring deltas:\n" + DebugUtil.debugDump(out.offsprings) + "\n");

		assertTrue("Remaining delta is not a MODIFY delta", out.remainder.isModify());
		assertEquals("Wrong # of remaining modifications", 1, out.remainder.getModifications().size());
		assertEquals("Wrong # of offspring deltas", 5, out.offsprings.size());
		assertEquals("Wrong # of modifications in offspring 0", 1, out.offsprings.get(0).getModifications().size());
		assertEquals("Wrong # of assignments to add in offspring 0", 1, out.offsprings.get(0).getModifications().iterator().next().getValuesToAdd().size());
		assertEquals("Wrong # of modifications in offspring 1", 1, out.offsprings.get(1).getModifications().size());
		assertEquals("Wrong # of assignments to delete in offspring 1", 1, out.offsprings.get(1).getModifications().iterator().next().getValuesToDelete().size());
		assertEquals("Wrong # of modifications in offspring 2", 1, out.offsprings.get(2).getModifications().size());
		assertEquals("Wrong # of assignments to delete in offspring 2", 1, out.offsprings.get(2).getModifications().iterator().next().getValuesToDelete().size());
		// fragile - can be swapped if hashcodes change
		assertEquals("Wrong # of modifications in offspring 3", 2, out.offsprings.get(3).getModifications().size());
		assertEquals("Wrong # of modifications in offspring 4", 1, out.offsprings.get(4).getModifications().size());

		assertDeltaOid(out.remainder, OID);
		for (ObjectDelta<UserType> offspring : out.offsprings) {
			assertDeltaOid(offspring, OID);
		}
	}


	/**
	 * Analogy of:
	 * MODIFY/replace (credentials/password) + MODIFY/add (credentials/password/metadata)   [MID-4593]
	 */
	@Test      // MID-4690
	public void testObjectDeltaUnion() throws Exception {
		final String TEST_NAME="testObjectDeltaUnion";
		displayTestTile(TEST_NAME);
		// GIVEN

		ProtectedStringType value = new ProtectedStringType();
		value.setClearValue("hi");
		PasswordType newPassword = new PasswordType(getPrismContext()).value(value);
		ObjectDelta<UserType> userDelta1 = DeltaBuilder.deltaFor(UserType.class, getPrismContext())
				.item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD).replace(newPassword)
				.asObjectDeltaCast("001");
		MetadataType newMetadata = new MetadataType(getPrismContext()).requestorComment("comment");
		ObjectDelta<UserType> userDelta2 = DeltaBuilder.deltaFor(UserType.class, getPrismContext())
				.item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA)
					.add(newMetadata)
				.asObjectDeltaCast("001");

		// WHEN
		ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);

		// THEN
		display("result", userDeltaUnion);

		PrismObject<UserType> userWithSeparateDeltas = new UserType(getPrismContext()).asPrismObject();
		userDelta1.applyTo(userWithSeparateDeltas);
		userDelta2.applyTo(userWithSeparateDeltas);
		display("userWithSeparateDeltas after", userWithSeparateDeltas);

		PrismObject<UserType> userWithUnion = new UserType(getPrismContext()).asPrismObject();
		userDeltaUnion.applyTo(userWithUnion);
		display("userWithUnion after", userWithUnion);

		ObjectDelta<UserType> diff = userWithSeparateDeltas.diff(userWithUnion, false, true);       // set to isLiteral = false after fixing MID-4688
		display("diff", diff.debugDump());
		assertTrue("Deltas have different effects:\n" + diff.debugDump(), diff.isEmpty());
	}
}
