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
package com.evolveum.midpoint.model.lens;

import static com.evolveum.midpoint.model.lens.LensTestConstants.REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDependencies extends AbstractInternalModelIntegrationTest {
		
	public static final File TEST_DIR = new File("src/test/resources/lens/dependencies");
	private static final File ACCOUNT_ELAINE_TEMPLATE_FILE = new File(TEST_DIR, "account-elaine-template.xml");
	
	@Autowired(required = true)
	private Projector projector;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		initDummy("a", initTask, initResult);
		initDummy("b", initTask, initResult); // depends on A
		initDummy("c", initTask, initResult); // depends on B
		initDummy("d", initTask, initResult); // depends on B
		
		initDummy("p", initTask, initResult); // depends on R (order 5)
		initDummy("r", initTask, initResult); // depends on P (order 0)
	}
	
	private void initDummy(String name, Task initTask, OperationResult initResult) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ConnectException {
		String resourceOid = getDummyOid(name);
		DummyResourceContoller resourceCtl = DummyResourceContoller.create(name.toUpperCase());
		resourceCtl.extendDummySchema();
		PrismObject<ResourceType> resource = importAndGetObjectFromFile(ResourceType.class, 
				getDummFile(name), resourceOid, initTask, initResult);
		resourceCtl.setResource(resource);
	}

	private File getDummFile(String name) {
		return new File(TEST_DIR, "resource-dummy-"+name+".xml");
	}

	private String getDummyOid(String name) {
		return "14440000-0000-0000-000"+name+"-000000000000";
	}
	
	private String getDummuAccountOid(String dummyName, String accountName) {
		return "14440000-0000-0000-000"+dummyName+"-10000000000"+accountName;
	}

	@Test
    public void test100SortToWavesIdependent() throws Exception {
		final String TEST_NAME = "test100SortToWaves";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext<ShadowType> accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, result);
        fillContextWithDummyElaineAccount(context, "a", result);
        
        context.recompute();
        display("Context before", context);        
        context.checkConsistence();
        
        // WHEN
        projector.sortAccountsToWaves(context);
        
        // THEN
        display("Context after", context);
        
        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
	}

	@Test
    public void test101SortToWavesAB() throws Exception {
		final String TEST_NAME = "test101SortToWavesAB";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext<ShadowType> accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, result);
        fillContextWithDummyElaineAccount(context, "a", result);
        fillContextWithDummyElaineAccount(context, "b", result);
        
        context.recompute();
        display("Context before", context);        
        context.checkConsistence();
        
        // WHEN
        projector.sortAccountsToWaves(context);
        
        // THEN
        display("Context after", context);
        
        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
        assertWave(context, getDummyOid("b"), 0, 1);
	}
	
	@Test
    public void test102SortToWavesABCD() throws Exception {
		final String TEST_NAME = "test102SortToWavesABCD";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext<ShadowType> accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, result);
        fillContextWithDummyElaineAccount(context, "a", result);
        fillContextWithDummyElaineAccount(context, "b", result);
        fillContextWithDummyElaineAccount(context, "c", result);
        fillContextWithDummyElaineAccount(context, "d", result);
        
        context.recompute();
        display("Context before", context);        
        context.checkConsistence();
        
        // WHEN
        projector.sortAccountsToWaves(context);
        
        // THEN
        display("Context after", context);
        
        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
        assertWave(context, getDummyOid("b"), 0, 1);
        assertWave(context, getDummyOid("c"), 0, 2);
        assertWave(context, getDummyOid("d"), 0, 2);
	}
	
	@Test
    public void test120SortToWavesBCUnsatisfied() throws Exception {
		final String TEST_NAME = "test120SortToWavesBCUnsatisfied";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "b", result);
        fillContextWithDummyElaineAccount(context, "c", result);
        
        context.recompute();
        display("Context before", context);        
        context.checkConsistence();
        
        try {
	        // WHEN
	        projector.sortAccountsToWaves(context);

	        display("Context after", context);
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        }        
	}

	
	@Test
    public void test201SortToWavesPR() throws Exception {
		final String TEST_NAME = "test201SortToWavesPR";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "p", result);
        fillContextWithDummyElaineAccount(context, "r", result);
        
        context.recompute();
        display("Context before", context);        
        context.checkConsistence();
        
        // WHEN
        projector.sortAccountsToWaves(context);
        
        // THEN
        display("Context after", context);
        
        assertWave(context, getDummyOid("p"), 0, 0);
        assertWave(context, getDummyOid("r"), 0, 1);
        assertWave(context, getDummyOid("p"), 5, 2);
	}

	
	private LensProjectionContext<ShadowType> fillContextWithDummyElaineAccount(
			LensContext<UserType, ShadowType> context, String dummyName, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		String resourceOid = getDummyOid(dummyName);
		String accountOid = getDummuAccountOid(dummyName,"e");
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_ELAINE_TEMPLATE_FILE);
		ShadowType accountType = account.asObjectable();
		accountType.setOid(accountOid);
		accountType.getResourceRef().setOid(resourceOid);
        provisioningService.applyDefinition(account, result);
        return fillContextWithAccount(context, account, result);
	}
	
	private void assertWave(LensContext<UserType, ShadowType> context,
			String resourceOid, int order, int expectedWave) {
		LensProjectionContext<ShadowType> ctxAccDummy = findAccountContext(context, resourceOid, order);
		assertNotNull("No context for "+resourceOid+", order="+order, ctxAccDummy);
        assertWave(ctxAccDummy, expectedWave);
	}

	private void assertWave(LensProjectionContext<ShadowType> projCtx, int expectedWave) {
		assertEquals("Wrong wave in "+projCtx, expectedWave, projCtx.getWave());
	}

	private LensProjectionContext<ShadowType> findAccountContext(LensContext<UserType, ShadowType> context,
			String resourceOid, int order) {
		ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(resourceOid, null);
		discr.setOrder(order);
		return context.findProjectionContext(discr);
	}
	
}
