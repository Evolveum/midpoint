/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest.gensync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Generic synchronization test. We create role and assign a resource to it.
 * Entitlement (group) should be created.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRoleEntitlement extends AbstractGenericSyncTest {

	private static String groupOid;

    @Test
    public void test050GetRolePirate() throws Exception {
		final String TEST_NAME = "test050GetRolePirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // WHEN
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);

        // THEN
        display("Role pirate", role);
        assertRolePirate(role);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
	}

	protected void assertRolePirate(PrismObject<RoleType> role) {
		assertObject(role);
		assertEquals("Wrong "+role+" OID (prism)", ROLE_PIRATE_OID, role.getOid());
		RoleType roleType = role.asObjectable();
		assertEquals("Wrong "+role+" OID (jaxb)", ROLE_PIRATE_OID, roleType.getOid());
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", "Pirate", roleType.getName());
	}

	@Test
    public void test100ModifyRoleAddEntitlement() throws Exception {
        final String TEST_NAME = "test100ModifyRoleAddEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        PrismReferenceValue linkRefVal = new PrismReferenceValue();
		linkRefVal.setObject(group);
		ReferenceDelta groupDelta = ReferenceDelta.createModificationAdd(RoleType.F_LINK_REF, getRoleDefinition(), linkRefVal);
		roleDelta.addModification(groupDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(roleDelta);

		dummyAuditService.clear();
        prepareNotifications();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

		// Check accountRef
		PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        display("Role pirate after", rolePirate);
		assertRolePirate(rolePirate);
		assertLinks(rolePirate, 1);
		groupOid = getSingleLinkOid(rolePirate);
        assertFalse("No linkRef oid", StringUtils.isBlank(groupOid));

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(accountShadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(accountModel, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check account in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, GROUP_PIRATE_DUMMY_DESCRIPTION);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

	}

    @Test
    public void test101GetGroup() throws Exception {
        final String TEST_NAME = "test101GetGroup";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
        // fetched after get.
        DummyGroup dummyGroup = getDummyGroup(null, GROUP_PIRATE_DUMMY_NAME);
        dummyGroup.replaceAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Bloodthirsty Pirates");

		// WHEN
		PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupOid, null , task, result);

		// THEN
		display("Group shadow (model)", shadow);
        assertDummyGroupShadowModel(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        assertSuccess(result);

        shadow.checkConsistence(true, true);

        IntegrationTestTools.assertAttribute(shadow, getAttributeQName(getDummyResourceObject(), DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
                "Bloodthirsty Pirates");
	}

	@Test
    public void test102GetGroupNoFetch() throws Exception {
        final String TEST_NAME = "test102GetGroupNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

		// WHEN
		PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupOid, options , task, result);

		display("Account", shadow);
		display("Account def", shadow.getDefinition());
		PrismContainer<Containerable> accountContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
	}

	@Test
    public void test103GetGroupRaw() throws Exception {
        final String TEST_NAME = "test103GetGroupRaw";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());

		// WHEN
		PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupOid, options , task, result);

		display("Account", shadow);
		display("Account def", shadow.getDefinition());
		PrismContainer<Containerable> accountContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		display("Account attributes def", accountContainer.getDefinition());
		display("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
	}

	@Test
    public void test108ModifyRoleAddEntitlementAgain() throws Exception {
        final String TEST_NAME = "test108ModifyRoleAddEntitlementAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        PrismReferenceValue linkRefVal = new PrismReferenceValue();
        linkRefVal.setObject(group);
        ReferenceDelta groupDelta = ReferenceDelta.createModificationAdd(RoleType.F_LINK_REF, getRoleDefinition(), linkRefVal);
        roleDelta.addModification(groupDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(roleDelta);

		dummyAuditService.clear();

		try {

			// WHEN
			modelService.executeChanges(deltas, null, task, result);

			// THEN
			assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
		} catch (SchemaException e) {
			// This is expected
			e.printStackTrace();
			// THEN
			String message = e.getMessage();
			assertMessageContains(message, "already contains entitlement");
			assertMessageContains(message, "group");
		}

		// Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
    }

	@Test
    public void test110GetRoleResolveEntitlement() throws Exception {
        final String TEST_NAME = "test110GetRoleResolveEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options =
        	SelectorOptions.createCollection(UserType.F_LINK, GetOperationOptions.createResolve());

		// WHEN
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, options, task, result);

        RoleType roleType = role.asObjectable();
        assertLinks(role, 1);
        PrismReferenceValue linkRef = getSingleLinkRef(role);
        assertEquals("OID mismatch in linkRefValue", groupOid, linkRef.getOid());
        assertNotNull("Missing account object in linkRefValue", linkRef.getObject());
        ShadowType shadow = roleType.getLink().get(0);
        assertDummyGroupShadowModel(shadow.asPrismObject(), groupOid, GROUP_PIRATE_DUMMY_NAME);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
	}

    @Test
    public void test111GetRoleResolveEntitlement() throws Exception {
        final String TEST_NAME = "test111GetRoleResolveEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolve(),
                        new ItemPath(UserType.F_LINK),
                        new ItemPath(UserType.F_LINK, ShadowType.F_RESOURCE)
                );

        // WHEN
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, options, task, result);

        RoleType roleType = role.asObjectable();
        assertLinks(role, 1);
        PrismReferenceValue linkRef = getSingleLinkRef(role);
        assertEquals("OID mismatch in linkRefValue", groupOid, linkRef.getOid());
        assertNotNull("Missing account object in linkRefValue", linkRef.getObject());
        ShadowType shadow = roleType.getLink().get(0);
        assertDummyGroupShadowModel(shadow.asPrismObject(), groupOid, GROUP_PIRATE_DUMMY_NAME);

        assertNotNull("Resource in account was not resolved", shadow.getResource());

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
    }

    @Test
    public void test112GetRoleResolveEntitlementNoFetch() throws Exception {
        final String TEST_NAME = "test112GetRoleResolveEntitlementNoFetch";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        GetOperationOptions getOpts = new GetOperationOptions();
        getOpts.setResolve(true);
        getOpts.setNoFetch(true);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(UserType.F_LINK, getOpts);

        // WHEN
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, options, task, result);

        RoleType roleType = role.asObjectable();
        assertLinks(role, 1);
        PrismReferenceValue linkRef = getSingleLinkRef(role);
        assertEquals("OID mismatch in linkRefValue", groupOid, linkRef.getOid());
        assertNotNull("Missing account object in linkRefValue", linkRef.getObject());
        ShadowType shadow = roleType.getLink().get(0);
        assertDummyGroupShadowRepo(shadow.asPrismObject(), groupOid, GROUP_PIRATE_DUMMY_NAME);

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
    }


	@Test
    public void test119ModifyRoleDeleteEntitlement() throws Exception {
		final String TEST_NAME = "test119ModifyRoleDeleteEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);
        group.setOid(groupOid);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        ReferenceDelta linkDelta = ReferenceDelta.createModificationDelete(RoleType.F_LINK_REF, getUserDefinition(), group);
        roleDelta.addModification(linkDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(roleDelta);

        prepareNotifications();

        // WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);

		// Check accountRef
        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        assertLinks(role, 0);

		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        	AssertJUnit.fail("Shadow " + groupOid + " still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

    }

    @Test
    public void test120AddEntitlement() throws Exception {
        final String TEST_NAME = "test120AddEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);
        ObjectDelta<ShadowType> groupDelta = ObjectDelta.createAddDelta(group);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(groupDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        groupOid = groupDelta.getOid();
        assertNotNull("No account OID in resulting delta", groupOid);
		// Check linkRef (should be none)
        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        assertLinks(role, 0);

		// Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(accountModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, "Scurvy pirates");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        // This is add. We do not yet have OID in request phase.
        dummyAuditService.assertTarget(shadow.getOid(), AuditEventStage.EXECUTION);
        dummyAuditService.assertExecutionSuccess();
    }

	@Test
    public void test121ModifyRoleLinkEntitlement() throws Exception {
        final String TEST_NAME = "test121ModifyRoleLinkEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.POSITIVE);

        prepareNotifications();

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        ReferenceDelta linkDelta = ReferenceDelta.createModificationAdd(RoleType.F_LINK_REF, getUserDefinition(), groupOid);
		roleDelta.addModification(linkDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(accountShadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(accountModel, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, "Scurvy pirates");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }


	@Test
    public void test128ModifyRoleUnlinkEntitlement() throws Exception {
        final String TEST_NAME = "test128ModifyRoleUnlinkEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> group = PrismTestUtil.parseObject(GROUP_PIRATE_DUMMY_FILE);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createEmptyModifyDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(group);
		ReferenceDelta linkDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), groupOid);
		roleDelta.addModification(linkDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        assertLinks(role, 0);

        // Check shadow (should be unchanged)
        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadowRepo, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check group (should be unchanged)
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, "Scurvy pirates");

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

    }

	@Test
    public void test129DeleteEntitlement() throws Exception {
        final String TEST_NAME = "test129DeleteEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createDeleteDelta(ShadowType.class, groupOid, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(shadowDelta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        assertLinks(role, 0);

		// Check is shadow is gone
        assertNoShadow(groupOid);

        // Check if dummy resource group is gone
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(groupOid);
        dummyAuditService.assertExecutionSuccess();

    }

	@Test
    public void test131ModifyRoleAssignEntitlement() throws Exception {
        final String TEST_NAME = "test131ModifyRoleAssignEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<RoleType> assignmentDelta = createAssignmentDelta(RoleType.class, ROLE_PIRATE_OID, RESOURCE_DUMMY_OID,
                ShadowKindType.ENTITLEMENT, "group", true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(assignmentDelta);

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess("executeChanges result", result);

		PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
		display("Role after change execution", role);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadowModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, ROLE_PIRATE_DESCRIPTION);
        // Just to be on a safe side, this is uppercase
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        displayProvisioningScripts();
        assertNoProvisioningScripts(); // MID-4008

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

	/**
	 * Modify the group (by model service). It should change the group on dummy resource and reflect inbound mappings
     * back to the role.
	 */
	@Test
    public void test132ModifyEntitlement() throws Exception {
        final String TEST_NAME = "test132ModifyEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<ShadowType> shadowDelta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
        		groupOid, dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
                prismContext, "Bloody Pirates");
        shadowDelta.addModificationReplaceProperty(
                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_CC),
                "MELEE123");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(shadowDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertEquals("Wrong role description", ROLE_PIRATE_DESCRIPTION, role.asObjectable().getDescription());
        // reflected by inbound
        PrismAsserts.assertPropertyValue(role,ROLE_EXTENSION_COST_CENTER_PATH, "MELEE123");
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

		// Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);

        // Check group
        // All the changes should be reflected to the group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
        PrismAsserts.assertPropertyValue(shadowModel,
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
        		"Bloody Pirates");

        // Check account in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, "Bloody Pirates");
        assertDummyGroupAttribute(null, GROUP_PIRATE_DUMMY_NAME, DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_CC, "MELEE123");

        assertNoProvisioningScripts(); // MID-4008

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 2);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1, 1);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

	@Test
    public void test139ModifyRoleUnassignEntitlement() throws Exception {
        final String TEST_NAME = "test139ModifyRoleUnassignEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<RoleType> assignmentDelta = createAssignmentDelta(RoleType.class, ROLE_PIRATE_OID, RESOURCE_DUMMY_OID,
                ShadowKindType.ENTITLEMENT, "group", false);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(assignmentDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 0);

        // Check is shadow is gone
        assertNoShadow(groupOid);

        // Check if dummy resource account is gone
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);
        // Just to be on a safe side
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        assertNoProvisioningScripts(); // MID-4008

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

	/**
	 * Assignment enforcement is set to RELATTIVE for this test. The group should be added.
	 */
	@Test
    public void test151ModifyRoleAssignEntitlementRelativeEnforcement() throws Exception {
		final String TEST_NAME = "test151ModifyRoleAssignEntitlementRelativeEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<RoleType> assignmentDelta = createAssignmentDelta(RoleType.class, ROLE_PIRATE_OID, RESOURCE_DUMMY_OID,
                ShadowKindType.ENTITLEMENT, "group", true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(assignmentDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadowModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, ROLE_PIRATE_DESCRIPTION);
        // Just to be on a safe side, this is uppercase
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

    }

	/**
	 * Assignment enforcement is set to RELATIVE for this test. The group should be gone.
	 */
	@Test
    public void test158ModifyRoleUnassignEntitlementRelativeEnforcement() throws Exception {
		final String TEST_NAME = "test158ModifyRoleUnassignEntitlementRelativeEnforcement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestRoleEntitlement.class.getName()
        		+ "." + TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<RoleType> assignmentDelta = createAssignmentDelta(RoleType.class, ROLE_PIRATE_OID, RESOURCE_DUMMY_OID,
                ShadowKindType.ENTITLEMENT, "group", false);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(assignmentDelta);

        // WHEN
		modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 0);

        // Check is shadow is gone
        assertNoShadow(groupOid);

        // Check if dummy resource account is gone
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);
        // Just to be on a safe side
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

    }

    /**
     * Modifies role property and assigns entitlement. This entitlement should be assigned reflecting the
     * modified value.
     */
    @Test
    public void test160ModifyRolePropertyAndAssignEntitlement() throws Exception {
        final String TEST_NAME = "test160ModifyRolePropertyAndAssignEntitlement";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<RoleType> roleDelta = createAssignmentDelta(RoleType.class, ROLE_PIRATE_OID, RESOURCE_DUMMY_OID,
                ShadowKindType.ENTITLEMENT, "group", true);
        roleDelta.addModificationReplaceProperty(RoleType.F_DESCRIPTION, "Band of Bloodthirsty Bashers");
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadowModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, "Band of Bloodthirsty Bashers");
        // Just to be on a safe side, this is uppercase
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();

    }

    /**
     * Modify role property. The modification should be reflected to group.
     */
    @Test
    public void test161ModifyRole() throws Exception {
        final String TEST_NAME = "test161ModifyRole";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationReplaceProperty(RoleType.class,
                ROLE_PIRATE_OID, RoleType.F_DESCRIPTION, prismContext, ROLE_PIRATE_DESCRIPTION);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, GROUP_PIRATE_DUMMY_NAME);
