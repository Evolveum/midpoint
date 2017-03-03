/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.intest.negative;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoCache;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentErrors extends AbstractInitializedModelIntegrationTest {
	
	private static final String TEST_DIR = "src/test/resources/negative";
	private static final String TEST_TARGET_DIR = "target/test/negative";
	
	private static final String USER_LEMONHEAD_NAME = "lemonhead";
	private static final String USER_LEMONHEAD_FULLNAME = "Lemonhead";
	private static final String USER_SHARPTOOTH_NAME = "sharptooth";
	private static final String USER_SHARPTOOTH_FULLNAME = "Sharptooth";
	private static final String USER_SHARPTOOTH_PASSWORD_1_CLEAR = "SHARPyourT33TH";
	private static final String USER_SHARPTOOTH_PASSWORD_2_CLEAR = "L00SEyourT33TH";
	private static final String USER_SHARPTOOTH_PASSWORD_3_CLEAR = "HAV3noT33TH";
	private static final String USER_REDSKULL_NAME = "redskull";
	private static final String USER_REDSKULL_FULLNAME = "Red Skull";

	private static final String USER_AFET_NAME = "afet";
	private static final String USER_AFET_FULLNAME = "Alfredo Fettucini";
	private static final String USER_BFET_NAME = "bfet";
	private static final String USER_BFET_FULLNAME = "Bill Fettucini";
	private static final String USER_CFET_NAME = "cfet";
	private static final String USER_CFET_FULLNAME = "Carlos Fettucini";

	protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentErrors.class);

	private PrismObject<ResourceType> resource;
	private String userLemonheadOid;
	private String userSharptoothOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
	}
	
	@Test
	public void test010RefinedSchemaWhite() throws Exception {
		final String TEST_NAME = "test010RefinedSchemaWhite";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		// WHEN
		PrismObject<ResourceType> resourceWhite = getObject(ResourceType.class, RESOURCE_DUMMY_WHITE_OID);
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceWhite, prismContext);
		display("Refined schema", refinedSchema);
		
		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));
		
		assertEquals("Unexpected kind in account definition", ShadowKindType.ACCOUNT, accountDef.getKind());
		assertTrue("Account definition in not default", accountDef.isDefaultInAKind());
		assertEquals("Wrong intent in account definition", SchemaConstants.INTENT_DEFAULT, accountDef.getIntent());
		assertFalse("Account definition is deprecated", accountDef.isDeprecated());
		assertFalse("Account definition in auxiliary", accountDef.isAuxiliary());

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canAdd());
		assertFalse("UID has update", uidDef.canModify());
		assertTrue("No UID read", uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canAdd());
		assertTrue("No NAME update", nameDef.canModify());
		assertTrue("No NAME read", nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));		
	}
	
	/**
	 * The "white" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test100UserJackAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test100UserJackAssignBlankAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_WHITE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyAuditService.clear();
                
		// WHEN
		//not expected that it fails, insted the fatal error in the result is excpected
		modelService.executeChanges(deltas, null, task, result);
        
        result.computeStatus();
        
        display(result);
        // This has to be a partial error as some changes were executed (user) and others were not (account)
        TestUtil.assertPartialError(result);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
        dummyAuditService.assertExecutionMessage();
		
	}
	
	/**
	 * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test101AddUserCharlesAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test101AddUserCharlesAssignBlankAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> userCharles = createUser("charles", "Charles L. Charles");
        fillinUserAssignmentAccountConstruction(userCharles, RESOURCE_DUMMY_WHITE_OID);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(userCharles);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        //we do not expect this to throw an exception. instead the fatal error in the result is excpected
		modelService.executeChanges(deltas, null, task, result);
		        
        result.computeStatus();
        TestUtil.assertFailure(result);
        
        // Even though the operation failed the addition of a user should be successful. Let's check if user was really added.
        String userOid = userDelta.getOid();
        assertNotNull("No user OID in delta after operation", userOid);
        
        PrismObject<UserType> userAfter = getUser(userOid);
        assertUser(userAfter, userOid, "charles", "Charles L. Charles", null, null, null);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
        dummyAuditService.assertExecutionMessage();
		
	}
		

	@Test
    public void test200UserLemonheadAssignAccountBrokenNetwork() throws Exception {
		final String TEST_NAME = "test200UserLemonheadAssignAccountBrokenNetwork";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = createUser(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME);
        addObject(user);
        userLemonheadOid = user.getOid();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        getDummyResource().setBreakMode(BreakMode.NETWORK);
        dummyAuditService.clear();
                
		// WHEN
		//not expected that it fails, instead the error in the result is expected
		modelService.executeChanges(deltas, null, task, result);
        
        result.computeStatus();
        
        display(result);
        // This has to be a partial error as some changes were executed (user) and others were not (account)
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(user.getOid());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.HANDLED_ERROR);
        dummyAuditService.assertExecutionMessage();
		
	}
	
	// PARTIAL_ERROR: Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.
	// TODO: timeout or explicit retry
//	@Test
//    public void test205UserLemonheadRecovery() throws Exception {
//		final String TEST_NAME = "test205UserLemonheadRecovery";
//        TestUtil.displayTestTile(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//                
//        dummyResource.setBreakMode(BreakMode.NONE);
//        dummyAuditService.clear();
//                
//		// WHEN
//		//not expected that it fails, instead the error in the result is expected
//        modelService.recompute(UserType.class, userLemonheadOid, task, result);
//        
//        result.computeStatus();
//        
//        display(result);
//        // This has to be a partial error as some changes were executed (user) and others were not (account)
//        TestUtil.assertSuccess(result);
//        
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertTarget(userLemonheadOid);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.HANDLED_ERROR);
//        dummyAuditService.assertExecutionMessage();
//		
//	}

	@Test
    public void test210UserSharptoothAssignAccountBrokenGeneric() throws Exception {
		final String TEST_NAME = "test210UserSharptoothAssignAccountBrokenGeneric";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = createUser(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME);
        CredentialsType credentialsType = new CredentialsType();
        PasswordType passwordType = new PasswordType();
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(USER_SHARPTOOTH_PASSWORD_1_CLEAR);
		passwordType.setValue(passwordPs);
		credentialsType.setPassword(passwordType);
		user.asObjectable().setCredentials(credentialsType);
        addObject(user);
        userSharptoothOid = user.getOid();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        getDummyResource().setBreakMode(BreakMode.GENERIC);
        dummyAuditService.clear();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		//not expected that it fails, instead the error in the result is expected
		modelService.executeChanges(deltas, null, task, result);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        
        display(result);
        // This has to be a partial error as some changes were executed (user) and others were not (account)
        TestUtil.assertPartialError(result);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class, OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertTarget(user.getOid());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
        dummyAuditService.assertExecutionMessage();
		
        LensContext<UserType> lastLensContext = lensDebugListener.getLastLensContext();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = lastLensContext.getExecutedDeltas();
        display("Executed deltas", executedDeltas);
        assertEquals("Unexpected number of execution deltas in context", 2, executedDeltas.size());
        Iterator<ObjectDeltaOperation<? extends ObjectType>> i = executedDeltas.iterator();
        ObjectDeltaOperation<? extends ObjectType> deltaop1 = i.next();
        assertEquals("Unexpected result of first executed deltas", OperationResultStatus.SUCCESS, deltaop1.getExecutionResult().getStatus());
        ObjectDeltaOperation<? extends ObjectType> deltaop2 = i.next();
        assertEquals("Unexpected result of second executed deltas", OperationResultStatus.FATAL_ERROR, deltaop2.getExecutionResult().getStatus());
        
	}
	
	/**
	 * User has assigned account. We recover the resource (clear break mode) and recompute.
	 * The account should be created.
	 */
	@Test
    public void test212UserSharptoothAssignAccountRecovery() throws Exception {
		final String TEST_NAME = "test212UserSharptoothAssignAccountRecovery";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
                
        getDummyResource().resetBreakMode();
        dummyAuditService.clear();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(userSharptoothOid, task, result);

        // THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertDummyAccount(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDummyPassword(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_PASSWORD_1_CLEAR);
	}
	
	/**
	 * Change user password. But there is error on the resource.
	 * User password should be changed, account password unchanged and there
	 * should be handled error in the result. Delta is remembered in the shadow.
	 * MID-3569
	 */
	@Test
    public void test214UserSharptoothChangePasswordNetworkError() throws Exception {
		testUserSharptoothChangePasswordError("test214UserSharptoothChangePasswordNetworkError",
				BreakMode.NETWORK, USER_SHARPTOOTH_PASSWORD_1_CLEAR, USER_SHARPTOOTH_PASSWORD_2_CLEAR,
				OperationResultStatus.HANDLED_ERROR);
	}

	/**
	 * Change user password. But there is error on the resource.
	 * User password should be changed, account password unchanged and there
	 * should be partial error in the result. We have no idea what's going on here.
	 * MID-3569
	 */
	@Test
    public void test215UserSharptoothChangePasswordGenericError() throws Exception {
		testUserSharptoothChangePasswordError("test215UserSharptoothChangePasswordGenericError",
				BreakMode.GENERIC, USER_SHARPTOOTH_PASSWORD_1_CLEAR, USER_SHARPTOOTH_PASSWORD_3_CLEAR,
				OperationResultStatus.PARTIAL_ERROR);
	}

	public void testUserSharptoothChangePasswordError(final String TEST_NAME, BreakMode breakMode, String oldPassword, String newPassword, OperationResultStatus expectedResultStatus) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
                
        getDummyResource().setBreakMode(breakMode);
        dummyAuditService.clear();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserChangePassword(userSharptoothOid, newPassword, task, result);

        // THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertStatus(result, expectedResultStatus);
        
        getDummyResource().resetBreakMode();
        
        PrismObject<UserType> userAfter = getUser(userSharptoothOid);
		display("User afte", userAfter);
        assertEncryptedUserPassword(userAfter, newPassword);
        
        assertDummyAccount(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDummyPassword(null, USER_SHARPTOOTH_NAME, oldPassword);
	}
	
	/**
	 * Assign account to user, delete the account shadow (not the account), recompute the user. 
	 * We expect that the shadow will be re-created and re-linked.
	 * 
	 * This is tried on the default dummy resource where synchronization is enabled.
	 */
	@Test
    public void test220UserAssignAccountDeletedShadowRecomputeSync() throws Exception {
		final String TEST_NAME = "test220UserAssignAccountDeletedShadowRecomputeSync";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		//GIVEN
		PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(TEST_NAME, RESOURCE_DUMMY_OID, null,
				USER_AFET_NAME, USER_AFET_FULLNAME);
		String shadowOidBefore = getSingleLinkOid(user);
		Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		
		// WHEN
        recomputeUser(user.getOid(), task, result);
        
		// THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result,2);
        
        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfter = getSingleLinkOid(user);
        display("Shadow OID after", shadowOidAfter);
        PrismObject<ShadowType> shadowAfter = repositoryService.getObject(ShadowType.class, shadowOidAfter, null, result);
        display("Shadow after", shadowAfter);
        
        assertFalse("New and old shadow OIDs are the same", shadowOidBefore.equals(shadowOidAfter));
        
        // ... and again ...
        
        task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        result = task.getResult();
		
		// WHEN
        recomputeUser(user.getOid(), task, result);
        
		// THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result,2);
        
        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfterAfter = getSingleLinkOid(user);
        display("Shadow OID after the second time", shadowOidAfterAfter);

        assertEquals("The shadow OIDs has changed after second recompute", shadowOidAfter, shadowOidAfterAfter);
	}
	
	/**
	 * Assign account to user, delete the account shadow (not the account), recompute the user. 
	 * We expect ObjectAlreadyExistsException.
	 * 
	 * This is tried on the red dummy resource where there is no synchronization.
	 */
	@Test
    public void test222UserAssignAccountDeletedShadowRecomputeNoSync() throws Exception {
		final String TEST_NAME = "test222UserAssignAccountDeletedShadowRecomputeNoSync";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		//GIVEN
		PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(TEST_NAME, RESOURCE_DUMMY_RED_OID, RESOURCE_DUMMY_RED_NAME,
				USER_BFET_NAME, USER_BFET_FULLNAME);
		Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        try {
	        // WHEN
        	recomputeUser(user.getOid(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (ObjectAlreadyExistsException e) {
        	// this is expected
        	result.computeStatus();
        	TestUtil.assertFailure(result);
        }
        
        user = getUser(user.getOid());
        display("User after", user);
        assertNoLinkedAccount(user);
        
        // and again ...
        
        task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        result = task.getResult();
        
        try {
	        // WHEN
        	recomputeUser(user.getOid(), task, result);
	        
	        AssertJUnit.fail("Unexpected success");
        } catch (ObjectAlreadyExistsException e) {
        	// this is expected
        	result.computeStatus();
        	TestUtil.assertFailure(result);
        }
        
        user = getUser(user.getOid());
        display("User after", user);
        assertNoLinkedAccount(user);
        
	}
	
	/**
	 * Assign account to user, delete the account shadow (not the account), recompute the user. 
	 * We expect that the shadow will be re-created and re-linked.
	 * 
	 * This is tried on the yellow dummy resource where there is reduced synchronization config.
	 */
	@Test
    public void test224UserAssignAccountDeletedShadowRecomputeReducedSync() throws Exception {
		final String TEST_NAME = "test224UserAssignAccountDeletedShadowRecomputeReducedSync";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		//GIVEN
		PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(TEST_NAME, 
				RESOURCE_DUMMY_YELLOW_OID, RESOURCE_DUMMY_YELLOW_NAME,
				USER_CFET_NAME, USER_CFET_FULLNAME);
		String shadowOidBefore = getSingleLinkOid(user);
		Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		
		// WHEN
        recomputeUser(user.getOid(), task, result);
        
		// THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result,2);
        
        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfter = getSingleLinkOid(user);
        display("Shadow OID after", shadowOidAfter);
        PrismObject<ShadowType> shadowAfter = repositoryService.getObject(ShadowType.class, shadowOidAfter, null, result);
        display("Shadow after", shadowAfter);
        
        assertFalse("New and old shadow OIDs are the same", shadowOidBefore.equals(shadowOidAfter));
        
        // ... and again ...
        
        task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        result = task.getResult();
		
		// WHEN
        recomputeUser(user.getOid(), task, result);
        
		// THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result,2);
        
        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfterAfter = getSingleLinkOid(user);
        display("Shadow OID after the second time", shadowOidAfterAfter);

        assertEquals("The shadow OIDs has changed after second recompute", shadowOidAfter, shadowOidAfterAfter);
        
	}
		
	private PrismObject<UserType> setupUserAssignAccountDeletedShadowRecompute(final String TEST_NAME, String dummyResourceOid, 
			String dummyResourceName, String userName, String userFullName) throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentErrors.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().resetBreakMode();
        
        PrismObject<UserType> user = createUser(userName, userFullName);
        AssignmentType assignmentType = createConstructionAssignment(dummyResourceOid, ShadowKindType.ACCOUNT, null);
        user.asObjectable().getAssignment().add(assignmentType);
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
		user.asObjectable().setActivation(activationType);
        addObject(user);
        
        // precondition
        assertDummyAccount(dummyResourceName, userName, userFullName, true);
        
        // Re-read user to get the links
        user = getUser(user.getOid());
        display("User before", user);
        String shadowOidBefore = getSingleLinkOid(user);
        
        // precondition
        PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, shadowOidBefore, null, result);
        display("Shadow before", shadowBefore);
        
        // delete just the shadow, not the account
        repositoryService.deleteObject(ShadowType.class, shadowOidBefore, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoRepoCache();        
        dummyAuditService.clear();
        
        return user;        		
	}

	
	
}
