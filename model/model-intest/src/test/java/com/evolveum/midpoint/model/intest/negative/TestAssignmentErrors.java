/*
 * Copyright (c) 2010-2013 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestModelServiceContract;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
		
	protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentErrors.class);
	
	private PrismObject<ResourceType> resource;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
	}
	
	/**
	 * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test100UserJackAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test100UserJackAssignBlankAccount";
        displayTestTile(this, "TEST_NAME");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_WHITE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		//not expected that it fails, insted the fatal error in the result is excpected
		modelService.executeChanges(deltas, null, task, result);
        
        result.computeStatus();
        
        display(result);
        // This has to be a partial error as some changes were executed (user) and others were not (account)
        IntegrationTestTools.assertPartialError(result);
		
	}
	
	/**
	 * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
	 * this results in account without any attributes. It should fail.
	 */
	@Test
    public void test101AddUserCharlesAssignBlankAccount() throws Exception {
		final String TEST_NAME = "test101AddUserCharlesAssignBlankAccount";
        displayTestTile(this, "TEST_NAME");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();
        
        PrismObject<UserType> userCharles = createUser("charles", "Charles L. Charles");
        fillinUserAssignmentAccountConstruction(userCharles, RESOURCE_DUMMY_WHITE_OID);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(userCharles);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
//        try {
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			//not expected that it fails, insted the fatal error in the result is excpected
//			AssertJUnit.fail("Unexpected success of modelService.executeChanges(), expected an exception");
//        } catch (SchemaException e) {
//        	// This is expected
//        	display("Expected exception", e);
//        }
        
        result.computeStatus();
        assertFailure(result);
        
        // Even though the operation failed the addition of a user should be successful. Let's check if user was really added.
        String userOid = userDelta.getOid();
        assertNotNull("No user OID in delta after operation", userOid);
        
        PrismObject<UserType> userAfter = getUser(userOid);
        assertUser(userAfter, userOid, "charles", "Charles L. Charles", null, null, null);
		
	}
		

}
