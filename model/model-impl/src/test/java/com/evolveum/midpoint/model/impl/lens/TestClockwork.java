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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestClockwork extends AbstractLensTest {

	@Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

	public TestClockwork() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		InternalMonitor.reset();
		InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);
	}

    // tests specific bug dealing with preservation of null values in focus secondary deltas
    @Test(enabled = true)
    public void test010SerializeAddUserBarbossa() throws Exception {
    	final String TEST_NAME = "test010SerializeAddUserBarbossa";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> bill = prismContext.parseObject(USER_BARBOSSA_FILE);
        fillContextWithAddUserDelta(context, bill);

        // WHEN

        clockwork.click(context, task, result);     // one round - compute projections

        System.out.println("Context before serialization = " + context.debugDump());

        LensContextType lensContextType = context.toLensContextType();
        String xml = prismContext.xmlSerializer().serializeRealValue(lensContextType, SchemaConstants.C_MODEL_CONTEXT);

        display("Serialized form", xml);

        LensContextType unmarshalledContainer = prismContext.parserFor(xml).xml().parseRealValue(LensContextType.class);
        LensContext context2 = LensContext.fromLensContextType(unmarshalledContainer, context.getPrismContext(), provisioningService, task, result);

        display("Context after deserialization", context);

        // THEN

        assertEquals("Secondary deltas are not preserved - their number differs", context.getFocusContext().getSecondaryDeltas().size(), context2.getFocusContext().getSecondaryDeltas().size());
        for (int i = 0; i < context.getFocusContext().getSecondaryDeltas().size(); i++) {
            assertEquals("Secondary delta #" + i + " is not preserved correctly", context.getFocusContext().getSecondaryDelta(i).debugDump(), context2.getFocusContext().getSecondaryDelta(i).debugDump());
        }
    }

	@Test
    public void test020AssignAccountToJackSync() throws Exception {
		final String TEST_NAME = "test020AssignAccountToJackSync";
        TestUtil.displayTestTitle(this, TEST_NAME);

        try {

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "." + TEST_NAME);
	        OperationResult result = task.getResult();

	        LensContext<UserType> context = createJackAssignAccountContext(result);

	        display("Input context", context);

	        assertFocusModificationSanity(context);
	        mockClockworkHook.reset();
	        mockClockworkHook.setRecord(true);
	        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

	        // WHEN
	        displayWhen(TEST_NAME);
	        clockwork.run(context, task, result);

	        // THEN
	        displayThen(TEST_NAME);
	        mockClockworkHook.setRecord(false);
	        display("Output context", context);
	        display("Hook contexts", mockClockworkHook);
	        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

	        assertJackAssignAccountContext(context);
	        assertJackAccountShadow(context);

	        List<LensContext<?>> hookContexts = mockClockworkHook.getContexts();
	        assertFalse("No contexts recorded by the hook", hookContexts.isEmpty());

		} finally {
			displayCleanup(TEST_NAME);
	    	mockClockworkHook.reset();
	    	unassignJackAccount();
	    	assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	    }
	}

	@Test
    public void test030AssignAccountToJackAsyncNoserialize() throws Exception {
		final String TEST_NAME = "test030AssignAccountToJackAsyncNoserialize";
        try {

        	assignAccountToJackAsync(TEST_NAME, false);

        } finally {
        	displayCleanup(TEST_NAME);
        	mockClockworkHook.reset();
        	unassignJackAccount();
        	assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        }
	}

	@Test
    public void test031AssignAccountToJackAsyncSerialize() throws Exception {
		final String TEST_NAME = "test031AssignAccountToJackAsyncSerialize";
        try {

        	assignAccountToJackAsync(TEST_NAME, true);

        } finally {
        	displayCleanup(TEST_NAME);
        	mockClockworkHook.reset();
        	unassignJackAccount();
        	assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        }
	}

	/**
	 * User barbossa has a direct account assignment. This assignment has an expression for enabledisable flag.
	 * Let's disable user, the account should be disabled as well.
	 */
	@Test
    public void test053ModifyUserBarbossaDisable() throws Exception {
		final String TEST_NAME = "test053ModifyUserBarbossaDisable";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context,
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        		ActivationStatusType.DISABLED);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        displayWhen(TEST_NAME);
        clockwork.run(context, task, result);

        // THEN
        displayThen(TEST_NAME);
        display("Output context", context);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.DISABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        // originally here was 6 (TODO why now four?)
        assertEquals("Unexpected number of account secondary changes", 4, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
        		new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        		ActivationStatusType.DISABLED);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
        		SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        ContainerDelta<TriggerType> triggerDelta = accountSecondaryDelta.findContainerDelta(ObjectType.F_TRIGGER);
        assertNotNull("No trigger delta in account secondary delta", triggerDelta);
        assertEquals("Wrong trigger delta size", 1, triggerDelta.getValuesToAdd().size());
        TriggerType triggerType = triggerDelta.getValuesToAdd().iterator().next().asContainerable();
        assertEquals("Wrong trigger URL", RecomputeTriggerHandler.HANDLER_URI, triggerType.getHandlerUri());
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
        TestUtil.assertBetween("Wrong trigger timestamp", start, end, triggerType.getTimestamp());

    }

	private void assignAccountToJackAsync(String testName, boolean serialize) throws Exception {
		displayTestTitle(testName);

		// GIVEN
        Task task = createTask(testName);
        OperationResult result = task.getResult();

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        LensContext<UserType> context = createJackAssignAccountContext(result);

//        display("Input context", context);

        assertFocusModificationSanity(context);
        mockClockworkHook.reset();
        mockClockworkHook.setRecord(true);
        mockClockworkHook.setAsynchronous(true);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        displayWhen(testName);
        while(context.getState() != ModelState.FINAL) {

        	display("CLICK START: "+context.getState());
        	HookOperationMode mode = clockwork.click(context, task, result);
        	display("CLICK END: "+context.getState());

        	assertTrue("Unexpected INITIAL state of the context", context.getState() != ModelState.INITIAL);
        	assertEquals("Wrong mode after click in "+context.getState(), HookOperationMode.BACKGROUND, mode);
        	assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        	if (serialize) {

        		display("Context before serialization", context);

                LensContextType lensContextType = context.toLensContextType();
                String xml = prismContext.xmlSerializer().serializeRealValue(lensContextType, SchemaConstants.C_MODEL_CONTEXT);

                display("Serialized form", xml);

                LensContextType unmarshalledContainer = prismContext.parserFor(xml).xml().parseRealValue(LensContextType.class);
                context = LensContext.fromLensContextType(unmarshalledContainer, context.getPrismContext(), provisioningService, task, result);

                display("Context after deserialization", context.debugDump());

                context.checkConsistence();
            }
        }

        // THEN
        displayThen(testName);
        mockClockworkHook.setRecord(false);
//        display("Output context", context);
//        display("Hook contexts", mockClockworkHook);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertJackAssignAccountContext(context);
        assertJackAccountShadow(context);
	}

	private void assertJackAssignAccountContext(LensContext<UserType> context) {
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        if (context.getFocusContext().getSecondaryDelta() != null) {
        	assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);
        }
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<?> executedDelta = getExecutedDelta(accContext);
        assertNotNull("No executed delta in "+accContext, executedDelta);
        assertEquals(ChangeType.ADD, executedDelta.getChangeType());
        PrismAsserts.assertPropertyAdd(executedDelta, getIcfsNameAttributePath(), "jack");
        PrismAsserts.assertPropertyAdd(executedDelta, getDummyResourceController().getAttributeFullnamePath(), "Jack Sparrow");

	}

	private ObjectDelta<?> getExecutedDelta(LensProjectionContext ctx) {
		List<LensObjectDeltaOperation<ShadowType>> executedDeltas = ctx.getExecutedDeltas();
		if (executedDeltas.isEmpty()) {
			return null;
		}
		if (executedDeltas.size() > 1) {
			AssertJUnit.fail("More than one executed delta in "+ctx);
		}
		return executedDeltas.get(0).getObjectDelta();
	}

	private void assertJackAccountShadow(LensContext<UserType> context) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        String accountOid = accContext.getOid();
        assertNotNull("No OID in account context "+accContext);

        PrismObject<ShadowType> newAccount = getShadowModel(accountOid);
        assertEquals(DEFAULT_INTENT, newAccount.findProperty(ShadowType.F_INTENT).getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceType()), "AccountObjectClass"),
                newAccount.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceType().getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(ShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceType()), "fullname")).getRealValue());
	}

	private LensContext<UserType> createJackAssignAccountContext(OperationResult result) throws SchemaException, ObjectNotFoundException, IOException, JAXBException {
		LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        return context;
	}

	private void unassignJackAccount() throws SchemaException, ObjectNotFoundException, IOException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
		Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + ".unassignJackAccount");
		LensContext<UserType> context = createUserLensContext();
		OperationResult result = task.getResult();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY);
        clockwork.run(context, task, result);
	}

}
