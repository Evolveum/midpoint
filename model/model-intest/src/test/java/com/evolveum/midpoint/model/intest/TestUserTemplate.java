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
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.util.TestUtil.assertFailure;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants.Field;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserTemplate extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static String jackEmployeeNumber;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);
	}
		
	@Test
    public void test100ModifyUserGivenName() throws Exception {
        TestUtil.displayTestTile(this, "test100ModifyUserGivenName");

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
        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userJack);
        assertAssignments(userJack, 1);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Jackie"));
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar monthLater = XmlTypeConverter.addDuration(now, XmlTypeConverter.createDuration("P1M"));
        assertTrigger(userJack, RecomputeTriggerHandler.HANDLER_URI, monthLater, 100000L);
	}
	
	@Test
    public void test101ModifyUserEmployeeTypePirate() throws Exception {
		final String TEST_NAME = "test101ModifyUserEmployeeTypePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        
        jackEmployeeNumber = userJackType.getEmployeeNumber();
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, jackEmployeeNumber.length());
        
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        XMLGregorianCalendar monthLater = XmlTypeConverter.addDuration(now, XmlTypeConverter.createDuration("P1M"));
        assertTrigger(userJack, RecomputeTriggerHandler.HANDLER_URI, monthLater, 100000L);
	}
	
	/**
	 * Switch employeeType from PIRATE to BUCCANEER. This makes one condition to go false and the other to go
	 * true. For the same role assignement value. So nothing should be changed.
	 */
	@Test
    public void test102ModifyUserEmployeeTypeBuccaneer() throws Exception {
		final String TEST_NAME = "test102ModifyUserEmployeeTypeBuccaneer";
        TestUtil.displayTestTile(this, TEST_NAME);

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
		
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "B666", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	@Test
    public void test103ModifyUserEmployeeTypeBartender() throws Exception {
		final String TEST_NAME = "test103ModifyUserEmployeeTypeBartender";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
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
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	@Test
    public void test105ModifyUserTelephoneNumber() throws Exception {
		final String TEST_NAME = "test105ModifyUserTelephoneNumber";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1234", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
	}
	
	@Test
    public void test106ModifyUserRemoveTelephoneNumber() throws Exception {
		final String TEST_NAME = "test106ModifyUserRemoveTelephoneNumber";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertNull("Unexpected telephone number: "+userJackType.getTelephoneNumber(), userJackType.getTelephoneNumber());
        assertEquals("Wrong Title", PrismTestUtil.createPolyStringType("Happy Pirate"), userJackType.getTitle());
	}
	
	@Test
    public void test107ModifyUserSetTelephoneNumber() throws Exception {
		final String TEST_NAME = "test107ModifyUserSetTelephoneNumber";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, prismContext, "1 222 3456789");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
	}
	
	@Test
    public void test110AssignDummy() throws Exception {
		final String TEST_NAME = "test110AssignDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
        IntegrationTestTools.assertExtensionProperty(userJack, PIRACY_COLORS, "none");
	}
	
	@Test
    public void test119UnAssignDummy() throws Exception {
		final String TEST_NAME = "test119UnAssignDummy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test150ModifyJackOrganizationalUnitRum() throws Exception {
		final String TEST_NAME = "test150ModifyJackOrganizationalUnitRum";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0004"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test151ModifyJackOrganizationalUnitOffense() throws Exception {
		final String TEST_NAME = "test151ModifyJackOrganizationalUnitOffense";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0003"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test152ModifyJackOrganizationalUnitAddRum() throws Exception {
		final String TEST_NAME = "test152ModifyJackOrganizationalUnitAddRum";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0004"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignments(userJack, 3);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test153ModifyJackOrganizationalUnitDeleteOffense() throws Exception {
		final String TEST_NAME = "test153ModifyJackOrganizationalUnitDeleteOffense";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0003"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertAssignments(userJack, 2);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	/**
	 * Creates org on demand.
	 */
	@Test
    public void test155ModifyJackOrganizationalUnitFD001() throws Exception {
		final String TEST_NAME = "test155ModifyJackOrganizationalUnitFD001";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("FD001"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);

        assertOnDemandOrgAssigned("FD001", userJack);
        
        assertAssignments(userJack, 3);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}
	
	/**
	 * Creates two orgs on demand.
	 */
	@Test
    public void test156ModifyJackOrganizationalUnitFD0023() throws Exception {
		final String TEST_NAME = "test156ModifyJackOrganizationalUnitFD0023";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		PrismTestUtil.createPolyString("FD002"), PrismTestUtil.createPolyString("FD003"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        
        assertOnDemandOrgAssigned("FD001", userJack);
        assertOnDemandOrgAssigned("FD002", userJack);
        assertOnDemandOrgAssigned("FD003", userJack);
                
        assertAssignments(userJack, 5);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}
	
	@Test
    public void test157ModifyJackDeleteOrganizationalUnitFD002() throws Exception {
		final String TEST_NAME = "test157ModifyJackDeleteOrganizationalUnitFD002";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                    
		// WHEN
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		PrismTestUtil.createPolyString("FD002"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        
        assertOnDemandOrgAssigned("FD001", userJack);
        assertOnDemandOrgAssigned("FD003", userJack);
                
        assertAssignments(userJack, 4);
        
        assertOnDemandOrgExists("FD002");
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: "+userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}
	
	private PrismObject<OrgType> assertOnDemandOrgExists(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PrismTestUtil.createPolyString(orgName));
		return org;
	}
	
	private void assertOnDemandOrgAssigned(String orgName, PrismObject<UserType> user) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = assertOnDemandOrgExists(orgName);
		PrismAsserts.assertPropertyValue(org, OrgType.F_DESCRIPTION, "Created on demand from user "+user.asObjectable().getName());
		assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());		
	}
	
	@Test
    public void test200AddUserRapp() throws Exception {
        TestUtil.displayTestTile(this, "test100ModifyUserGivenName");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test200AddUserRapp");
        OperationResult result = task.getResult();
    
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_RAPP_FILE);
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
        TestUtil.assertSuccess(result);
        
        assertEquals("Unexpected value of employeeNumber, maybe it was generated and should not be?", 
        		"D3ADB33F", userAfterType.getEmployeeNumber());
	}
	
	@Test
    public void test201AddUserLargo() throws Exception {
        TestUtil.displayTestTile(this, "test201AddUserLargo");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test201AddUserLargo");
        // This simulates IMPORT to trigger the channel-limited mapping
        task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_IMPORT));
        OperationResult result = task.getResult();
    
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_LARGO_FILE);
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
        TestUtil.assertSuccess(result);
        
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
	}
	
	@Test
    public void test202AddUserMonkey() throws Exception {
        TestUtil.displayTestTile(this, "test202AddUserMonkey");

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
        TestUtil.assertSuccess(result);
        
        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?", 
        		GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
	}
	
	/**
	 * Move the time to the future. See if the time-based mapping in user template is properly recomputed.
	 */
	@Test
    public void test800Kaboom() throws Exception {
		final String TEST_NAME = "test800Kaboom";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        now.add(XmlTypeConverter.createDuration("P1M1D"));
        clock.override(now);
        
        // WHEN
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true);
        
        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Kaboom!"));
        assertNoTrigger(userJack);
	}
	
	@Test
    public void test900DeleteUser() throws Exception {
		final String TEST_NAME = "test900DeleteUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
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
        TestUtil.assertFailure(result);
	}
	
}
