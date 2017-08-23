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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.FailableRunnable;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test behavior of connectors that have several instances (poolable connectors).
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConnectorMultiInstance extends AbstractConfiguredModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/multi");
	private static Trace LOGGER = TraceManager.getTrace(TestConnectorMultiInstance.class);
	
	protected DummyResource dummyResourceYellow;
    protected DummyResourceContoller dummyResourceCtlYellow;
	
	private String accountJackYellowOid;
	private String initialConnectorStaticVal;
	private String initialConnectorToString;
	private String accountGuybrushBlackOid;
	

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlYellow = initDummyResourcePirate(RESOURCE_DUMMY_YELLOW_NAME, RESOURCE_DUMMY_YELLOW_FILE, RESOURCE_DUMMY_YELLOW_OID, initTask, initResult);
		dummyResourceYellow = dummyResourceCtlYellow.getDummyResource();
        
		initDummyResourcePirate(RESOURCE_DUMMY_BLACK_NAME, RESOURCE_DUMMY_BLACK_FILE, RESOURCE_DUMMY_BLACK_OID, initTask, initResult);
        
		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
		repoAddObjectFromFile(PASSWORD_POLICY_BENEVOLENT_FILE, initResult);
		
        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, true, initResult);
        repoAddObjectFromFile(USER_ELAINE_FILE, true, initResult);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TEST_NAME);
        		
		// WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_YELLOW_OID, task);
		
		// THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Yellow dummy test result", testResult);
        
        assertEquals("Wrong YELLOW useless string", IntegrationTestTools.CONST_USELESS, dummyResourceYellow.getUselessString());
	}

	@Test
    public void test100JackAssignDummyYellow() throws Exception {
		final String TEST_NAME = "test100JackAssignDummyYellow";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        		
		// WHEN
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
		
		// THEN
		assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        accountJackYellowOid = getSingleLinkOid(userJack);
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        
        PrismObject<ShadowType> shadowYellow = getShadowModel(accountJackYellowOid);
        display("Shadow yellow", shadowYellow);
        
        assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 1);
        
        initialConnectorToString = getConnectorToString(shadowYellow, dummyResourceCtlYellow);
        initialConnectorStaticVal = getConnectorStaticVal(shadowYellow, dummyResourceCtlYellow);
	}
	
	/**
	 * This is sequential operation. Same connector instance should be reused.
	 */
	@Test
    public void test102ReadJackDummyYellowAgain() throws Exception {
		final String TEST_NAME = "test102ReadJackDummyYellowAgain";
		displayTestTitle(TEST_NAME);
        		
		// WHEN
        PrismObject<ShadowType> shadowYellow = getShadowModel(accountJackYellowOid);
		
		// THEN
        display("Shadow yellow", shadowYellow);
        
        assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 1);
        
        assertConnectorToString(shadowYellow, dummyResourceCtlYellow, initialConnectorToString);
        assertConnectorStaticVal(shadowYellow, dummyResourceCtlYellow, initialConnectorStaticVal);
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
	}
	
	/**
	 * Block the operation during read. Just to make sure that the stats for active
	 * connector instances work.
	 */
	@Test
    public void test110ReadJackDummyYellowBlocking() throws Exception {
		final String TEST_NAME = "test110ReadJackDummyYellowBlocking";
		displayTestTitle(TEST_NAME);
		
		dummyResourceYellow.setBlockOperations(true);
		final Holder<PrismObject<ShadowType>> shadowHolder = new Holder<>(); 
		
		// WHEN
		Thread t = executeInNewThread("get1", () -> {
					PrismObject<ShadowType> shadow = getShadowModel(accountJackYellowOid);
					LOGGER.trace("Got shadow {}", shadow);
					shadowHolder.setValue(shadow);
			});
		
		// Give the new thread a chance to get blocked
		Thread.sleep(200);
		assertConnectorInstances("yellow (blocked)", RESOURCE_DUMMY_YELLOW_OID, 1, 0);
		
		assertNull("Unexpected shadow", shadowHolder.getValue());
		
		dummyResourceYellow.unblock();
		
		// THEN
		t.join();
		
		dummyResourceYellow.setBlockOperations(false);
		
        PrismObject<ShadowType> shadowYellow = shadowHolder.getValue();
        assertNotNull("No shadow", shadowHolder.getValue());
		
        display("Shadow yellow", shadowYellow);
        
        assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 1);
        
        assertConnectorToString(shadowYellow, dummyResourceCtlYellow, initialConnectorToString);
        assertConnectorStaticVal(shadowYellow, dummyResourceCtlYellow, initialConnectorStaticVal);
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
	}
	
	/**
	 * Block one read operation and let go the other. Make sure that new connector instance is created
	 * for the second operation and that it goes smoothly.
	 */
	@Test
   public void test120ReadJackDummyYellowTwoOperationsOneBlocking() throws Exception {
		final String TEST_NAME = "test120ReadJackDummyYellowTwoOperationsOneBlocking";
		displayTestTitle(TEST_NAME);
		
		dummyResourceYellow.setBlockOperations(true);
		final Holder<PrismObject<ShadowType>> shadowHolder1 = new Holder<>(); 
		final Holder<PrismObject<ShadowType>> shadowHolder2 = new Holder<>();
		
		// WHEN
		Thread t1 = executeInNewThread("get1", () -> {
					PrismObject<ShadowType> shadow = getShadowModel(accountJackYellowOid);
					LOGGER.trace("Got shadow {}", shadow);
					shadowHolder1.setValue(shadow);
			});
		
		// Give the new thread a chance to get blocked
		Thread.sleep(200);
		
		assertConnectorInstances("yellow (blocked)", RESOURCE_DUMMY_YELLOW_OID, 1, 0);
		assertNull("Unexpected shadow 1", shadowHolder1.getValue());

		
		dummyResourceYellow.setBlockOperations(false);
		
		// This should not be blocked and it should proceed immediately
		
		Thread t2 = executeInNewThread("get2", () -> {
					PrismObject<ShadowType> shadow = getShadowModel(accountJackYellowOid);
					LOGGER.trace("Got shadow {}", shadow);
					shadowHolder2.setValue(shadow);
			});

		t2.join(1000);

		assertConnectorInstances("yellow (blocked)", RESOURCE_DUMMY_YELLOW_OID, 1, 1);
		
		assertNull("Unexpected shadow 1", shadowHolder1.getValue());
		
		dummyResourceYellow.unblock();
		
		t1.join();
		
		// THEN
		
       PrismObject<ShadowType> shadowYellow1 = shadowHolder1.getValue();
       assertNotNull("No shadow 1", shadowHolder1.getValue());
       display("Shadow yellow 1", shadowYellow1);
       
       PrismObject<ShadowType> shadowYellow2 = shadowHolder2.getValue();
       assertNotNull("No shadow 2", shadowHolder2.getValue());
       display("Shadow yellow 2", shadowYellow2);
       
       assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 2);
       
       assertConnectorToString(shadowYellow1, dummyResourceCtlYellow, initialConnectorToString);
       assertConnectorStaticVal(shadowYellow1, dummyResourceCtlYellow, initialConnectorStaticVal);
       
       assertConnectorToStringDifferent(shadowYellow2, dummyResourceCtlYellow, initialConnectorToString);
       assertConnectorStaticVal(shadowYellow2, dummyResourceCtlYellow, initialConnectorStaticVal);
       
       assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
	}
	
	/**
	 * Block two read operations. Make sure that new connector instance is created.
	 */
	@Test
   public void test125ReadJackDummyYellowTwoBlocking() throws Exception {
		final String TEST_NAME = "test125ReadJackDummyYellowTwoBlocking";
		displayTestTitle(TEST_NAME);
		
		dummyResourceYellow.setBlockOperations(true);
		final Holder<PrismObject<ShadowType>> shadowHolder1 = new Holder<>(); 
		final Holder<PrismObject<ShadowType>> shadowHolder2 = new Holder<>();
		
		// WHEN
		Thread t1 = executeInNewThread("get1", () -> {
					PrismObject<ShadowType> shadow = getShadowModel(accountJackYellowOid);
					LOGGER.trace("Got shadow {}", shadow);
					shadowHolder1.setValue(shadow);
			});
				
		Thread t2 = executeInNewThread("get2", new FailableRunnable() {
			@Override
			public void run() throws Exception {
					PrismObject<ShadowType> shadow = getShadowModel(accountJackYellowOid);
					LOGGER.trace("Got shadow {}", shadow);
					shadowHolder2.setValue(shadow);
			}
		});
		
		// Give the new threads a chance to get blocked
		Thread.sleep(500);
		assertConnectorInstances("yellow (blocked)", RESOURCE_DUMMY_YELLOW_OID, 2, 0);
		
		assertNull("Unexpected shadow 1", shadowHolder1.getValue());
		assertNull("Unexpected shadow 2", shadowHolder2.getValue());
		
		dummyResourceYellow.unblockAll();
		
		t1.join();
		t2.join();
		
		// THEN
		dummyResourceYellow.setBlockOperations(false);
		
       PrismObject<ShadowType> shadowYellow1 = shadowHolder1.getValue();
       assertNotNull("No shadow 1", shadowHolder1.getValue());
       display("Shadow yellow 1", shadowYellow1);
       
       PrismObject<ShadowType> shadowYellow2 = shadowHolder2.getValue();
       assertNotNull("No shadow 2", shadowHolder2.getValue());
       display("Shadow yellow 2", shadowYellow2);
       
       assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 2);
       
       assertConnectorToStringDifferent(shadowYellow2, dummyResourceCtlYellow, getConnectorToString(shadowYellow1, dummyResourceCtlYellow));

       assertConnectorStaticVal(shadowYellow1, dummyResourceCtlYellow, initialConnectorStaticVal);
       assertConnectorStaticVal(shadowYellow2, dummyResourceCtlYellow, initialConnectorStaticVal);
       
       assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
	}
	
	@Test
    public void test200GuybrushAssignDummyBlack() throws Exception {
		final String TEST_NAME = "test200GuybrushAssignDummyBlack";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        		
		// WHEN
		assignAccount(USER_GUYBRUSH_OID, RESOURCE_DUMMY_BLACK_OID, null, task, result);
		
		// THEN
		assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_GUYBRUSH_OID);
        accountGuybrushBlackOid = getSingleLinkOid(userJack);
        
        assertDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        
        PrismObject<ShadowType> shadowBlack = getShadowModel(accountGuybrushBlackOid);
        display("Shadow black", shadowBlack);
        
        assertConnectorInstances("black", RESOURCE_DUMMY_BLACK_OID, 0, 1);
        assertConnectorInstances("yellow", RESOURCE_DUMMY_YELLOW_OID, 0, 2);
        
        assertConnectorToStringDifferent(shadowBlack, getDummyResourceController(RESOURCE_DUMMY_BLACK_NAME), initialConnectorStaticVal);
        assertConnectorToStringDifferent(shadowBlack, getDummyResourceController(RESOURCE_DUMMY_BLACK_NAME), initialConnectorToString);
        assertConnectorStaticVal(shadowBlack, getDummyResourceController(RESOURCE_DUMMY_BLACK_NAME), initialConnectorStaticVal);

	}

	private Thread executeInNewThread(final String threadName, final FailableRunnable runnable) {
		Thread t = new Thread(() -> {
				try {
					login(userAdministrator);
					runnable.run();
				} catch (Throwable e) {
					LOGGER.error("Error in {}: {}", threadName, e.getMessage(), e);
				}
			});
		t.setName(threadName);
		t.start();
		return t;
	}
	
	private String getConnectorToString(PrismObject<ShadowType> shadow, DummyResourceContoller ctl) throws SchemaException {
		return ShadowUtil.getAttributeValue(shadow, ctl.getAttributeQName(DummyResource.ATTRIBUTE_CONNECTOR_TO_STRING));
	}
	
	private String getConnectorStaticVal(PrismObject<ShadowType> shadow, DummyResourceContoller ctl) throws SchemaException {
		return ShadowUtil.getAttributeValue(shadow, ctl.getAttributeQName(DummyResource.ATTRIBUTE_CONNECTOR_STATIC_VAL));
	}
	
	private void assertConnectorToString(PrismObject<ShadowType> shadow,
			DummyResourceContoller ctl, String expectedVal) throws SchemaException {
		String connectorVal = ShadowUtil.getAttributeValue(shadow, ctl.getAttributeQName(DummyResource.ATTRIBUTE_CONNECTOR_TO_STRING));
        assertEquals("Connector toString mismatch", expectedVal, connectorVal);
	}
	
	private void assertConnectorToStringDifferent(PrismObject<ShadowType> shadow,
			DummyResourceContoller ctl, String expectedVal) throws SchemaException {
		String connectorVal = getConnectorToString(shadow, ctl);
        assertFalse("Unexpected Connector toString, expected a different value: "+connectorVal, expectedVal.equals(connectorVal));
	}
	
	private void assertConnectorStaticVal(PrismObject<ShadowType> shadow,
			DummyResourceContoller ctl, String expectedVal) throws SchemaException {
		String connectorStaticVal = getConnectorStaticVal(shadow, ctl);
        assertEquals("Connector static val mismatch", expectedVal, connectorStaticVal);
	}

	private void assertConnectorInstances(String msg, String resourceOid, int expectedActive, int expectedIdle) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(TestConnectorMultiInstance.class.getName() + ".assertConnectorInstances");
        OperationResult result = task.getResult();
		List<ConnectorOperationalStatus> opStats = modelInteractionService.getConnectorOperationalStatus(resourceOid, task, result);
        display("connector stats "+msg, opStats);
        assertConnectorInstances(msg, opStats.get(0), expectedActive, expectedIdle);
	}
	
	private void assertConnectorInstances(String msg, ConnectorOperationalStatus opStats, int expectedActive, int expectedIdle) {
		assertEquals(msg+" unexpected number of active connector instances", (Integer)expectedActive, opStats.getPoolStatusNumActive());
		assertEquals(msg+" unexpected number of idle connector instances", (Integer)expectedIdle, opStats.getPoolStatusNumIdle());
	}
	
}
