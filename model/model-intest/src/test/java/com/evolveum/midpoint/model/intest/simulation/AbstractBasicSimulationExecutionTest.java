/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.schema.TaskExecutionMode;

import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.util.exception.CommonException;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.intest.TestPreviewChanges;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("there is a single ADD simulation delta (in testing storage)");
        assertTest100UserDeltas(simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there is a single ADD simulation delta (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest100UserDeltas(simulatedDeltas, "simulated deltas in persistent storage");
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        assertUserPrimaryAndSecondaryDeltas(modelContext);
    }

    private SimulationResultType getSimulationConfiguration() {
        if (isNativeRepository()) {
            return simulationResultManager.newConfiguration();
        } else {
            return null; // No simulation storage in old repo
        }
    }

    private void assertTest100UserDeltas(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .single()
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
                .assertModifiedPathsStrict( // This list may change if projector internals change
                        PATH_ACTIVATION_EFFECTIVE_STATUS,
                        PATH_ACTIVATION_ENABLE_TIMESTAMP,
                        FocusType.F_ITERATION,
                        FocusType.F_ITERATION_TOKEN);
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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("there are simulation deltas (in testing storage)");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest11xUserAndAccountDeltas(
                name, target, accountShouldExist,
                simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest11xUserAndAccountDeltas(
                    name, target, accountShouldExist,
                    simulatedDeltas, "simulated deltas in persistent storage");
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
                    .assertModifiedPathsStrict( // This list may change if projector internals change
                            ICFS_NAME_PATH, // by a mapping
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            ShadowType.F_LIFECYCLE_STATE); // "proposed" - hopefully this one will go away one day
        } else {
            assertThat(projectionContext.getSecondaryDelta()).as("projection secondary delta").isNull();
        }
        // @formatter:on
    }

    private void assertTest11xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            Collection<ObjectDelta<?>> simulatedDeltas, String message) {

        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
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
            assertAccountAddDelta(name, target, simulatedDeltas, message);
        } else {
            assertDeltaCollection(simulatedDeltas, message)
                    .assertSize(1); // user delta only
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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // simulated shadows?
        simResult.assertNoExecutedNorAuditedDeltas();

        and("there are simulation deltas (in testing storage)");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest12xUserAndAccountDeltas(
                name, target, accountShouldExist,
                simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest12xUserAndAccountDeltas(
                    name, target, accountShouldExist,
                    simulatedDeltas, "simulated deltas in persistent storage");
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
            Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
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
            assertAccountAddDelta(name, target, simulatedDeltas, message);
        } else {
            assertDeltaCollection(simulatedDeltas, message)
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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(createLinkRefDelta(userOid, target)),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("there are simulation deltas (in testing storage)");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest13xUserAndAccountDeltas(
                name, target, accountShouldExist,
                simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest13xUserAndAccountDeltas(
                    name, target, accountShouldExist,
                    simulatedDeltas, "simulated deltas in persistent storage");
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
    }

    private void assertTest13xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            Collection<ObjectDelta<?>> simulatedDeltas, String message) {

        // @formatter:off
        if (accountShouldExist) {
            assertDeltaCollection(simulatedDeltas, message)
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .assertModifiedPathsStrict(
                                PATH_METADATA_LAST_PROVISIONING_TIMESTAMP,
                                PATH_METADATA_MODIFY_CHANNEL,
                                PATH_METADATA_MODIFY_TIMESTAMP,
                                PATH_METADATA_MODIFIER_REF,
                                PATH_METADATA_MODIFY_TASK_REF,
                                PATH_METADATA_MODIFY_APPROVER_REF,
                                PATH_METADATA_MODIFY_APPROVAL_COMMENT,
                                UserType.F_LINK_REF)
                    .end();
            assertAccountAddDelta(name, target, simulatedDeltas, message);
        } else {
            assertDeltaCollection(simulatedDeltas, message)
                    .assertSize(0); // even no user delta should be there (really?)
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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(createAssignmentDelta(userOid, target)),
                        getExecutionMode(), simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedNorAuditedDeltas();

        and("there are simulation deltas (in testing storage)");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfiguration();
        assertTest14xUserAndAccountDeltas(
                name, target, accountShouldExist,
                simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest14xUserAndAccountDeltas(
                    name, target, accountShouldExist,
                    simulatedDeltas, "simulated deltas in persistent storage");
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
    }

    private void assertTest14xUserAndAccountDeltas(
            String name, DummyTestResource target, boolean accountShouldExist,
            Collection<ObjectDelta<?>> simulatedDeltas, String message) {

        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                    .assertModifiedPaths(
                            UserType.F_ASSIGNMENT,
                            PATH_METADATA_MODIFY_CHANNEL,
                            PATH_METADATA_MODIFY_TIMESTAMP,
                            PATH_METADATA_MODIFIER_REF,
                            PATH_METADATA_MODIFY_TASK_REF,
                            PATH_METADATA_MODIFY_APPROVER_REF,
                            PATH_METADATA_MODIFY_APPROVAL_COMMENT)
                .assertModifications(7 + (accountShouldExist ? 2 : 0));

        if (accountShouldExist) {
            assertDeltaCollection(simulatedDeltas, message)
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .assertModifiedPaths(
                                PATH_METADATA_LAST_PROVISIONING_TIMESTAMP,
                                UserType.F_LINK_REF)
                    .end();
            assertAccountAddDelta(name, target, simulatedDeltas, message);
        } else {
            assertDeltaCollection(simulatedDeltas, message)
                    .assertSize(1); // user delta only
        }
        // @formatter:on
    }

    private void assertAccountAddDelta(
            String name, DummyTestResource target, Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        assertDeltaCollection(simulatedDeltas, message)
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
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
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInSimulationMode(
                        getExecutionMode(),
                        simulationConfiguration,
                        task,
                        result,
                        (localSimResult) ->
                                importSingleAccountRequest()
                                        .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                                        .withNameValue("test200")
                                        .withTaskExecutionMode(getExecutionMode())
                                        .build()
                                        .executeOnForeground(result));

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("deltas are correct (in testing storage)");
        assertTest20xDeltas("test200", simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            Collection<ObjectDelta<?>> simulatedDeltas = simResult.getStoredDeltas(result);
            assertTest20xDeltas("test200", simulatedDeltas, "simulated deltas in persistent storage");
        }

        and("shadow should not have full sync info set");
        assertShadowAfter(
                findAccountByUsername("test200", RESOURCE_SIMPLE_PRODUCTION_SOURCE.getResource(), task, result))
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertSynchronizationSituation(null);
    }

    private void assertTest20xDeltas(String name, Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
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
                .by().changeType(ChangeType.MODIFY).objectType(ShadowType.class).index(0).find()
                    .assertModifiedPathsStrict( // fragile, may change when projector changes
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            PATH_METADATA_MODIFY_CHANNEL,
                            PATH_METADATA_MODIFY_TIMESTAMP,
                            PATH_METADATA_MODIFIER_REF,
                            PATH_METADATA_MODIFY_TASK_REF,
                            PATH_METADATA_MODIFY_APPROVER_REF,
                            PATH_METADATA_MODIFY_APPROVAL_COMMENT)
                .end();
        // @formatter:on
    }

    /**
     * As {@link #test200SimulatedAccountImportNoProjectionsForeground()} but on background.
     */
    @Test(enabled = false) // not working for now
    public void test205SimulatedAccountImportNoProjectionsBackground() throws Exception {
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        if (simulationConfiguration == null) {
            throw new SkipException("Simulations not supported here");
        }

        Task task = getTestTask();
        OperationResult result = task.getResult();

        SimulationResultContext simContext = simulationResultManager.newSimulationResult(simulationConfiguration, result);

        objectsCounter.remember(result);

        given("an account on production source");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.addAccount("test205");

        when("the account is imported");
        executeAccountImportOnBackground("test205", result);

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);

        and("there are simulation deltas in persistent storage");
        Collection<ObjectDelta<?>> simulatedDeltas = simContext.getStoredDeltas(result);
        assertTest20xDeltas("test205", simulatedDeltas, "simulated deltas in persistent storage");

        and("shadow should not have full sync info set");
        assertShadowAfter(
                findAccountByUsername("test205", RESOURCE_SIMPLE_PRODUCTION_SOURCE.getResource(), task, result))
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertSynchronizationSituation(null);
    }

    private void executeAccountImportOnBackground(String name, OperationResult result)
            throws CommonException {
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withNameValue(name)
                .withTaskExecutionMode(getExecutionMode())
                .build()
                .execute(result);
    }

    private boolean isDevelopmentConfiguration() {
        return !getExecutionMode().isProductionConfiguration();
    }
}