//        assertEnableTimestampShadow(shadowModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_PIRATE_DUMMY_NAME, ROLE_PIRATE_DESCRIPTION);
        // Just to be on a safe side, this is uppercase
        assertNoDummyGroup(ROLE_PIRATE_NAME);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test180RenameRole() throws Exception {
		final String TEST_NAME = "test180RenameRole";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationReplaceProperty(RoleType.class,
                ROLE_PIRATE_OID, RoleType.F_NAME, prismContext, PrismTestUtil.createPolyString("Privateers"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
        display("Role after change execution", role);
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, "privateers");
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check group
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        display("Entitlement shadow after", shadowModel);
        assertDummyGroupShadowModel(shadowModel, groupOid, "privateers");
//        assertEnableTimestampShadow(shadowModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup("privateers", ROLE_PIRATE_DESCRIPTION);

        // Check if dummy resource account is gone
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);
        // Just to be on a safe side, this is uppercase
        assertNoDummyGroup(ROLE_PIRATE_NAME);

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Delete role while it has an assignment. The group should be gone.
     */
	@Test
    public void test199DeleteRole() throws Exception {
        final String TEST_NAME = "test199DeleteRole";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<RoleType> roleDelta = ObjectDelta.createDeleteDelta(RoleType.class, ROLE_PIRATE_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

		try {
			PrismObject<RoleType> role = getRole(ROLE_PIRATE_OID);
			AssertJUnit.fail("Privateers are still alive!");
		} catch (ObjectNotFoundException ex) {
			// This is OK
		}

        // Check is shadow is gone
        assertNoShadow(groupOid);

        // Check if dummy resource account is gone
        assertNoDummyGroup("privateers");
        // Just to be on a safe side
        assertNoDummyGroup("Privateers");
        assertNoDummyGroup(GROUP_PIRATE_DUMMY_NAME);
        assertNoDummyGroup(ROLE_PIRATE_NAME);

     // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, RoleType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(ROLE_PIRATE_OID);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Swashbuckler role has an assignment of meta-role already. Adding the swashbuckler role should
     * automatically create a group.
     * Also the object template should add description to this role.
     */
    @Test
    public void test200AddRoleSwashbuckler() throws Exception {
        final String TEST_NAME = "test200AddRoleSwashbuckler";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        prepareTest(AssignmentPolicyEnforcementType.RELATIVE);

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_SWASHBUCKLER_FILE);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createAddDelta(role);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(roleDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        assertNotNull("No account OID in resulting delta", groupOid);
        // Check linkRef (should be none)
        role = getRole(ROLE_SWASHBUCKLER_OID);
        display("Role after", role);
        // Set by object template
        PrismAsserts.assertPropertyValue(role,RoleType.F_DESCRIPTION, ROLE_SWASHBUCKLER_DESCRIPTION);
        // reflected by inbound
        PrismAsserts.assertPropertyValue(role,ROLE_EXTENSION_COST_CENTER_PATH, "META0000");
        assertLinks(role, 1);
        groupOid = getSingleLinkOid(role);

        // Check shadow
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, groupOid, null, result);
        assertDummyGroupShadowRepo(shadow, groupOid, GROUP_SWASHBUCKLER_DUMMY_NAME);
//        assertEnableTimestampShadow(shadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, groupOid, null, task, result);
        assertDummyGroupShadowModel(accountModel, groupOid, GROUP_SWASHBUCKLER_DUMMY_NAME);
//        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check group in dummy resource
        assertDummyGroup(GROUP_SWASHBUCKLER_DUMMY_NAME, ROLE_SWASHBUCKLER_DESCRIPTION);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, RoleType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, RoleType.class); // link
        dummyAuditService.assertExecutionDeltas(1,1);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, RoleType.class); // inbound
        dummyAuditService.assertTarget(ROLE_SWASHBUCKLER_OID);
        dummyAuditService.assertExecutionSuccess();
    }

	private void prepareTest(AssignmentPolicyEnforcementType enforcement) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcement);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
	}

}
