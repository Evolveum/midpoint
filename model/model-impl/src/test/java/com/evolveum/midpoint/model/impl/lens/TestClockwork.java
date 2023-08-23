/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestClockwork extends AbstractLensTest {

    @Autowired private Clockwork clockwork;
    @Autowired private TaskManager taskManager;

    private static final TestObject<ResourceType> RESOURCE_TEMPLATE = TestObject.file(
            TEST_DIR, "resource-template.xml", "50070cb6-46f0-439e-ab77-29b82ed80d93");

    private static final TestObject<ResourceType> RESOURCE_SPECIFIC_1 = TestObject.file(
            TEST_DIR, "resource-specific-1.xml", "94d2600e-37ac-4739-a037-246434c40535");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);
    }

    // tests specific bug dealing with preservation of null values in focus secondary deltas
    @Test
    public void test010SerializeAddUserBarbossa() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> bill = prismContext.parseObject(USER_BARBOSSA_FILE);
        fillContextWithAddUserDelta(context, bill);
        context.setState(ModelState.INITIAL);

        when();
        clockwork.click(context, task, result); // one round - compute projections

        displayDumpable("Context before serialization", context);

        LensContextType lensContextType = context.toBean();
        String xml = prismContext.xmlSerializer().serializeRealValue(lensContextType, SchemaConstants.C_MODEL_CONTEXT);

        displayValue("Serialized form", xml);

        LensContextType unmarshalledContainer = prismContext.parserFor(xml).xml().parseRealValue(LensContextType.class);
        LensContext<?> context2 = LensContext.fromLensContextBean(unmarshalledContainer, task, result);

        displayDumpable("Context after deserialization", context2);

        then();
        assertEquals("Secondary deltas are not preserved - their number differs",
                context.getFocusContext().getArchivedSecondaryDeltas().size(),
                context2.getFocusContext().getArchivedSecondaryDeltas().size());
        for (int i = 0; i < context.getFocusContext().getArchivedSecondaryDeltas().size(); i++) {
            assertEquals(
                    "Secondary delta #" + i + " is not preserved correctly, "
                            + "expected:\n" + context.getFocusContext().getArchivedSecondaryDeltas().get(i).debugDump()
                            + "but was\n" + context2.getFocusContext().getArchivedSecondaryDeltas().get(i).debugDump(),
                    context2.getFocusContext().getArchivedSecondaryDeltas().get(i),
                    context.getFocusContext().getArchivedSecondaryDeltas().get(i));
        }
    }

    @Test
    public void test020AssignAccountToJackSync() throws Exception {
        try {
            given();
            Task task = getTestTask();
            OperationResult result = task.getResult();

            LensContext<UserType> context = createJackAssignAccountContext(result);

            displayDumpable("Input context", context);

            assertFocusModificationSanity(context);
            mockClockworkHook.reset();
            mockClockworkHook.setRecord(true);
            rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

            // WHEN
            when();
            clockwork.run(context, task, result);

            // THEN
            then();
            mockClockworkHook.setRecord(false);
            displayDumpable("Output context", context);
            displayDumpable("Hook contexts", mockClockworkHook);
            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

            assertJackAssignAccountContext(context);
            assertJackAccountShadow(context);

            List<LensContext<?>> hookContexts = mockClockworkHook.getContexts();
            assertFalse("No contexts recorded by the hook", hookContexts.isEmpty());

        } finally {
            displayCleanup();
            mockClockworkHook.reset();
            unassignJackAccount();
            assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        }
    }

    @Test
    public void test030AssignAccountToJackAsyncNoSerialize() throws Exception {
        try {
            assignAccountToJackAsync(false);
        } finally {
            displayCleanup();
            mockClockworkHook.reset();
            unassignJackAccount();
            assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        }
    }

    @Test
    public void test031AssignAccountToJackAsyncSerialize() throws Exception {
        try {
            assignAccountToJackAsync(true);
        } finally {
            displayCleanup();
            mockClockworkHook.reset();
            unassignJackAccount();
            assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        }
    }

    /**
     * User barbossa has a direct account assignment.
     * This assignment has an expression for enable/disable flag.
     * Let's disable user, the account should be disabled as well.
     */
    @Test
    public void test053ModifyUserBarbossaDisable() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(
                context, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.DISABLED);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        clockwork.run(context, task, result);

        // THEN
        then();
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.DISABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Secondary delta is not null", accountSecondaryDelta);
    }

    private void assignAccountToJackAsync(boolean serialize) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);

        LensContext<UserType> context = createJackAssignAccountContext(result);

        assertFocusModificationSanity(context);
        mockClockworkHook.reset();
        mockClockworkHook.setRecord(true);
        mockClockworkHook.setAsynchronous(true);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        context.setStartedIfNotYet();
        while (context.getState() != ModelState.FINAL) {
            display("CLICK START: " + context.getState());
            HookOperationMode mode = clockwork.click(context, task, result);
            display("CLICK END: " + context.getState());

            assertNotSame("Unexpected INITIAL state of the context",
                    context.getState(), ModelState.INITIAL);
            assertEquals("Wrong mode after click in " + context.getState(),
                    HookOperationMode.BACKGROUND, mode);
            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

            if (serialize) {
                displayDumpable("Context before serialization", context);

                LensContextType lensContextType = context.toBean();
                String xml = prismContext.xmlSerializer().serializeRealValue(
                        lensContextType, SchemaConstants.C_MODEL_CONTEXT);

                displayValue("Serialized form", xml);

                LensContextType unmarshalledContainer =
                        prismContext.parserFor(xml).xml().parseRealValue(LensContextType.class);
                context = LensContext.fromLensContextBean(unmarshalledContainer, task, result);

                displayValue("Context after deserialization", context.debugDump());

                context.checkConsistence();
            }
        }

        then();
        mockClockworkHook.setRecord(false);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertJackAssignAccountContext(context);
        assertJackAccountShadow(context);
    }

    private void assertJackAssignAccountContext(LensContext<UserType> context) {
        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        if (context.getFocusContext().getSecondaryDelta() != null) {
            assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                    ActivationStatusType.ENABLED);
        }
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> projContexts = context.getProjectionContexts();
        assertThat(projContexts).as("projection contexts").hasSize(1);
        LensProjectionContext accContext = projContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        assertEquals(SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<?> executedDelta = getExecutedDelta(accContext);
        assertNotNull("No executed delta in " + accContext, executedDelta);
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
            AssertJUnit.fail("More than one executed delta in " + ctx);
        }
        return executedDeltas.get(0).getObjectDelta();
    }

    private void assertJackAccountShadow(LensContext<UserType> context)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        String accountOid = accContext.getOid();
        assertNotNull("No OID in account context " + accContext, accountOid);

        PrismObject<ShadowType> newAccount = getShadowModel(accountOid);
        assertEquals(DEFAULT_INTENT, newAccount.findProperty(ShadowType.F_INTENT).getRealValue());
        getDummyResourceType();
        assertEquals(RI_ACCOUNT_OBJECT_CLASS, newAccount.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceType().getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(ShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaConstants.ICFS_NAME).getRealValue());
        getDummyResourceType();
        assertEquals("Jack Sparrow", attributes.findProperty(new ItemName(MidPointConstants.NS_RI, "fullname")).getRealValue());
    }

    private LensContext<UserType> createJackAssignAccountContext(OperationResult result)
            throws SchemaException, ObjectNotFoundException, IOException {
        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        return context;
    }

    private void unassignJackAccount()
            throws SchemaException, ObjectNotFoundException, IOException, PolicyViolationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + ".unassignJackAccount");
        LensContext<UserType> context = createUserLensContext();
        OperationResult result = task.getResult();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY);
        clockwork.run(context, task, result);
    }

    @Test
    public void test100AddResourceFromTemplate() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("there is a resource template");
        importObject(RESOURCE_TEMPLATE, task, result);

        when("adding a specific resource");
        modelService.executeChanges(
                List.of(
                        DeltaFactory.Object.createAddDelta(
                                RESOURCE_SPECIFIC_1.get())),
                null,
                task,
                result);

        then("resource is added successfully");
        assertSuccess(result);
        assertResourceAfter(RESOURCE_SPECIFIC_1.oid);
    }
}
