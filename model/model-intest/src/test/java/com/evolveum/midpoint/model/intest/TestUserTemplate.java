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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserTemplate extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/object-template");

	protected static final File ROLE_RASTAMAN_FILE = new File(TEST_DIR, "role-rastaman.xml");
	protected static final String ROLE_RASTAMAN_OID = "81ac6b8c-225c-11e6-ab0f-87a169c85cca";

	protected static final File USER_TEMPLATE_MAROONED_FILE = new File(TEST_DIR, "user-template-marooned.xml");
	protected static final String USER_TEMPLATE_MAROONED_OID = "766215e8-5f1e-11e6-94bb-c3b21af53235";


	private static final String ACCOUNT_STAN_USERNAME = "stan";
	private static final String ACCOUNT_STAN_FULLNAME = "Stan the Salesman";

	private static final String EMPLOYEE_TYPE_MAROONED = "marooned";

	private static final int NUMBER_OF_IMPORTED_ROLES = 5;

	private static String jackEmployeeNumber;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAddObjectFromFile(ROLE_RASTAMAN_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOMATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOCRATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTODIDACTIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOGRAPHIC_FILE, initResult);

        repoAddObjectFromFile(USER_TEMPLATE_MAROONED_FILE, initResult);
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_COMPLEX_OID, initResult);
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, EMPLOYEE_TYPE_MAROONED, USER_TEMPLATE_MAROONED_OID, initResult);
	}

	protected int getNumberOfRoles() {
		return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<SystemConfigurationType> systemConfiguration = modelService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
        		null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("System config", systemConfiguration);
        assertNotNull("no system config", systemConfiguration);
        List<ObjectPolicyConfigurationType> defaultObjectPolicyConfiguration = systemConfiguration.asObjectable().getDefaultObjectPolicyConfiguration();
        assertNotNull("No object policy", defaultObjectPolicyConfiguration);
        assertEquals("Wrong object policy size", 4, defaultObjectPolicyConfiguration.size());       // third + fourth are conflict resolution rules
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, null, USER_TEMPLATE_COMPLEX_OID);
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, EMPLOYEE_TYPE_MAROONED, USER_TEMPLATE_MAROONED_OID);

        assertRoles(getNumberOfRoles());
	}

	private void assertObjectTemplate(List<ObjectPolicyConfigurationType> defaultObjectPolicyConfigurations,
			QName objectType, String subtype, String userTemplateOid) {
		for (ObjectPolicyConfigurationType objectPolicyConfiguration: defaultObjectPolicyConfigurations) {
			if (MiscUtil.equals(objectPolicyConfiguration.getType(), objectType) &&
					MiscUtil.equals(objectPolicyConfiguration.getSubtype(), subtype) &&
					MiscUtil.equals(objectPolicyConfiguration.getObjectTemplateRef().getOid(), userTemplateOid)) {
				return;
			}
		}
		AssertJUnit.fail("Object template for "+objectType+":"+subtype+"="+userTemplateOid+" not found");
	}

	@Test
    public void test100ModifyUserGivenName() throws Exception {
		final String TEST_NAME = "test100ModifyUserGivenName";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_GIVEN_NAME, prismContext, new PolyString("Jackie"));
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

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

        // original value of 0 should be gone now, because the corresponding item in user template is marked as non-tolerant
        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L, 456L);

        // timezone mapping is normal-strength. The source (locality) has not changed.
        // The mapping should not be activated (MID-3040)
        PrismAsserts.assertNoItem(userJack, UserType.F_TIMEZONE);
	}

    @Test
    public void test101ModifyUserEmployeeTypePirate() throws Exception {
		final String TEST_NAME = "test101ModifyUserEmployeeTypePirate";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "PIRATE");
        // Make sure that the user has no employeeNumber so it will be generated by userTemplate
        userDelta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER);
        deltas.add(userDelta);

		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
	    AssignmentType pirateAssignment = assertAssignedRole(userJack, ROLE_PIRATE_OID);
	    assertEquals("Wrong originMappingName", "assignment-from-employeeType", pirateAssignment.getMetadata().getOriginMappingName());
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "BUCCANEER");
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
		AssignmentType pirateAssignment = assertAssignedRole(userJack, ROLE_PIRATE_OID);
		// the value was already there; so the identifier should remain intact
		assertEquals("Wrong originMappingName", "assignment-from-employeeType", pirateAssignment.getMetadata().getOriginMappingName());
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_EMPLOYEE_TYPE, prismContext, "BARTENDER");
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_COST_CENTER, prismContext, "X000");
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, prismContext, "1234");
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

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
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
	}

	@Test
    public void test106ModifyUserRemoveTelephoneNumber() throws Exception {
		final String TEST_NAME = "test106ModifyUserRemoveTelephoneNumber";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
        		USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, prismContext);
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);

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
        assertNull("Unexpected telephone number: " + userJackType.getTelephoneNumber(), userJackType.getTelephoneNumber());
        assertEquals("Wrong Title", PrismTestUtil.createPolyStringType("Happy Pirate"), userJackType.getTitle());
	}

	@Test
    public void test107ModifyUserSetTelephoneNumber() throws Exception {
		final String TEST_NAME = "test107ModifyUserSetTelephoneNumber";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, task, result, "1 222 3456789");

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

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
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
	}

	/**
	 * Reconcile the user. Check that nothing really changes.
	 * MID-3040
	 */
	@Test
    public void test120ReconcileUser() throws Exception {
		final String TEST_NAME = "test121ModifyUserReplaceLocality";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

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
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());

        // timezone mapping is normal-strength. This is reconciliation.
        // The mapping should not be activated (MID-3040)
        PrismAsserts.assertNoItem(userJack, UserType.F_TIMEZONE);
	}

	/**
	 * MID-3040
	 */
	@Test
    public void test121ModifyUserReplaceLocality() throws Exception {
		final String TEST_NAME = "test121ModifyUserReplaceLocality";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Tortuga"));

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

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

        PrismAsserts.assertEqualsPolyString("Wrong locality", "Tortuga", userJackType.getLocality());
        assertEquals("Wrong timezone", "High Seas/Tortuga", userJackType.getTimezone());

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
	}

	@Test
    public void test140AssignDummy() throws Exception {
		final String TEST_NAME = "test140AssignDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
        IntegrationTestTools.assertExtensionProperty(userJack, PIRACY_COLORS, "none");
	}

	@Test
    public void test149UnAssignDummy() throws Exception {
		final String TEST_NAME = "test149UnAssignDummy";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0004"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
		AssignmentType rumAssignment = assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
		assertEquals("Wrong originMappingName", "Org mapping", rumAssignment.getMetadata().getOriginMappingName());
		assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertAssignments(userJack, 2);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test151ModifyJackOrganizationalUnitOffense() throws Exception {
		final String TEST_NAME = "test151ModifyJackOrganizationalUnitOffense";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0003"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
		AssignmentType offenseAssignment = assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
		assertEquals("Wrong originMappingName", "Org mapping", offenseAssignment.getMetadata().getOriginMappingName());
		assertHasOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignments(userJack, 2);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test152ModifyJackOrganizationalUnitAddRum() throws Exception {
		final String TEST_NAME = "test152ModifyJackOrganizationalUnitAddRum";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("F0004"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
		AssignmentType rumAssignment = assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
		assertEquals("Wrong originMappingName", "Org mapping", rumAssignment.getMetadata().getOriginMappingName());
		AssignmentType offenseAssignment = assertAssignedOrg(userJack, ORG_MINISTRY_OF_OFFENSE_OID);
		assertEquals("Wrong originMappingName", "Org mapping", offenseAssignment.getMetadata().getOriginMappingName());
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
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
//        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
	}

	@Test
    public void test153ModifyJackOrganizationalUnitDeleteOffense() throws Exception {
		final String TEST_NAME = "test153ModifyJackOrganizationalUnitDeleteOffense";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
	 * Reconcile user Jack, see that everything is OK.
	 */
	@Test
    public void test156ReconcileJack() throws Exception {
		final String TEST_NAME = "test156ReconcileJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        reconcileUser(USER_JACK_OID, task, result);

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
	}

	/**
	 * Creates two orgs on demand.
	 */
	@Test
    public void test157ModifyJackOrganizationalUnitFD0023() throws Exception {
		final String TEST_NAME = "test157ModifyJackOrganizationalUnitFD0023";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
    public void test159ModifyJackDeleteOrganizationalUnitFD002() throws Exception {
		final String TEST_NAME = "test159ModifyJackDeleteOrganizationalUnitFD002";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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

    @Test
    public void test160ModifyUserGivenNameAgain() throws Exception {
        TestUtil.displayTestTitle(this, "test160ModifyUserGivenNameAgain");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestUserTemplate.class.getName() + ".test160ModifyUserGivenNameAgain");
        OperationResult result = task.getResult();

        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, prismContext, new PolyString("JACKIE"));
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        ObjectDeltaOperation<?> objectDeltaOperation = dummyAuditService.getExecutionDelta(0, ChangeType.MODIFY, UserType.class);
        assertEquals("unexpected number of modifications in audited delta", 10, objectDeltaOperation.getObjectDelta().getModifications().size());   // givenName + badLuck + modifyTimestamp
        PropertyDelta badLuckDelta = objectDeltaOperation.getObjectDelta().findPropertyDelta(new ItemPath(UserType.F_EXTENSION, PIRACY_BAD_LUCK));
        assertNotNull("badLuck delta was not found", badLuckDelta);
        List<PrismValue> oldValues = (List<PrismValue>) badLuckDelta.getEstimatedOldValues();
        assertNotNull("badLuck delta has null estimatedOldValues field", oldValues);
        PrismAsserts.assertEqualsCollectionUnordered("badLuck delta has wrong estimatedOldValues", oldValues, new PrismPropertyValue<Long>(123L), new PrismPropertyValue<Long>(456L));
    }

    @Test
    public void test162ModifyUserGivenNameAgainPhantomChange() throws Exception {
    	final String TEST_NAME = "test162ModifyUserGivenNameAgainPhantomChange";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User before", userBefore);

        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, prismContext, new PolyString("JACKIE"));      // this is a phantom change
        deltas.add(userDelta);

        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L);

        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        ObjectDeltaOperation<?> objectDeltaOperation = dummyAuditService.getExecutionDelta(0, ChangeType.MODIFY, UserType.class);
        assertEquals("unexpected number of modifications in audited delta", 7, objectDeltaOperation.getObjectDelta().getModifications().size());   // givenName + modifyTimestamp, modifyChannel, modifierRef
    }

    @Test
    public void test165ModifyUserGivenNameAgainAgain() throws Exception {
    	final String TEST_NAME = "test165ModifyUserGivenNameAgainAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User before", userBefore);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, prismContext, new PolyString("jackie"));
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // all the values should be gone now, because the corresponding item in user template is marked as non-tolerant
        PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_EXTENSION, PIRACY_BAD_LUCK));
    }

    private PrismObject<OrgType> assertOnDemandOrgExists(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PrismTestUtil.createPolyString(orgName));
		return org;
	}

	private void assertOnDemandOrgAssigned(String orgName, PrismObject<UserType> user) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<OrgType> org = assertOnDemandOrgExists(orgName);
		PrismAsserts.assertPropertyValue(org, OrgType.F_DESCRIPTION, "Created on demand from user "+user.asObjectable().getName());
		assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
	}

	/**
	 * Setting employee type to THIEF is just one part of the condition to assign
	 * the Thief role. The role should not be assigned now.
	 */
	@Test
    public void test170ModifyUserGuybrushEmployeeTypeThief() throws Exception {
		final String TEST_NAME = "test170ModifyUserGuybrushEmployeeTypeThief";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, task, result, "THIEF");

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedNoRole(userAfter);
	}

	/**
	 * Setting honorificPrefix satisfies the condition to assign
	 * the Thief role.
	 */
	@Test
    public void test172ModifyUserGuybrushHonorificPrefix() throws Exception {
		final String TEST_NAME = "test172ModifyUserGuybrushHonorificPrefix";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result,
        		PrismTestUtil.createPolyString("Thf."));

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		AssignmentType thiefAssignment = assertAssignedRole(userAfter, ROLE_THIEF_OID);
		assertEquals("Wrong originMappingName", "assignment-from-employeeType-thief", thiefAssignment.getMetadata().getOriginMappingName());
	}

	/**
	 * Removing honorificPrefix should make the condition false again, which should cause
	 * that Thief role is unassigned.
	 */
	@Test
    public void test174ModifyUserGuybrushHonorificPrefixNone() throws Exception {
		final String TEST_NAME = "test174ModifyUserGuybrushHonorificPrefixNone";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedRole(userBefore, ROLE_THIEF_OID);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedNoRole(userAfter);
	}

	/**
	 * Setting employee type to marooned. This should cause switch to different user template.
	 */
	@Test
    public void test180ModifyUserGuybrushEmployeeTypeMarooned() throws Exception {
		final String TEST_NAME = "test180ModifyUserGuybrushEmployeeTypeMarooned";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, task, result, EMPLOYEE_TYPE_MAROONED);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		assertEquals("Wrong costCenter", "NOCOST", userAfter.asObjectable().getCostCenter());

		assertAssignedNoRole(userAfter);
	}

	@Test
    public void test189ModifyUserGuybrushEmployeeTypeNone() throws Exception {
		final String TEST_NAME = "test189ModifyUserGuybrushEmployeeTypeNone";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		assertEquals("Wrong costCenter", "NOCOST", userAfter.asObjectable().getCostCenter());

		assertAssignedNoRole(userAfter);
	}

	/**
	 * Assignment mapping with domain. Control: nothing should happen.
	 * MID-3692
	 */
	@Test
    public void test190ModifyUserGuybrushOrganizationWhateveric() throws Exception {
		final String TEST_NAME = "test190ModifyUserGuybrushOrganizationWhateveric";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);
        assertAssignments(userBefore, 1);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result, createPolyString("Whateveric"));

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION, createPolyString("Whateveric"));

		assertAssignedNoRole(userAfter);
		assertAssignments(userAfter, 1);
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test191ModifyUserGuybrushOrganizationAutomatic() throws Exception {
		final String TEST_NAME = "test191ModifyUserGuybrushOrganizationAutomatic";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result, createPolyString("AUTO-matic"));

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
				createPolyString("AUTO-matic"));

		AssignmentType autoAssignment = assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertEquals("Wrong originMappingName", "automappic", autoAssignment.getMetadata().getOriginMappingName());
		assertAssignments(userAfter, 2);

		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test192ModifyUserGuybrushOrganizationAddMixed() throws Exception {
		final String TEST_NAME = "test192ModifyUserGuybrushOrganizationAddMixed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("DEMO-cratic"),
        		createPolyString("AUTO-cratic"),
        		createPolyString("plutocratic"),
        		createPolyString("AUTO-didactic")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
				createPolyString("AUTO-matic"),
				createPolyString("DEMO-cratic"),
        		createPolyString("AUTO-cratic"),
        		createPolyString("plutocratic"),
        		createPolyString("AUTO-didactic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
		assertAssignments(userAfter, 4);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test193ModifyUserGuybrushOrganizationAddOutOfDomain() throws Exception {
		final String TEST_NAME = "test193ModifyUserGuybrushOrganizationAddOutOfDomain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("meritocratic"),
        		createPolyString("piratocratic")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
				createPolyString("AUTO-matic"),
				createPolyString("DEMO-cratic"),
        		createPolyString("AUTO-cratic"),
        		createPolyString("plutocratic"),
        		createPolyString("AUTO-didactic"),
        		createPolyString("meritocratic"),
        		createPolyString("piratocratic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
		assertAssignments(userAfter, 4);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test194ModifyUserGuybrushOrganizationDeleteMixed() throws Exception {
		final String TEST_NAME = "test194ModifyUserGuybrushOrganizationDeleteMixed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("AUTO-matic"),
        		createPolyString("plutocratic"),
        		createPolyString("meritocratic"),
        		createPolyString("AUTO-didactic")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
				createPolyString("DEMO-cratic"),
        		createPolyString("AUTO-cratic"),
        		createPolyString("piratocratic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignments(userAfter, 2);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test195ModifyUserGuybrushOrganizationDeleteOutOfDomain() throws Exception {
		final String TEST_NAME = "test195ModifyUserGuybrushOrganizationDeleteOutOfDomain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("piratocratic"),
        		createPolyString("DEMO-cratic")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
        		createPolyString("AUTO-cratic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignments(userAfter, 2);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * Make sure that the manually assigned roles will not mess with the mapping.
	 * MID-3692
	 */
	@Test
    public void test196GuybrushAssignCaptain() throws Exception {
		final String TEST_NAME = "test196GuybrushAssignCaptain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
        		createPolyString("AUTO-cratic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
		assertAssignments(userAfter, 3);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * Make sure that a role automatically assigned by a different mapping will not mess with this mapping.
	 * MID-3692
	 */
	@Test
    public void test197ModifyGuybrushEmployeeTypePirate() throws Exception {
		final String TEST_NAME = "test197ModifyGuybrushEmployeeTypePirate";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID,  UserType.F_EMPLOYEE_TYPE, task, result, "PIRATE");

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
        		createPolyString("AUTO-cratic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
		assertAssignedRole(userAfter, ROLE_PIRATE_OID);
		assertAssignments(userAfter, 4);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * Make sure that changes in this mapping will not influence other assigned roles.
	 * MID-3692
	 */
	@Test
    public void test198AModifyUserGuybrushOrganizationAddInDomain() throws Exception {
		final String TEST_NAME = "test198AModifyUserGuybrushOrganizationAddInDomain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("AUTO-graphic"),
        		createPolyString("AUTO-matic")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
				createPolyString("Whateveric"),
        		createPolyString("AUTO-cratic"),
        		createPolyString("AUTO-graphic"),
        		createPolyString("AUTO-matic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
		assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
		assertAssignedRole(userAfter, ROLE_PIRATE_OID);
		assertAssignments(userAfter, 6);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * Make sure that changes in this mapping will not influence other assigned roles.
	 * MID-3692
	 */
	@Test
    public void test198BModifyUserGuybrushOrganizationDeleteMixed() throws Exception {
		final String TEST_NAME = "test198BModifyUserGuybrushOrganizationDeleteMixed";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 6);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
        		createPolyString("AUTO-cratic"),
        		createPolyString("Whateveric")
        	);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
        		createPolyString("AUTO-graphic"),
        		createPolyString("AUTO-matic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
		assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
		assertAssignedRole(userAfter, ROLE_PIRATE_OID);
		assertAssignments(userAfter, 5);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test199AGuyBrushModifyEmployeeTypeWannabe() throws Exception {
		final String TEST_NAME = "test199AGuyBrushModifyEmployeeTypeWannabe";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 5);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID,  UserType.F_EMPLOYEE_TYPE, task, result, "wannabe");

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
        		createPolyString("AUTO-graphic"),
        		createPolyString("AUTO-matic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
		assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
		assertAssignments(userAfter, 4);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692
	 */
	@Test
    public void test199BGuyBrushUnassignCaptain() throws Exception {
		final String TEST_NAME = "test199BGuyBrushUnassignCaptain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
        		createPolyString("AUTO-graphic"),
        		createPolyString("AUTO-matic")
			);

		assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
		assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
		assertAssignments(userAfter, 3);

		// Make sure nothing was created on demand
		assertRoles(getNumberOfRoles());
	}

	/**
	 * MID-3692, MID-3700
	 */
	@Test
    public void test199CModifyUserGuybrushOrganizationCleanup() throws Exception {
		final String TEST_NAME = "test199CModifyUserGuybrushOrganizationCleanup";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result);

		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
		display("User after", userAfter);

		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);

		assertAssignedNoRole(userAfter);

		assertRoles(getNumberOfRoles());
	}

	@Test
    public void test200AddUserRapp() throws Exception {
		final String TEST_NAME = "test200AddUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
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
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber, maybe it was generated and should not be?",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
	}

	@Test
    public void test201AddUserLargo() throws Exception {
        TestUtil.displayTestTitle(this, "test201AddUserLargo");

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

        // locality is null; the description comes from inbound mapping on dummy resource
        // PrismAsserts.assertPropertyValue(userAfter, UserType.F_DESCRIPTION, "Came from null");

        // TODO TEMPORARILY allowing value of "Imported user", because the inbound mapping is not applied because of
        // the "locality" attribute is null (Skipping inbound for {...}location in Discr(RSD(account (default)
        // @10000000-0000-0000-0000-000000000004)): Not a full shadow and account a priori delta exists, but
        // doesn't have change for processed property.
        //
        // Either we fix this or recommend setting volatility=unpredictable for such situations.
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
        TestUtil.displayTestTitle(this, "test202AddUserMonkey");

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
	 * MID-3186
	 */
	@Test
    public void test204AddUserHerman() throws Exception {
		final String TEST_NAME = "test204AddUserHerman";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_HERMAN_FILE);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        deltas.add(userDelta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_HERMAN_OID, null, task, result);
        assertUser(userAfter, USER_HERMAN_OID, USER_HERMAN_USERNAME, USER_HERMAN_FULL_NAME, "Herman", "Toothrot");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Monkey Island");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
	}

	@Test
    public void test220AssignRoleSailorToUserRapp() throws Exception {
		final String TEST_NAME = "test220AssignRoleSailorToUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		assignRole(USER_RAPP_OID, ROLE_SAILOR_OID, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 2);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-TITANIC", userAfterType.getCostCenter());
	}

	/**
	 * MID-3028
	 */
	@Test
    public void test229UnassignRoleSailorFromUserRapp() throws Exception {
		final String TEST_NAME = "test220AssignRoleSailorToUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		unassignRole(USER_RAPP_OID, ROLE_SAILOR_OID, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
	}


	/**
	 * Role Captains has focus mapping for the same costCenter as is given
	 * by the user template.
	 */
	@Test
    public void test230AssignRoleCaptainToUserRapp() throws Exception {
		final String TEST_NAME = "test230AssignRoleCaptainToUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		assignRole(USER_RAPP_OID, ROLE_CAPTAIN_OID, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
	}

	/**
	 * Object template mapping for cost center is weak, role mapping is normal.
	 * Direct modification should override both.
	 */
	@Test
    public void test232ModifyUserRappCostCenter() throws Exception {
		final String TEST_NAME = "test232ModifyUserRappCostCenter";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_COST_CENTER, task, result, "CC-RAPP");

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * Role Captains has focus mapping for the same costCenter as is given
	 * by the user template.
	 * MID-3028
	 */
	@Test
    public void test239UnassignRoleCaptainFromUserRapp() throws Exception {
		final String TEST_NAME = "test239UnassignRoleCaptainFromUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		unassignRole(USER_RAPP_OID, ROLE_CAPTAIN_OID, task, result);

		// THEN
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	@Test
    public void test240ModifyUserRappLocalityScabb() throws Exception {
		final String TEST_NAME = "test240ModifyUserRappLocalityScabb";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User before", userBefore);

		assertEquals("Wrong timezone", "High Seas/null", userBefore.asObjectable().getTimezone());
		assertEquals("Wrong locale", null, userBefore.asObjectable().getLocale());

		// WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Scabb Island"));

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        assertEquals("Wrong timezone", "High Seas/Scabb Island", userAfterType.getTimezone());
        assertEquals("Wrong locale", "SC", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}


	/**
	 * Role Rastaman has focus mapping for the same timezone as is given
	 * by the user template. This mapping is normal strength. Even though
	 * it is evaluated after the template the mapping, role assignment is an
	 * explicit delta and the mapping should be applied.
	 * MID-3040
	 */
	@Test
    public void test242AssignRoleRastamanToUserRapp() throws Exception {
		final String TEST_NAME = "test242AssignRoleRastamanToUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		assignRole(USER_RAPP_OID, ROLE_RASTAMAN_OID, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * Role Rastaman has focus mapping for the same timezone as is given
	 * by the user template. This mapping is normal strength. It is evaluated
	 * after the template the mapping, so it should not be applied because
	 * there is already a-priori delta from the template.
	 * MID-3040
	 */
	@Test
    public void test244ModifyUserRappLocalityCoffin() throws Exception {
		final String TEST_NAME = "test244ModifyUserRappLocalityCoffin";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User before", userBefore);

		// WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("Coffin"));

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        assertEquals("Wrong timezone", "High Seas/Coffin", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * Similar to test244, but also use reconcile option.
	 * MID-3040
	 */
	@Test
    public void test245ModifyUserRappLocalityUnderReconcile() throws Exception {
		final String TEST_NAME = "test245ModifyUserRappLocalityUnderReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User before", userBefore);

		ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_RAPP_OID, new ItemPath(UserType.F_LOCALITY),
				PrismTestUtil.createPolyString("Six feet under"));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		ModelExecuteOptions options = ModelExecuteOptions.createReconcile();

		// WHEN
		modelService.executeChanges(deltas, options, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        assertEquals("Wrong timezone", "High Seas/Six feet under", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * Changing timezone. timezone is a target of (normal) object template mapping and
	 * (normal) role mapping. But as this is primary delta none of the mappings should
	 * be applied.
	 * MID-3040
	 */
	@Test
    public void test246ModifyUserRappTimezoneMonkey() throws Exception {
		final String TEST_NAME = "test246ModifyUserRappTimezoneMonkey";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User before", userBefore);

		// WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_TIMEZONE, task, result, "Monkey Island");

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        assertEquals("Wrong timezone", "Monkey Island", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * Changing locale. Locale is a target of (weak) object template mapping and
	 * (normal) role mapping. But as this is primary delta none of the mappings should
	 * be applied.
	 * MID-3040
	 */
	@Test
    public void test247ModifyUserRappLocaleMI() throws Exception {
		final String TEST_NAME = "test247ModifyUserRappLocaleMI";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User before", userBefore);

		// WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_LOCALE, task, result, "MI");

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
		display("User after", userAfter);

		assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        // The normal mapping from the rastaman role was applied at this point
        // This is sourceless mapping and there is no a-priori delta
        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());

        assertEquals("Wrong locale", "MI", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	@Test
    public void test249UnassignRoleRastamanFromUserRapp() throws Exception {
		final String TEST_NAME = "test249UnassignRoleRastamanFromUserRapp";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		unassignRole(USER_RAPP_OID, ROLE_RASTAMAN_OID, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLinks(userAfter, 1);

        // Role is unassigned. The mapping was authoritative, so it removed the value
        assertEquals("Wrong timezone", null, userAfterType.getTimezone());

        assertEquals("Wrong locale", "MI", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
        		"D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
	}

	/**
	 * MID-3186
	 */
	@Test
    public void test300ImportStanFromEmeraldResource() throws Exception {
		final String TEST_NAME = "test300ImportStanFromEmeraldResource";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount dummyAccountBefore = new DummyAccount(ACCOUNT_STAN_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
        		ACCOUNT_STAN_FULLNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
        		"Melee Island");
        dummyResourceEmerald.addAccount(dummyAccountBefore);

        PrismObject<ShadowType> shadowBefore = findAccountByUsername(ACCOUNT_STAN_USERNAME, resourceDummyEmerald);
        display("Shadow before", shadowBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(shadowBefore.getOid(), task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_STAN_USERNAME);
        display("User after", userAfter);
        assertNotNull("No stan user", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(ACCOUNT_STAN_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Melee Island"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Melee Island");
	}

	/**
	 * Modify stan accoutn and reimport from the emerald resource. Make sure that
	 * the normal mapping for locality in the object template is properly activated (as there is
	 * an delta from inbound mapping in the emerald resource).
	 * MID-3186
	 */
	@Test
    public void test302ModifyStanAccountAndReimport() throws Exception {
		final String TEST_NAME = "test302ModifyStanAccountAndReimport";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount dummyAccountBefore = dummyResourceEmerald.getAccountByUsername(ACCOUNT_STAN_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
        		"Booty Island");

        PrismObject<ShadowType> shadowBefore = findAccountByUsername(ACCOUNT_STAN_USERNAME, resourceDummyEmerald);
        display("Shadow before", shadowBefore);

		// WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(shadowBefore.getOid(), task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_STAN_USERNAME);
        display("User after", userAfter);
        assertNotNull("No stan user", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(ACCOUNT_STAN_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Booty Island"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Booty Island");
	}

	/**
	 * Move the time to the future. See if the time-based mapping in user template is properly recomputed.
	 */
	@Test
    public void test800Kaboom() throws Exception {
		final String TEST_NAME = "test800Kaboom";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        now.add(XmlTypeConverter.createDuration("P1M1D"));
        clock.override(now);

        // WHEN
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Kaboom!"));
        assertNoTrigger(userJack);
	}

	@Test
    public void test900DeleteUser() throws Exception {
		final String TEST_NAME = "test900DeleteUser";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

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

	@Test
    public void test950CreateUserJackWithoutTemplate() throws Exception {
		final String TEST_NAME = "test950CreateUserJackWithoutTemplate";
        displayTestTitle(TEST_NAME);

        // GIVEN
        setDefaultUserTemplate(null);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        addObject(USER_JACK_FILE, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignments(userJack, 0);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

		PrismAsserts.assertNoItem(userJack, UserType.F_ORGANIZATIONAL_UNIT);

	}

	/**
	 * Would creates org on demand if the template would be active. But it is not.
	 */
	@Test
    public void test952ModifyJackOrganizationalUnitFD004() throws Exception {
		final String TEST_NAME = "test952ModifyJackOrganizationalUnitFD004";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("FD004"));

		// THEN
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		display("User after", userJack);

		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
		PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("FD004"));

        assertAssignments(userJack, 0);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        PrismObject<OrgType> org = findObjectByName(OrgType.class, "FD004");
        assertNull("Found org "+org+" but not expecting it", org);
	}

	/**
	 * Set the template. Reconcile the user that should have org created on demand (but does not).
	 * The org should be created.
	 */
	@Test
    public void test960ReconcileUserJackWithTemplate() throws Exception {
		final String TEST_NAME = "test960ModifyUserJackWithTemplate";
        displayTestTitle(TEST_NAME);

        // GIVEN
        setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		// WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
 		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
 		display("User after", userJack);

 		PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
		assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
		assertOnDemandOrgAssigned("FD004", userJack);

		assertAssignments(userJack, 2);
		assertLinks(userJack, 1);
	}
}
