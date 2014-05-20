/*
 * Copyright (c) 2010-2014 Evolveum
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
import static com.evolveum.midpoint.test.util.TestUtil.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestModelServiceContract;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
	
	protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentErrors.class);

	private PrismObject<ResourceType> resource;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
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
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);

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
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = createUser(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME);
        addObject(user);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyResource.setBreakMode(BreakMode.NETWORK);
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

	@Test
    public void test210UserSharptoothAssignAccountBrokenGeneric() throws Exception {
		final String TEST_NAME = "test210UserSharptoothAssignAccountBrokenGeneric";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = createUser(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME);
        addObject(user);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyResource.setBreakMode(BreakMode.GENERIC);
        dummyAuditService.clear();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		//not expected that it fails, instead the error in the result is expected
		modelService.executeChanges(deltas, null, task, result);
        
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
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class, OperationResultStatus.FATAL_ERROR);
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

}
