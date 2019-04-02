/*
 * Copyright (c) 2016-2019 Evolveum
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

package com.evolveum.midpoint.testing.story.ldap;


import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.opends.server.types.Attribute;
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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.testing.story.TestTrafo;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Testing PolyString all the way to LDAP connector. The PolyString data should be translated
 * to LDAP "language tag" attributes (attribute options).
 * MID-5210
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapPolyString extends AbstractLdapTest {

	public static final File TEST_DIR = new File(LDAP_TEST_DIR, "polystring");

	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

	private static final String[] JACK_FULL_NAME_LANG_EN_SK = {
			"en", "Jack Sparrow",
			"sk", "Džek Sperou"
		};

	private PrismObject<ResourceType> resourceOpenDj;

	private String accountJackOid;

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

		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);	
		openDJController.setResource(resourceOpenDj);

		DebugUtil.setDetailedDebugDump(true);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpLdap();
	}

	/**
	 * Simple test, more like a sanity test that everything works OK with simple polystrings (no lang yet).
	 */
	@Test
    public void test050AssignAccountOpenDjSimple() throws Exception {
		final String TEST_NAME = "test050AssignAccountOpenDjSimple";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		accountJackOid = assertUserAfter(USER_JACK_OID)
			.singleLink()
				.getOid();
		
		assertModelShadow(accountJackOid);
		
		Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
		display("Jack LDAP entry", accountEntry);
		assertCn(accountEntry, USER_JACK_FULL_NAME);
		assertDescription(accountEntry, USER_JACK_FULL_NAME /* no langs here (yet) */);
	}
	
	@Test
    public void test059UnassignAccountOpenDjSimple() throws Exception {
		final String TEST_NAME = "test059UnassignAccountOpenDjSimple";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

		assertUserAfter(USER_JACK_OID)
			.links()
				.assertNone();
		
		assertNoShadow(accountJackOid);
		
		Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
		display("Jack LDAP entry", accountEntry);
		assertNull("Unexpected LDAP entry for jack", accountEntry);
	}
	
	/**
	 * Things are getting interesting here. We set up Jack's full name with
	 * a small set of 'lang' values.
	 * No provisioning yet. Just to make sure midPoint core works.
	 */
	@Test
    public void test100ModifyJackFullNameLang() throws Exception {
		final String TEST_NAME = "test100ModifyJackFullNameLang";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME);
        newFullName.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_EN_SK));
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
        	.fullName()
        		.display()
        		.assertOrig(USER_JACK_FULL_NAME)
        		.assertLangs(JACK_FULL_NAME_LANG_EN_SK)
        		.end()
			.links()
				.assertNone();
        	

	}
	
	private Entry getLdapEntryByUid(String uid) throws DirectoryException {
		return openDJController.searchSingle("uid="+uid);
	}

	private void assertCn(Entry entry, String expectedValue) {
		OpenDJController.assertAttribute(entry, "cn", expectedValue);
	}

	private void assertDescription(Entry entry, String expectedOrigValue, String... params) {
		OpenDJController.assertAttributeLang(entry, "description", expectedOrigValue, params);
	}


}