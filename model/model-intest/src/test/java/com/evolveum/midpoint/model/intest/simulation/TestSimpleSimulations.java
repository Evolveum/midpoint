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

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.schema.TaskExecutionMode;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.intest.TestPreviewChanges;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Basic scenarios of simulations: actions executed from the foreground, no create-on-demand.
 *
 * See {@link TestPreviewChangesCoD} for create-on-demand related tests.
 *
 * Structure:
 *
 * - `test1xx` deal with creation, modification, and deletion of user objects
 * - `test2xx` deal with importing single accounts from source resources
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSimpleSimulations extends AbstractSimulationsTest {

    /** Currently we make sure that no {@link FocusType} objects are added, but no {@link ShadowType} ones as well. */
    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

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
                executeInProductionSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedDeltas();

        and("there is a single ADD simulation delta (in testing storage)");
        assertTest100UserDeltas(simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there is a single ADD simulation delta (in persistent storage)");
            //Collection<ObjectDelta<?>> simulatedDeltas = retrieve simulated deltas from the persistent storage
            //assertTest100UserDeltas(simResult.getSimulatedDeltas(), "simulated deltas in persistent storage");
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        assertUserPrimaryAndSecondaryDeltas(modelContext);
    }

    private SimulationResultType getSimulationConfiguration() {
        if (!isNativeRepository()) {
            return null; // No simulation storage in old repo
        }
        // TODO create the configuration
        return simulationResultManager.newConfiguration();
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
                .assertModifiedPaths( // This list may change if projector internals change
                        PATH_ACTIVATION_EFFECTIVE_STATUS,
                        PATH_ACTIVATION_ENABLE_TIMESTAMP,
                        FocusType.F_ITERATION,
                        FocusType.F_ITERATION_TOKEN);
        // @formatter:on
    }

    /**
     * Creating a user with a linked account.
     *
     * This is similar to {@link TestPreviewChanges#test100ModifyUserAddAccountBundle()}, but using simulated execution
     * instead of "preview changes".
     */
    @Test
    public void test110CreateUserWithLinkedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test110")
                .linkRef(
                        ObjectTypeUtil.createObjectRefWithFullObject(
                                createAccount()));

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                executeInProductionSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        simulationConfiguration, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedDeltas();

        and("there are simulation deltas (in testing storage)");
        assertTest110UserAndAccountDeltas(simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            // TODO
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        // The deltas are the same as in test100. The linkRef delta is audited but (currently) it is not among secondary deltas.
        assertUserPrimaryAndSecondaryDeltas(modelContext);

        Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        assertThat(projectionContexts).as("projection contexts").hasSize(1);
        ModelProjectionContext projectionContext = projectionContexts.iterator().next();
        // @formatter:off
        assertDelta(projectionContext.getPrimaryDelta(), "account primary delta")
                .display()
                .assertAdd()
                .objectToAdd() // these items were explicitly "added"
                    .assertItems(ShadowType.F_RESOURCE_REF, ShadowType.F_OBJECT_CLASS, ShadowType.F_KIND, ShadowType.F_INTENT);
        assertDelta(projectionContext.getSummarySecondaryDelta(), "summary secondary delta")
                .display()
                .assertModify()
                .assertModifiedPaths( // This list may change if projector internals change
                        ICFS_NAME_PATH, // by a mapping
                        ShadowType.F_ITERATION,
                        ShadowType.F_ITERATION_TOKEN,
                        ShadowType.F_LIFECYCLE_STATE); // "proposed" - hopefully this one will go away one day
        // @formatter:on
    }

    private ShadowType createAccount() {
        return new ShadowType()
                .resourceRef(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default");
        // Name should be computed by mappings
    }

    private void assertTest110UserAndAccountDeltas(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .objectToAdd()
                        .assertName("test110")
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent()
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end()
                        .asFocus()
                        .activation()
                            .assertEffectiveStatus(ActivationStatusType.ENABLED)
                            .assertEnableTimestampPresent()
                        .end()
                    .end()
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                    .assertModifiedPaths(UserType.F_LINK_REF)
                .end()
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
                    .objectToAdd()
                        .assertNoName() // currently, there is no object name there
                        .asShadow()
                        .assertResource(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid)
                        .assertObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent("default")
                        .attributes()
                            .assertValue(ICFS_NAME, "test110");
        // @formatter:on
    }

    /**
     * Creating a user with an assigned account.
     *
     * This is similar to assignment tests in {@link TestPreviewChanges}, but using simulated execution
     * instead of "preview changes".
     */
    @Test
    public void test120CreateUserWithAssignedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test120")
                .assignment(
                        new AssignmentType()
                                .construction(
                                        new ConstructionType()
                                                .resourceRef(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid, ResourceType.COMPLEX_TYPE)));

        when("user is created in simulation");
        SimulationResultType simulationConfiguration = getSimulationConfiguration();
        SimulationResult simResult =
                traced(() -> executeInProductionSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        simulationConfiguration, task, result));

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // simulated shadows?
        simResult.assertNoExecutedDeltas();

        and("there are simulation deltas (in testing storage)");
        assertTest120UserAndAccountDeltas(simResult.getSimulatedDeltas(), "simulated deltas in testing storage");

        if (simulationConfiguration != null) {
            and("there are simulation deltas (in persistent storage)");
            // TODO
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // Only basic assertions this time. We hope that everything is OK there.
        assertThat(modelContext.getFocusContext()).as("focus context").isNotNull();
        assertThat(modelContext.getProjectionContexts()).as("projection contexts").hasSize(1);
    }

    private void assertTest120UserAndAccountDeltas(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .objectToAdd()
                        .assertName("test120")
                        .objectMetadata()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent()
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI)
                        .end()
                        .asFocus()
                        .activation()
                            .assertEffectiveStatus(ActivationStatusType.ENABLED)
                            .assertEnableTimestampPresent()
                        .end()
                        .assignments()
                            .single()
                                .assertResource(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid)
                            .end()
                        .end()
                    .end()
                .end()
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
                    .objectToAdd()
                        .assertNoName() // currently, there is no object name there
                        .asShadow()
                        .assertResource(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid)
                        .assertObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent("default")
                        .attributes()
                            .assertValue(ICFS_NAME, "test120")
                        .end()
                    .end()
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).index(0).find()
                    .assertModifiedPaths(UserType.F_LINK_REF)
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).index(1).find()
                    .assertModifiedPaths( // Why exactly these? This is very brittle and may change at any time.
                            ItemPath.create(UserType.F_ASSIGNMENT, 1L, PATH_ACTIVATION_EFFECTIVE_STATUS),
                            ItemPath.create(UserType.F_ASSIGNMENT, 1L, PATH_METADATA_MODIFY_CHANNEL),
                            ItemPath.create(UserType.F_ASSIGNMENT, 1L, PATH_METADATA_MODIFY_TIMESTAMP),
                            ItemPath.create(UserType.F_ASSIGNMENT, 1L, PATH_METADATA_MODIFIER_REF),
                            ItemPath.create(UserType.F_ASSIGNMENT, 1L, PATH_METADATA_MODIFY_TASK_REF),
                            ItemPath.create(PATH_METADATA_MODIFY_CHANNEL),
                            ItemPath.create(PATH_METADATA_MODIFY_TIMESTAMP),
                            ItemPath.create(PATH_METADATA_MODIFIER_REF),
                            ItemPath.create(PATH_METADATA_MODIFY_TASK_REF),
                            ItemPath.create(PATH_METADATA_MODIFY_APPROVER_REF),
                            ItemPath.create(PATH_METADATA_MODIFY_APPROVAL_COMMENT))
                .end();
        // @formatter:on
    }

    /**
     * Simulated import from production source.
     */
    @Test
    public void test200SimulatedProductionImport() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("an account on production source");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.addAccount("test200");

        when("the account is imported");
        SimulationResult simResult =
                executeInProductionSimulationMode(
                        null,
                        task,
                        () ->
                                importSingleAccountRequest()
                                        .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                                        .withNameValue("test200")
                                        .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                                        .build()
                                        .executeOnForeground(result));

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);
        simResult.assertNoExecutedDeltas();

        and("deltas are correct (in testing storage)");
        assertTest200Deltas(simResult.getSimulatedDeltas(), "simulated deltas in testing storage");
    }

    private void assertTest200Deltas(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, message)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .objectToAdd()
                        .assertName("test200")
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
                    .end()
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                    .assertModifiedPaths(UserType.F_LINK_REF)
                .end()
                .by().changeType(ChangeType.MODIFY).objectType(ShadowType.class).find()
                    .assertModifiedPaths( // fragile, may change when projector changes
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            PATH_METADATA_MODIFY_CHANNEL,
                            PATH_METADATA_MODIFY_TIMESTAMP,
                            PATH_METADATA_MODIFIER_REF,
                            PATH_METADATA_MODIFY_TASK_REF,
                            PATH_METADATA_MODIFY_APPROVER_REF,
                            PATH_METADATA_MODIFY_APPROVAL_COMMENT);
        // @formatter:on
    }
}
