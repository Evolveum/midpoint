package com.evolveum.midpoint.testing.longtest;
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


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRunAs extends AbstractLongTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "runas");

    private static final int NUM_INITIAL_USERS = 4;

	private static final String RESOURCE_DUMMY_NAME = null;
	private static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	private static final String RESOURCE_DUMMY_OID = "2f454e92-c9e8-11e7-8f60-17bc95e695f8";
	
	protected static final File USER_ROBOT_FILE = new File (TEST_DIR, "user-robot.xml");
	protected static final String USER_ROBOT_OID = "20b4d7c0-c9e9-11e7-887c-7fe1dc65a3ed";
	protected static final String USER_ROBOT_USERNAME = "robot";
	
	protected static final File USER_TEMPLATE_PLAIN_FILE = new File(TEST_DIR, "user-template-plain.xml");
	protected static final String USER_TEMPLATE_PLAIN_OID = "d7b2f8fc-c9ea-11e7-98bd-eb714a446e68";
	
	protected static final File USER_TEMPLATE_RUNAS_FILE = new File(TEST_DIR, "user-template-runas.xml");
	protected static final String USER_TEMPLATE_RUNAS_OID = "8582e1e2-c9ee-11e7-8fa9-63e7c62604c6";
	
	protected static final String ORG_PIRATES = "Pirates";

	private static final int NUM_ORG_MAPPINGS = 10;
	private static final int WARM_UP_ROUNDS = 30;
	
	private long baselineRunTime;
	private long baselineRepoReadCountIncrement;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);

		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
		repoAddObjectFromFile(USER_ROBOT_FILE, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_NAME,
				RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);

		repoAddObjectFromFile(USER_TEMPLATE_PLAIN_FILE, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_RUNAS_FILE, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        //initProfiling - start
        ProfilingDataManager profilingManager = ProfilingDataManager.getInstance();

        Map<ProfilingDataManager.Subsystem, Boolean> subsystems = new HashMap<>();
        subsystems.put(ProfilingDataManager.Subsystem.MODEL, true);
        subsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        profilingManager.configureProfilingDataManagerForTest(subsystems, true);

        profilingManager.appendProfilingToTest();
        //initProfiling - end
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTitle(this, TEST_NAME);

        assertUsers(NUM_INITIAL_USERS);
	}

	/**
	 * MID-3816
	 */
	@Test
    public void test100AssignAccountDummyToBarbossa() throws Exception {
		final String TEST_NAME = "test100AssignAccountDummyToBarbossa";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_BARBOSSA_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME,
        		USER_BARBOSSA_FULL_NAME, true);
        display("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
        		"Some say robot -- administrator");
	}
	
	/**
	 * MID-3816
	 */
	@Test
    public void test109UnassignAccountDummyFromBarbossa() throws Exception {
		final String TEST_NAME = "test109UnassignAccountDummyFromBarbossa";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_BARBOSSA_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_NAME, USER_BARBOSSA_USERNAME);
	}
	
	@Test
    public void test200CleanupPlain() throws Exception {
		final String TEST_NAME = "test200CleanupPlain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);
		
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);

		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_PLAIN_OID, result);
		assertSuccess(result);
	}

	
	/**
	 * Warm up JVM, so we have stable and comparable results
	 */
	@Test
    public void test205WarmUp() throws Exception {
		warmUp("test205WarmUp");
	}
	
	/**
	 * Set the baseline - no runAs
	 */
	@Test
    public void test210BarbossaSetOrganizationPlain() throws Exception {
		final String TEST_NAME = "test210BarbossaSetOrganizationPlain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        // WHEN
        displayWhen(TEST_NAME);
        long starMillis = System.currentTimeMillis();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
        long endMillis = System.currentTimeMillis();
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        long readCountIncremenet = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        display("Run time "+(endMillis - starMillis)+"ms, repo read count increment " + readCountIncremenet);
        baselineRunTime = endMillis - starMillis;
        baselineRepoReadCountIncrement = readCountIncremenet;

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);
		
		assertUserOrgs(userAfter, ORG_PIRATES, USER_ADMINISTRATOR_USERNAME);

	}
	
	@Test
    public void test300CleanupRunAs() throws Exception {
		final String TEST_NAME = "test300CleanupRunAs";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);
		
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);

		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_RUNAS_OID, result);
		assertSuccess(result);
	}
	
	/**
	 * Warm up JVM, so we have stable and comparable results
	 */
	@Test
    public void test305WarmUp() throws Exception {
		warmUp("test305WarmUp");
	}

	
	/**
	 * MID-3844
	 */
	@Test
    public void test310BarbossaSetOrganizationRunAs() throws Exception {
		final String TEST_NAME = "test310BarbossaSetOrganizationRunAs";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        // WHEN
        displayWhen(TEST_NAME);
        long starMillis = System.currentTimeMillis();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
        long endMillis = System.currentTimeMillis();
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        long readCountIncremenet = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        long runTimeMillis = (endMillis - starMillis);
        display("Run time "+runTimeMillis+"ms, repo read count increment " + readCountIncremenet);
        long percentRuntimeIncrease = (runTimeMillis - baselineRunTime)*100/baselineRunTime;
        display("Increase over baseline", 
        		"  run time: "+(runTimeMillis - baselineRunTime) + " (" + percentRuntimeIncrease + "%) \n" +
        		"  repo read: "+(readCountIncremenet - baselineRepoReadCountIncrement));
        
        assertEquals("High increase over repo read count baseline", 2, readCountIncremenet - baselineRepoReadCountIncrement);
        if (percentRuntimeIncrease > 20) {
        	fail("Too high run time increase over baseline: "+percentRuntimeIncrease+"% "+baselineRunTime+"ms -> "+runTimeMillis+"ms");
        }
        

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);
		
		assertUserOrgs(userAfter, ORG_PIRATES, USER_ROBOT_USERNAME);

	}

    private void warmUp(final String TEST_NAME) throws Exception {
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);

        // WHEN
        displayWhen(TEST_NAME);
        long firstTime = warmUpRound(0, task, result);
        long lastTime = 0;
        long sumTime = firstTime;
        for (int i = 1; i < WARM_UP_ROUNDS; i++) {
        	lastTime = warmUpRound(i, task, result);
        	sumTime += lastTime;
        }
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        long readCountIncremenet = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
        display("Warm up run times: first "+(firstTime)+"ms, last " + lastTime + ", average "+(sumTime/WARM_UP_ROUNDS)+"ms");

        PrismObject<UserType> userAfter = getUser(USER_BARBOSSA_OID);
		display("User after", userAfter);
		
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);
		PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATIONAL_UNIT);

	}

	private long warmUpRound(int round, Task task, OperationResult result) throws Exception {
	    rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);
	
	    // WHEN
	    long starMillis = System.currentTimeMillis();
	    modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, createPolyString(ORG_PIRATES));
	    long endMillis = System.currentTimeMillis();
	    
	    // THEN
	    assertSuccess(result);
	
	    long readCountIncremenet = getCounterIncrement(InternalCounters.REPOSITORY_READ_COUNT);
	    display("Warm up round "+round+" run time "+(endMillis - starMillis)+"ms, repo read count increment " + readCountIncremenet);
	    
	    modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result /* no value */);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);
	    
	    return  endMillis - starMillis;
	}


	private void assertUserOrgs(PrismObject<UserType> user, String organization, String principalUsername) {
		PrismAsserts.assertPropertyValue(user, UserType.F_ORGANIZATION, createPolyString(organization));
		PrismAsserts.assertPropertyValue(user, UserType.F_ORGANIZATIONAL_UNIT, expectedOrgUnits(organization, principalUsername));		
	}

	private PolyString[] expectedOrgUnits(String organization, String principalUsername) {
		PolyString[] out = new PolyString[NUM_ORG_MAPPINGS];
		for (int i = 0; i < NUM_ORG_MAPPINGS; i++) {
			out[i] = createPolyString(String.format("%03d%s: %s", i + 1, organization, principalUsername));
		}
		return out;
	}
	

}
