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
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants.Field;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
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
import com.evolveum.midpoint.test.DummyResourceContoller;
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
import com.evolveum.polygon.common.XMLGregorianCalendarUtils;
import com.mchange.v1.db.sql.Schema;

/**
 * @author katkav
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInbounds extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private String jackEmployeeNumber;
	private String guybrushShadowOrangeOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		setDefaultUserTemplate(USER_TEMPLATE_INBOUNDS_OID);
		assumeResourceAssigmentPolicy(RESOURCE_DUMMY_GREEN_OID, AssignmentPolicyEnforcementType.RELATIVE, false);
	}

    @Test
    public void test101ModifyUserEmployeeTypePirate() throws Exception {
		final String TEST_NAME = "test101ModifyUserEmployeeTypePirate";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestInbounds.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "PIRATE");
        // Make sure that the user has no employeeNumber so it will be generated by userTemplate
        userDelta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER);
        userDelta.addModificationAddProperty(SchemaConstants.PATH_ACTIVATION_VALID_FROM, XmlTypeConverter
				.createXMLGregorianCalendar(System.currentTimeMillis()));
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
//        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_PIRATE_GREEN_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertEquals("Unexpected number of organizationalUnits", 1, userJackType.getOrganizationalUnit().size());
        assertEquals("Wrong organizationalUnit", PrismTestUtil.createPolyStringType("The crew of pirate"), userJackType.getOrganizationalUnit().get(0));
//        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        
        
        jackEmployeeNumber = userJackType.getEmployeeNumber();
        assertNotNull("Wrong employee number. Expected not null value, got " + jackEmployeeNumber, jackEmployeeNumber);
//        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?",
//                GenerateExpressionEvaluator.DEFAULT_LENGTH, jackEmployeeNumber.length());
        
//        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
//        XMLGregorianCalendar monthLater = XmlTypeConverter.addDuration(now, XmlTypeConverter.createDuration("P1M"));
//        assertTrigger(userJack, RecomputeTriggerHandler.HANDLER_URI, monthLater, 100000L);
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
        Task task = taskManager.createTaskInstance(TestInbounds.class.getName() + "." + TEST_NAME);
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
//        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userJack, ROLE_BUCCANEER_GREEN_OID);
        assertAssignments(userJack, 1);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
//        assertEquals("Unexpected number of organizationalUnits", 1, userJackType.getOrganizationalUnit().size());
        assertEquals("Wrong organizationalUnit", PrismTestUtil.createPolyStringType("The crew of buccaneer"), userJackType.getOrganizationalUnit().get(1));
//        assertEquals("Wrong costCenter", "B666", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}
	
	@Test
    public void test103DeleteUserEmployeeTypeBartender() throws Exception {
		final String TEST_NAME = "test103ModifyUserEmployeeTypeBartender";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestInbounds.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
    
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "BUCCANEER");
        deltas.add(userDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);
        
		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
//        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_GREEN_OID);
        assertNotAssignedRole(userJack, ROLE_BUCCANEER_GREEN_OID);
        assertNoAssignments(userJack);
        
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
//        assertEquals("Unexpected number of organizationalUnits", 0, userJackType.getOrganizationalUnit().size());
//        assertEquals("Wrong organizationalUnit", PrismTestUtil.createPolyStringType("The crew of pirate"), userJackType.getOrganizationalUnit().get(0));
//        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
	}

	/**
	 * Not much happens here. Just ordinary account assign. Just make sure
	 * that the inbound mappings do not fail for empty values and that 
	 * we have a good environment for the following tests.
	 * MID-2689
	 */
	@Test
    public void test200AssignAccountOrgange() throws Exception {
		final String TEST_NAME = "test200AssignAccountOrgange";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
    
		// WHEN
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, 
        		USER_GUYBRUSH_FULL_NAME, USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 2);
        
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("Orange account", dummyAccount);
        
        guybrushShadowOrangeOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        PrismObject<ShadowType> shadowOrange = getShadowModel(guybrushShadowOrangeOid);
        display("Orange shadow", shadowOrange);
        
	}
	
	/**
	 * Modify 'gossip' on account (through shadow). That attribute has an inbound
	 * expression that creates an assignment. Make sure it is processed properly.
	 * MID-2689
	 */
	@Test
    public void test202ModifyAccountOrgangeGossip() throws Exception {
		final String TEST_NAME = "test202ModifyAccountOrgangeGossip";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
    
		// WHEN
		modifyObjectAddProperty(ShadowType.class, guybrushShadowOrangeOid, 
				dummyResourceCtlOrange.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
				task, result, ROLE_PIRATE_OID);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, 
        		USER_GUYBRUSH_FULL_NAME, USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 2);
        
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("Orange account", dummyAccount);
        
        guybrushShadowOrangeOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        PrismObject<ShadowType> shadowOrange = getShadowModel(guybrushShadowOrangeOid);
        display("Orange shadow", shadowOrange);
        
	}
	
	/**
	 * Discovered by accident. Just make sure that another change will not destroy anything.
	 * MID-3080
	 */
	@Test
    public void test204AssignAccountOrgangeAgain() throws Exception {
		final String TEST_NAME = "test204AssignAccountOrgangeAgain";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
    
		// WHEN
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, 
        		USER_GUYBRUSH_FULL_NAME, USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 2);
        
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("Orange account", dummyAccount);
        
        guybrushShadowOrangeOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        PrismObject<ShadowType> shadowOrange = getShadowModel(guybrushShadowOrangeOid);
        display("Orange shadow", shadowOrange);
        
	}
	
	/**
	 * Remove the value of 'gossip' attribute on account (through shadow). 
	 * That attribute has an inbound expression that removes an assignment.
	 * Make sure it is processed properly.
	 * MID-2689
	 */
	@Test
    public void test209ModifyAccountOrgangeGossipRemove() throws Exception {
		final String TEST_NAME = "test209ModifyAccountOrgangeGossipRemove";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
    
		// WHEN
		modifyObjectDeleteProperty(ShadowType.class, guybrushShadowOrangeOid, 
				dummyResourceCtlOrange.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
				task, result, ROLE_PIRATE_OID);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, 
        		USER_GUYBRUSH_FULL_NAME, USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 2);
        
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        display("Orange account", dummyAccount);
        
        guybrushShadowOrangeOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_ORANGE_OID);
        PrismObject<ShadowType> shadowOrange = getShadowModel(guybrushShadowOrangeOid);
        display("Orange shadow", shadowOrange);
        
	}
	
}
