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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserTemplate extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static String accountOid;
	private static String jackEmployeeNumber;
	
	public TestUserTemplate() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);
	}
		
	@Test
    public void test100ModifyUserGivenName() throws Exception {
        displayTestTile(this, "test100ModifyUserGivenName");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test100ModifyUserGivenName");
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_GIVEN_NAME, prismContext, new PolyString("Jackie"));
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack, "Jackie Sparrow", "Jackie", "Sparrow");
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userJack);
        assertAssignments(userJack, 1);
        
        result.computeStatus();
        assertSuccess(result);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
	}
	
	@Test
    public void test101ModifyUserEmployeeTypePirate() throws Exception {
		final String TEST_NAME = "test101ModifyUserEmployeeTypePirate";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "PIRATE");
        // Make sure that the user has no employeeNumber so it will be generated by userTemplate
        userDelta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        
        jackEmployeeNumber = userJackType.getEmployeeNumber();
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, jackEmployeeNumber.length());
	}
	
	/**
	 * Switch employeeType from PIRATE to BUCCANEER. This makes one condition to go false and the other to go
	 * true. For the same role assignement value. So nothing should be changed.
	 */
	@Test
    public void test102ModifyUserEmployeeTypeBuccaneer() throws Exception {
		final String TEST_NAME = "test102ModifyUserEmployeeTypeBuccaneer";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "BUCCANEER");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
		
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "B666", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	@Test
    public void test103ModifyUserEmployeeTypeBartender() throws Exception {
		final String TEST_NAME = "test103ModifyUserEmployeeTypeBartender";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "BARTENDER");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	/**
	 * Cost center has two mappings. Strong mapping should not be applied here as the condition is false.
	 * The weak mapping should be overridden by the change we try here.
	 */
	@Test
    public void test104ModifyUserCostCenter() throws Exception {
		final String TEST_NAME = "test104ModifyUserCostCenter";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_COST_CENTER, prismContext, "X000");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	@Test
    public void test105ModifyUserTelephoneNumber() throws Exception {
		final String TEST_NAME = "test105ModifyUserTelephoneNumber";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, prismContext, "1234");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1234", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
	}
	
	@Test
    public void test106ModifyUserRemoveTelephoneNumber() throws Exception {
		final String TEST_NAME = "test106ModifyUserRemoveTelephoneNumber";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, prismContext);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertNull("Unexpected telephone number: "+userJackType.getTelephoneNumber(), userJackType.getTelephoneNumber());
        assertEquals("Wrong Title", PrismTestUtil.createPolyStringType("Happy Pirate"), userJackType.getTitle());
	}
	
	@Test
    public void test200AddUserRapp() throws Exception {
        displayTestTile(this, "test100ModifyUserGivenName");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test200AddUserRapp");
        OperationResult result = task.getResult();
    
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_RAPP_FILENAME));
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);
        
        UserType userAfterType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userAfterType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Unexpected value of employeeNumber, maybe it was generated and should not be?", 
        		"D3ADB33F", userAfterType.getEmployeeNumber());
	}
	
	@Test
    public void test201AddUserLargo() throws Exception {
        displayTestTile(this, "test201AddUserLargo");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test201AddUserLargo");
        // This simulates IMPORT to trigger the channel-limited mapping
        task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_IMPORT));
        OperationResult result = task.getResult();
    
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_LARGO_FILENAME));
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_LARGO_OID, null, task, result);
		display("Largo after", userAfter);
        assertUser(userAfter, USER_LARGO_OID, "largo", "Largo LaGrande", "Largo", "LaGrande");
        
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_DESCRIPTION, "Imported user");
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 2);
                
        UserType userAfterType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userAfterType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
	}
	
	@Test
    public void test202AddUserMonkey() throws Exception {
        displayTestTile(this, "test202AddUserMonkey");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test202AddUserMonkey");
        OperationResult result = task.getResult();
    
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_THREE_HEADED_MONKEY_FILENAME));
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_THREE_HEADED_MONKEY_OID, null, task, result);
		display("User after", userAfter);
//        assertUser(userAfter, USER_THREE_HEADED_MONKEY_OID, "monkey", " Monkey", null, "Monkey");
        assertUser(userAfter, USER_THREE_HEADED_MONKEY_OID, "monkey", "Three-Headed Monkey", null, "Monkey");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);
                
        UserType userAfterType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userAfterType.getLinkRef().size());
        
        result.computeStatus();
        assertSuccess(result);
        
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
	}
	
	@Test
    public void test900DeleteUser() throws Exception {
        displayTestTile(this, "test900DeleteUser");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test900DeleteUser");
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class,
        		USER_JACK_OID, prismContext);
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		try {
			PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
			display("User after", userJack);
			assert false : "User was not deleted: "+userJack;
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		// TODO: check on resource
		
		result.computeStatus();
        assertFailure(result);
	}
	
}
