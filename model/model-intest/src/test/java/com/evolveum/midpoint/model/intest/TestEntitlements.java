/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

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

	public static final File ROLE_ORG_GROUPING_FILE = new File(TEST_DIR, "role-org-grouping.xml");
	public static final String ROLE_ORG_GROUPING_OID = "171add4c-25f4-11e8-9ea1-6f9ae2cfd841";
	
	public static final File ROLE_ORG_GROUPING_REPO_FILE = new File(TEST_DIR, "role-org-grouping-repo.xml");
	public static final String ROLE_ORG_GROUPING_REPO_OID = "02bdd108-261f-11e8-ac3a-bf48bd1c4e40";

	public static final File ROLE_CREW_OF_GUYBRUSH_FILE = new File(TEST_DIR, "role-crew-of-guybrush.xml");
	public static final String ROLE_CREW_OF_GUYBRUSH_OID = "93d3e436-3c6c-11e7-8168-23796882a64e";

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

	public static final String ACCOUNT_GUYBRUSH_DUMMY_ORANGE_USERNAME = "guybrush";

	private static final QName RESOURCE_DUMMY_GROUP_OBJECTCLASS = new QName(RESOURCE_DUMMY_NAMESPACE, "GroupObjectClass");

	private static final String USER_WALLY_NAME = "wally";
	private static final String USER_WALLY_FULLNAME = "Wally B. Feed";

	private static final String ORG_GROUP_PREFIX = "org-";
	private static final String OU_CLUB_SPITTERS = "spitters";
	private static final String OU_CLUB_DIVERS = "divers";
	private static final String OU_CLUB_SCI_FI = "sci-fi";

	private ActivationType jackSwashbucklerAssignmentActivation;

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectFromFile(ROLE_SWASHBUCKLER_FILE);
        importObjectFromFile(ROLE_LANDLUBER_FILE);
        importObjectFromFile(ROLE_MAPMAKER_FILE);
        importObjectFromFile(ROLE_CREW_OF_GUYBRUSH_FILE);
        importObjectFromFile(ROLE_ORG_GROUPING_FILE);
        importObjectFromFile(ROLE_ORG_GROUPING_REPO_FILE);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        rememberSteadyResources();
    }

    /**
     * Add group shadow using model service. Group should appear on dummy resource.
     */
    @Test
    public void test100AddGroupShadowSwashbucklers() throws Exception {
		final String TEST_NAME = "test100AddGroupShadowSwashbucklers";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        assertSteadyResources();

        PrismObject<ShadowType> group = prismContext.parseObject(SHADOW_GROUP_DUMMY_SWASHBUCKLERS_FILE);

		// WHEN
        addObject(group, task, result);

        // THEN
        assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group created on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        
        assertSteadyResources();
	}

	@Test
    public void test101GetGroupShadowSwashbucklers() throws Exception {
		final String TEST_NAME = "test101GetGroupShadowSwashbucklers";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, null, task, result);

        // THEN
        assertSuccess(result);
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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_SHADOW_GUYBRUSH_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

		// WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        addObject(SHADOW_GROUP_DUMMY_LANDLUBERS_FILE);

        PrismObject<UserType> user = createUser(USER_WALLY_NAME, USER_WALLY_FULLNAME, true);
        addObject(user);

		// WHEN
        assignRole(user.getOid(), ROLE_LANDLUBER_OID, task, result);

        // THEN
        assertSuccess(result);

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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyGroup mapmakers = new DummyGroup(GROUP_DUMMY_MAPMAKERS_NAME);
        getDummyResource().addGroup(mapmakers);

        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(user.getOid(), ROLE_MAPMAKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_DUMMY_MAPMAKERS_NAME, USER_WALLY_NAME, getDummyResource());
	}

    @Test
    public void test224UnassignRoleMapmakerFromWally() throws Exception {
		final String TEST_NAME = "test224UnassignRoleMapmakerFromWally";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(user.getOid(), ROLE_MAPMAKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_WALLY_NAME);
	}


    @Test
    public void test300AddRoleWimp() throws Exception {
		final String TEST_NAME = "test300AddRoleWimp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        addObject(ROLE_WIMP_FILE, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION,
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMembers(dummyGroup);

        DummyGroup dummyGroupAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertNoGroupMembers(dummyGroupAtOrange);
        
        assertSteadyResources();
    }

    @Test
    public void test302AddRoleBrute() throws Exception {
		final String TEST_NAME = "test302AddRoleBrute";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        addObject(ROLE_BRUTE_FILE, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertNoGroupMembers(dummyGroupBrute);

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertNoGroupMembers(dummyGroupBruteWannabe);
    }

    @Test
    public void test304AddRoleThug() throws Exception {
		final String TEST_NAME = "test304AddRoleThug";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        addObject(ROLE_THUG_FILE, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMembers(dummyGroupThug);

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMembers(dummyGroupThugWannabe);
    }

    @Test
    public void test310AssignRoleWimpToLargo() throws Exception {
		final String TEST_NAME = "test310AssignRoleWimpToLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        addObject(USER_LARGO_FILE);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_WIMP_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_DUMMY_WIMPS_NAME, USER_LARGO_USERNAME, getDummyResource());

        DummyGroup dummyGroupWimpAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_LARGO_USERNAME);
	}

    @Test
    public void test312AssignRoleBruteToLargo() throws Exception {
		final String TEST_NAME = "test312AssignRoleBruteToLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_BRUTE_NAME, USER_LARGO_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_LARGO_USERNAME);
	}

    @Test
    public void test313UnAssignRoleBruteFromLargo() throws Exception {
		final String TEST_NAME = "test313UnAssignRoleBruteFromLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupBrute = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupBrute);
        display("Group", dummyGroupBrute);
        assertNoGroupMembers(dummyGroupBrute);

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertNoGroupMembers(dummyGroupBruteWannabe);

		assertGroupMember(GROUP_DUMMY_WIMPS_NAME, USER_LARGO_USERNAME, getDummyResource());

        DummyGroup dummyGroupAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group @orange", dummyGroupAtOrange);
        assertGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
	}

    @Test
    public void test314AssignRoleThugToLargo() throws Exception {
		final String TEST_NAME = "test314AssignRoleThugToLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_LARGO_OID, ROLE_THUG_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_THUG_NAME, USER_LARGO_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_LARGO_USERNAME);
	}

    @Test
    public void test315UnAssignRoleThugFromLargo() throws Exception {
		final String TEST_NAME = "test315UnAssignRoleThugFromLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_THUG_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMembers(dummyGroupThug);

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMembers(dummyGroupThugWannabe);

		assertGroupMember(GROUP_DUMMY_WIMPS_NAME, USER_LARGO_USERNAME, getDummyResource());

        DummyGroup dummyGroupAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_LARGO_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("newLargo"));

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_LARGO_USERNAME);

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroup, "newLargo");

        DummyGroup dummyGroupAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group", dummyGroupAtOrange);
        assertNoGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroupAtOrange, "newLargo");
    }

    @Test
    public void test319UnassignRoleWimpFromLargo() throws Exception {
		final String TEST_NAME = "test319UnassignRoleWimpFromLargo";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_LARGO_OID, ROLE_WIMP_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_LARGO_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, "newLargo");

        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertNoGroupMember(dummyGroup, USER_LARGO_USERNAME);
        assertNoGroupMember(dummyGroup, "newLargo");

        DummyGroup dummyGroupAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
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
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        addObject(USER_RAPP_FILE);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test322AssignRoleWimpToRapp() throws Exception {
		final String TEST_NAME = "test322AssignRoleWimpToRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_WIMP_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_DUMMY_WIMPS_NAME, USER_RAPP_USERNAME, getDummyResource());

        DummyGroup dummyGroupWimpAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test324AssignRoleThugToRapp() throws Exception {
		final String TEST_NAME = "test324AssignRoleThugToRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_THUG_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		assertGroupMember(GROUP_THUG_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_DUMMY_WIMPS_NAME, USER_RAPP_USERNAME, getDummyResource());

        DummyGroup dummyGroupWimpAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test327UnassignRoleWimpFromRapp() throws Exception {
		final String TEST_NAME = "test327UnassignRoleWimpFromRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_WIMP_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertNoGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertNoGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_THUG_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test328UnassignRoleThugFromRapp() throws Exception {
		final String TEST_NAME = "test328UnassignRoleThugFromRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_THUG_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DummyGroup dummyGroupThug = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupThug);
        display("Group", dummyGroupThug);
        assertNoGroupMember(dummyGroupThug, USER_RAPP_USERNAME);

        DummyGroup dummyGroupThugWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_THUG_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupThugWannabe);
        display("Wannabe Group", dummyGroupThugWannabe);
        assertNoGroupMember(dummyGroupThugWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimp = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimp);
        display("Group", dummyGroupWimp);
        assertNoGroupMember(dummyGroupWimp, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimpAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpAtOrange);
        display("Group @orange", dummyGroupWimpAtOrange);
        assertNoGroupMember(dummyGroupWimpAtOrange, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);
	}

    @Test
    public void test329UnAssignRoleBruteFromRapp() throws Exception {
		final String TEST_NAME = "test329UnAssignRoleBruteFromRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_BRUTE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);

		assertGroupMember(GROUP_BRUTE_NAME, USER_RAPP_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));

        DummyGroup dummyGroupBruteWannabe = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_BRUTE_NAME + "-wannabe");
        assertNotNull("No wannabe group on orange dummy resource", dummyGroupBruteWannabe);
        display("Wannabe Group", dummyGroupBruteWannabe);
        // Orange resource has explicit referential integrity switched off
        assertGroupMember(dummyGroupBruteWannabe, USER_RAPP_USERNAME);

        DummyGroup dummyGroupWimps = getDummyResource().getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroupWimps);
        display("Group", dummyGroupWimps);
        assertNoGroupMembers(dummyGroupWimps);

        DummyGroup dummyGroupWimpsAtOrange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupWimpsAtOrange);
        display("Group @orange", dummyGroupWimpsAtOrange);
        // Orange resource has explicit referential integrity switched off
        assertNoGroupMember(dummyGroupWimpsAtOrange, USER_RAPP_USERNAME);
	}

    @Test
    public void test350AssignOrangeAccountToGuybrushAndRapp() throws Exception {
		final String TEST_NAME = "test350AssignOrangeAccountToGuybrushAndRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);
        assignAccount(USER_RAPP_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        DummyAccount accountGuybrush = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        display("Account guybrush", accountGuybrush);
        DummyAccount accountRapp = assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);
        display("Account rapp", accountRapp);
	}

    /**
     * MID-2668
     */
    @Test
    public void test351AssignRoleCrewOfGuybrushToRapp() throws Exception {
		final String TEST_NAME = "test351AssignRoleCrewOfGuybrushToRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);

        // preconditions
        assertNotNull("No rapp", userRappBefore);
        assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_RAPP_OID, ROLE_CREW_OF_GUYBRUSH_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User guybrush after", userGuybrushAfter);
        String guybrushShadowOid = getLinkRefOid(userGuybrushAfter, RESOURCE_DUMMY_ORANGE_OID);
        PrismObject<ShadowType> guybrushShadow = getShadowModel(guybrushShadowOid);
        display("Shadow guybrush", guybrushShadow);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        String rappShadowOid = getSingleLinkOid(userRappAfter);
        PrismObject<ShadowType> rappShadow = getShadowModel(rappShadowOid);
        display("Shadow rapp", rappShadow);
        assertAssociation(rappShadow, RESOURCE_DUMMY_ORANGE_ASSOCIATION_CREW_QNAME, guybrushShadowOid);

        DummyAccount dummyOrangeAccountRapp = getDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);
        display("Rapp account", dummyOrangeAccountRapp);
        assertDummyAccountAttribute(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME,
        		DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}

    /**
     * MID-2668
     */
    @Test
    public void test358UnassignRoleCrewOfGuybrushToRapp() throws Exception {
		final String TEST_NAME = "test358UnassignRoleCrewOfGuybrushToRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);

        // preconditions
        assertNotNull("No rapp", userRappBefore);
        assertDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_RAPP_OID, ROLE_CREW_OF_GUYBRUSH_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User guybrush after", userGuybrushAfter);
        String guybrushShadowOid = getLinkRefOid(userGuybrushAfter, RESOURCE_DUMMY_ORANGE_OID);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp before", userRappAfter);
        String rappShadowOid = getSingleLinkOid(userRappAfter);
        PrismObject<ShadowType> rappShadow = getShadowModel(rappShadowOid);
        display("Shadow rapp", rappShadow);
        assertNoAssociation(rappShadow, RESOURCE_DUMMY_ORANGE_ASSOCIATION_CREW_QNAME, guybrushShadowOid);

        DummyAccount dummyOrangeAccountRapp = getDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);
        display("Rapp account", dummyOrangeAccountRapp);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME,
        		DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME);
	}

    @Test
    public void test359UnassignOrangeAccountFromGuybrushAndRapp() throws Exception {
		final String TEST_NAME = "test359UnassignOrangeAccountFromGuybrushAndRapp";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);
        unassignAccount(USER_RAPP_OID, RESOURCE_DUMMY_ORANGE_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_ORANGE_NAME, USER_RAPP_USERNAME);
        
        assertSteadyResources();
	}

    @Test
    public void test600AssignRolePirateToJack() throws Exception {
		final String TEST_NAME = "test600AssignRolePirateToJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    @Test
    public void test610AssignRoleSwashbucklerToJack() throws Exception {
		final String TEST_NAME = "test610AssignRoleSwashbucklerToJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, "grog");

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test620UnAssignSwashbucklerFromJack() throws Exception {
		final String TEST_NAME = "test620UnAssignSwashbucklerFromJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    /**
     * Assign role with entitlement. The assignment is not yet valid.
     */
    @Test
    public void test630AssignRoleSwashbucklerToJackValidity() throws Exception {
		final String TEST_NAME = "test630AssignRoleSwashbucklerToJackValidity";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        jackSwashbucklerAssignmentActivation = new ActivationType();

        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(60*60*1000)); // one hour ahead
        jackSwashbucklerAssignmentActivation.setValidFrom(validFrom);

        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(3*60*60*1000)); // three hours ahead
        jackSwashbucklerAssignmentActivation.setValidTo(validTo);

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, jackSwashbucklerAssignmentActivation, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate");
	}

    @Test
    public void test640JackRoleSwashbucklerBecomesValid() throws Exception {
		final String TEST_NAME = "test640JackRoleSwashbucklerBecomesValid";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, "grog");


        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test645JackRoleSwashbucklerIsValid() throws Exception {
		final String TEST_NAME = "test645JackRoleSwashbucklerIsValid";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, "grog");

        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test650JackRoleSwashbucklerBecomesInvalid() throws Exception {
		final String TEST_NAME = "test650JackRoleSwashbucklerBecomesInvalid";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        clock.overrideDuration("PT2H");

		// WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        assertSuccess(result);

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
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);

        // Title is tolerant. Reconcile will not remove the value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test659UnassignRoleSwashbucklerFromJack() throws Exception {
		final String TEST_NAME = "test659UnassignRoleSwashbucklerFromJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, jackSwashbucklerAssignmentActivation.clone(), task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 1);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        // Drink is non-tolerant. Reconcile will remove the value.
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);

        // Title is tolerant. Reconcile will not remove the value.
        // It might seem that role unassign should remove the Swashbuckler value. But it should not
        // because the role is not valid (expired)
        assertDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Bloody Pirate", "Swashbuckler");
	}

    @Test
    public void test699UnassignRolePirateFromJack() throws Exception {
		final String TEST_NAME = "test699UnassignRolePirateFromJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Guybrush has an entitlement (swashbucklers), but after recomputation it should go away.
	 */
	@Test
	public void test700ReconcileGuybrush() throws Exception {
		final String TEST_NAME = "test700ReconcileGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		clock.resetOverride();

		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);
		dumpOrangeGroups(task, result);

		// GIVEN

		DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
		dummyGroup.addMember(USER_GUYBRUSH_USERNAME);

		assertGroupMember(getDummyGroup(null, GROUP_DUMMY_SWASHBUCKLERS_NAME), USER_GUYBRUSH_USERNAME);

		// WHEN
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);
		assertNoGroupMember(getDummyGroup(null, GROUP_DUMMY_SWASHBUCKLERS_NAME), USER_GUYBRUSH_USERNAME);
		
		assertSteadyResources();
	}

	/**
	 * Add account to a group using model service (shadow operations) - preparation for next test (tolerantValuePatterns)
	 */
	@Test
	public void test710AssociateGuybrushToLandlubers() throws Exception {
		final String TEST_NAME = "test710AssociateGuybrushToLandlubers";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<ShadowType> delta =
				IntegrationTestTools.createEntitleDelta(ACCOUNT_SHADOW_GUYBRUSH_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				SHADOW_GROUP_DUMMY_LANDLUBERS_OID, prismContext);

		// WHEN
		executeChanges(delta, null, task, result);

		// THEN
		assertSuccess(result);

		assertGroupMember(GROUP_DUMMY_LANDLUBERS_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
	}


	/**
	 * Add account to 2 groups using model service (shadow operations) - preparation for next test (intolerantValuePatterns)
	 */
	@Test
	public void test715AssociateGuybrushToThugs() throws Exception {
		final String TEST_NAME = "test715AssociateGuybrushToThugs";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_ORANGE_OID, "default", task, result);
		dumpUserAndAccounts(USER_GUYBRUSH_OID);

		PrismObject<ShadowType> orangeAccount = findAccountShadowByUsername(USER_GUYBRUSH_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_ORANGE_NAME), result);
		assertNotNull("No orange account for guybrush", orangeAccount);

		ObjectDelta<ShadowType> delta1 =
				IntegrationTestTools.createEntitleDelta(orangeAccount.getOid(),
						getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
						getGroupShadow(getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME), getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME).getGroupObjectClass(), "thug", task, result).getOid(),
								prismContext);

		ObjectDelta<ShadowType> delta2 =
				IntegrationTestTools.createEntitleDelta(orangeAccount.getOid(),
						getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
						getGroupShadow(getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME), getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME).getGroupObjectClass(), "thug-wannabe", task, result) .getOid(),
								prismContext);

		ObjectDelta<ShadowType> delta = ObjectDelta.summarize(delta1, delta2);

		// WHEN
		displayWhen(TEST_NAME);
		executeChanges(delta, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		assertGroupMember(GROUP_THUG_NAME, ACCOUNT_GUYBRUSH_DUMMY_ORANGE_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));
		assertGroupMember(GROUP_THUG_NAME+"-wannabe", ACCOUNT_GUYBRUSH_DUMMY_ORANGE_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));
	}

	/**
	 * Tolerant entitlement (landlubers, thug) should be kept. Other (thug-wannabe) should not.
	 */
	@Test
	public void test720ReconcileGuybrush() throws Exception {
		final String TEST_NAME = "test720ReconcileGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		dumpUserAndAccounts(USER_GUYBRUSH_OID);

		// GIVEN
		assertGroupMember(getDummyGroup(null, GROUP_DUMMY_LANDLUBERS_NAME), USER_GUYBRUSH_USERNAME);

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		dumpUserAndAccounts(getUser(USER_GUYBRUSH_OID), task, result);
		assertGroupMember(GROUP_DUMMY_LANDLUBERS_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(GROUP_THUG_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));
		assertNoGroupMember(GROUP_THUG_NAME+"-wannabe", ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource(RESOURCE_DUMMY_ORANGE_NAME));
	}
	
	@Test
	public void test729CleanupGuybrush() throws Exception {
		final String TEST_NAME = "test729CleanupGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		PrismObject<UserType> userBefore = dumpUserAndAccounts(USER_GUYBRUSH_OID);

		// WHEN
		displayWhen(TEST_NAME);
		unassignAll(userBefore, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 0);
		assertLinks(userAfter, 0);
		
		assertNoDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}
	
	/**
	 * Guybrush is assigned to a group that induces an association to a dummy group that
	 * does not exists yet.
	 * All the associations to groups that exist should be provisioned. The non-existed group
	 * cannot be provisionined yet. But there should be no error.
	 */
	@Test
	public void test750PrepareGuybrushFuturePerfect() throws Exception {
		final String TEST_NAME = "test750PrepareGuybrushFuturePerfect";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		getDummyResourceController().addGroup(orgGroupName(OU_CLUB_SPITTERS));
		getDummyResourceController().addGroup(orgGroupName(OU_CLUB_DIVERS));
		modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result, 
				createPolyString(OU_CLUB_SPITTERS), createPolyString(OU_CLUB_DIVERS), createPolyString(OU_CLUB_SCI_FI));

		PrismObject<UserType> userBefore = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userBefore, 0);
		assertLinks(userBefore, 0);

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(USER_GUYBRUSH_OID, ROLE_ORG_GROUPING_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 1);
		assertLinks(userAfter, 1);
		
		assertGroupMember(orgGroupName(OU_CLUB_SPITTERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_DIVERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertNoDummyGroup(orgGroupName(OU_CLUB_SCI_FI));
	}
	
	private String orgGroupName(String ou) {
		return ORG_GROUP_PREFIX + ou;
	}
	
	/**
	 * Create the missing group. Reconcile guybrush. Then guybrush should be associated
	 * with the group.
	 */
	@Test
	public void test752GuybrushFuturePerfect() throws Exception {
		final String TEST_NAME = "test752GuybrushFuturePerfect";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		getDummyResourceController().addGroup(orgGroupName(OU_CLUB_SCI_FI));

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 1);
		assertLinks(userAfter, 1);
		
		assertGroupMember(orgGroupName(OU_CLUB_SPITTERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_DIVERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_SCI_FI), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
	}
	
	@Test
	public void test759CleanupGuybrush() throws Exception {
		final String TEST_NAME = "test759CleanupGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		dumpUserAndAccounts(USER_GUYBRUSH_OID);

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(USER_GUYBRUSH_OID, ROLE_ORG_GROUPING_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 0);
		assertLinks(userAfter, 0);
		
		assertNoDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}

	/**
	 * MID-4509
	 */
	@Test
	public void test760GuybrushOrgGroupsRepo() throws Exception {
		final String TEST_NAME = "test760GuybrushOrgGroupsRepo";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> userBefore = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userBefore, 0);
		assertLinks(userBefore, 0);

		// WHEN
		displayWhen(TEST_NAME);
		assignRole(USER_GUYBRUSH_OID, ROLE_ORG_GROUPING_REPO_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 1);
		assertLinks(userAfter, 1);
		
		assertGroupMember(orgGroupName(OU_CLUB_SPITTERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_DIVERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_SCI_FI), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
	}
	
	@Test
	public void test765ReconcileGuybrush() throws Exception {
		final String TEST_NAME = "test765ReconcileGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		dummyAuditService.clear();

		// WHEN
		displayWhen(TEST_NAME);
		reconcileUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 1);
		assertLinks(userAfter, 1);
		
		assertGroupMember(orgGroupName(OU_CLUB_SPITTERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_DIVERS), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		assertGroupMember(orgGroupName(OU_CLUB_SCI_FI), ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResource());
		
		display("Audit", dummyAuditService);
		dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
	public void test769CleanupGuybrush() throws Exception {
		final String TEST_NAME = "test769CleanupGuybrush";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		dumpUserAndAccounts(USER_GUYBRUSH_OID);

		// WHEN
		displayWhen(TEST_NAME);
		unassignRole(USER_GUYBRUSH_OID, ROLE_ORG_GROUPING_REPO_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		PrismObject<UserType> userAfter = dumpUserAndAccounts(USER_GUYBRUSH_OID);
		assertAssignments(userAfter, 0);
		assertLinks(userAfter, 0);
		
		assertNoDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}
	/**
	 * MID-4021
	 */
	@Test
    public void test800AssignRoleSwashbucklerToJackNone() throws Exception {
		final String TEST_NAME = "test800AssignRoleSwashbucklerToJackNone";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // preconditions
        assertJackClean();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertSteadyResources();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test805ReconcileJackNone() throws Exception {
		final String TEST_NAME = "test805ReconcileJackNone";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test809UnAssignRoleSwashbucklerFromJackNone() throws Exception {
		final String TEST_NAME = "test809UnAssignRoleSwashbucklerFromJackNone";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackNoAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test810AssignRoleSwashbucklerToJackPositive() throws Exception {
		final String TEST_NAME = "test810AssignRoleSwashbucklerToJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // preconditions
        assertJackClean();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test815ReconcileJackPositive() throws Exception {
		final String TEST_NAME = "test815ReconcileJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test817UnAssignRoleSwashbucklerFromJackPositive() throws Exception {
		final String TEST_NAME = "test817UnAssignRoleSwashbucklerFromJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test819ReconcileJackPositive() throws Exception {
		final String TEST_NAME = "test819ReconcileJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        // Group association is non-tolerant.
        // So, account should remain, but the group should be gone
        // (removed by reconciliation)
        assertJackAccountNoSwashbuckler();
	}

	/**
	 * Jack has account and entitlement from previous test. Now we are in full
	 * enforcement. Recompute should remove the account and entitlement.
	 * MID-4021
	 */
	@Test
    public void test820RecomputeJackFull() throws Exception {
		final String TEST_NAME = "test820RecomputeJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // preconditions
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

		// WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackNoAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test822AssignRoleSwashbucklerToJackFull() throws Exception {
		final String TEST_NAME = "test822AssignRoleSwashbucklerToJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // preconditions
        assertJackClean();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test825ReconcileJackFull() throws Exception {
		final String TEST_NAME = "test825ReconcileJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 1);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test827UnAssignRoleSwashbucklerFromJackFull() throws Exception {
		final String TEST_NAME = "test827UnAssignRoleSwashbucklerFromJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackNoAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test829ReconcileJackFull() throws Exception {
		final String TEST_NAME = "test829ReconcileJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackNoAccountNoSwashbuckler();
	}

	/**
	 * For next few tests keep account assigned, add/remove just the entitlement assignment
	 * MID-4021
	 */
	@Test
    public void test830AssignJackAccountDummy() throws Exception {
		final String TEST_NAME = "test830AssignJackAccountDummy";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // preconditions
        assertJackClean();

		// WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertJackAccountNoSwashbuckler();
 	}

	/**
	 * MID-4021
	 */
	@Test
    public void test840AssignRoleSwashbucklerToJackNone() throws Exception {
		final String TEST_NAME = "test840AssignRoleSwashbucklerToJackNone";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // preconditions
        assertJackJustAccount();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 2);

        assertJackAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test849UnassignRoleSwashbucklerFromJackNone() throws Exception {
		final String TEST_NAME = "test849UnassignRoleSwashbucklerFromJackNone";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertJackAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test850AssignRoleSwashbucklerToJackPositive() throws Exception {
		final String TEST_NAME = "test850AssignRoleSwashbucklerToJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // preconditions
        assertJackJustAccount();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 2);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test859UnassignRoleSwashbucklerToJackPositive() throws Exception {
		final String TEST_NAME = "test859UnassignRoleSwashbucklerToJackPositive";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertJackAccountSwashbuckler();
	}

	/**
	 * Jack has account and entitlement from previous test. Now we are in full
	 * enforcement. Recompute should remove the entitlement.
	 * MID-4021
	 */
	@Test
    public void test860RecomputeJackFull() throws Exception {
		final String TEST_NAME = "test860RecomputeJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertJackAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test862AssignRoleSwashbucklerToJackFull() throws Exception {
		final String TEST_NAME = "test862AssignRoleSwashbucklerToJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // preconditions
        assertJackJustAccount();

		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_SWASHBUCKLER_OID);
        assertAssignments(userAfter, 2);

        assertJackAccountSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test869UnassignRoleSwashbucklerToJackFull() throws Exception {
		final String TEST_NAME = "test869UnassignRoleSwashbucklerToJackFull";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SWASHBUCKLER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertJackAccountNoSwashbuckler();
	}

	/**
	 * MID-4021
	 */
	@Test
    public void test899UnAssignAccountJackDummy() throws Exception {
		final String TEST_NAME = "test899UnAssignAccountJackDummy";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		// WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);

        assertJackNoAccountNoSwashbuckler();
        
        assertSteadyResources();
	}

	private void assertJackClean() throws SchemaViolationException, ConflictException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	private void assertJackJustAccount() throws SchemaViolationException, ConflictException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ConnectException, FileNotFoundException {
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Account: yes, group: yes
	 */
    private void assertJackAccountSwashbuckler() throws SchemaViolationException, ConflictException, ConnectException, FileNotFoundException {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * Account: yes, group: no
	 */
 	private void assertJackAccountNoSwashbuckler() throws SchemaViolationException, ConflictException, ConnectException, FileNotFoundException {
 		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
 		DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
		assertNotNull("No group on dummy resource", dummyGroup);
		display("Group", dummyGroup);
		assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
		 		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
		assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);
 	}

	/**
	 * Account: no, group: no
	 */
	private void assertJackNoAccountNoSwashbuckler() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		DummyGroup dummyGroup = getDummyResource().getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION,
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	@SuppressWarnings("unused")
	private void dumpUsersAndTheirAccounts(Task task, OperationResult result) throws Exception {
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, options, task, result);
		for (PrismObject<UserType> user : users) {
			dumpUserAndAccounts(user, task, result);
		}
	}

	private void dumpUserAndAccounts(PrismObject<UserType> user)
			throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("dumpUserAndAccounts");
		OperationResult result = task.getResult();
		dumpUserAndAccounts(user, task, result);
	}
	
	private PrismObject<UserType> dumpUserAndAccounts(String userOid)
			throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("dumpUserAndAccounts");
		OperationResult result = task.getResult();
		PrismObject<UserType> user = getUser(userOid);
		dumpUserAndAccounts(user, task, result);
		return user;
	}
	
	private void dumpUserAndAccounts(PrismObject<UserType> user, Task task, OperationResult result)
			throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {
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

	private void dumpOrangeGroups(Task task, OperationResult result)
			throws Exception {
		System.out.println("--------------------------------------------- Orange --------------------");
		display("Orange groups", getDummyResource(RESOURCE_DUMMY_ORANGE_NAME).listGroups());
		SearchResultList<PrismObject<ShadowType>> orangeGroupsShadows = modelService
				.searchObjects(ShadowType.class, ObjectQueryUtil.createResourceAndObjectClassQuery(
						RESOURCE_DUMMY_ORANGE_OID, getDummyResourceController(RESOURCE_DUMMY_ORANGE_NAME).getGroupObjectClass(), prismContext), null, task,
						result);
		display("Orange groups shadows", orangeGroupsShadows);
		System.out.println("--------------------------------------------- Orange End ----------------");
	}

	private PrismObject<ShadowType> getGroupShadow(DummyResourceContoller dummyResourceCtl, QName objectClass, String name, Task task,
			OperationResult result) throws Exception {
		PrismObject<ResourceType> resource = dummyResourceCtl.getResource();
		ObjectClassComplexTypeDefinition groupDef = RefinedResourceSchema.getRefinedSchema(resource)
				.findObjectClassDefinition(dummyResourceCtl.getGroupObjectClass());
		ResourceAttributeDefinition<Object> nameDef = groupDef.findAttributeDefinition(SchemaConstants.ICFS_NAME);
		assertNotNull("No icfs:name definition", nameDef);
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassFilterPrefix(resource.getOid(), objectClass, prismContext)
				.and().item(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), nameDef).eq(name)
				.build();
		SearchResultList<PrismObject<ShadowType>> shadows =
				modelService.searchObjects(ShadowType.class, query, null, task, result);
		assertEquals("Wrong # of results for " + name + " of " + objectClass + " at " + resource, 1, shadows.size());
		return shadows.get(0);
	}

	private void assertGroupMember(String groupName, String accountName, DummyResource dummyResource)
			throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		DummyGroup dummyGroup = dummyResource.getGroupByName(groupName);
		assertNotNull("No group " + dummyGroup + " on " + dummyResource, dummyGroup);
		display("group", dummyGroup);
		assertGroupMember(dummyGroup, accountName);
	}

	private void assertNoGroupMember(String groupName, String accountName, DummyResource dummyResource)
			throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		DummyGroup dummyGroup = dummyResource.getGroupByName(groupName);
		assertNotNull("No group " + dummyGroup + " on " + dummyResource, dummyGroup);
		display("group", dummyGroup);
		assertNoGroupMember(dummyGroup, accountName);
	}

}


