/*
 * Copyright (C) 2014-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Test delta operation on real midpoint schema. Similar to TestDelta in prism, but this is using the
 * real thing and not just testing schema.
 *
 * @author Radovan Semancik
 */
public class TestSchemaDelta extends AbstractSchemaTest {

    @Test
    public void testAssignmentNullIdApplyToObject() throws Exception {
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

        AssignmentType a = new AssignmentType(getPrismContext())       // The value id is null
                .description("jamalalicha patlama paprtala");
        ObjectDelta<UserType> userDelta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(a)
                .asObjectDelta(USER_JACK_OID);

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
    public void testAddInducementNullIdApplyToObject() throws Exception {
        // GIVEN
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

        AssignmentType i = new AssignmentType(getPrismContext())       // The value id is null
                .description("jamalalicha patlama paprtala");
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT).add(i)
                .asObjectDelta(ROLE_CONSTRUCTION_OID);

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
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
                        RoleType.F_INDUCEMENT, inducement);

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
        // GIVEN
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

        //Delta
        AssignmentType inducement = new AssignmentType();
        inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
                        RoleType.F_INDUCEMENT, inducement);

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
        // GIVEN
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);
        System.out.println("Role before delta application:");
        System.out.println(role.debugDump());

        //Delta
        AssignmentType inducement = new AssignmentType();
        inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
                        RoleType.F_INDUCEMENT, inducement);

        // WHEN
        ItemDeltaCollectionsUtil.applyTo(roleDelta.getModifications(), role);

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
        // GIVEN
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

        //Delta
        ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(ROLE_CONSTRUCTION_RESOURCE_OID);
        resourceRef.setType(ObjectTypes.RESOURCE.getTypeQName());
        construction.setResourceRef(resourceRef);
        // No container ID
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
                        ItemPath.create(RoleType.F_INDUCEMENT, ROLE_CONSTRUCTION_INDUCEMENT_ID, AssignmentType.F_CONSTRUCTION),
                        construction);

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
        // GIVEN
        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

        //Delta
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        // No container ID
        ObjectDelta<RoleType> roleDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID,
                        ItemPath.create(RoleType.F_INDUCEMENT, ROLE_CONSTRUCTION_INDUCEMENT_ID, AssignmentType.F_ACTIVATION),
                        activationType);

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
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

        //Delta
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        // No container ID
        ObjectDelta<UserType> userDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(UserType.class, USER_JACK_OID,
                        ItemPath.create(UserType.F_ASSIGNMENT, USER_JACK_ASSIGNMENT_ID, AssignmentType.F_ACTIVATION),
                        activationType);

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

    @Test
    public void testAddAssignmentSameOidDifferentTargetType() throws Exception {
        // GIVEN
        PrismObject<UserType> user = new UserType(getPrismContext())
                .name("test")
                .oid("oid1")
                .beginAssignment()
                .id(1L)
                .targetRef("target-oid-1", RoleType.COMPLEX_TYPE)
                .<UserType>end()
                .asPrismObject();

        ObjectDelta<UserType> userDelta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType().id(2L).targetRef("target-oid-1", OrgType.COMPLEX_TYPE))
                .asObjectDelta("oid1");

        // WHEN
        userDelta.applyTo(user);

        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong # of assignments", 2, user.asObjectable().getAssignment().size());
    }

    // subtract of single-valued PCV from multivalued one
    @Test
    public void testSubtractAssignmentFromAddDelta() throws Exception {
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
        ObjectDelta<UserType> addDelta = DeltaFactory.Object.createAddDelta(user);

        // WHEN
        PrismContainerDefinition<AssignmentType> assignmentDef = PrismTestUtil.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class).clone();
        assignmentDef.toMutable().setMaxOccurs(1);
        PrismContainer<AssignmentType> assignmentContainer = assignmentDef.instantiate();

        PrismContainerValue<AssignmentType> assignmentValue = ObjectTypeUtil
                .createAssignmentTo("00000001-d34d-b33f-f00d-000000000002", ObjectTypes.ROLE, getPrismContext())
                .asPrismContainerValue();
        assignmentContainer.add(assignmentValue);

        System.out.println("Delta before operation:\n" + addDelta.debugDump() + "\n");
        System.out.println("Assignment to subtract:\n" + assignmentValue.debugDump() + "\n");
        boolean removed = addDelta.subtract(ItemPath.create(SchemaConstants.PATH_ASSIGNMENT), assignmentValue, false, false);

        // THEN
        System.out.println("Delta after operation:\n" + addDelta.debugDump() + "\n");
        System.out.println("Removed: " + removed + "\n");

        assertTrue("Not removed", removed);
        assertTrue("Remaining delta is not an ADD delta", addDelta.isAdd());
        assertEquals("Wrong # of remaining assignments", 2, addDelta.getObjectToAdd().asObjectable().getAssignment().size());
    }

    @Test
    public void testSubtractAssignmentFromModifyDelta() throws Exception {
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
        user.asObjectable().getAssignment().get(0).setId(9999L);
        AssignmentType assignment9999 = new AssignmentType();
        assignment9999.setId(9999L);
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(assignment9999)
                .asObjectDelta(user.getOid());

        // WHEN
        PrismContainerDefinition<AssignmentType> assignmentDef = PrismTestUtil.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class).clone();
        assignmentDef.toMutable().setMaxOccurs(1);
        PrismContainer<AssignmentType> assignmentContainer = assignmentDef.instantiate();

        PrismContainerValue<AssignmentType> assignmentValue =
                ObjectTypeUtil.createAssignmentTo("00000001-d34d-b33f-f00d-000000000002", ObjectTypes.ROLE,
                                getPrismContext())
                        .asPrismContainerValue();
        assignmentValue.setId(9999L);
        assignmentContainer.add(assignmentValue);

        System.out.println("Delta before operation:\n" + delta.debugDump() + "\n");
        System.out.println("Assignment to subtract:\n" + assignmentValue.debugDump() + "\n");
        boolean removed = delta.subtract(ItemPath.create(SchemaConstants.PATH_ASSIGNMENT), assignmentValue, true, false);

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
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
        String OID = "user-oid-1";
        user.setOid(OID);
        ObjectDelta<UserType> addDelta = DeltaFactory.Object.createAddDelta(user);

        // WHEN
        ObjectDelta.FactorOutResultSingle<UserType> out = addDelta.factorOut(singleton(ItemPath.create(UserType.F_ASSIGNMENT)), true);

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
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
        String OID = "user-oid-1";
        user.setOid(OID);
        ObjectDelta<UserType> addDelta = DeltaFactory.Object.createAddDelta(user);

        // WHEN
        ObjectDelta.FactorOutResultSingle<UserType> out = addDelta.factorOut(asList(ItemPath.create(UserType.F_GIVEN_NAME), ItemPath.create(UserType.F_FAMILY_NAME)), true);

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
        // GIVEN
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_BILL_FILE);
        String OID = "user-oid-1";
        user.setOid(OID);
        ObjectDelta<UserType> addDelta = DeltaFactory.Object.createAddDelta(user);

        // WHEN
        ObjectDelta.FactorOutResultMulti<UserType> out = addDelta.factorOutValues(ItemPath.create(UserType.F_ASSIGNMENT), true);

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
        // GIVEN
        String OID = "oid1";
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType().id(101L), new AssignmentType().id(102L))
                .add(ObjectTypeUtil.createAssignmentTo("oid-r", ObjectTypes.ROLE, getPrismContext()))
                .item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_LIFECYCLE_STATE).replace("draft")
                .item(UserType.F_GIVEN_NAME).replace("bill")
                .asObjectDelta(OID);

        // WHEN
        ObjectDelta.FactorOutResultSingle<UserType> out = delta.factorOut(singleton(ItemPath.create(UserType.F_ASSIGNMENT)), true);

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
        // GIVEN
        String OID = "oid1";
        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType().id(101L), new AssignmentType().id(102L))
                .add(ObjectTypeUtil.createAssignmentTo("oid-r", ObjectTypes.ROLE, getPrismContext()))
                .item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_LIFECYCLE_STATE).replace("draft")
                .item(UserType.F_ASSIGNMENT, 100L, AssignmentType.F_DESCRIPTION).replace("descr")
                .item(UserType.F_ASSIGNMENT, 77L, AssignmentType.F_LIFECYCLE_STATE).replace("active")
                .item(UserType.F_GIVEN_NAME).replace("bill")
                .asObjectDelta(OID);

        // WHEN
        ObjectDelta.FactorOutResultMulti<UserType> out = delta.factorOutValues(ItemPath.create(UserType.F_ASSIGNMENT), true);

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
        // GIVEN
        ProtectedStringType value = new ProtectedStringType();
        value.setClearValue("hi");
        PasswordType newPassword = new PasswordType().value(value);
        ObjectDelta<UserType> userDelta1 = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD).replace(newPassword)
                .asObjectDelta("001");
        MetadataType newMetadata = new MetadataType().requestorComment("comment");
        ObjectDelta<UserType> userDelta2 = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA)
                .add(newMetadata)
                .asObjectDelta("001");

        // WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDeltaCollectionsUtil.union(userDelta1, userDelta2);

        // THEN
        displayValue("result", userDeltaUnion);

        PrismObject<UserType> userWithSeparateDeltas = new UserType(getPrismContext()).asPrismObject();
        userDelta1.applyTo(userWithSeparateDeltas);
        userDelta2.applyTo(userWithSeparateDeltas);
        displayValue("userWithSeparateDeltas after", userWithSeparateDeltas);

        PrismObject<UserType> userWithUnion = new UserType(getPrismContext()).asPrismObject();
        userDeltaUnion.applyTo(userWithUnion);
        displayValue("userWithUnion after", userWithUnion);

        // set to isLiteral = false after fixing MID-4688
        ObjectDelta<UserType> diff = userWithSeparateDeltas.diff(userWithUnion, EquivalenceStrategy.LITERAL);
        displayValue("diff", diff.debugDump());
        assertTrue("Deltas have different effects:\n" + diff.debugDump(), diff.isEmpty());
    }
}
