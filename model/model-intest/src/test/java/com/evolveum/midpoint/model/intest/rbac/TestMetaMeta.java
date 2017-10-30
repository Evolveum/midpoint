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
package com.evolveum.midpoint.model.intest.rbac;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMetaMeta extends AbstractRbacTest {
	
	protected static final File TEST_DIR = new File("src/test/resources/rbac/metameta");
	
	// WORLD
	
	protected static final File ROLE_META_META_WORLD_FILE = new File(TEST_DIR, "role-meta-meta-world.xml");
	protected static final String ROLE_META_META_WORLD_OID = "fd52fec2-bd56-11e7-9143-b381baa5aaed";
	protected static final String ROLE_META_META_WORLD_NAME = "World";
	
	protected static final File ROLE_META_GREEK_GROUP_FILE = new File(TEST_DIR, "role-meta-greek-group.xml");
	protected static final String ROLE_META_GREEK_GROUP_OID = "d6cf3170-bd57-11e7-be18-6fc3f5bc8e28";
	protected static final String ROLE_META_GREEK_GROUP_NAME = "Greeks";
	
	protected static final File ROLE_META_LATIN_GROUP_FILE = new File(TEST_DIR, "role-meta-latin-group.xml");
	protected static final String ROLE_META_LATIN_GROUP_OID = "5ecd5e8a-bd58-11e7-b4b2-2f6fc8d9b267";
	protected static final String ROLE_META_LATIN_GROUP_NAME = "Romans";
	
	protected static final String GROUP_ALPHA_NAME = "alpha";
	protected static final String GROUP_BETA_NAME = "beta";
	protected static final String GROUP_A_NAME = "a";
	
	protected Map<String,String> groupRoleOidMap = new HashMap<>();
	
	
	// LEGACY
	
	protected static final File ROLE_META_META_LEGACY_FILE = new File(TEST_DIR, "role-meta-meta-legacy.xml");
	protected static final String ROLE_META_META_LEGACY_OID = "7c1c759c-bd68-11e7-ae5e-276b40bf04e6";
	protected static final String ROLE_META_META_LEGACY_NAME = "Legacy";
	
	protected static final File ROLE_META_LEGACY_ONE_FILE = new File(TEST_DIR, "role-meta-legacy-one.xml");
	protected static final String ROLE_META_LEGACY_ONE_OID = "e902b8d8-bd68-11e7-a98c-f7b5a9a28d97";
	protected static final String ROLE_META_LEGACY_ONE_NAME = "metaone";
	
	protected static final String GROUP_ONE_NAME = "one";
	
	private String groupOneRoleOid;


	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Test
    public void test050AddMetaMeta() throws Exception {
		final String TEST_NAME = "test050AddMetaMeta";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        addObject(ROLE_META_META_WORLD_FILE, task, result);

        // THEN
        assertSuccess(result);
		PrismObject<RoleType> role = getObject(RoleType.class, ROLE_META_META_WORLD_OID);
		display("Metametarole after", role);
		
		assertObject(role);
		RoleType roleType = role.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", ROLE_META_META_WORLD_NAME, roleType.getName());
		
		assertAssignments(role, 1); // just direct assignment
		assertRoleMembershipRef(role /* no values */);
				
		assertLinks(role, 1);
		String shadowOid = roleType.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
		
		String groupName = ROLE_META_META_WORLD_NAME.toLowerCase();
		
		DummyGroup dummyGroup = getDummyGroup(null, groupName);
		assertNotNull("No dummy group "+groupName, dummyGroup);
	}
	
	@Test
    public void test100AddGreeks() throws Exception {
		final String TEST_NAME = "test100AddGreeks";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        addObject(ROLE_META_GREEK_GROUP_FILE, task, result);

        // THEN
        assertSuccess(result);

        assertMetaRole(ROLE_META_GREEK_GROUP_OID, ROLE_META_GREEK_GROUP_NAME);

	}
	
	@Test
    public void test110AddRomans() throws Exception {
		final String TEST_NAME = "test110AddRomans";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        addObject(ROLE_META_LATIN_GROUP_FILE, task, result);

        // THEN
        assertSuccess(result);

        assertMetaRole(ROLE_META_LATIN_GROUP_OID, ROLE_META_LATIN_GROUP_NAME);

	}

	@Test
    public void test200CreateAlpha() throws Exception {
		final String TEST_NAME = "test200CreateAlpha";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        String alphaRoleOid = addGreekRole(GROUP_ALPHA_NAME, task, result);

        // THEN
        assertSuccess(result);

        rememberGroupRoleOid(GROUP_ALPHA_NAME, alphaRoleOid);
        readAndAssertGreekGroupRole(alphaRoleOid, GROUP_ALPHA_NAME);

	}
	
	@Test
    public void test202CreateBeta() throws Exception {
		final String TEST_NAME = "test202CreateBeta";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        String alphaRoleOid = addGreekRole(GROUP_BETA_NAME, task, result);

        // THEN
        assertSuccess(result);

        rememberGroupRoleOid(GROUP_BETA_NAME, alphaRoleOid);
        readAndAssertGreekGroupRole(alphaRoleOid, GROUP_BETA_NAME);
	}
	
	@Test
    public void test210CreateA() throws Exception {
		final String TEST_NAME = "test210CreateA";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        String alphaRoleOid = addLatinkRole(GROUP_A_NAME, task, result);

        // THEN
        assertSuccess(result);

        rememberGroupRoleOid(GROUP_A_NAME, alphaRoleOid);
        readAndAssertLatinGroupRole(alphaRoleOid, GROUP_A_NAME);
	}
	
	/**
	 * MID-4109
	 */
	@Test
    public void test300AssignAlphaToJack() throws Exception {
		final String TEST_NAME = "test300AssignAlphaToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignGroupRole(USER_JACK_OID, GROUP_ALPHA_NAME, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());

        readAndAssertGreekGroupMember(USER_JACK_OID, GROUP_ALPHA_NAME);
	}
	
	@Test
    public void test309UnassignAlphaFromJack() throws Exception {
		final String TEST_NAME = "test309UnassignAlphaFromJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        unassignGroupRole(USER_JACK_OID, GROUP_ALPHA_NAME, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());
        
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		display("User after", user);
		assertAssignments(user, 0);
		assertRoleMembershipRef(user /* nothing */);
				
		assertNoDummyAccount(USER_JACK_USERNAME);
		assertNoDummyGroupMember(null, GROUP_ALPHA_NAME, USER_JACK_USERNAME);
	}
	
	/**
	 * MID-4109
	 */
	@Test
    public void test310AssignAToJack() throws Exception {
		final String TEST_NAME = "test310AssignAToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignGroupRole(USER_JACK_OID, GROUP_A_NAME, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());

        readAndAssertGreekGroupMember(USER_JACK_OID, GROUP_A_NAME);
	}

	/**
	 * MID-4109
	 */
	@Test
    public void test312AssignBetaToJack() throws Exception {
		final String TEST_NAME = "test312AssignBetaToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignGroupRole(USER_JACK_OID, GROUP_BETA_NAME, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());

        readAndAssertGreekGroupMember(USER_JACK_OID, GROUP_BETA_NAME, GROUP_A_NAME);
	}

	@Test
    public void test319UnassignBetaAFromJack() throws Exception {
		final String TEST_NAME = "test319UnassignBetaAFromJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> focusDelta = createAssignmentFocusDelta(UserType.class, USER_JACK_OID, getGroupRoleOid(GROUP_BETA_NAME), RoleType.COMPLEX_TYPE, null, (Consumer<AssignmentType>)null, false);
        focusDelta.addModification(createAssignmentModification(getGroupRoleOid(GROUP_A_NAME), RoleType.COMPLEX_TYPE, null, null, false));
        
        // WHEN
		modelService.executeChanges(MiscSchemaUtil.createCollection(focusDelta), null, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());
        
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		display("User after", user);
		assertAssignments(user, 0);
		assertRoleMembershipRef(user /* nothing */);
				
		assertNoDummyAccount(USER_JACK_USERNAME);
		assertNoDummyGroupMember(null, GROUP_ALPHA_NAME, USER_JACK_USERNAME);
		assertNoDummyGroupMember(null, GROUP_BETA_NAME, USER_JACK_USERNAME);
		assertNoDummyGroupMember(null, GROUP_A_NAME, USER_JACK_USERNAME);
	}
	
	@Test
    public void test900LegacyAddMetaMeta() throws Exception {
		final String TEST_NAME = "test900LegacyAddMetaMeta";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        addObject(ROLE_META_META_LEGACY_FILE, task, result);

        // THEN
        assertSuccess(result);
		PrismObject<RoleType> role = getObject(RoleType.class, ROLE_META_META_LEGACY_OID);
		display("Metametarole after", role);
		
		assertObject(role);
		RoleType roleType = role.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", ROLE_META_META_LEGACY_NAME, roleType.getName());
		
		assertAssignments(role, 1); // just direct assignment
		assertRoleMembershipRef(role /* no values */);
				
		assertLinks(role, 1);
		String shadowOid = roleType.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
		
		String groupName = ROLE_META_META_LEGACY_NAME.toLowerCase();
		
		DummyGroup dummyGroup = getDummyGroup(null, groupName);
		assertNotNull("No dummy group "+groupName, dummyGroup);
	}
	
	@Test
    public void test910LegacyAddMetaLegacyOne() throws Exception {
		final String TEST_NAME = "test910LegacyAddMetaLegacyOne";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        addObject(ROLE_META_LEGACY_ONE_FILE, task, result);

        // THEN
        assertSuccess(result);
		PrismObject<RoleType> role = getObject(RoleType.class, ROLE_META_LEGACY_ONE_OID);
		display("Metarole after", role);
		
		assertObject(role);
		RoleType roleType = role.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", ROLE_META_LEGACY_ONE_NAME, roleType.getName());
		
		assertAssigned(role, ROLE_META_META_LEGACY_OID, RoleType.COMPLEX_TYPE);
		assertAssignments(role, 1);
		assertRoleMembershipRef(role, ROLE_META_META_LEGACY_OID);
				
		assertLinks(role, 1);
		String shadowOid = roleType.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
		
		String groupName = ROLE_META_LEGACY_ONE_NAME.toLowerCase();
		
		DummyGroup dummyGroup = getDummyGroup(null, groupName);
		assertNotNull("No dummy group "+groupName, dummyGroup);
	}
	
	@Test
    public void test920LegacyCreateGroupOne() throws Exception {
		final String TEST_NAME = "test920LegacyCreateGroupOne";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
		PrismObject<RoleType> roleBefore = createObject(RoleType.class, GROUP_ONE_NAME);
		RoleType roleType = roleBefore.asObjectable();
		roleType
			.beginAssignment()
				.targetRef(ROLE_META_LEGACY_ONE_OID, RoleType.COMPLEX_TYPE);

        // WHEN
		groupOneRoleOid = addObject(roleBefore, task, result);

        // THEN
        assertSuccess(result);
        
        
        PrismObject<RoleType> roleAfter = getObject(RoleType.class, groupOneRoleOid);
		display("Role after", roleAfter);
		assertObject(roleAfter);
		RoleType roleTypeAfter = roleAfter.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+roleAfter+" name", GROUP_ONE_NAME, roleTypeAfter.getName());
		
		assertAssigned(roleAfter, ROLE_META_LEGACY_ONE_OID, RoleType.COMPLEX_TYPE);
		assertAssignments(roleAfter, 1);
		assertRoleMembershipRef(roleAfter, ROLE_META_LEGACY_ONE_OID);
		
		DummyGroup dummyGroupOne = getDummyGroup(null, GROUP_ONE_NAME);
		assertNotNull("No dummy group "+GROUP_ONE_NAME, dummyGroupOne);
		
		assertLinks(roleAfter, 1);
		String shadowOid = roleTypeAfter.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
	}
	
	@Test
    public void test930LegacyAssignGroupOneToJack() throws Exception {
		final String TEST_NAME = "test930LegacyAssignGroupOneToJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, groupOneRoleOid, task, result);

        // THEN
        assertSuccess(result);
        
        display("Dummy resource", getDummyResource());

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertAssignedRole(user, groupOneRoleOid);
        assertAssignments(user, 1);
        assertRoleMembershipRef(user, groupOneRoleOid);
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
        display("User shadow (model)", accountShadow);
        
        assertDefaultDummyAccount(user.getName().getOrig(), user.asObjectable().getFullName().getOrig(), true);
        assertDummyGroupMember(null, ROLE_META_LEGACY_ONE_NAME, user.getName().getOrig());
        assertNoDummyGroupMember(null, GROUP_ONE_NAME, user.getName().getOrig());

	}

	
	private PrismObject<UserType> readAndAssertGreekGroupMember(String userOid, String... roleNames) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = getUser(userOid);
        display("User after", user);
        List<String> roleOids = Arrays.stream(roleNames).map(this::getGroupRoleOid).collect(Collectors.toList());
        assertAssignedRoles(user, roleOids);
        assertAssignments(user, roleOids.size());
        assertRoleMembershipRefs(user, roleOids);
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
        display("User shadow (model)", accountShadow);
        
        assertDefaultDummyAccount(user.getName().getOrig(), user.asObjectable().getFullName().getOrig(), true);
        for (String roleName: roleNames) {
        	assertDummyGroupMember(null, roleName, user.getName().getOrig());
        }
        
        return user;
	}


	private void rememberGroupRoleOid(String roleName, String roleOid) {
		groupRoleOidMap.put(roleName, roleOid);
	}

	private String getGroupRoleOid(String roleName) {
		return groupRoleOidMap.get(roleName);
	}

	private void assignGroupRole(String userOid, String roleName, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		assignRole(userOid, getGroupRoleOid(roleName), task, result);
	}

	private void unassignGroupRole(String userOid, String roleName, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		unassignRole(userOid, getGroupRoleOid(roleName), task, result);
	}

	private PrismObject<RoleType> readAndAssertGreekGroupRole(String roleOid, String groupName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SchemaViolationException, ConflictException {
		return readAndAssertGroupRole(roleOid, ROLE_META_GREEK_GROUP_OID, groupName);
	}

	private PrismObject<RoleType> readAndAssertLatinGroupRole(String roleOid, String groupName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SchemaViolationException, ConflictException {
		return readAndAssertGroupRole(roleOid, ROLE_META_LATIN_GROUP_OID, groupName);
	}

	private PrismObject<RoleType> readAndAssertGroupRole(String roleOid, String metaroleOid, String groupName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SchemaViolationException, ConflictException {
		PrismObject<RoleType> role = getObject(RoleType.class, roleOid);
		display("Role after", role);
		assertObject(role);
		RoleType roleType = role.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", groupName, roleType.getName());
		
		assertAssigned(role, metaroleOid, RoleType.COMPLEX_TYPE);
		assertAssignments(role, 1);
		assertRoleMembershipRef(role, metaroleOid);
		
		DummyGroup dummyGroup = getDummyGroup(null, groupName);
		assertNotNull("No dummy group "+groupName, dummyGroup);
		
		assertLinks(role, 1);
		String shadowOid = roleType.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
		
		return role;
	}
	
	private PrismObject<RoleType> assertMetaRole(String metaroleOid, String metaroleName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SchemaViolationException, ConflictException {
		PrismObject<RoleType> role = getObject(RoleType.class, metaroleOid);
		display("Metarole after", role);
		
		assertObject(role);
		RoleType roleType = role.asObjectable();
		
		PrismAsserts.assertEqualsPolyString("Wrong "+role+" name", metaroleName, roleType.getName());
		
		assertAssigned(role, ROLE_META_META_WORLD_OID, RoleType.COMPLEX_TYPE);
		assertAssignments(role, 1);
		assertRoleMembershipRef(role, ROLE_META_META_WORLD_OID);
				
		assertLinks(role, 1);
		String shadowOid = roleType.getLinkRef().get(0).getOid();
		PrismObject<ShadowType> groupShadowModel = getShadowModel(shadowOid);
		display("Role shadow (model)", groupShadowModel);
		
		String groupName = metaroleName.toLowerCase();
		
		DummyGroup dummyGroup = getDummyGroup(null, groupName);
		assertNotNull("No dummy group "+groupName, dummyGroup);
		
		return role;
	}



	private String addGreekRole(String roleName, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		return addGrupRole(roleName, ROLE_META_GREEK_GROUP_OID, task, result);
	}
	
	private String addLatinkRole(String roleName, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		return addGrupRole(roleName, ROLE_META_LATIN_GROUP_OID, task, result);
	}

	private String addGrupRole(String roleName, String metaroleOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<RoleType> role = createGroupRole(roleName, metaroleOid);
		return addObject(role, task, result);
	}	

	private PrismObject<RoleType> createGroupRole(String roleName, String metaroleOid) throws SchemaException {
		PrismObject<RoleType> role = createObject(RoleType.class, roleName);
		RoleType roleType = role.asObjectable();
		roleType
			.beginAssignment()
				.targetRef(metaroleOid, RoleType.COMPLEX_TYPE);
		return role;
	}

}
