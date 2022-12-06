/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Basic scenarios of simulations.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSimpleSimulations extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/simulation/simple");

    private static final DummyTestResource RESOURCE_DUMMY_SIMPLE = new DummyTestResource(
            TEST_DIR, "resource-dummy-simple.xml", "3f8d6dee-9663-496f-a718-b3c27234aca7", "simple");

    /** Currently we make sure that no {@link FocusType} objects are added, but no {@link ShadowType} ones as well. */
    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_SIMPLE.initAndTest(this, initTask, initResult);
    }

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
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedDeltas();

        and("there is a single ADD simulation delta");
        // @formatter:off
        assertDeltaCollection(simResult.getSimulatedDeltas(), "simulated deltas after")
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

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        assertUserPrimaryAndSecondaryDeltas(modelContext);
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
        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows
        simResult.assertNoExecutedDeltas();

        and("there are simulation deltas");
        // @formatter:off
        assertDeltaCollection(simResult.getSimulatedDeltas(), "simulated deltas after")
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
                        .assertResource(RESOURCE_DUMMY_SIMPLE.oid)
                        .assertObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent("default")
                        .attributes()
                            .assertValue(ICFS_NAME, "test110");
        // @formatter:on

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
                .resourceRef(RESOURCE_DUMMY_SIMPLE.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default");
        // Name should be computed by mappings
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
                                                .resourceRef(RESOURCE_DUMMY_SIMPLE.oid, ResourceType.COMPLEX_TYPE)));

        when("user is created in simulation");
        SimulationResult simResult =
                traced(() -> executeInSimulationMode(
                        List.of(user.asPrismObject().createAddDelta()),
                        task, result));

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // simulated shadows?
        simResult.assertNoExecutedDeltas();

        and("there are simulation deltas");
        // @formatter:off
        assertDeltaCollection(simResult.getSimulatedDeltas(), "simulated deltas after")
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
                                .assertResource(RESOURCE_DUMMY_SIMPLE.oid)
                            .end()
                        .end()
                    .end()
                .end()
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
                    .objectToAdd()
                        .assertNoName() // currently, there is no object name there
                        .asShadow()
                        .assertResource(RESOURCE_DUMMY_SIMPLE.oid)
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

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // Only basic assertions this time. We hope that everything is OK there.
        assertThat(modelContext.getFocusContext()).as("focus context").isNotNull();
        assertThat(modelContext.getProjectionContexts()).as("projection contexts").hasSize(1);
    }
}
