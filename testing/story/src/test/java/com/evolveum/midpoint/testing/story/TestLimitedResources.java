/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;

/**
 * Test for resources with limited capabilities.
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLimitedResources extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "limited-resources");
	
	protected static final File RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_FILE = new File(TEST_DIR, "resource-dummy-no-attribute-add-delete.xml");
	protected static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID = "5098fa64-8386-11e8-b2e2-83ada4418787";
	protected static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME = "no-attribute-add-delete";
	private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;
	private static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_SHIP_PREFIX = "Spirit of ";

	private static final String OU_TREASURE_HUNT = "Treasure Hunt";
	private static final String OU_LOOTING = "Looting";
	private static final String OU_SWASHBACKLING = "Swashbuckling";
	private static final String OU_SAILING = "Sailing";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_FILE, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, initTask, initResult);
	}
	
	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);

        PrismObject<ResourceType> resourceNoAAD = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		List<Object> configuredCapabilities = resourceNoAAD.asObjectable().getCapabilities().getConfigured().getAny();
		AddRemoveAttributeValuesCapabilityType capARAV = CapabilityUtil.getCapability(configuredCapabilities,
				AddRemoveAttributeValuesCapabilityType.class);
		assertNotNull("No configured AddRemoveAttributeValuesCapabilityType", capARAV);
		assertTrue("Configured AddRemoveAttributeValuesCapabilityType is not disabled", Boolean.FALSE.equals(capARAV.isEnabled()));
	}
	
	@Test
	public void test100AssignJackAccountNoAttributeAddDelete() throws Exception {
		final String TEST_NAME = "test100AssignJackAccountNoAttributeAddDelete";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, null, task, result);

		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		getSingleLinkOid(userAfter);
		
		assertAADWeapon(USER_JACK_USERNAME /* no value */);
	}
	
	@Test
	public void test102AddJackOrganizationalUnitTreasureHunt() throws Exception {
		final String TEST_NAME = "test102AddJackOrganizationalUnitTreasureHunt";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		createPolyString(OU_TREASURE_HUNT));
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		getSingleLinkOid(userAfter);
		
		assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT);
	}
	
	@Test
	public void test104AddJackOrganizationalUnitLootingSailing() throws Exception {
		final String TEST_NAME = "test104AddJackOrganizationalUnitLootingSailing";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		createPolyString(OU_LOOTING), createPolyString(OU_SAILING));
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		getSingleLinkOid(userAfter);
		
		assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT, OU_LOOTING, OU_SAILING);
	}
	
	@Test
	public void test106DeleteJackOrganizationalUnitLooting() throws Exception {
		final String TEST_NAME = "test106DeleteJackOrganizationalUnitLooting";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		createPolyString(OU_LOOTING));
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		getSingleLinkOid(userAfter);
		
		assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT, OU_SAILING);
	}
	
	/**
	 *  MID-4607
	 */
	@Test
	public void test108DeleteJackOrganizationalUnitTreasureHuntSailing() throws Exception {
		final String TEST_NAME = "test108DeleteJackOrganizationalUnitTreasureHuntSailing";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, 
        		createPolyString(OU_TREASURE_HUNT), createPolyString(OU_SAILING));
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		getSingleLinkOid(userAfter);
		
		assertAADWeapon(USER_JACK_USERNAME /* no values */);
	}
	
	private void assertAADWeapon(String username, String... ous) throws SchemaViolationException, ConflictException {
		assertDummyAccount(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, username);
		Object[] weapons = new String[ous.length];
		for (int i=0; i<ous.length; i++) {
			weapons[i] = RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_SHIP_PREFIX + ous[i];
		}
		assertDummyAccountAttribute(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, username,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, weapons);
	}
}
