package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2013 Evolveum
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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.sleepycat.je.rep.impl.RepGroupProtocol.Fail;

/**
 * 
 * @author Katarina Valalikova
 *
 */

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScience  extends AbstractStoryTest {
	
	private static final String TEST_DIR = "src/test/resources/science";
	
	public static final String NS_SCIENCE_EXT = "http://midpoint.evolveum.com/xml/ns/science/user/ext";
	private static final QName SCIENCE_EXTENSION_UID_QNAME = new QName(NS_SCIENCE_EXT, "aixUserId");
	
	private static final File ROLE_STATISTICS_FILE = new File(TEST_DIR, "/role-statistics.xml");
	private static final String ROLE_STATISTICS_OID = "23d90f70-1924-419e-9beb-78a8bde6d261";
	
//	private static final File ROLE_MATH_FILE = new File(TEST_DIR, "/role-math.xml");
//	private static final String ROLE_MATH_OID = "";
	
	private static final File GROUP_STATS_USERS_LDIF_FILE = new File(TEST_DIR, "group-stats.ldif");
	
	private static final File RESOURCE_OPENDJ_AD_SIMULATION_FILE = new File(TEST_DIR, "resource-opendj-ad-simulation.xml");
	private static final String RESOURCE_OPENDJ_AD_SIMULATION_OID = "10000000-0000-0000-0000-0000000001ad";
	
	private static final File RESOURCE_DUMMY_STATS_FILE = new File(TEST_DIR, "resource-dummy-stats.xml");
	private static final String RESOURCE_DUMMY_STATS_OID = "10000000-0000-0000-0000-0000000005sa";
	protected static final String RESOURCE_DUMMY_STATS_ID = "stats";
	
	private static final File RESOURCE_DUMMY_UNIX_FILE = new File(TEST_DIR, "resource-dummy-unix.xml");
	private static final String RESOURCE_DUMMY_UNIX_OID = "10000000-0000-0000-0000-0000000004ax";
	protected static final String RESOURCE_DUMMY_UNIX_ID = "unix";
	
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SHELL_NAME = "Shell";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SYSTEM_NAME = "SYSTEM";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DIR_NAME = "Dir";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_UID_NAME = "Uid";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DESCRIPTION_NAME = "Description";
	
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_STATS_DESC_NAME = "Desc";
	
	private static final String NS_RESOURCE_INSTANCE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
	
	private static final QName UNIX_INTERNAL_ID = new QName(NS_RESOURCE_INSTANCE, DummyAccount.ATTR_INTERNAL_ID);
	
	
	protected static DummyResource dummyResourceUnix;
	protected static DummyResourceContoller dummyResourceCtlUnix;
	protected ResourceType resourceDummyUnixType;
	protected PrismObject<ResourceType> resourceDummyUnix;
	
	protected static DummyResource dummyResourceStats;
	protected static DummyResourceContoller dummyResourceCtlStats;
	protected ResourceType resourceDummyStatsType;
	protected PrismObject<ResourceType> resourceDummyStats;
	
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
		
		// Roles
		repoAddObjectFromFile(ROLE_STATISTICS_FILE, RoleType.class, initResult);
//		repoAddObjectFromFile(ROLE_MATH_FILE, RoleType.class, initResult);
		
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_AD_SIMULATION_FILE, RESOURCE_OPENDJ_AD_SIMULATION_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		openDJController.addEntryFromLdifFile(GROUP_STATS_USERS_LDIF_FILE);
		
		// Resources
		dummyResourceCtlUnix = DummyResourceContoller.create(RESOURCE_DUMMY_UNIX_ID, resourceDummyUnix);
		dummyResourceCtlUnix.populateWithDefaultSchema();
		DummyObjectClass dummyUnixAccountObjectClass = dummyResourceCtlUnix.getDummyResource().getAccountObjectClass();
		dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DESCRIPTION_NAME, String.class, false, false);
		dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_DIR_NAME, String.class, false, false);
		dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SHELL_NAME, String.class, false, false);
		dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_SYSTEM_NAME, String.class, false, false);
		dummyResourceCtlUnix.addAttrDef(dummyUnixAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_UNIX_UID_NAME, Integer.class, false, false);
