/*
 * Copyright (c) 2016-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * 
 * Recon should delete resourceAttribute 
 * 
 * Users and the roles being assigned in tests:
 * User0: IT-Role-HR

 *
 * 
 * 
 * @author michael gruber
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconNullValue extends AbstractStoryTest {
	

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "recon-null-value");

	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	/// private static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new
	/// QName(RESOURCE_OPENDJ_NAMESPACE, "group");

	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";

	private static final String USER_0_NAME = "User0";

	private static final String LDAP_INTENT_DEFAULT = "default";
	
	private static final String ACCOUNT_ATTRIBUTE_TITLE = "title";
	
	
	private ResourceType resourceOpenDjType;
	private PrismObject<ResourceType> resourceOpenDj;

	@Override
	protected String getTopOrgOid() {
		return ORG_TOP_OID;
	}

	private File getTestDir() {
		return TEST_DIR;
	}

	
	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServerRI();
	}

	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		//Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, new File(getTestDir(), "resource-opendj.xml"), RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);

		//org
		importObjectFromFile(new File(getTestDir(), "org-top.xml"), initResult);
		
		//role
		importObjectFromFile(new File(getTestDir(), "role-ldap.xml"), initResult);
		
		//template
		importObjectFromFile(new File(getTestDir(), "object-template-user.xml"), initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

		OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
		TestUtil.assertSuccess(testResultOpenDj);

		dumpOrgTree();
		dumpLdap();
		display("FINISHED: test000Sanity");
	}
	
	

	@Test
	public void test100CreateUsers() throws Exception {
		final String TEST_NAME = "test200CreateUsers";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestReconNullValue.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> user0Before = createUser(USER_0_NAME, "givenName0", "familyName0", true);
		

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		display("Adding user0", user0Before);
		addObject(user0Before, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		PrismObject<UserType> user0After = getObjectByName(UserType.class, USER_0_NAME);
		display("user0 after", user0After);
		
		dumpOrgTree();

	}
	
	/**
	 * add honorificPrefix
	 * 
	 * in resource account value for title should have been added
	 * 
	 */
	@Test
	public void test140AddHonorificPrefix() throws Exception {
		final String TEST_NAME = "test140AddHonorificPrefix";
		displayTestTitle(TEST_NAME);

		 // GIVEN
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();


		//TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);
		PrismObject<UserType> userNewPrism = userBefore.clone();
		prismContext.adopt(userNewPrism);
		UserType userNew = userNewPrism.asObjectable();
		userNew.setHonorificPrefix(null);

		ObjectDelta<UserType> delta = userBefore.diff(userNewPrism);
		display("Modifying user with delta", delta);

		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

		// WHEN
		displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);


		// THEN
		displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User after deleting attribute honorificPrefix", userAfter);


        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute deletion", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute deletion", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath(ACCOUNT_ATTRIBUTE_TITLE));

	}

	
	/**
	 * remove title in Resoure Attributer
	 * 
	 * in resource account value for title should have been removed
	 * 
	 */
	@Test //MID-4567
	public void test150RemoveTitleRA() throws Exception {
		final String TEST_NAME = "test150RemoveTitleRA";
		displayTestTitle(TEST_NAME);

		 // GIVEN
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();


		//TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getObjectByName(UserType.class, USER_0_NAME);
        display("User before", userBefore);
		
        
        openDJController.executeLdifChange("dn: uid="+USER_0_NAME+",ou=people,dc=example,dc=com\n"+
                "changetype: modify\n"+
                "add: title\n"+
                "title: Earl");

		// WHEN
		displayWhen(TEST_NAME);
		modelService.recompute(UserType.class, userBefore.getOid(), null, task, result);
		
		

		// THEN
		displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getObjectByName(UserType.class, USER_0_NAME);
        display("User smack after adding attribute title", userAfter);


        String accountOid = getLinkRefOid(userAfter, RESOURCE_OPENDJ_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow after attribute addition", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel after attribute addition", accountModel);

        PrismAsserts.assertNoItem(accountModel, openDJController.getAttributePath( ACCOUNT_ATTRIBUTE_TITLE));

	}

	private void dumpLdap() throws DirectoryException {
		display("LDAP server tree", openDJController.dumpTree());
		display("LDAP server content", openDJController.dumpEntries());
	}


	protected <F extends com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType> PrismObject<F> getObjectByName(
			Class clazz, String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
			CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<F> object = (PrismObject<F>) findObjectByName(clazz, name);
		assertNotNull("The object " + name + " of type " + clazz + " is missing!", object);
		display(clazz + " " + name, object);
		PrismAsserts.assertPropertyValue(object, F.F_NAME, PrismTestUtil.createPolyString(name));
		return object;
	}

}