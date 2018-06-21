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
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.DependencyProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
	private DependencyProcessor dependencyProcessor;

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

		initDummy("x", initTask, initResult); // depends on Y (circular)
		initDummy("y", initTask, initResult); // depends on Z (circular)
		initDummy("z", initTask, initResult); // depends on X (circular)
	}

	private void initDummy(String name, Task initTask, OperationResult initResult) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ConnectException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		String resourceOid = getDummyOid(name);
		DummyResourceContoller resourceCtl = DummyResourceContoller.create(name.toUpperCase());
		resourceCtl.extendSchemaPirate();
		// Expected warnings: dependencies
		PrismObject<ResourceType> resource = importAndGetObjectFromFileIgnoreWarnings(ResourceType.class,
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
		final String TEST_NAME = "test100SortToWavesIdependent";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        fillContextWithDummyElaineAccount(context, "a", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
	}

	@Test
    public void test101SortToWavesAB() throws Exception {
		final String TEST_NAME = "test101SortToWavesAB";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        fillContextWithDummyElaineAccount(context, "a", task, result);
        fillContextWithDummyElaineAccount(context, "b", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
        assertWave(context, getDummyOid("b"), 0, 1);
	}

	@Test
    public void test102SortToWavesABCD() throws Exception {
		final String TEST_NAME = "test102SortToWavesABCD";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        fillContextWithDummyElaineAccount(context, "a", task, result);
        fillContextWithDummyElaineAccount(context, "b", task, result);
        fillContextWithDummyElaineAccount(context, "c", task, result);
        fillContextWithDummyElaineAccount(context, "d", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "b", task, result);
        fillContextWithDummyElaineAccount(context, "c", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        try {
	        // WHEN
        	dependencyProcessor.sortProjectionsToWaves(context);

	        display("Context after", context);
	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// this is expected
        }
	}


	@Test
    public void test151SortToWavesPR() throws Exception {
		final String TEST_NAME = "test151SortToWavesPR";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "p", task, result);
        fillContextWithDummyElaineAccount(context, "r", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, getDummyOid("p"), 0, 0);
        assertWave(context, getDummyOid("r"), 0, 1);
        assertWave(context, getDummyOid("p"), 5, 2);
	}

	/**
	 * Different ordering of contexts as compared to previous tests. This results
	 * in different order of computation.
	 */
	@Test
    public void test152SortToWavesRP() throws Exception {
		final String TEST_NAME = "test152SortToWavesRP";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "r", task, result);
        fillContextWithDummyElaineAccount(context, "p", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, getDummyOid("p"), 0, 0);
        assertWave(context, getDummyOid("r"), 0, 1);
        assertWave(context, getDummyOid("p"), 5, 2);
	}

	@Test
    public void test200SortToWavesIdependentDeprovision() throws Exception {
		final String TEST_NAME = "test200SortToWavesIdependentDeprovision";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        setDelete(accountContext);
        setDelete(fillContextWithDummyElaineAccount(context, "a", task, result));

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 0);
	}

	@Test
    public void test201SortToWavesABDeprovision() throws Exception {
		final String TEST_NAME = "test201SortToWavesABDeprovision";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        setDelete(accountContext);
        setDelete(fillContextWithDummyElaineAccount(context, "a", task, result));
        setDelete(fillContextWithDummyElaineAccount(context, "b", task, result));

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 1);
        assertWave(context, getDummyOid("b"), 0, 0);
	}

	@Test
    public void test202SortToWavesABCDDeprovision() throws Exception {
		final String TEST_NAME = "test202SortToWavesABCDDeprovision";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);
        setDelete(accountContext);
        setDelete(fillContextWithDummyElaineAccount(context, "a", task, result));
        setDelete(fillContextWithDummyElaineAccount(context, "b", task, result));
        setDelete(fillContextWithDummyElaineAccount(context, "c", task, result));
        setDelete(fillContextWithDummyElaineAccount(context, "d", task, result));

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        // WHEN
        dependencyProcessor.sortProjectionsToWaves(context);

        // THEN
        display("Context after", context);

        assertWave(context, RESOURCE_DUMMY_OID, 0, 0);
        assertWave(context, getDummyOid("a"), 0, 2);
        assertWave(context, getDummyOid("b"), 0, 1);
        assertWave(context, getDummyOid("c"), 0, 0);
        assertWave(context, getDummyOid("d"), 0, 0);
	}

	private void setDelete(LensProjectionContext accountContext) {
		accountContext.setPrimaryDelta(ObjectDelta.createDeleteDelta(ShadowType.class, accountContext.getOid(), prismContext));
	}

	@Test
    public void test300SortToWavesXYZCircular() throws Exception {
		final String TEST_NAME = "test300SortToWavesXYZCircular";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestDependencies.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_ELAINE_OID, result);
        fillContextWithDummyElaineAccount(context, "x", task, result);
        fillContextWithDummyElaineAccount(context, "y", task, result);
        fillContextWithDummyElaineAccount(context, "z", task, result);

        context.recompute();
        display("Context before", context);
        context.checkConsistence();

        try {
	        // WHEN
        	dependencyProcessor.sortProjectionsToWaves(context);

	        AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
        	// This is expected
        	display("Expected exception", e);
        }

	}

	private LensProjectionContext fillContextWithDummyElaineAccount(
			LensContext<UserType> context, String dummyName, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, IOException, ExpressionEvaluationException {
		String resourceOid = getDummyOid(dummyName);
		String accountOid = getDummuAccountOid(dummyName,"e");
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_ELAINE_TEMPLATE_FILE);
		ShadowType accountType = account.asObjectable();
		accountType.setOid(accountOid);
		accountType.getResourceRef().setOid(resourceOid);
        provisioningService.applyDefinition(account, task, result);
        return fillContextWithAccount(context, account, task, result);
	}

	private void assertWave(LensContext<UserType> context,
			String resourceOid, int order, int expectedWave) {
		LensProjectionContext ctxAccDummy = findAccountContext(context, resourceOid, order);
		assertNotNull("No context for "+resourceOid+", order="+order, ctxAccDummy);
        assertWave(ctxAccDummy, expectedWave);
	}

	private void assertWave(LensProjectionContext projCtx, int expectedWave) {
		assertEquals("Wrong wave in "+projCtx, expectedWave, projCtx.getWave());
	}

	private LensProjectionContext findAccountContext(LensContext<UserType> context,
			String resourceOid, int order) {
		ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(resourceOid, ShadowKindType.ACCOUNT, null);
		discr.setOrder(order);
		return context.findProjectionContext(discr);
	}

}
