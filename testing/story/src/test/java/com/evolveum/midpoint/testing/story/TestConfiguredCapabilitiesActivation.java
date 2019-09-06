/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test for resources with configured capabilities (MID-5400)
 * 
 * @author Gustav Palos
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConfiguredCapabilitiesActivation extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "configured-capabilities-activation");
	
	protected static final File RESOURCE_DUMMY_ACTIVATION_FILE = new File(TEST_DIR, "resource-dummy-activation.xml");
	protected static final String RESOURCE_DUMMY_ACTIVATION_OID = "4bac305c-ed1f-4919-9670-e11863156811";
	protected static final String RESOURCE_DUMMY_ACTIVATION_NAME = "activation";

	private static final File RESOURCE_DUMMY_ACTIVATION_NATIVE_FILE = new File(TEST_DIR, "resource-dummy-activation-native.xml");
	private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_OID = "4bac305c-ed1f-4919-aaaa-e11863156811";
	private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME = "activation-native";
	
	protected static final File SHADOW_FILE = new File(TEST_DIR, "shadow-sample.xml");
	protected static final String SHADOW_OID = "6925f5a7-2acb-409b-b2e1-94a6534a9745";

	private static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
	private static final String ROLE_PIRATE_OID = "34713dae-8d56-4717-b184-86d02c9a2361";

	private DummyResourceContoller dummyActivationNativeController;
	private DummyResource dummyActivationNativeResource;
	private ResourceType resourceActivationNativeType;
	private PrismObject<ResourceType> resourceActivationNative;


	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_ACTIVATION_NAME, RESOURCE_DUMMY_ACTIVATION_FILE, RESOURCE_DUMMY_ACTIVATION_OID, initTask, initResult);
		addObject(SHADOW_FILE);

//		dummyActivationNativeController = DummyResourceContoller.create(RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME, resourceActivationNative);
//		dummyActivationNativeController.extendSchemaPirate();
//		dummyActivationNativeResource = dummyActivationNativeController.getDummyResource();
//		resourceActivationNative = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_ACTIVATION_NATIVE_FILE, RESOURCE_DUMMY_ACTIVATION_NATIVE_OID, initTask, initResult);
//		resourceActivationNativeType = resourceActivationNative.asObjectable();
//		dummyActivationNativeController.setResource(resourceActivationNative);

		dummyActivationNativeController = initDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME, RESOURCE_DUMMY_ACTIVATION_NATIVE_FILE, RESOURCE_DUMMY_ACTIVATION_NATIVE_OID, initTask, initResult);
		dummyActivationNativeResource = dummyActivationNativeController.getDummyResource();

		importObjectFromFile(ROLE_PIRATE_FILE, initResult);
	}
	
	@Test
	public void test000ImportAccount() throws Exception {
		final String TEST_NAME = "test000ImportAccount";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);

		modelService.importFromResource(SHADOW_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
/*		List<Object> configuredCapabilities = resourceNoAAD.asObjectable().getCapabilities().getConfigured().getAny();
		UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(configuredCapabilities,
				UpdateCapabilityType.class);
		assertNotNull("No configured UpdateCapabilityType", capUpdate);
		assertTrue("Configured addRemoveAttributeValues is not disabled", Boolean.FALSE.equals(capUpdate.isAddRemoveAttributeValues()));
*/	
	}

	@Test
	public void test010assignJack() throws Exception {
		String TEST_NAME = "test010assignJack";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		//GIVEN
		PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
		UserAsserter.forUser(userJackBefore).activation().assertAdministrativeStatus(ActivationStatusType.ENABLED);
		UserAsserter.forUser(userJackBefore).links().assertLinks(0);

		//WHEN
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
		display("Result:\n", result);
		assertSuccess(result);


		//THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
		UserAsserter.forUser(userJackAfter).links().assertLinks(1);

		DummyAccount jackAccount = dummyActivationNativeResource.getAccountByUsername(USER_JACK_USERNAME);
		display("Jack Dummy Account: ", jackAccount.debugDump());
		String enableDisableValue = jackAccount.getAttributeValue("privileges");
		AssertJUnit.assertEquals("Unexpected activation status value: " + enableDisableValue, "false", enableDisableValue);
	}

	@Test
	public void test011modifyActivationJack() throws Exception {
		String TEST_NAME = "test011modifyActivationJack";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		UserAsserter.forUser(userJack).activation().assertAdministrativeStatus(ActivationStatusType.ENABLED);

		//WHEN
		modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);

		//THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
		UserAsserter.forUser(userJackAfter).activation().assertAdministrativeStatus(ActivationStatusType.DISABLED);

		DummyAccount jackAccount = dummyActivationNativeResource.getAccountByUsername(USER_JACK_USERNAME);
		display("Jack Dummy Account: ", jackAccount.debugDump());

		String enableDisableValue = jackAccount.getAttributeValue("privileges");
		AssertJUnit.assertEquals("Unexpected activation status value: " + enableDisableValue, "true", enableDisableValue);
	}
	
}
