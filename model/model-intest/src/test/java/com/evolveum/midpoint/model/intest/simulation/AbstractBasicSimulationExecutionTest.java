/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.TAG_FOCUS_ASSIGNMENT_CHANGED;
import static com.evolveum.midpoint.model.test.CommonInitialObjects.TAG_FOCUS_ENABLED;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.intest.TestPreviewChanges;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Basic scenarios of production/development simulations: e.g., no create-on-demand.
 *
 * See {@link TestPreviewChangesCoD} for create-on-demand related tests.
 * See {@link TestRealExecution} for executing real operations against development-mode configuration objects.
 *
 * Structure:
 *
 * - `test1xx` deal with creation, modification, and deletion of user objects
 * - `test2xx` deal with importing single accounts from source resources
 * - `test3xx` deal with simulated archetypes, roles, assignments/inducements, mappings
 */
public abstract class AbstractBasicSimulationExecutionTest extends AbstractSimulationsTest {

    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

    abstract TaskExecutionMode getExecutionMode();

    /**
     * Creating a user without an account.
     */
    @Test
    public void test100CreateUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test100");

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");

        assertTest100SimulationResult(simResult, false);
        if (isNativeRepository()) {
            assertTest100SimulationResult(simResult, true);
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        assertUserPrimaryAndSecondaryDeltas(modelContext);
    }

