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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMapleLeaf extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "mapleLeaf");
	
	public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/story/mapleLeaf/ext";
	
	private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
	
	private static final File SECURITY_POLICY_FILE = new File(TEST_DIR, "security-policy.xml");
	
	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000000";
	
	private static final File ROLE_SQUIRREL_FILE = new File(TEST_DIR, "role-squirrel.xml");
	private static final String ROLE_SQUIRREL_OID = "301188c5-2545-4123-96ed-8aabda7c3710";
	
	private static final File ROLE_META_MONKEY_DONKEY = new File(TEST_DIR, "meta-role-monkey-donkey.xml");
	
	private static final File ROLE_MAPLE_LEAF_FACULTY_LICENSE = new File(TEST_DIR, "role-maple-leaf-faculty-license.xml");
	private static final String ROLE_MAPLE_LEAF_FACULTY_LICENSE_OID = "00000000-role-meta-0000-000011112222";
	private static final File ROLE_MAPLE_LEAF_FACULTY = new File(TEST_DIR, "role-maple-leaf-faculty.xml");
	private static final String ROLE_MAPLE_LEAF_FACULTY_OID = "00000000-role-0000-0000-000011112222";
	
	private static final File ROLE_MAPLE_LEAF_GRADUATE_LICENSE = new File(TEST_DIR, "role-maple-leaf-graduate-license.xml");
	private static final String ROLE_MAPLE_LEAF_GRADUATE_LICENSE_OID = "00000000-role-meta-0000-000011113333";
	private static final File ROLE_MAPLE_LEAF_GRADUATE = new File(TEST_DIR, "role-maple-leaf-graduate.xml");
	private static final String ROLE_MAPLE_LEAF_GRADUATE_OID = "00000000-role-0000-0000-000011113333";
	
	private static final File OBJECT_TEMPLATE_USER = new File(TEST_DIR, "object-template-user.xml");
	private static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";
	
	private static final String LDIF_GROUPS =  TEST_DIR + "/mapleLeafGroups.ldif";
	
	private static final String NS_RESOURCE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
	
	protected static final ItemPath ACTIVATION_EFFECTIVE_STATUS_PATH = new ItemPath(UserType.F_ACTIVATION,
			ActivationType.F_EFFECTIVE_STATUS);	
	
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
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
				RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		openDJController.addEntriesFromLdifFile(LDIF_GROUPS);
		
		importObjectFromFile(ROLE_MAPLE_LEAF_FACULTY_LICENSE);
		importObjectFromFile(ROLE_MAPLE_LEAF_FACULTY);
		importObjectFromFile(ROLE_MAPLE_LEAF_GRADUATE_LICENSE);
		importObjectFromFile(ROLE_MAPLE_LEAF_GRADUATE);
		importObjectFromFile(ROLE_META_MONKEY_DONKEY);
		importObjectFromFile(ROLE_SQUIRREL_FILE);
		importObjectFromFile(SECURITY_POLICY_FILE);
		importObjectFromFile(OBJECT_TEMPLATE_USER);

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
	public void test004assignRoleMapleLeafFaculty() throws Exception {
		final String TEST_NAME = "test004assignRoleMapleLeafFaculty";
		displayTestTitle(TEST_NAME);
		
		//when
		displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_MAPLE_LEAF_FACULTY_OID);
		
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
		ShadowType shadowType = shadow.asObjectable();
		List<ShadowAssociationType> associations = shadowType.getAssociation();
		assertFalse(associations.isEmpty(), "Expected 2 associations, but no one exists");
		assertEquals(associations.size(), 2, "Unexpected number of associations");
		
		openDJController.assertUniqueMember("cn=mapleLeafFaculty,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		openDJController.assertUniqueMember("cn=mapleLeafFacultyLicense,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		
	}
	
	private boolean containRoleMemebrShip(List<ObjectReferenceType> roleMemberships, String roleId) {
		for (ObjectReferenceType ref : roleMemberships) {
			if (ref.getOid().equals(roleId)) {
				return true;
			}
		}
		return false;
	}
	
	@Test
	public void test005assignRoleMapleLeafGraduate() throws Exception {
		final String TEST_NAME = "test005assignRoleMapleLeafGraduate";
		displayTestTitle(TEST_NAME);
		
		//when
		displayWhen(TEST_NAME);
		assignRole(USER_JACK_OID, ROLE_MAPLE_LEAF_GRADUATE_OID);
		
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
		ShadowType shadowType = shadow.asObjectable();
		List<ShadowAssociationType> associations = shadowType.getAssociation();
		assertFalse(associations.isEmpty(), "Expected 4 associations, but no one exists");
		assertEquals(associations.size(), 4, "Unexpected number of associations");
		
		openDJController.assertUniqueMember("cn=mapleLeafFaculty,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		openDJController.assertUniqueMember("cn=mapleLeafFacultyLicense,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		openDJController.assertUniqueMember("cn=mapleLeafGraduate,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		openDJController.assertUniqueMember("cn=mapleLeafGraduateLicense,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
	}
	
	@Test
	public void test006unassignRoleMapleLeafFaculty() throws Exception {
		final String TEST_NAME = "test006unassignRoleMapleLeafFaculty";
		displayTestTitle(TEST_NAME);
		
		//when
		displayWhen(TEST_NAME);
		unassignRole(USER_JACK_OID, ROLE_MAPLE_LEAF_FACULTY_OID);
		
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
		ShadowType shadowType = shadow.asObjectable();
		List<ShadowAssociationType> associations = shadowType.getAssociation();
		assertFalse(associations.isEmpty(), "Expected 2 associations, but no one exists");
		assertEquals(associations.size(), 2, "Unexpected number of associations");
		
		openDJController.assertUniqueMember("cn=mapleLeafGraduate,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
		openDJController.assertUniqueMember("cn=mapleLeafGraduateLicense,ou=groups,dc=example,dc=com", "uid=jack,ou=People,dc=example,dc=com");
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
		PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
		String accountOid = assertAccount(userJackBefore, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadowBefore = getShadowModel(accountOid);
		display("Shadow before: ", shadowBefore.asObjectable());
		PrismProperty<String> carLicenseBefore = shadowBefore.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(NS_RESOURCE, "carLicense")));
		assertNotNull("Unexpected car license: " + carLicenseBefore, carLicenseBefore);
		AssertJUnit.assertNotNull("Unexpected value in car license: " + carLicenseBefore.getRealValue(), carLicenseBefore.getRealValue());
		
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
		PrismObject<ShadowType> shadowAfter = getShadowModel(accountOid);
		display("Shadow after: ", shadowAfter.asObjectable());
		PrismProperty<String> carLicenseAfter = shadowAfter.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(NS_RESOURCE, "carLicense")));
		assertNotNull("Unexpected car license: " + carLicenseAfter, carLicenseAfter);
		AssertJUnit.assertNotNull("Unexpected value in car license: " + carLicenseAfter.getRealValue(), carLicenseAfter.getRealValue());
		assertNotEquals(carLicenseBefore.getRealValue(), carLicenseAfter.getRealValue(), "Unexpected values. Before: " + carLicenseBefore.getRealValue() + ", after: " + carLicenseAfter.getRealValue());
	}
	
	@Test
	public void test101resetPassword() throws Exception {
		final String TEST_NAME = "test101resetPassword";
		displayTestTitle(TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.assertPassword("uid=jack,ou=People,dc=example,dc=com", "oldValue");
		
		//when
		displayWhen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		ExecuteCredentialResetRequestType executeCredentialResetRequest = new ExecuteCredentialResetRequestType();
		executeCredentialResetRequest.setResetMethod("passwordReset");
		executeCredentialResetRequest.setUserEntry("123passwd456");
		modelInteractionService.executeCredentialsReset(user, executeCredentialResetRequest, task, result);
		
		//THEN
		displayThen(TEST_NAME);
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		UserType userTypeAfter = userAfter.asObjectable();
		
		CredentialsType credentials = userTypeAfter.getCredentials();
		assertNotNull("Oooops, something unexpected happenned - no credentials found in user " + userAfter, credentials);
		PasswordType password = credentials.getPassword();
		assertNotNull("Oooops, something unexpected happenned - no password defined for user " + userAfter, password);
		
		String clearTextValue = protector.decryptString(password.getValue());
		assertEquals(clearTextValue, "123passwd456", "Passwords don't match");
		assertTrue(BooleanUtils.isTrue(password.isForceChange()), "Expected force change set to true, but was: " + BooleanUtils.isTrue(password.isForceChange()));
		
		openDJController.assertPassword("uid=jack,ou=People,dc=example,dc=com", "oldValue");
	}
	
	@Test
	public void test200setArchivedAdministrativeStatus() throws Exception {
		final String TEST_NAME = "test200setArchivedAdministrativeStatus";
		displayTestTitle(TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, null, task, result, ActivationStatusType.ARCHIVED);
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		PrismProperty<ActivationStatusType> administrativeStatus = user.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		assertNotNull("No administrative status property present.", administrativeStatus);
		assertEquals(administrativeStatus.getRealValue(), ActivationStatusType.ARCHIVED, "Unexpected administrative status");
		PrismProperty<ActivationStatusType> effectiveStatus = user.findProperty(ACTIVATION_EFFECTIVE_STATUS_PATH);
		assertNotNull("No effective status property present.", effectiveStatus);
		assertEquals(effectiveStatus.getRealValue(), ActivationStatusType.ARCHIVED, "Unexpected effective status");
		
	}
	
	@Test
	public void test201setUndefinedAdministrativeStatus() throws Exception {
		final String TEST_NAME = "test201setUndefinedAdministrativeStatus";
		displayTestTitle(TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		//WHEN
		PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		String accountOid = assertAccount(userBefore, RESOURCE_OPENDJ_OID);
		recomputeUser(USER_JACK_OID, task, result);
		
		ObjectDelta<UserType> unlinkDelta = createModifyUserUnlinkAccount(USER_JACK_OID, resourceOpenDj);
		executeChanges(unlinkDelta, null, task, result);
		assertNotLinked(USER_JACK_OID, accountOid);
		
		//THEN
		displayThen(TEST_NAME);
		modelService.importFromResource(accountOid, task, result);
		
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		PrismProperty<ActivationStatusType> administrativeStatus = userAfter.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		assertNull("Administrative status still set.", administrativeStatus);
		PrismProperty<ActivationStatusType> effectiveStatus = userAfter.findProperty(ACTIVATION_EFFECTIVE_STATUS_PATH);
		assertNotNull("No effective status property present.", effectiveStatus);
		assertEquals(effectiveStatus.getRealValue(), ActivationStatusType.ENABLED, "Unexpected effective status");
		
	}
}
