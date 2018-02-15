/*
 * Copyright (c) 2016-2017 Evolveum
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMapleLeaf extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "mapleLeaf");
	
	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000000";
	
	private static final File ROLE_SQUIRREL_FILE = new File(TEST_DIR, "role-squirrel.xml");
	private static final String ROLE_SQUIRREL_OID = "301188c5-2545-4123-96ed-8aabda7c3710";
	
	private static final File ROLE_META_MONKEY_DONKEY = new File(TEST_DIR, "meta-role-monkey-donkey.xml");
	
	private static final String NS_RESOURCE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
				RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		importObjectFromFile(ROLE_META_MONKEY_DONKEY);
		importObjectFromFile(ROLE_SQUIRREL_FILE);

	}
	
	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
		assertSuccess(testResultOpenDj);

		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		assertNotNull("No system configuration", systemConfiguration);
		display("System config", systemConfiguration);

		PrismObject<RoleType> roleSquirel = modelService.getObject(RoleType.class,
				ROLE_SQUIRREL_OID, null, task, result);
		assertNotNull("No role squirel, probably probelm with initialization", roleSquirel);
		result.computeStatus();
		assertSuccess("Role not fetch successfully", result);

	}
	
	@Test
	public void test001addUser() throws Exception {
		final String TEST_NAME = "test001addUser";
		displayTestTitle(TEST_NAME);
		
		
		//when
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_OPENDJ_OID, "default");
		
		//then
		displayThen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertNotNull("User is null", user);
		
		assertLinks(user, 1);
		
		PrismReference ref = user.findReference(UserType.F_LINK_REF);
		String shadowOid = ref.getOid();
		assertNotNull("Reference without oid? Something went wrong.", shadowOid);
		
		PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
		assertNotNull("Shadow not found", shadow);
		ResourceAttributeContainer shadowContainer = ShadowUtil.getAttributesContainer(shadow);
		ResourceAttribute<String> initials = shadowContainer.findAttribute(new QName(NS_RESOURCE, "initials"));
		assertEquals(initials.size(), 3, "Expected 3 values in attribute, but found " + initials.size());
		
		Collection<String> values = initials.getRealValues();
		assertTrue(values.contains("monkey"), "No monkey found among values");
		assertTrue(values.contains("donkey"), "No donkey found among values");
		assertTrue(values.contains("mcconkey"), "No mcconkey found among values");
	}
	
	@Test
	public void test002assignRoleSquirrel() throws Exception {
		final String TEST_NAME = "test002assignRoleSquirrel";
		displayTestTitle(TEST_NAME);
		
		
		//when
		displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_SQUIRREL_OID);
		
		//then
		displayThen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertNotNull("User is null", user);
		
		assertLinks(user, 1);
		
		PrismReference ref = user.findReference(UserType.F_LINK_REF);
		String shadowOid = ref.getOid();
		assertNotNull("Reference without oid? Something went wrong.", shadowOid);
		
		PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
		assertNotNull("Shadow not found", shadow);
		ResourceAttributeContainer shadowContainer = ShadowUtil.getAttributesContainer(shadow);
		ResourceAttribute<String> initials = shadowContainer.findAttribute(new QName(NS_RESOURCE, "initials"));
		assertEquals(initials.size(), 3, "Expected 3 values in attribute, but found " + initials.size());
		
		Collection<String> values = initials.getRealValues();
		assertTrue(values.contains("monkey"), "No monkey found among values");
		assertTrue(values.contains("donkey"), "No donkey found among values");
		assertTrue(values.contains("squirrel"), "No squirrel found among values");
	}
	
	@Test
	public void test003unassignRoleSquirrel() throws Exception {
		final String TEST_NAME = "test003unassignRoleSquirrel";
		displayTestTitle(TEST_NAME);
		
		//when
		displayWhen(TEST_NAME);
		unassignRole(USER_JACK_OID, ROLE_SQUIRREL_OID);
		
		//then
		displayThen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertNotNull("User is null", user);
		
		assertLinks(user, 1);
		
		PrismReference ref = user.findReference(UserType.F_LINK_REF);
		String shadowOid = ref.getOid();
		assertNotNull("Reference without oid? Something went wrong.", shadowOid);
		
		PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
		assertNotNull("Shadow not found", shadow);
		ResourceAttributeContainer shadowContainer = ShadowUtil.getAttributesContainer(shadow);
		ResourceAttribute<String> initials = shadowContainer.findAttribute(new QName(NS_RESOURCE, "initials"));
		assertEquals(initials.size(), 3, "Expected 3 values in attribute, but found " + initials.size());
		
		Collection<String> values = initials.getRealValues();
		assertTrue(values.contains("monkey"), "No monkey found among values");
		assertTrue(values.contains("donkey"), "No donkey found among values");
		assertTrue(values.contains("mcconkey"), "No mcconkey found among values");
	}
	
	@Test
	public void test100changePasswordForceChange() throws Exception {
		final String TEST_NAME = "test100changePasswordForceChange";
		displayTestTitle(TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		//given
		ProtectedStringType passwd = new ProtectedStringType();
		passwd.setClearValue("oldValue");
		ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, SchemaConstants.PATH_PASSWORD_VALUE, passwd);
		executeChanges(userDelta, null, task, result);
		
		openDJController.assertPassword("uid=jack,ou=People,dc=example,dc=com", "oldValue");
		
		//when
		displayWhen(TEST_NAME);
		passwd = new ProtectedStringType();
		passwd.setClearValue("somenewValue");
		userDelta = createModifyUserReplaceDelta(USER_JACK_OID, SchemaConstants.PATH_PASSWORD_VALUE, passwd);
		userDelta.addModificationReplaceProperty(new ItemPath(SchemaConstants.PATH_PASSWORD, PasswordType.F_FORCE_CHANGE), Boolean.TRUE);
		
		executeChanges(userDelta, null, task, result);
		
		//THEN
		
		displayThen(TEST_NAME);
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		UserType userTypeAfter = userAfter.asObjectable();
		
		CredentialsType credentials = userTypeAfter.getCredentials();
		assertNotNull("Oooops, something unexpected happenned - no credentials found in user " + userAfter, credentials);
		PasswordType password = credentials.getPassword();
		assertNotNull("Oooops, something unexpected happenned - no password defined for user " + userAfter, password);
		
		String clearTextValue = protector.decryptString(password.getValue());
		assertEquals(clearTextValue, "somenewValue", "Passwords don't match");
		assertTrue(BooleanUtils.isTrue(password.isForceChange()), "Expected force change set to true, but was: " + BooleanUtils.isTrue(password.isForceChange()));
		
		openDJController.assertPassword("uid=jack,ou=People,dc=example,dc=com", "oldValue");
	}
}
