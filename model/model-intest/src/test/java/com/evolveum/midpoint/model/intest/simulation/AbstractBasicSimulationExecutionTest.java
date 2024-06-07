/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.model.test.asserter.ProcessedObjectAsserter;
import com.evolveum.midpoint.model.test.asserter.ProcessedObjectsAsserter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.intest.TestPreviewChanges;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
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
 *
 * *On native repo only!*
 */
public abstract class AbstractBasicSimulationExecutionTest extends AbstractSimulationsTest {

    final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

    TaskExecutionMode getExecutionMode() {
        return getExecutionMode(false);
    }

    abstract TaskExecutionMode getExecutionMode(boolean shadowSimulation);

    /** Checks whether we can obtain a definition for given metric. */
    @Test
    public void test010GetMetricDefinition() {
        when("obtaining a definition for a known metric");
        var def = simulationResultManager.getMetricDefinition(METRIC_ATTRIBUTE_MODIFICATIONS_ID);

        then("it is OK");
        displayDumpable(METRIC_ATTRIBUTE_MODIFICATIONS_ID, def);
        assertThat(def).as("definition").isNotNull();
        assertThat(def.getIdentifier()).as("identifier").isEqualTo(METRIC_ATTRIBUTE_MODIFICATIONS_ID);
        assertThat(def.getComputation()).as("computation item").isNotNull();

        when("obtaining a definition for unknown metric");
        var def2 = simulationResultManager.getMetricDefinition("nonsense");

        then("it's null");
        assertThat(def2).isNull();
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
                .name("test100")
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_CUSTOMER.oid, ArchetypeType.COMPLEX_TYPE));

        when("user is created in simulation");
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);

        and("simulation result is OK");
        SimulationResultType resultBean = assertSimulationResultAfter(simResult)
                .assertMetricValueByEventMark(MARK_USER_ADD.oid, BigDecimal.ONE)
                .assertObjectsAdded(1)
                .assertObjectsModified(0)
                .assertObjectsDeleted(0)
                .assertObjectsUnchanged(0)
                .assertObjectsProcessed(1)
                .getObjectable();

        // TODO write an asserter for this
        SimulationMetricValuesType mv =
                SimulationResultTypeUtil.getMetricValuesBeanByMarkOid(resultBean, MARK_USER_ADD.oid);
        displayDumpable("metric value", mv);
        List<SimulationMetricPartitionType> partitions = Objects.requireNonNull(mv).getPartition();
        assertThat(partitions).as("metric partitions").hasSize(1);
        SimulationMetricPartitionScopeType scope = partitions.get(0).getScope();
        assertThat(scope.getTypeName()).as("type name").isEqualTo(UserType.COMPLEX_TYPE);
        assertThat(scope.getStructuralArchetypeOid()).as("archetype OID").isEqualTo(ARCHETYPE_CUSTOMER.oid);

        // @formatter:off
        assertProcessedObjects(simResult)
                .display()
                .single()
                    .assertState(ObjectProcessingStateType.ADDED)
                    .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                    .assertType(UserType.class)
                    .delta()
                        .assertAdd()
                        .assertObjectTypeClass(UserType.class)
                        .objectToAdd()
                            .assertName("test100")
                            .valueMetadataSingle()
                                .assertRequestTimestampPresent()
                                .assertCreateTimestampPresent()
                                .assertCreateChannel(CHANNEL_USER_URI)
                            .end().end()
                            .asFocus()
                                .activation()
                                    .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                    .assertEnableTimestampPresent();
        // @formatter:on

        // This is a little bit out of scope for this test, but let's keep it here for now (same for all tests in this class)
        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        assertUserPrimaryAndSecondaryDeltasWithArchetype(modelContext);
    }

    private void assertUserPrimaryAndSecondaryDeltasWithArchetype(ModelContext<?> modelContext) {
        ModelElementContext<?> focusContext = modelContext.getFocusContextRequired();
        // @formatter:off
        assertDelta(focusContext.getPrimaryDelta(), "primary delta")
                .display()
                .assertAdd()
                .objectToAdd()
                    .assertItems(UserType.F_NAME, UserType.F_ASSIGNMENT); // The primary delta is that simple
        assertDelta(focusContext.getSummarySecondaryDelta(), "summary secondary delta")
                .display()
                .assertModify()
                .assertModifiedExclusive( // This list may change if projector internals change
                        PATH_ACTIVATION_EFFECTIVE_STATUS,
                        PATH_ACTIVATION_ENABLE_TIMESTAMP,
                        FocusType.F_ITERATION,
                        FocusType.F_ITERATION_TOKEN,
                        InfraItemName.METADATA,
                        FocusType.F_ROLE_MEMBERSHIP_REF,
                        FocusType.F_ARCHETYPE_REF);
        // @formatter:on
    }

    private void assertUserPrimaryAndSecondaryDeltasNoArchetype(ModelContext<?> modelContext) {
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
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows

        and("simulation result is OK");
        assertSimulationResultAfter(simResult);
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfigurationSeen();
        // @formatter:off
        assertProcessedObjectsAfter(simResult)
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                    .delta()
                    .objectToAdd()
                        .assertName(name)
                        .valueMetadataSingle()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent(accountShouldExist)
                            .assertCreateChannel(CHANNEL_USER_URI)
                        .end().end()
                        .asFocus()
                            .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                .assertEnableTimestampPresent()
                            .end()
                            .assertLiveLinks(accountShouldExist ? 1 : 0)
                        .end()
                    .end();
        // @formatter:on

        Collection<? extends ProcessedObject<?>> processedObjects = getProcessedObjects(simResult);
        if (accountShouldExist) {
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "processed objects")
                    .assertSize(1); // user only
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);

        // The user deltas are the same as in test100.
        // The linkRef delta is audited but (currently) it is not among secondary deltas.
        assertUserPrimaryAndSecondaryDeltasNoArchetype(modelContext);

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
                            ShadowType.F_ACTIVATION, // admin status + timestamp (by a mapping) - present only in
                            ShadowType.F_PURPOSE); // midPoint assumes the password is not known to the user
        } else {
            assertThat(projectionContext.getSecondaryDelta()).as("projection secondary delta").isNull();
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
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // simulated shadows?

        and("simulation result is OK");
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfigurationSeen();
        assertSimulationResultAfter(simResult);
        // @formatter:off
        assertProcessedObjectsAfter(simResult)
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                    .delta()
                    .objectToAdd()
                        .assertName(name)
                        .valueMetadataSingle()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertLastProvisioningTimestampPresent(accountShouldExist)
                            .assertCreateChannel(CHANNEL_USER_URI)
                        .end().end()
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
        // @formatter:on

        Collection<? extends ProcessedObject<?>> processedObjects = getProcessedObjects(simResult);
        if (accountShouldExist) {
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "")
                    .assertSize(1); // user delta only
        }

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // Only basic assertions this time. We hope that everything is OK there.
        assertThat(modelContext.getFocusContext()).as("focus context").isNotNull();
        assertThat(modelContext.getProjectionContexts()).as("projection contexts").hasSize(1);
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
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(createLinkRefDelta(userOid, target)),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows

        and("simulation result is OK");
        assertSimulationResultAfter(simResult);

        boolean accountShouldExist = targetIsProduction || isDevelopmentConfigurationSeen();
        // @formatter:off
        if (accountShouldExist) {
            Collection<? extends ProcessedObject<?>> processedObjects = getProcessedObjects(simResult );
            assertProcessedObjects(processedObjects, "processed objects")
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .assertEventMarks()
                        .delta()
                        .assertModifiedExclusive(
                                UserType.F_LINK_REF,
                                InfraItemName.METADATA)
                    .end();
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(simResult)
                    .display()
                    .by().objectType(UserType.class).find()
                        .assertState(ObjectProcessingStateType.UNMODIFIED)
                    .end()
                    .assertSize(1);
        }
        // @formatter:on

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
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
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(createAssignmentDelta(userOid, target)),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new object is created");
        objectsCounter.assertNoNewObjects(result); // in the future there may be some simulated shadows

        and("simulation result is OK");
        assertSimulationResultAfter(simResult);

        Collection<? extends ProcessedObject<?>> processedObjects = getProcessedObjects(simResult);
        boolean accountShouldExist = targetIsProduction || isDevelopmentConfigurationSeen();
        // @formatter:off
        assertProcessedObjects(processedObjects, "objects")
                .display()
                .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                    .assertEventMarks(MARK_FOCUS_ASSIGNMENT_CHANGED)
                    .delta()
                    .assertModified(
                            UserType.F_ASSIGNMENT,
                            InfraItemName.METADATA)
                .assertModifications(7 + (accountShouldExist ? 2 : 0));

        if (accountShouldExist) {
            assertProcessedObjects(processedObjects, "objects")
                    .display()
                    .by().changeType(ChangeType.MODIFY).objectType(UserType.class).find()
                        .delta()
                        .assertModified(
                                PATH_METADATA_LAST_PROVISIONING_TIMESTAMP_NAMES_ONLY,
                                UserType.F_LINK_REF)
                    .end();
            assertAccountAdded(name, target, processedObjects);
        } else {
            assertProcessedObjects(processedObjects, "objects")
                    .assertSize(1); // user delta only
        }
        // @formatter:on

        and("the model context is OK");
        ModelContext<?> modelContext = simResult.getLastModelContext();
        displayDumpable("model context", modelContext);
        // some asserts here (are these interesting, after all?)
    }

    private void assertAccountAdded(
            String name, DummyTestResource target, Collection<? extends ProcessedObject<?>> processedObjects)
            throws SchemaException {
        assertProcessedObjects(processedObjects, "objects")
                .by().changeType(ChangeType.ADD).objectType(ShadowType.class).find()
                    .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
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
                        .valueMetadataSingle()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertCreateChannel(SchemaConstants.CHANNEL_USER_URI);
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

        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(delta),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);

        and("simulation result is OK");
        assertSimulationResultAfter(simResult)
                .assertObjectsAdded(0)
                .assertObjectsModified(1)
                .assertObjectsDeleted(0)
                .assertObjectsProcessed(1);

        // @formatter:off
        assertProcessedObjects(simResult)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventMarks(MARK_FOCUS_ACTIVATED)
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
        TestSimulationResult simResult = importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withNameValue("test200")
                .withTaskExecutionMode(getExecutionMode())
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);

        and("simulation result is OK");
        assertSimulationResultAfter(simResult);
        // @formatter:off
        assertProcessedObjects(simResult)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                    .delta()
                        .objectToAdd()
                            .assertName("test200")
                            .valueMetadataSingle()
                                .assertRequestTimestampPresent()
                                .assertCreateTimestampPresent()
                                .assertLastProvisioningTimestampPresent()
                                .assertCreateChannel(CHANNEL_IMPORT_URI)
                            .end().end()
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
                    .assertEventMarks()
                    .delta()
                    .assertModifiedExclusive(
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            InfraItemName.METADATA)
                .end();
        // @formatter:on

        and("shadow should not have full sync info set");
        assertTest20xShadow("test200", task, result);
    }

    /**
     * As {@link #test200SimulatedAccountImportNoProjectionsForeground()} but on background.
     */
    @Test
    public void test205SimulatedAccountImportNoProjectionsBackground() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        objectsCounter.remember(result);

        given("an account on production source");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.addAccount("test205");

        when("the account is imported (on background)");
        String taskOid = executeProductionAccountImportOnBackground("test205", false, result);

        then("no new objects should be created (except for one shadow), no model deltas really executed");
        objectsCounter.assertShadowOnlyIncrement(1, result);

        and("processed objects are OK");
        TestSimulationResult simResult = getTaskSimResult(taskOid, result);
        // @formatter:off
        assertProcessedObjects(simResult)
                .display()
                .by().changeType(ChangeType.ADD).objectType(UserType.class).find()
                    .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                    .delta()
                        .objectToAdd()
                            .assertName("test205")
                            .valueMetadataSingle()
                                .assertRequestTimestampPresent()
                                .assertCreateTimestampPresent()
                                .assertLastProvisioningTimestampPresent()
                                .assertCreateChannel(CHANNEL_IMPORT_URI)
                            .end().end()
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
                    .assertEventMarks()
                    .delta()
                    .assertModifiedExclusive(
                            ShadowType.F_ITERATION,
                            ShadowType.F_ITERATION_TOKEN,
                            InfraItemName.METADATA)
                .end();
        // @formatter:on

        and("shadow should not have full sync info set");
        assertTest20xShadow("test205", task, result);
    }

    private void assertTest20xShadow(String name, Task task, OperationResult result) throws CommonException {
        assertShadowAfter(
                findAccountByUsername(name, RESOURCE_SIMPLE_PRODUCTION_SOURCE.get(), task, result))
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default")
                .assertIsExists()
                .assertSynchronizationSituation(SynchronizationSituationType.UNMATCHED)
                .assertSynchronizationSituationDescriptionUpdatedButNotFull();
    }

    @SuppressWarnings("SameParameterValue")
    private String executeProductionAccountImportOnBackground(String name, boolean shadowSimulation, OperationResult result)
            throws CommonException, IOException {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withNameValue(name)
                .withTaskExecutionMode(getExecutionMode(shadowSimulation))
                .execute(result);
    }

    /**
     * In a special low-level shadow simulation mode we simulate the classification and re-classification of shadows.
     *
     * For production resource.
     */
    @Test
    public void test210SimulatedClassificationAndReclassificationOnProductionResource() throws Exception {
        executeSimulatedClassificationAndReclassification(
                RESOURCE_SIMPLE_PRODUCTION_SOURCE,
                "test210",
                true);
    }

    /**
     * As {@link #test210SimulatedClassificationAndReclassificationOnProductionResource()} but on development resource.
     */
    @Test
    public void test215SimulatedClassificationAndReclassificationOnDevelopmentResource() throws Exception {
        executeSimulatedClassificationAndReclassification(
                RESOURCE_SIMPLE_DEVELOPMENT_SOURCE,
                "test215",
                false);
    }

    private void executeSimulatedClassificationAndReclassification(
            DummyTestResource resource, String accountName, boolean isProductionResource) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("an account on production source");
        resource.controller.addAccount(accountName);

        // STEP 1: low-level "shadow-simulated" import -> produces classification/correlation simulation result

        when("the account is imported with shadow-simulation (on background)");
        objectsCounter.remember(result);
        String taskOid1 =
                importAccountsRequest()
                        .withResourceOid(resource.oid)
                        .withNameValue(accountName)
                        .withTaskExecutionMode(getExecutionMode(true))
                        .execute(result);

        then("no new objects are created (except for one shadow)");
        objectsCounter.assertShadowOnlyIncrement(1, result);

        and("there are two simulation deltas (for now) - about classification and correlation");
        // This is a known limitation: two ProcessedObject records for a single shadow are distinct, although they shouldn't be
        TestSimulationResult simResult1 = getTaskSimResult(taskOid1, result);
        // @formatter:off
        var second = assertProcessedObjects(simResult1)
                .display()
                .assertSize(2)
                .by().eventMarkOid(MARK_SHADOW_CLASSIFICATION_CHANGED.oid).find(
                        po -> assertClassificationAs(po, "default"))
                .by().withoutEventMarkOid(MARK_SHADOW_CLASSIFICATION_CHANGED.oid).find();
        // @formatter:on

        if (isProductionResource || isDevelopmentConfigurationSeen()) {
            assertCorrelationAsNoOwner(second);
        } else {
            assertNoCorrelationChange(second); // Synchronization configuration is invisible here
        }

        // STEP 2: high-level "normally simulated" import -> classifies the shadow + produces clockwork simulation result

        when("the account is imported with 'normal' simulation (on background)");
        objectsCounter.remember(result);
        String taskOid2 =
                importAccountsRequest()
                        .withResourceOid(resource.oid)
                        .withNameValue(accountName)
                        .withTaskExecutionMode(getExecutionMode(false))
                        .execute(result);

        then("no new objects are created");
        objectsCounter.assertNoNewObjects(result);

        and("shadow has correct kind/intent");
        PrismObject<ShadowType> shadow2 = findShadowByPrismName(accountName, resource.get(), result);
        assertShadow(shadow2, "after simulation")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default");

        if (isProductionResource || isDevelopmentConfigurationSeen()) {
            and("there is user creation and shadow modification simulation delta, as this is 'normal' simulation");
            // @formatter:off
            assertProcessedObjects(getTaskSimResult(taskOid2, result))
                    .display()
                    .by().objectType(UserType.class).changeType(ChangeType.ADD).find().end()
                    .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                        .delta()
                            .assertModifiedExclusive(
                                    ShadowType.F_ITERATION,
                                    ShadowType.F_ITERATION_TOKEN,
                                    InfraItemName.METADATA)
                        .end()
                    .end()
                    .assertSize(2);
            // @formatter:on
        } else {
            and("there are no simulation deltas, as the configuration is not seen by the task");
            // @formatter:off
            assertProcessedObjects(getTaskSimResult(taskOid2, result))
                    .display()
                    .assertSize(0);
            // @formatter:on
        }

        // STEP 3: changing the account, so it now should belong to a different type (to see if re-classification will take place)
        //  - low-level "shadow-simulated" import -> produces classification simulation result

        when("account 'type' attribute is changed to 'person'");
        resource.controller.getDummyResource().getAccountByName(accountName)
                .addAttributeValue(ATTR_TYPE, "person");

        and("the account is imported with shadow-simulation (on background)");
        objectsCounter.remember(result);
        String taskOid3 =
                importAccountsRequest()
                        .withResourceOid(resource.oid)
                        .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                        .withNameValue(accountName)
                        .withTaskExecutionMode(getExecutionMode(true))
                        .execute(result);

        then("no new objects are created");
        objectsCounter.assertNoNewObjects(result);

        and("shadow has unchanged kind/intent in repo");
        PrismObject<ShadowType> shadow3 = findShadowByPrismName(accountName, resource.get(), result);
        assertShadow(shadow3, "after simulation")
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("default");

        TestSimulationResult simResult3 = getTaskSimResult(taskOid3, result);
        if (isProductionResource) {
            // Resource is production mode, so the task can see it regardless of the configuration set used by it.
            // Re-classification is not engaged because of the production status of the resource/object-type.
            // But correlation is run (again), as the owner was not determined yet.
            and("no reclassification delta, but correlation one is there");
            // @formatter:off
            assertProcessedObjects(simResult3)
                    .display()
                    .assertSize(1)
                    .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                        .delta()
                            .assertModified(
                                    // This is the only "real" modification.
                                    // Other ones are phantom ones and may be removed in the future.
                                    ShadowType.F_CORRELATION.append(ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP))
                        .end()
                        .assertEventMarks() // none, as the correlation state is not changed even if dummy delta may be present
                    .end();
            // @formatter:on
        } else if (!isDevelopmentConfigurationSeen()) {
            // The resource is development mode only and task sees only the production configuration set:
            // so, no classification, no correlation, no synchronization. Except for sync timestamp.
            and("there are no real classification nor correlation deltas (as the task uses the production configuration)");
            assertProcessedObjects(simResult3)
                    .display()
                    .assertSize(1)
                    .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find(
                            po -> po.assertEventMarks()
                                    .delta(d -> d.assertModifiedExclusive(ShadowType.F_SYNCHRONIZATION_TIMESTAMP)));
        } else {
            // Resource is development mode, task sees the development configuration.
            // Hence, both re-classification and re-correlation occurs.
            and("there is a re-classification delta");
            // @formatter:off
            assertProcessedObjects(simResult3)
                    .display()
                    .by().eventMarkOid(MARK_SHADOW_CLASSIFICATION_CHANGED.oid).find()
                        .delta()
                            .assertModify()
                            .assertModification(ShadowType.F_INTENT, "default", "person")
                            .assertModifications(1)
                        .end()
                    .end()
                    .by().noEventMarks().find()
                        .delta()
                            .assertModified(
                                    // This is the only "real" modification.
                                    // Other ones are phantom ones and may be removed in the future.
                                    ShadowType.F_CORRELATION.append(ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP))
                        .end()
                    .end()
                    .assertSize(2); // temporarily
            // @formatter:on

            assertSimulationResult(simResult3, "after")
                    .display()
                    .assertMetricValueByEventMark(MARK_SHADOW_CLASSIFICATION_CHANGED.oid, BigDecimal.ONE);

            REPORT_SIMULATION_VALUES_CHANGED.export()
                    .withDefaultParametersValues(simResult3.getSimulationResultRef())
                    .execute(result);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>> assertClassificationAs(
            ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>> po, String intent) {
        return po.assertEventMarks(MARK_SHADOW_CLASSIFICATION_CHANGED)
                .delta()
                .assertModify()
                .assertModification(ShadowType.F_KIND, null, ShadowKindType.ACCOUNT)
                .assertModification(ShadowType.F_INTENT, null, intent)
                .assertModifications(2)
                .end();
    }

    private void assertCorrelationAsNoOwner(ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>> po) {
        po.assertEventMarks(MARK_SHADOW_CORRELATION_STATE_CHANGED)
                .delta()
                .assertModify()
                .assertModified(CORRELATION_START_TIMESTAMP_PATH)
                .assertModified(CORRELATION_END_TIMESTAMP_PATH)
                .assertModification(CORRELATION_SITUATION_PATH, CorrelationSituationType.NO_OWNER)
                .assertModification(ShadowType.F_SYNCHRONIZATION_SITUATION, SynchronizationSituationType.UNMATCHED)
                .assertModified(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION)
                .assertModified(ShadowType.F_SYNCHRONIZATION_TIMESTAMP);
    }

    private void assertNoCorrelationChange(ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>> po) {
        po.assertEventMarks()
                .delta()
                .assertModify()
                .assertNotModified(
                        CORRELATION_START_TIMESTAMP_PATH,
                        CORRELATION_END_TIMESTAMP_PATH,
                        CORRELATION_SITUATION_PATH,
                        ShadowType.F_SYNCHRONIZATION_SITUATION,
                        ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION)
                .assertModified(ShadowType.F_SYNCHRONIZATION_TIMESTAMP);
    }

    /**
     * Simulated foreground re-classification (production resource).
     *
     * MID-8613
     */
    @Test
    public void test220SimulatedForegroundReclassificationOnProductionResource() throws Exception {
        executeSimulatedForegroundReclassification(
                RESOURCE_SIMPLE_PRODUCTION_SOURCE,
                "test220",
                true);
    }

    /**
     * As {@link #test220SimulatedForegroundReclassificationOnProductionResource()} but on development resource.
     */
    @Test
    public void test225SimulatedForegroundReclassificationOnDevelopmentResource() throws Exception {
        executeSimulatedForegroundReclassification(
                RESOURCE_SIMPLE_DEVELOPMENT_SOURCE,
                "test225",
                false);
    }

    private void executeSimulatedForegroundReclassification(
            DummyTestResource resource, String accountName, boolean isProductionResource) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("an account on production source, classified by regular search");
        resource.controller.addAccount(accountName);
        var objectsBefore = modelService.searchObjects(
                ShadowType.class,
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .and().item(ICFS_NAME_PATH).eq(accountName)
                        .build(),
                null, task, result);
        assertThat(objectsBefore).as("matching shadows").hasSize(1);
        assertShadow(objectsBefore.get(0), "before")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_DEFAULT);

        when("shadow is changed so it will belong to 'person' type");
        resource.controller.getDummyResource().getAccountByName(accountName)
                .addAttributeValue(ATTR_TYPE, "person");

        and("shadow is retrieved in development-low-level-simulation mode");
        task.setExecutionMode(getExecutionMode(true));
        var objectsAfter = modelService.searchObjects(
                ShadowType.class,
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .and().item(ICFS_NAME_PATH).eq(accountName)
                        .build(),
                null, task, result);
        assertThat(objectsAfter).as("matching shadows").hasSize(1);

        if (!isProductionResource && isDevelopmentConfigurationSeen()) {
            then("the retrieved shadow has new intent");
            assertShadow(objectsAfter.get(0), "after")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent("person");
        } else {
            then("the retrieved shadow has still old intent");
            assertShadow(objectsAfter.get(0), "after")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(INTENT_DEFAULT);
        }
    }

    /**
     * Import from production resource that ends in error.
     *
     * MID-8622
     */
    @Test
    public void test230ImportWithError() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String name = "test230";

        given("an account on production source");
        var account = RESOURCE_SIMPLE_PRODUCTION_SOURCE.addAccount(name);
        account.addAttributeValue(ATTR_TYPE, "employee");
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, "12345");

        and("user with conflicting name exists");
        UserType conflicting = new UserType()
                .name(name)
                .employeeNumber("999999"); // intentionally different
        addObject(conflicting, task, result);

        when("account is imported in simulation mode");
        var simResult = importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "employee"))
                .withNameValue(name)
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .withNotAssertingSuccess()
                .executeOnForegroundSimulated(null, task, result);

        then("result is fatal error");
        assertThatOperationResult(result)
                .isFatalError();

        and("result contains information about the error");
        var processedObject = assertProcessedObjects(simResult)
                .display()
                .single()
                .assertType(ShadowType.class)
                .assertState(ObjectProcessingStateType.UNMODIFIED)
                .assertResultStatus(OperationResultStatus.FATAL_ERROR)
                .assertResult(r ->
                        r.isFatalError()
                                .hasMessageContaining("Found conflicting existing object"))
                .getProcessedObject();

        displayValueAsXml("processed object", processedObject.toBean());
    }

    /**
     * There is a synchronization reaction in `proposed` state.
     * It should be ignored in production mode.
     *
     * MID-8671
     */
    @Test
    public void test240ExecutingDevelopmentModeSyncReaction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String name = "test240";
        ResourceObjectTypeIdentification employeeTypeId =
                ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "employee");

        given("an account on production source with an imported user");
        var account = RESOURCE_SIMPLE_PRODUCTION_SOURCE.addAccount(name);
        account.addAttributeValue(ATTR_TYPE, "employee");
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, "11111");

        and("its imported owner");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withTypeIdentification(employeeTypeId)
                .withNameValue(name)
                .executeOnForeground(result);
        assertUserBeforeByUsername(name);

        when("the account is deleted and the simulated reconciliation is run");
        RESOURCE_SIMPLE_PRODUCTION_SOURCE.controller.deleteAccount(name);
        var taskOid = reconcileAccountsRequest()
                .withResourceOid(RESOURCE_SIMPLE_PRODUCTION_SOURCE.oid)
                .withTaskExecutionMode(getExecutionMode())
                .withTypeIdentification(employeeTypeId)
                .withNameValue(name)
                .withNotAssertingSuccess()
                .execute(result);

        then("the user still exists");
        assertUserAfterByUsername(name);

        boolean actionVisible = isDevelopmentConfigurationSeen();
        and(actionVisible ? "simulation result contains user deletion" : "simulation result does NOT contain user deletion");
        var a = assertProcessedObjects(taskOid, "after")
                .display();
        if (actionVisible) {
            a.by().objectType(ShadowType.class).state(ObjectProcessingStateType.UNMODIFIED).find().end()
                    .by().objectType(UserType.class).state(ObjectProcessingStateType.DELETED).find().end()
                    .assertSize(2);
        } else {
            a.assertSize(0); // Even the shadow is not visible, because the clockwork was not executed
        }
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
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);

        and("simulation result is OK");
        assertSimulationResultAfter(simResult);
        // @formatter:off
        UserType userAfter = (UserType) assertProcessedObjects(simResult)
                .display()
                .single()
                .assertEventMarks(MARK_USER_ADD, MARK_FOCUS_ACTIVATED)
                .delta()
                    .assertAdd()
                    .assertObjectTypeClass(UserType.class)
                    .objectToAdd()
                        .assertName("test300")
                        .valueMetadataSingle()
                            .assertRequestTimestampPresent()
                            .assertCreateTimestampPresent()
                            .assertCreateChannel(CHANNEL_USER_URI)
                        .end().end()
                        .asFocus()
                            .activation()
                                .assertEffectiveStatus(ActivationStatusType.ENABLED)
                                .assertEnableTimestampPresent()
                            .end()
                            .getObjectable();
        // @formatter:on

        Set<String> orgs = userAfter.getOrganization().stream()
                .map(PolyStringType::getOrig)
                .collect(Collectors.toSet());
        if (isDevelopmentConfigurationSeen()) {
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

    /**
     * Creates a user of archetype {@link #ARCHETYPE_PERSON_DEV_TEMPLATE} that is a production archetype pointing
     * to a development-mode object template.
     */
    @Test
    public void test310CreatePersonWithDevelopmentTemplate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);

        given("a user");
        UserType user = new UserType()
                .name("test310")
                .assignment(
                        new AssignmentType()
                                .targetRef(ARCHETYPE_PERSON_DEV_TEMPLATE.ref()));

        when("user is created in simulation");
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(user.asPrismObject().createAddDelta()),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);

        and("simulation result is OK");
        UserType userAfter = (UserType) assertProcessedObjects(simResult)
                .display()
                .single()
                .delta().objectToAdd().getObjectable();

        Set<String> orgs = userAfter.getOrganization().stream()
                .map(PolyStringType::getOrig)
                .collect(Collectors.toSet());
        if (isDevelopmentConfigurationSeen()) {
            assertThat(orgs).as("user orgs").containsExactlyInAnyOrder("template:person-dev-template");
        } else {
            assertThat(orgs).as("user orgs").isEmpty();
        }
    }

    /**
     * When the structural archetype is changed, the "processed object" record should refer to the old archetype.
     *
     * MID-8610
     */
    @Test
    public void test320SimulateArchetypeChange() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user with archetype 'person'");
        UserType user = new UserType()
                .name("test320")
                .assignment(ARCHETYPE_PERSON.assignmentTo());
        addObject(user, task, result);

        when("archetype is changed to 'customer' (in simulation)");
        TestSimulationResult simResult =
                executeWithSimulationResult(
                        List.of(deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT)
                                .replace(ARCHETYPE_CUSTOMER.assignmentTo())
                                .asObjectDelta(user.getOid())),
                        getExecutionMode(), defaultSimulationDefinition(), task, result);

        then("simulation result points still to 'person' archetype");
        assertProcessedObjects(simResult)
                .display()
                .single()
                .assertStructuralArchetypeOid(ARCHETYPE_PERSON.oid);
    }

    private boolean isDevelopmentConfigurationSeen() {
        return !getExecutionMode().isProductionConfiguration();
    }
}