//		dummyResourceCtlUnix.extendSchemaAd();;
		dummyResourceUnix = dummyResourceCtlUnix.getDummyResource();
		resourceDummyUnix = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UNIX_FILE, RESOURCE_DUMMY_UNIX_OID, initTask, initResult);
		resourceDummyUnixType = resourceDummyUnix.asObjectable();
		dummyResourceCtlUnix.setResource(resourceDummyUnix);
		
		dummyResourceCtlStats = DummyResourceContoller.create(RESOURCE_DUMMY_STATS_ID, resourceDummyStats);
		dummyResourceCtlStats.populateWithDefaultSchema();
		DummyObjectClass dummyStatsAccountObjectClass = dummyResourceCtlStats.getDummyResource().getAccountObjectClass();
		dummyResourceCtlStats.addAttrDef(dummyStatsAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_STATS_DESC_NAME, String.class, false, false);
		
		dummyResourceStats = dummyResourceCtlStats.getDummyResource();
		resourceDummyStats = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_STATS_FILE, RESOURCE_DUMMY_STATS_OID, initTask, initResult);
		resourceDummyStatsType = resourceDummyStats.asObjectable();
		dummyResourceCtlStats.setResource(resourceDummyStats);
		
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestScience.class.getName() + "." + TEST_NAME);
        
        OperationResult testResultStats = modelService.testResource(RESOURCE_DUMMY_STATS_OID, task);
        TestUtil.assertSuccess(testResultStats);
        
        OperationResult testResultUnix = modelService.testResource(RESOURCE_DUMMY_UNIX_OID, task);
        TestUtil.assertSuccess(testResultUnix);
        
        OperationResult testResultAd = modelService.testResource(RESOURCE_OPENDJ_AD_SIMULATION_OID, task);
        TestUtil.assertSuccess(testResultAd);
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
	}
	
	@Test
	public void test100jackAssignRoleStatistics() throws Exception {
		final String TEST_NAME = "test100jackAssignRoleStatistics";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestScience.class.getName() + "." + TEST_NAME);
		
		OperationResult result = task.getResult();
		
		assignRole(USER_JACK_OID, ROLE_STATISTICS_OID);
		
		PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
		AssertJUnit.assertNotNull("User jack not found", userJack);
		UserType jackType = userJack.asObjectable();
		
		IntegrationTestTools.display("User Jack", jackType);
		
		AssertJUnit.assertEquals("Wrong number of link refs", 3, jackType.getLinkRef().size());
		
		assertUserJack(userJack);
		assertLinks(userJack, 3);
		String accountStatsOid = getLinkRefOid(userJack, RESOURCE_DUMMY_STATS_OID);
		String accountUnixOid = getLinkRefOid(userJack, RESOURCE_DUMMY_UNIX_OID);
		String accountOpenDjOid = getLinkRefOid(userJack, RESOURCE_OPENDJ_AD_SIMULATION_OID);
		
		PrismObject<ShadowType> shadowStats = provisioningService.getObject(ShadowType.class, accountStatsOid, null, task, result);
		IntegrationTestTools.display("Stats account: ", shadowStats);
		PrismObject<ShadowType> shadowUnix = provisioningService.getObject(ShadowType.class, accountUnixOid, null, task, result);
		IntegrationTestTools.display("Unix account: ", shadowUnix);
		PrismObject<ShadowType> shadowOpenDj = provisioningService.getObject(ShadowType.class, accountOpenDjOid, null, task, result);
		IntegrationTestTools.display("AD account: ", shadowOpenDj);
		
		
		//internalId on unix dummy resource and title on openDJ simulation must be the same
		PrismProperty unixId = shadowUnix.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, UNIX_INTERNAL_ID));
		PrismProperty openDjSyncedId = shadowOpenDj.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(NS_RESOURCE_INSTANCE, "title")));
		PrismAsserts.assertEquals("Unix id was not synced to the opendj properly.", String.valueOf(unixId.getAnyRealValue()), openDjSyncedId.getAnyRealValue());
		
//		PrismProperty<Integer> generatedValue = userJack.findExtensionItem(SCIENCE_EXTENSION_UID_QNAME);
// 		assertNotNull("Generated id value must not be null", generatedValue);
// 		assertFalse("Generated value must not be empty", generatedValue.isEmpty());
		
	}

	
	@Test
	public void test200delteUserJack() throws Exception {
		final String TEST_NAME = "test100jackAssignRoleStatistics";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestScience.class.getName() + "." + TEST_NAME);
		
		OperationResult result = task.getResult();
		
		deleteObject(UserType.class, USER_JACK_OID, task, result);
		
		result.computeStatus();
		
		IntegrationTestTools.display("Result: ", result);
		AssertJUnit.assertTrue("Unexpected failure", result.isSuccess());
		
		try {
			repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
			AssertJUnit.fail("Unexpected user jack. It must not exist.");
		} catch (ObjectNotFoundException ex){
			//this is OK
		}
		
				
	}

}
