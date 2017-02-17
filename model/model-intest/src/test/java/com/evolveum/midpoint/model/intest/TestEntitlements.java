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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * Test of account-entitlement association.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestEntitlements extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/entitlements");

	public static final File ROLE_SWASHBUCKLER_FILE = new File(TEST_DIR, "role-swashbuckler.xml");
	public static final String ROLE_SWASHBUCKLER_OID = "10000000-0000-0000-0000-000000001601";

	public static final File ROLE_LANDLUBER_FILE = new File(TEST_DIR, "role-landluber.xml");
	public static final String ROLE_LANDLUBER_OID = "10000000-0000-0000-0000-000000001603";

	public static final File ROLE_WIMP_FILE = new File(TEST_DIR, "role-wimp.xml");
	public static final String ROLE_WIMP_OID = "10000000-0000-0000-0000-000000001604";

	public static final File ROLE_MAPMAKER_FILE = new File(TEST_DIR, "role-mapmaker.xml");
	public static final String ROLE_MAPMAKER_OID = "10000000-0000-0000-0000-000000001605";

	public static final File ROLE_BRUTE_FILE = new File(TEST_DIR, "role-brute.xml");
	public static final String ROLE_BRUTE_OID = "10000000-0000-0000-0000-000000001606";
	public static final String ROLE_BRUTE_NAME = "Brute";
	public static final String GROUP_BRUTE_NAME = "brute";

	public static final File ROLE_THUG_FILE = new File(TEST_DIR, "role-thug.xml");
	public static final String ROLE_THUG_OID = "10000000-0000-0000-0000-000000001607";
	public static final String ROLE_THUG_NAME = "Thug";
	public static final String GROUP_THUG_NAME = "thug";

	public static final File SHADOW_GROUP_DUMMY_SWASHBUCKLERS_FILE = new File(TEST_DIR, "group-swashbucklers.xml");
	public static final String SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID = "20000000-0000-0000-3333-000000000001";
	public static final String GROUP_DUMMY_SWASHBUCKLERS_NAME = "swashbucklers";
	public static final String GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION = "Scurvy swashbucklers";

	public static final File SHADOW_GROUP_DUMMY_LANDLUBERS_FILE = new File(TEST_DIR, "group-landlubers.xml");
	public static final String SHADOW_GROUP_DUMMY_LANDLUBERS_OID = "20000000-0000-0000-3333-000000000003";
	public static final String GROUP_DUMMY_LANDLUBERS_NAME = "landlubers";
	public static final String GROUP_DUMMY_LANDLUBERS_DESCRIPTION = "Earthworms";
	public static final String GROUP_DUMMY_WIMPS_NAME = "wimps";
	public static final String GROUP_DUMMY_MAPMAKERS_NAME = "mapmakers";

	private static final QName RESOURCE_DUMMY_GROUP_OBJECTCLASS = new QName(RESOURCE_DUMMY_NAMESPACE, "GroupObjectClass");

	private static final String USER_WALLY_NAME = "wally";
	private static final String USER_WALLY_FULLNAME = "Wally B. Feed";

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectFromFile(ROLE_SWASHBUCKLER_FILE);
        importObjectFromFile(ROLE_LANDLUBER_FILE);
        importObjectFromFile(ROLE_MAPMAKER_FILE);
    }

    /**
     * Add group shadow using model service. Group should appear on dummy resource.
     */
    @Test
    public void test100AddGroupShadowSwashbucklers() throws Exception {
		final String TEST_NAME = "test100AddGroupShadowSwashbucklers";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();


        PrismObject<ShadowType> group = prismContext.parseObject(SHADOW_GROUP_DUMMY_SWASHBUCKLERS_FILE);

		// WHEN
        addObject(group, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group created on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
	}

	@Test
    public void test101GetGroupShadowSwashbucklers() throws Exception {
		final String TEST_NAME = "test101GetGroupShadowSwashbucklers";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        display("Shadow", shadow);

        assertShadowModel(shadow, SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, GROUP_DUMMY_SWASHBUCKLERS_NAME, getDummyResourceType(), RESOURCE_DUMMY_GROUP_OBJECTCLASS);
        IntegrationTestTools.assertAttribute(shadow, getDummyResourceType(),
        		DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION);
	}

    /**
     * Add account to a group using model service (shadow operations).
     */
    @Test
    public void test110AssociateGuybrushToSwashbucklers() throws Exception {
		final String TEST_NAME = "test110AssociateGuybrushToSwashbucklers";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_SHADOW_GUYBRUSH_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group created on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}

    @Test
    public void test200AssignRoleSwashbucklerToJack() throws Exception {
		final String TEST_NAME = "test200AssignRoleSwashbucklerToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);
	}

    @Test
    public void test209UnAssignRoleSwashbucklerFromJack() throws Exception {
		final String TEST_NAME = "test209UnAssignRoleSwashbucklerFromJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);
        // TODO: assert role assignment

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

    /**
     * Create the group from midPoint. Therefore the shadow exists.
     */
    @Test
    public void test220AssignRoleLandluberToWally() throws Exception {
		final String TEST_NAME = "test220AssignRoleLandluberToWally";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        addObject(SHADOW_GROUP_DUMMY_LANDLUBERS_FILE);

        PrismObject<UserType> user = createUser(USER_WALLY_NAME, USER_WALLY_FULLNAME, true);
        addObject(user);

		// WHEN
        assignRole(user.getOid(), ROLE_LANDLUBER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_LANDLUBERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, USER_WALLY_NAME);
	}

    /**
     * Create the group directly on resource. Therefore the shadow does NOT exists.
     */
    @Test
    public void test222AssignRoleMapmakerToWally() throws Exception {
		final String TEST_NAME = "test222AssignRoleMapmakerToWally";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        DummyGroup mapmakers = new DummyGroup(GROUP_DUMMY_MAPMAKERS_NAME);
        getDummyResource().addGroup(mapmakers);

        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(user.getOid(), ROLE_MAPMAKER_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertGroupMember(dummyGroup, USER_WALLY_NAME);
	}

    @Test
    public void test224UnassignRoleMapmakerFromWally() throws Exception {
		final String TEST_NAME = "test224UnassignRoleMapmakerFromWally";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(user.getOid(), ROLE_MAPMAKER_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_WALLY_NAME);
	}


    @Test
    public void test300AddRoleWimp() throws Exception {
		final String TEST_NAME = "test300AddRoleWimp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(ROLE_WIMP_FILE, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMembers(dummyGroup);

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertNoGroupMembers(dummyGroupAtOrange);
    }

    @Test
    public void test302AddRoleBrute() throws Exception {
		final String TEST_NAME = "test302AddRoleBrute";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(ROLE_BRUTE_FILE, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertNoGroupMembers(dummyGroupBrute);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertNoGroupMembers(dummyGroupBruteWannabe);
    }

    @Test
    public void test304AddRoleThug() throws Exception {
		final String TEST_NAME = "test304AddRoleThug";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(ROLE_THUG_FILE, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMembers(dummyGroupThug);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMembers(dummyGroupThugWannabe);
    }

    @Test
    public void test310AssignRoleWimpToLargo() throws Exception {
		final String TEST_NAME = "test310AssignRoleWimpToLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        addObject(USER_LARGO_FILE);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_WIMP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroupWimp, USER_LARGO_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_LARGO_USERNAME);
	}

    @Test
    public void test312AssignRoleBruteToLargo() throws Exception {
		final String TEST_NAME = "test312AssignRoleBruteToLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroupBrute, USER_LARGO_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_LARGO_USERNAME);
	}

    @Test
    public void test313UnAssignRoleBruteFromLargo() throws Exception {
		final String TEST_NAME = "test313UnAssignRoleBruteFromLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertNoGroupMembers(dummyGroupBrute);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertNoGroupMembers(dummyGroupBruteWannabe);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, USER_LARGO_USERNAME);

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
	}

    @Test
    public void test314AssignRoleThugToLargo() throws Exception {
		final String TEST_NAME = "test314AssignRoleThugToLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_THUG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertGroupMember(dummyGroupThug, USER_LARGO_USERNAME);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_LARGO_USERNAME);
	}

    @Test
    public void test315UnAssignRoleThugFromLargo() throws Exception {
		final String TEST_NAME = "test315UnAssignRoleThugFromLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_THUG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMembers(dummyGroupThug);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMembers(dummyGroupThugWannabe);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, USER_LARGO_USERNAME);

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
	}

    /**
     * after renaming user largo it should be also propagated to the associations
     * @throws Exception
     */
    @Test
    public void test317RenameLargo() throws Exception {
		final String TEST_NAME = "test317RenameLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_LARGO_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("newLargo"));

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_LARGO_USERNAME);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroup, "newLargo");

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group", dummyGroupAtOrange);
        assertNoGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroupAtOrange, "newLargo");
    }

    @Test
    public void test319UnassignRoleWimpFromLargo() throws Exception {
		final String TEST_NAME = "test319UnassignRoleWimpFromLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_WIMP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_LARGO_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, "newLargo");

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_LARGO_USERNAME);
        assertNoGroupMember(dummyGroup, "newLargo");

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertNoGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
        // Orange resource has explicit referential integrity switched off
        assertGroupMember(dummyGroupAtOrange, "newLargo");
	}

    /**
     * Similar routine than 31x, but different ordering.
     */
    @Test
    public void test320AssignRoleBruteToRapp() throws Exception {
		final String TEST_NAME = "test320AssignRoleBruteToRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        addObject(USER_RAPP_FILE);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test322AssignRoleWimpToRapp() throws Exception {
		final String TEST_NAME = "test322AssignRoleWimpToRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_WIMP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test324AssignRoleThugToRapp() throws Exception {
		final String TEST_NAME = "test324AssignRoleThugToRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_THUG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertGroupMember(dummyGroupThug, USER_RAPP_USERNAME);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test327UnassignRoleWimpFromRapp() throws Exception {
		final String TEST_NAME = "test327UnassignRoleWimpFromRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_WIMP_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertNoGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertNoGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertGroupMember(dummyGroupThug, USER_RAPP_USERNAME);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test328UnassignRoleThugFromRapp() throws Exception {
		final String TEST_NAME = "test328UnassignRoleThugFromRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_THUG_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMember(dummyGroupThug, USER_RAPP_USERNAME);

        DummyGroup dummyGroupThugWannabe = dummyResourceOrange.getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertNoGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertNoGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test329UnAssignRoleBruteFromRapp() throws Exception {
		final String TEST_NAME = "test329UnAssignRoleBruteFromRapp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBrute = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        // Orange resource has explicit referential integrity switched off
        assertGroupMember(dummyGroupBrute, USER_RAPP_USERNAME);

        DummyGroup dummyGroupBruteWannabe = dummyResourceOrange.getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        // Orange resource has explicit referential integrity switched off
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimps = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimps);
        display("Group", dummyGroupWimps);
        assertNoGroupMembers(dummyGroupWimps);

        DummyGroup dummyGroupWimpsAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpsAtOrange);
        display("Group @orange", dummyGroupWimpsAtOrange);
        // Orange resource has explicit referential integrity switched off
        assertNoGroupMember(dummyGroupWimpsAtOrange, USER_RAPP_USERNAME);
	}

    @Test
    public void test600AssignRolePirateToJack() throws Exception {
		final String TEST_NAME = "test600AssignRolePirateToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    @Test
    public void test610AssignRoleSwashbucklerToJack() throws Exception {
		final String TEST_NAME = "test610AssignRoleSwashbucklerToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum", "grog");

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test620UnAssignSwashbucklerFromJack() throws Exception {
		final String TEST_NAME = "test620UnAssignSwashbucklerFromJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    /**
     * Assign role with entitlement. The assignment is not yet valid.
     */
    @Test
    public void test630AssignRoleSwashbucklerToJackValidity() throws Exception {
		final String TEST_NAME = "test630AssignRoleSwashbucklerToJackValidity";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();

        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead
        activationType.setValidFrom(validFrom);

        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(3*60*60*1000)); // three hours ahead
        activationType.setValidTo(validTo);

		// WHEN
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, activationType, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    @Test
    public void test640JackRoleSwashbucklerBecomesValid() throws Exception {
		final String TEST_NAME = "test640JackRoleSwashbucklerBecomesValid";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        clock.overrideDuration("PT2H");

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum", "grog");


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test645JackRoleSwashbucklerIsValid() throws Exception {
		final String TEST_NAME = "test645JackRoleSwashbucklerIsValid";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum", "grog");

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test650JackRoleSwashbucklerBecomesInvalid() throws Exception {
		final String TEST_NAME = "test650JackRoleSwashbucklerBecomesInvalid";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        clock.overrideDuration("PT2H");

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User jack", user);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        // Drink is non-tolerant. Reconcile will remove the value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        // Title is tolerant. Reconcile will not remove the value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

	/**
	 * Guybrush has an entitlement (swashbucklers), but after recomputation it should go away.
	 */
	@Test
	public void test700ReconcileGuybrush() throws Exception {
		final String TEST_NAME = "test700ReconcileGuybrush";
		TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		clock.resetOverride();

		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);

		// GIVEN
		assertGroupMember(getDummyGroup(null, GROUP_DUMMY_SWASHBUCKLERS_NAME), USER_GUYBRUSH_USERNAME);

		// WHEN
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);
		assertNoGroupMember(getDummyGroup(null, GROUP_DUMMY_SWASHBUCKLERS_NAME), USER_GUYBRUSH_USERNAME);
	}

	/**
	 * Add account to a group using model service (shadow operations) - preparation for next test (tolerantValuePatterns)
	 */
	@Test
	public void test710AssociateGuybrushToLandlubers() throws Exception {
		final String TEST_NAME = "test710AssociateGuybrushToLandlubers";
		TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<ShadowType> delta =
				IntegrationTestTools.createEntitleDelta(ACCOUNT_SHADOW_GUYBRUSH_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				SHADOW_GROUP_DUMMY_LANDLUBERS_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

		// WHEN
		modelService.executeChanges(deltas, null, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_LANDLUBERS_NAME);
		assertNotNull("No group present on dummy resource", dummyGroup);
		display("Group", dummyGroup);
		assertGroupMember(dummyGroup, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}

	/**
	 * Tolerant entitlement (landlubers) should be kept.
	 */
	@Test
	public void test720ReconcileGuybrush() throws Exception {
		final String TEST_NAME = "test720ReconcileGuybrush";
		TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);

		// GIVEN
		assertGroupMember(getDummyGroup(null, GROUP_DUMMY_LANDLUBERS_NAME), USER_GUYBRUSH_USERNAME);

		// WHEN
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);
		assertGroupMember(getDummyGroup(null, GROUP_DUMMY_LANDLUBERS_NAME), USER_GUYBRUSH_USERNAME);
	}

	@SuppressWarnings("unused")
	private void dumpUsersAndTheirAccounts(Task task, OperationResult result) throws Exception {
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, options, task, result);
		for (PrismObject<UserType> user : users) {
			dumpUserAndAccounts(user, task, result);
		}
	}

	private void dumpUserAndAccounts(PrismObject<UserType> user, Task task, OperationResult result)
			throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
			SecurityViolationException {
		display("user", user);
		for (ObjectReferenceType linkRef : user.asObjectable().getLinkRef()) {
			PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, linkRef.getOid(), null, task, result);
			display("shadow", shadow);
		}
	}

	@SuppressWarnings("unused")
	private void dumpAccountAndGroups(PrismObject<UserType> user, String dummyResourceName)
			throws ConflictException, SchemaViolationException, FileNotFoundException, ConnectException {
		String userName = user.getName().getOrig();
		DummyAccount dummyAccount = getDummyAccount(dummyResourceName, userName);
		display("dummy account: " + dummyResourceName, dummyAccount);
		List<DummyGroup> groups = getGroupsForUser(dummyResourceName, userName);
		display("dummy account groups: " + dummyResourceName, groups);
	}

	private List<DummyGroup> getGroupsForUser(String dummyResourceName, String userName)
			throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		return DummyResource.getInstance(dummyResourceName).listGroups().stream()
				.filter(g -> g.containsMember(userName))
				.collect(Collectors.toList());
	}

}


