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
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestEntertainment extends AbstractStoryTest {

	public static Trace LOGGER = TraceManager.getTrace(TestEntertainment.class);

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "entertainment");

	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

	private static final File ROLE_META_CREATE_ORG_GROUPS_FILE = new File(TEST_DIR,
			"role-meta-org-groups.xml");
	private static final String ROLE_META_CREATE_ORG_GROUPS_OID = "10000000-ent0-0000-0000-000000006601";

	private static final File ORG_GAMES_TOP_FILE = new File(TEST_DIR, "org-games-top.xml");
	private static final String ORG_GAMES_TOP_OID = "00000000-8888-6666-ent0-100000000001";

	private static final File ORG_POKER_FILE = new File(TEST_DIR, "org-poker.xml");
	private static final String ORG_POKER_OID = "00000000-8888-6666-ent0-100000000002";

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
		
		importObjectFromFile(ROLE_META_CREATE_ORG_GROUPS_FILE);

		

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

		PrismObject<RoleType> metaRole = modelService.getObject(RoleType.class,
				ROLE_META_CREATE_ORG_GROUPS_OID, null, task, result);
		assertNotNull("No metarole, probably probelm with initialization", metaRole);
		result.computeStatus();
		assertSuccess("Role not fetch successfully", result);

	}

	@Test
	public void test001AddParentOrg() throws Exception {
		final String TEST_NAME = "test001AddParentOrg";
		displayTestTitle(TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		addObject(ORG_GAMES_TOP_FILE);

		// THEN
		PrismObject<OrgType> orgGames = modelService.getObject(OrgType.class, ORG_GAMES_TOP_OID, null, task,
				result);
		assertNotNull("No top org for games found", orgGames);
		result.computeStatus();
		assertSuccess("Error while getting top games org", result);

		OrgType orgGamesType = orgGames.asObjectable();
		assertLinks(orgGames, 2);

		List<ObjectReferenceType> linkRefs = orgGamesType.getLinkRef();

		// SHADOW 1
		ShadowType shadowType1 = getAndAssertShadowSuccess(linkRefs.get(0), task, result);

		// SHADOW 2
		ShadowType shadowType2 = getAndAssertShadowSuccess(linkRefs.get(1), task, result);

		assertIntents(shadowType1, shadowType2);

	}
	
	@Test
	public void test002AddChildOrg() throws Exception {
		final String TEST_NAME = "test002AddChildOrg";
		displayTestTitle(TEST_NAME);
		Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		addObject(ORG_POKER_FILE);

		// THEN
		Collection<String> uniqueMembers = openDJController.getGroupUniqueMembers("cn=Games,ou=groups,dc=example,dc=com");
		assertNotNull("null unique members", uniqueMembers);
		assertEquals("Expected exactly one member", 1, uniqueMembers.size());
		
		openDJController.assertUniqueMember("cn=Games,ou=groups,dc=example,dc=com", "cn=Poker,ou=groups,dc=example,dc=com");
		
		PrismObject<OrgType> orgGames = modelService.getObject(OrgType.class, ORG_POKER_OID, null, task,
				result);
		assertNotNull("No top org for games found", orgGames);
		result.computeStatus();
		assertSuccess("Error while getting top games org", result);

		OrgType orgGamesType = orgGames.asObjectable();
		assertLinks(orgGames, 2);

		List<ObjectReferenceType> linkRefs = orgGamesType.getLinkRef();

		// SHADOW 1
		ShadowType shadowType1 = getAndAssertShadowSuccess(linkRefs.get(0), task, result);

		// SHADOW 2
		ShadowType shadowType2 = getAndAssertShadowSuccess(linkRefs.get(1), task, result);

		assertIntents(shadowType1, shadowType2);

	}
	
	private ShadowType getAndAssertShadowSuccess(ObjectReferenceType ort, Task task, OperationResult result) throws Exception {
		assertNotNull("Unexpected (null) reference to shadow", ort);
		PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, ort.getOid(),
				null, task, result);
		assertNotNull("No shadow for org found", shadow);
		ShadowType shadowType = shadow.asObjectable();
		result.computeStatus();
		assertSuccess("Overal error while getting shadow", result);
		TestUtil.assertSuccess("Problem with getting concrete shadow: fetchResult", shadowType.getFetchResult());
		assertNull("Unexpected error in shadow: result", shadowType.getResult());
		return shadowType;
	}
	
	private void assertIntents(ShadowType shadowType1, ShadowType shadowType2) {
		String intentShadow1 = shadowType1.getIntent();
		String intentShadow2 = shadowType2.getIntent();
		if ((intentShadow1.equals("group-org-local") && intentShadow2.equals("group-org-global"))
				|| (intentShadow1.equals("group-org-global") && intentShadow2.equals("group-org-local"))) {
			// EVERYTHING OK
		} else {
			fail("Shadow intents are not correct, expected one group-org-local and one group-org-global, but got: "
					+ intentShadow1 + ", " + intentShadow2);
		}
	}

}
