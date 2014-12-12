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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;

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
        
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
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
        
        assertShadowModel(shadow, SHADOW_GROUP_DUMMY_SWASHBUCKLERS_OID, GROUP_DUMMY_SWASHBUCKLERS_NAME, resourceDummyType, RESOURCE_DUMMY_GROUP_OBJECTCLASS);
        IntegrationTestTools.assertAttribute(shadow, resourceDummyType, 
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
        
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
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
        // TODO: assert role assignment
        
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_SWASHBUCKLERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertEquals("Wrong group description", GROUP_DUMMY_SWASHBUCKLERS_DESCRIPTION, 
        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertGroupMember(dummyGroup, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertGroupMember(dummyGroup, ACCOUNT_JACK_DUMMY_USERNAME);
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
                
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_LANDLUBERS_NAME);
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
		dummyResource.addGroup(mapmakers);
        
        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);
        
		// WHEN
        assignRole(user.getOid(), ROLE_MAPMAKER_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
                
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertGroupMember(dummyGroup, USER_WALLY_NAME);
	}

    
    @Test
    public void test300AddRoleWimp() throws Exception {
		final String TEST_NAME = "test300AddRoleWimp";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        addObject(ROLE_WIMP_FILE, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
                
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
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
    public void test310AssignRoleWimpToLargo() throws Exception {
		final String TEST_NAME = "test310AssignRoleWimpToLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(USER_LARGO_FILE);
        
		// WHEN
        assignRole(USER_LARGO_OID, ROLE_WIMP_OID, task, result);
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
                
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
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
    public void test311RenameLargo() throws Exception {
		final String TEST_NAME = "test311RenameLargo";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestEntitlements.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        modifyUserReplace(USER_LARGO_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("newLargo"));
        
        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
                
        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
//        assertEquals("Wrong group description", GROUP_DUMMY_LANDLUBERS_DESCRIPTION, 
//        		dummyGroup.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNoGroupMember(dummyGroup, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroup, "newLargo");

        DummyGroup dummyGroupAtOrange = dummyResourceOrange.getGroupByName(GROUP_DUMMY_WIMPS_NAME);
        assertNotNull("No group on orange dummy resource", dummyGroupAtOrange);
        display("Group", dummyGroupAtOrange);
        assertNoGroupMember(dummyGroupAtOrange, USER_LARGO_USERNAME);
        assertGroupMember(dummyGroupAtOrange, "newLargo");
    }

}