    private void assertTest100SimulationResult(SimulationResult simResult, boolean persistent) throws SchemaException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .single()
                    .assertState(ObjectProcessingStateType.ADDED)
                    .assertEventTags(TAG_USER_ADD)
                    .assertType(UserType.class)
                    .delta()
                        .assertAdd()
                        .assertObjectTypeClass(UserType.class)
                        .objectToAdd()
                            .assertName("test100")
                            .objectMetadata()
                                .assertRequestTimestampPresent()
                                .assertCreateTimestampPresent()
                                .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                            .end()
                            .asFocus()
                                .activation()
                                    .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                    .assertEnableTimestampPresent();
        // @formatter:on
    }

    private void assertUserPrimaryAndSecondaryDeltas(ModelContext<?> modelContext) {
        ModelElementContext<?> focusContext = modelContext.getFocusContextRequired();
        // @formatter:off
        assertDelta(focusContext.getPrimaryDelta(), "primary delta")
                .display()
                .assertAdd()
                .objectToAdd()
                    .assertItems(UserType.F_NAME); // The primary delta is that simple
        assertDelta(focusContext.getSummarySecondaryDelta(), "summary secondary delta")
                .display()
                .assertModify()
                .assertModifiedExclusive( // This list may change if projector internals change
                        PATH_ACTIVATION_EFFECTIVE_STATUS,
                        PATH_ACTIVATION_ENABLE_TIMESTAMP,
                        FocusType.F_ITERATION,
                        FocusType.F_ITERATION_TOKEN,
                        FocusType.F_METADATA);
        // @formatter:on
    }

    /**
     * Creating a user with a linked account on production resource.
     *
     * This is similar to {@link TestPreviewChanges#test100ModifyUserAddAccountBundle()}, but using simulated execution
     * instead of "preview changes".
     */
    @Test
    public void test110CreateUserWithLinkedProductionAccount() throws Exception {
        executeTest11xCreateUserWithLinkedAccount("test110", RESOURCE_SIMPLE_PRODUCTION_TARGET, true);
    }

    /**
     * Creating a user with a linked account on development resource.
     */
    @Test
    public void test115CreateUserWithLinkedDevelopmentAccount() throws Exception {
        executeTest11xCreateUserWithLinkedAccount("test115", RESOURCE_SIMPLE_DEVELOPMENT_TARGET, false);
    }

    private void executeTest11xCreateUserWithLinkedAccount(String name, DummyTestResource target, boolean targetIsProduction)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name(name)
                .linkRef(createLinkRefWithFullObject(target));

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest11xSimulationResult(name, target, accountShouldExist, simResult, false);
        if (isNativeRepository()) {
            assertTest11xSimulationResult(name, target, accountShouldExist, simResult, true);
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        // The user deltas are the same as in test100.
        // The linkRef delta is audited but (currently) it is not among secondary deltas.
        assertUserPrimaryAndSecondaryDeltas(modelContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertThat(projectionContexts).as("projection contexts").hasSize(1);
        ModelProjectionContext projectionContext = projectionContexts.iterator().next();
        // @formatter:off
        // Currently, this delta is there also for development resource in production mode. It was requested but not executed.
        assertDelta(projectionContext.getPrimaryDelta(), "account primary delta")
                .display()
                .assertAdd()
                .objectToAdd() // these items were explicitly "added"
                    .assertItems(ShadowType.F_RESOURCE_REF, ShadowType.F_OBJECT_CLASS, ShadowType.F_KIND, ShadowType.F_INTENT);
        if (accountShouldExist) {
            assertDelta(projectionContext.getSummarySecondaryDelta(), "summary secondary delta")
                    .display()
                    .assertModify()
                    .assertModifiedExclusive( // This list may change if projector internals change
                            ICFS_NAME_PATH, // by a mapping
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            ShadowType.F_LIFECYCLE_STATE); // "proposed" - hopefully this one will go away one day
        } else {
            assertThat(projectionContext.getSecondaryDelta()).as("projection secondary delta").isNull();
        }
        // @formatter:on
    }

    private void assertTest11xSimulationResult(
            String name, DummyTestResource target, boolean accountShouldExist,
            SimulationResult simResult, boolean persistentOverride)
            throws SchemaException {
        Collection<ProcessedObject<?>> processedObjects = getProcessedObjects(simResult, persistentOverride);
        // @formatter:off
        assertProcessedObjects(processedObjects, getProcessedObjectsDesc(simResult, persistentOverride))
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventTags(TAG_USER_ADD)
                    .delta()
                    .objectToAdd()
                        .assertName(name)
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent(accountShouldExist)
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end()
                        .asFocus()
                            .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                .assertEnableTimestampPresent()
                            .end()
                            .assertLiveLinks(accountShouldExist ? 1 : 0)
                        .end()
                    .end();

        if (accountShouldExist) {
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "processed objects")
                    .assertSize(1); // user only
        }
        // @formatter:on
    }

    /**
     * Creating a user with an assigned production account.
     *
     * This is similar to assignment tests in {@link TestPreviewChanges}, but using simulated execution
     * instead of "preview changes".
     */
    @Test
    public void test120CreateUserWithAssignedProductionAccount() throws Exception {
        executeTest12xCreateUserWithAssignedAccount("test120", RESOURCE_SIMPLE_PRODUCTION_TARGET, true);
    }

    /**
     * Creating a user with an assigned development account.
     */
    @Test
    public void test125CreateUserWithAssignedDevelopmentAccount() throws Exception {
        executeTest12xCreateUserWithAssignedAccount("test125", RESOURCE_SIMPLE_DEVELOPMENT_TARGET, false);
    }

    private void executeTest12xCreateUserWithAssignedAccount(String name, DummyTestResource target, boolean targetIsProduction)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name(name)
                .assignment(
                        createAssignmentValue(target));

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // simulated shadows?
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest12xUserAndAccountDeltas(name, target, accountShouldExist, simResult, false);
        if (isNativeRepository()) {
            assertTest12xUserAndAccountDeltas(name, target, accountShouldExist, simResult, true);
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // Only basic assertions this time. We hope that everything is OK there.
        assertThat(modelContext.getFocusContext()).as("focus context").isNotNull();
        assertThat(modelContext.getProjectionContexts()).as("projection contexts").hasSize(1);
    }

    private void assertTest12xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            SimulationResult simResult, boolean persistentOverride)
            throws SchemaException {
        Collection<ProcessedObject<?>> processedObjects = getProcessedObjects(simResult, persistentOverride);
        // @formatter:off
        assertProcessedObjects(processedObjects, getProcessedObjectsDesc(simResult, persistentOverride))
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .delta()
                    .objectToAdd()
                        .assertName(name)
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent(accountShouldExist)
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end()
                        .asFocus()
                            .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                .assertEnableTimestampPresent()
                            .end()
                            .assignments()
                                .single()
                                    .assertResource(target.oid)
                                .end()
                            .end()
                            .assertLiveLinks(accountShouldExist ? 1 : 0)
                        .end()
                    .end();
        if (accountShouldExist) {
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "")
                    .assertSize(1); // user delta only
        }
        // @formatter:on
    }

    /**
     * Links an account on production resource.
     */
    @Test
    public void test130LinkProductionAccount() throws Exception {
        executeTest13xLinkAccount("test130", RESOURCE_SIMPLE_PRODUCTION_TARGET, true);
    }

    /**
     * Links an account on development resource.
     */
    @Test
    public void test135LinkDevelopmentAccount() throws Exception {
        executeTest13xLinkAccount("test135", RESOURCE_SIMPLE_DEVELOPMENT_TARGET, false);
    }

    private void executeTest13xLinkAccount(String name, DummyTestResource target, boolean targetIsProduction)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user in repository");
        String userOid = addUser(name, task, result);

        objectsCounter.remember(result);

        when("account is linked in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(createLinkRefDelta(userOid, target)),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest13xUserAndAccountDeltas(name, target, accountShouldExist, simResult, false);
        if (isNativeRepository()) {
            assertTest13xUserAndAccountDeltas(name, target, accountShouldExist, simResult, true);
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
    }

    private void assertTest13xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            SimulationResult simResult, boolean persistentOverride)
            throws SchemaException {

        // @formatter:off
        if (accountShouldExist) {
            Collection<ProcessedObject<?>> processedObjects = getProcessedObjects(simResult, persistentOverride);
            assertProcessedObjects(processedObjects, getProcessedObjectsDesc(simResult, persistentOverride))
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .assertEventTags()
                        .delta()
                        .assertModifiedExclusive(
                                UserType.F_LINK_REF,
                                UserType.F_METADATA)
                    .end();
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(simResult, persistentOverride)
                    .display()
                    .by().objectType(UserType.class).find()
                        .assertState(ObjectProcessingStateType.UNMODIFIED)
                    .end()
                    .assertSize(1);
        }
        // @formatter:on
    }

    /**
     * Assigns an account on production resource.
     */
    @Test
    public void test140AssignProductionAccount() throws Exception {
        executeTest14xAssignAccount("test140", RESOURCE_SIMPLE_PRODUCTION_TARGET, true);
    }

    /**
     * Assigns an account on development resource.
     */
    @Test
    public void test145AssignDevelopmentAccount() throws Exception {
        executeTest14xAssignAccount("test145", RESOURCE_SIMPLE_DEVELOPMENT_TARGET, false);
    }

    private void executeTest14xAssignAccount(String name, DummyTestResource target, boolean targetIsProduction)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user in repository");
        String userOid = addUser(name, task, result);

        objectsCounter.remember(result);

        when("account is linked in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(createAssignmentDelta(userOid, target)),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest14xUserAndAccountDeltas(name, target, accountShouldExist, simResult, false);
        if (isNativeRepository()) {
            assertTest14xUserAndAccountDeltas(name, target, accountShouldExist, simResult, true);
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
    }

    private void assertTest14xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            SimulationResult simResult, boolean persistentOverride)
            throws SchemaException {

        Collection<ProcessedObject<?>> processedObjects = getProcessedObjects(simResult, persistentOverride);
        // @formatter:off
        assertProcessedObjects(processedObjects, "objects")
                .display()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                    .assertEventTags(TAG_FOCUS_ASSIGNMENT_CHANGED)
                    .delta()
                    .assertModified(
                            UserType.F_ASSIGNMENT,
                            UserType.F_METADATA)
                .assertModifications(7 + (accountShouldExist ? 2 : 0));

        if (accountShouldExist) {
            assertProcessedObjects(processedObjects, "objects")
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .delta()
                        .assertModified(
                                PATH_METADATA_LAST_PROVISIONING_TIMESTAMP,
                                UserType.F_LINK_REF)
                    .end();
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "objects")
                    .assertSize(1); // user delta only
        }
        // @formatter:on
    }

    private void assertAccountAdded(String name, DummyTestResource target, Collection<ProcessedObject<?>> processedObjects) {
        assertProcessedObjects(processedObjects, "objects")
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
                    .assertEventTags()
                    .delta()
                    .objectToAdd()
                        .assertNoName() // currently, there is no object name there
                        .asShadow()
                        .assertResource(target.oid)
                        .assertObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent("default")
                        .attributes()
                            .assertValue(ICFS_NAME, name)
                        .end()
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end();
    }

    /** Enabling a user. */
    @Test
    public void test150EnableUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user (in repository)");
        UserType user = new UserType()
                .name("test150")
                .activation(new ActivationType()
                        .administrativeStatus(ActivationStatusType.DISABLED));
        addObject(user, task, result);

        objectsCounter.remember(result);

        when("user is enabled in simulation");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH)
                .replace(ActivationStatusType.ENABLED)
                .asObjectDelta(user.getOid());

        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(delta),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest150UserDeltas(simResult, false);
        if (isNativeRepository()) {
            assertTest150UserDeltas(simResult, true);
        }
    }

    private void assertTest150UserDeltas(SimulationResult simResult, boolean persistent) throws SchemaException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_FOCUS_ENABLED)
                .end()
                .assertSize(1);
        // @formatter:on
    }

    /**
     * Simulated import from production source. Shadow should be created, but the user should not.
     * There should be no persistent information about shadow being linked to the user.
     *
     * User should have no projections.
     */
    @Test
    public void test200SimulatedAccountImportNoProjectionsForeground() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("an account on production source");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.addAccount("test200");

        when("the account is imported");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult = importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withNameValue("test200")
                .withTaskExecutionMode(getExecutionMode())
                .executeOnForegroundSimulated(simulationConfiguration, task, result);

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest20xSimulationResult("test200", simResult, false);
        if (isNativeRepository()) {
            assertTest20xSimulationResult("test200", simResult, true);
        }

        and("shadow should not have full sync info set");
        assertTest20xShadow("test200", task, result);
    }

    private void assertTest20xSimulationResult(String name, SimulationResult simResult, boolean persistent)
            throws SchemaException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventTags(TAG_USER_ADD)
                    .delta()
                        .objectToAdd()
                            .assertName(name)
                            .objectMetadata()
                                .assertRequestTimestampPresent()
                                .assertCreateTimestampPresent()
                                .assertLastProvisioningTimestampPresent()
                                .assertCreateChannel(CHANNEL_IMPORT_URI)
                            .end()
                            .asFocus()
                                .activation()
                                    .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                    .assertEnableTimestampPresent()
                                .end()
                                .assertLiveLinks(1)
                            .end()
                        .end()
                    .end()
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(ShadowType.class).index(0).find()
                    .assertEventTags()
                    .delta()
                    .assertModifiedExclusive(
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            ShadowType.F_METADATA)
                .end();
        // @formatter:on
    }

    /**
     * As {@link #test200SimulatedAccountImportNoProjectionsForeground()} but on background.
     */
    @Test
    public void test205SimulatedAccountImportNoProjectionsBackground() throws Exception {
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        if (simulationConfiguration == null) {
            throw new SkipException("Simulations not supported here");
        }

        Task task = getTestTask();
        OperationResult result = task.getResult();
        objectsCounter.remember(result);

        given("an account on production source");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.addAccount("test205");

        when("the account is imported (on background)");
        String taskOid = executeAccountImportOnBackground("test205", result);

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);

        and("processed objects are OK");
        assertTest20xSimulationResult("test205", getTaskSimResult(taskOid, result), true);

        and("shadow should not have full sync info set");
        assertTest20xShadow("test205", task, result);
    }

    private void assertTest20xShadow(String name, Task task, OperationResult result) throws CommonException {
        assertShadowAfter(
                findAccountByUsername(name, RESOURCE_SIMPLE_PRODUCTION_SOURCE.get(), task, result))
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertSynchronizationSituation(null);
    }

    @SuppressWarnings("SameParameterValue")
    private String executeAccountImportOnBackground(String name, OperationResult result)
            throws CommonException {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withNameValue(name)
                .withTaskExecutionMode(getExecutionMode())
                .execute(result);
    }

    /**
     * Creates a user of archetype `person`, with a rich (conditional) configuration:
     *
     * - development-mode assignment of a metarole (with induced focus mapping)
     * - development-mode inducement of a focus mapping
     * - development-mode inducement of a regular role
     * - regular inducement of a development-mode role
     * - template with:
     * ** included development-mode sub-template
     * ** development-mode mapping
     * ** regular mapping
     *
     * So the result depends on the actual evaluation mode.
     */
    @Test
    public void test300CreatePerson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test300")
                .assignment(
                        new AssignmentType()
                                .targetRef(ARCHETYPE_PERSON.ref()));

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getDefaultSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest300UserDeltas(simResult, false);
        if (isNativeRepository()) {
            assertTest300UserDeltas(simResult, true);
        }
    }

    private void assertTest300UserDeltas(SimulationResult simResult, boolean persistent) throws SchemaException {
        // @formatter:off
        FocusType user = assertProcessedObjects(simResult, persistent)
                .display()
                .single()
                .assertEventTags(TAG_USER_ADD)
                    .delta()
                    .assertAdd()
                    .assertObjectTypeClass(UserType.class)
                    .objectToAdd()
                        .assertName("test300")
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end()
                        .asFocus()
                            .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                .assertEnableTimestampPresent()
                            .end()
                            .getObjectable();
        // @formatter:on

        Set<String> orgs = ((UserType) user).getOrganization().stream()
                .map(PolyStringType::getOrig)
                .collect(Collectors.toSet());
        if (isDevelopmentConfiguration()) {
            assertThat(orgs).as("user orgs").containsExactlyInAnyOrder(
                    "template:person (proposed)",
                    "template:person (active)",
                    "template:person-included-dev",
                    "archetype:person",
                    "metarole",
                    "role:person",
                    "role:person-dev");
        } else {
            assertThat(orgs).as("user orgs").containsExactlyInAnyOrder("template:person (active)");
        }
    }

    private boolean isDevelopmentConfiguration() {
        return !getExecutionMode().isProductionConfiguration();
    }
}
