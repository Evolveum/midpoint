/*
 * Copyright (c) 2016 mythoss, Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNullAttribute extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "nullattribute");

	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000002222";

	protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	//see
	protected static final String RESOURCE_DUMMY_ID = "DUMMY";
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_NAMESPACE = MidPointConstants.NS_RI;

	private static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME = "fullname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP = "ship";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON = "weapon";

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

	public static final File ROLE_ACCOUNTONLY_FILE = new File(TEST_DIR, "role-accountonly.xml");
	public static final String ROLE_ACCOUNTONLY_OID = "10000000-0000-0000-0000-000000000601";

	public static final File ROLE_SHIPNWEAPON_FILE = new File(TEST_DIR, "role-shipnweapon.xml");
	public static final String ROLE_SHIPNWEAPON_OID = "10000000-0000-0000-0000-000000000602";

	public static final File USER_SMACK_FILE = new File(TEST_DIR, "user-smack.xml");
	public static final String USER_SMACK_OID = "c0c010c0-d34d-b33f-f00d-111111111112";


	protected static final String EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/samples/piracy";


	@Autowired(required = true)
	private ReconciliationTaskHandler reconciliationTaskHandler;

	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;


	@Override
	protected void startResources() throws Exception {

	}

	@AfterClass
	public static void stopResources() throws Exception {

	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);


		// Resources
		//default instance, no instance id
		//when id is set it is required to be present in resource.xml (I guess)
		dummyResourceCtl = DummyResourceContoller.create(null, resourceDummy);
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtl.getDummyResource().getAccountObjectClass();

		//attributes
		dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME, String.class, false, false);
		dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SHIP, String.class, false, false);
		dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON, String.class, false, false);

		dummyResource = dummyResourceCtl.getDummyResource();
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE,
				RESOURCE_DUMMY_OID, initTask, initResult);
		dummyResourceCtl.setResource(resourceDummy);
//		dummyResource.setSyncStyle(DummySyncStyle.SMART);

		//
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);


		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);


		// Role
		importObjectFromFile(ROLE_ACCOUNTONLY_FILE, initResult);
		importObjectFromFile(ROLE_SHIPNWEAPON_FILE, initResult);

		PrismObject<RoleType> rolesw = getRole(ROLE_SHIPNWEAPON_OID);
		 display("role shipNWeapon initial", rolesw);

		//User
		 importObjectFromFile(USER_SMACK_FILE, initResult);

	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);

		TestUtil.assertSuccess(testResult);


	}

	/**
	 * assign role "account only"
	 * role should be assigned and fullname should be set in resource account
	 */
	@Test
	public void test010UserSmackAssignAccountOnlyRole() throws Exception {
		final String TEST_NAME = "test010UserSmackAssignAccountOnlyRole";
		TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
		Task task = taskManager.createTaskInstance(TestNullAttribute.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        assignRole(USER_SMACK_OID, ROLE_ACCOUNTONLY_OID, task, result);


        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SMACK_OID);
        //display("User jack after role account only assignment", user);

        assertAssignedRole(user, ROLE_ACCOUNTONLY_OID);
        assertNotAssignedRole(user, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(user, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after role account only assignment", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after role account only assignment", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath( DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME),"Smack Sparrow");
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_SHIP));
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON));


	}

	/**
	 * set extension/ship
	 * role ShipNWeapon should be assigned (beacause of objecttemplate)
	 * in resource account values for fullname, ship and weapon should exist
	 */
	@Test
	public void test020UserSmackSetAttribute() throws Exception {
		final String TEST_NAME = "test020UserSmackSetAttribute";
		TestUtil.displayTestTitle(this, TEST_NAME);

		 // GIVEN
		Task task = taskManager.createTaskInstance(TestNullAttribute.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

		PrismObject<UserType> smack = getUser(USER_SMACK_OID);
		display("smack initial: "+smack.debugDump());


		 // WHEN
		@SuppressWarnings("unchecked, raw")
		Collection<ObjectDelta<? extends ObjectType>> deltas =
				(Collection) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_EXTENSION, new QName(EXTENSION_NS, "ship")).add("Black Pearl")
				.asObjectDeltas(USER_SMACK_OID);
		modelService.executeChanges(deltas, null, task, result);

		// THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SMACK_OID);
        display("User smack after setting attribute piracy:ship", user);

        assertAssignedRole(user, ROLE_ACCOUNTONLY_OID);
        assertAssignedRole(user, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(user, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after role shipnweapon assignment", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after role shipnweapon assignment", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath( DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME),"Smack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath( DUMMY_ACCOUNT_ATTRIBUTE_SHIP),"Black Pearl");
        // weapon is not in user's extension (MID-3326)
        //PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON),"pistol");

	}

	/**
	 * remove extension/ship
	 * role ShipNWeapon should be unassigned (beacause of objecttemplate)
	 * in resource account only value for fullname should still exist, ship and weapon should have been removed
	 * MID-3325
	 */
	@Test // MID-3325
	public void test030UserSmackRemoveAttribute() throws Exception {
		final String TEST_NAME = "test030UserSmackRemoveAttribute";
		TestUtil.displayTestTitle(this, TEST_NAME);

		 // GIVEN
		Task task = taskManager.createTaskInstance(TestNullAttribute.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();


		//TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getUser(USER_SMACK_OID);
        display("User before", userBefore);
		PrismObject<UserType> userNewPrism = userBefore.clone();
		prismContext.adopt(userNewPrism);
		if (userNewPrism.getExtension()==null)userNewPrism.createExtension();
		PrismContainer<?> ext = userNewPrism.getExtension();
		ext.setPropertyRealValue(new QName(EXTENSION_NS, "ship"), null);

		ObjectDelta<UserType> delta = userBefore.diff(userNewPrism);
		display("Modifying user with delta", delta);

		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);


		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_SMACK_OID);
        display("User smack after deleting attribute piracy:ship", userAfter);

        assertAssignedRole(userAfter, ROLE_ACCOUNTONLY_OID);
        assertNotAssignedRole(userAfter, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after attribute deletion", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after attribute deletion", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath( DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME),"Smack Sparrow");
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON));
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_SHIP));


	}

}
