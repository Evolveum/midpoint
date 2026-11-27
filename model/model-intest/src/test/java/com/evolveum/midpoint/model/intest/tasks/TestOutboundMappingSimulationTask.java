/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.asserter.ProcessedObjectsAsserter;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Tests the outbound mapping simulation task functionality in PREVIEW mode.
 *
 * == Test Data Architecture
 *
 * This test verifies an outbound mappings simulation task, that copies {@code personalNumber} from users
 * to {@code employeeNumber} on accounts (shadows).
 *
 * === Users
 *
 * The {@code users.xml} file defines users organized by {@code organizationalUnit} into several groups by the type
 * of tests they are used in. Check the users filter in individual tests to see what group is used.
 *
 * Additionally, 1 system admin user is added by the framework, what in some tests increases the number of processed
 * objects.
 *
 * === Accounts
 *
 * The {@code accounts.csv} file defines accounts, all with an empty delineator (matching the default objectType in
 * {@code dummy-resource.xml}). Accounts are correlated with users via the {@code correlator} attribute matching the
 * user's {@code name} (the correlator is defined in the resource xml file).
 *
 * === Test Isolation
 *
 * Tests isolate specific scenarios by calling {@link #setUsersFilterToTask(String)} to set a focus
 * objects filter on the simulation task. The filter uses {@code organizationalUnit} values to select
 * which users are processed, enabling isolated testing of different mapping simulation behaviors.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestOutboundMappingSimulationTask extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "tasks/mapping-simulation/outbound");
    private static final String DUMMY_RESOURCE_OID = "9e776e33-90b0-4550-a367-46948b317e34";
    private static final File USERS = new File(TEST_DIR, "users.xml");
    private static final File ACCOUNTS = new File(TEST_DIR, "accounts.csv");
    private static final File SIMULATION_TASK = new File(TEST_DIR, "task-mapping-simulation.xml");
    private static final String SIMULATION_TASK_OID = "1c670eb2-e07f-477d-a2cd-b5377d2827ff";
    private static final File RANGE_SIMULATION_TASK = new File(TEST_DIR, "task-mapping-simulation-range.xml");
    private static final String RANGE_SIMULATION_TASK_OID = "6c4f3a2e-3f0b-4c3e-9a1d-5e1361700001";

    private TestTask mappingTask;
    private TestTask rangeMappingTask;
    private DummyTestResource resource;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        CommonInitialObjects.addMarks(this, initTask, initResult);

        this.resource = DummyTestResource.fromFile(TEST_DIR, "dummy-resource.xml", DUMMY_RESOURCE_OID, "mapping-test")
                .withAccountsFromCsv(ACCOUNTS);
        this.resource.init(this, initTask, initResult);
        this.mappingTask = TestTask.fromFile(SIMULATION_TASK, SIMULATION_TASK_OID);
        this.rangeMappingTask = TestTask.fromFile(RANGE_SIMULATION_TASK, RANGE_SIMULATION_TASK_OID);
        repoAddObjectsFromFile(USERS, UserType.class, initResult);
        assertUsers(9); // Including one admin.
    }

    @BeforeMethod
    void initObjects() throws Exception {
        this.mappingTask.initWithOverwrite(this, getTestTask(), getTestOperationResult());
        this.rangeMappingTask.initWithOverwrite(this, getTestTask(), getTestOperationResult());
    }

    @Test
    void noFocusFilterIsUsed_simulateMappings_allUsersShouldBeProcessed() throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping");
        and("No accounts are linked with users");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("All users should be processed.");
        and("Shadows for users who have personalNumber will be modified (employeeNumber added/replaced).");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                .assertObjectsProcessed(9) // All the users are used for simulation.
                .assertObjectsModified(5); // Only 5 of them has a personal number.
        assertNoProcessedFocuses();
        assertProcessedShadowsCount(9)
                .then(TestOutboundMappingSimulationTask::filterModified)
                .assertSize(5)
                .then(assertModifiedAttribute("employeeNumber"));
    }

    @Test
    void focusFilterMatchesUserWithoutProjectionAndPersonalNumber_simulateMappings_fakeShadowShouldBeUsedForEvaluation()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping.");
        and("No accounts are linked with users");
        and("User's filter matches one user without any projection and without a personal number");
        setUsersFilterToTask("organizationalUnit = \"Without projection\" and personalNumber not exists");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("User matched by the filter should be used for simulation.");
        and("New 'fake' shadow should be processed as a user's projection.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                // Only user matching the filter is used for simulation
                .assertObjectsProcessed(1)
                // Because the user does not have any personal number and the shadow is basically empty, there is no
                // modification.
                .assertObjectsModified(0);
        assertNoProcessedFocuses();
        assertProcessedShadowsCount(1)
                .then(assertModifiedObjectsCount(0));
    }

    @Test
    void focusFilterMatchesUserWithoutProjectionButWithPersonalNumber_simulateMappings_fakeShadowShouldBeUsedForEvaluation()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping.");
        and("No accounts are linked with users");
        and("User's filter matches one user without any projection, but with an existing personal number");
        setUsersFilterToTask("organizationalUnit = \"Without projection\" and personalNumber exists");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("User matched by the filter should be used for simulation.");
        and("New 'fake' shadow should be processed as a user's projection.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                // Only user matching the filter are used for simulation
                .assertObjectsProcessed(1)
                // One shadow should be modified, because the personal number in user exists, but the fake shadow
                // does not have any employee number.
                .assertObjectsModified(1);
        assertNoProcessedFocuses();
        assertProcessedShadowsCount(1)
                .then(assertModifiedObjectsCount(1))
                .then(assertModifiedAttribute("employeeNumber"));
    }

    @Test
    void userWithPersonalNumberAndProjectionWithoutEmpNumber_simulateMappings_personalNumberShouldBeMappedToEmpNumber()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping.");
        and("No accounts are linked with users");
        and("User's filter matches one user with an existing personal number and projection");
        and("Projection does not have employee number");
        setUsersFilterToTask("organizationalUnit = \"With projection\" and personalNumber exists");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("User matched by the filter should be used for simulation.");
        and("Existing shadow should be processed as a user's projection.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                // Only user matching the filter are used for simulation
                .assertObjectsProcessed(1)
                // One shadow should be modified, because the personal number in user exists, but the shadow
                // does not have any employee number.
                .assertObjectsModified(1);
        assertNoProcessedFocuses();
        assertProcessedShadowsCount(1)
                .then(assertModifiedObjectsCount(1))
                .then(assertModifiedAttribute("employeeNumber"))
                .then(assertObjectIdentity("John"));

    }

    @Test
    void userWithoutPersonalNumberButWithProjectionWithoutEmpNumber_simulateMappings_projectionShouldNotBeChanged()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping.");
        and("No accounts are linked with users");
        and("User's filter matches one user without a personal number but with existing projection");
        and("Projection does not have employee number");
        setUsersFilterToTask("organizationalUnit = \"With projection\" and personalNumber not exists");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("User matched by the filter should be used for simulation.");
        and("Existing shadow should be processed as a user's projection.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                // Only user matching the filter are used for simulation
                .assertObjectsProcessed(1)
                // Because the user does not have any personal number and the shadow does not have employee number,
                // there is no modification.
                .assertObjectsModified(0);
        assertNoProcessedFocuses();
        assertProcessedShadowsCount(1)
                .then(assertModifiedObjectsCount(0))
                .then(assertObjectIdentity("Mark"));
    }

    @Test
    void simulateMappingWithDifferentOutcomes_eventMarksShouldBeSetAccordinglyToMappingOutcome()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains one explicitly defined mapping");
        and("Filtered users and their projections have items corresponding to various mapping outcome cases");
        setUsersFilterToTask("organizationalUnit = \"Marking\"");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("processed objects should contain event marks corresponding to the change");
        final Map<String, String> shadowNameToOidMap = extractShadowOidsAndCorrelationValues(result);
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(4)
                .assertObjectsModified(3);
        assertProcessedShadowsCount(4)
                .then(assertUnModifiedObjectsCount(1))
                .then(assertModifiedObjectsCount(3))
                .then(assertContainsEventMark(shadowNameToOidMap.get("connor"),
                        SystemObjectsType.MARK_ITEM_VALUE_NOT_CHANGED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("cena"),
                        SystemObjectsType.MARK_ITEM_VALUE_ADDED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("snow"),
                        SystemObjectsType.MARK_ITEM_VALUE_REMOVED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("rambo"),
                        SystemObjectsType.MARK_ITEM_VALUE_MODIFIED.value()));
    }

    /**
     * MID-13617: a mapping with an explicit range (target/set) needs the original values of the target attribute.
     * A user without a projection has no such values, the same as an empty shadow. The simulation must not fail.
     */
    @Test
    void mappingWithRangeAndUserWithoutProjection_simulateMappings_fakeShadowShouldBeUsedAndTaskShouldNotFail()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains a mapping with an explicit range.");
        and("User's filter matches one user without any projection, but with a personal number");
        setUsersFilterToTask(RANGE_SIMULATION_TASK_OID,
                "organizationalUnit = \"Without projection\" and personalNumber exists");

        when("Mapping simulation task is run on the resource.");
        rangeMappingTask.rerun(result);

        then("The user should be processed, and the personal number should be added to the 'fake' shadow.");
        assertSimulationResult(rangeMappingTask.oid, "Assert mapping simulation result metrics.")
                .assertObjectsProcessed(1)
                .assertObjectsModified(1);
        assertProcessedShadowsCount(rangeMappingTask.oid, 1)
                .then(assertModifiedObjectsCount(1))
                .then(assertModifiedAttribute("employeeNumber"));
    }

    /**
     * The range of the mapping does not change the outcomes for existing accounts: the original values are taken
     * from the shadows. The expected results are the same as for the mapping without a range.
     */
    @Test
    void mappingWithRangeAndExistingAccounts_simulateMappings_originalValuesShouldBeTakenFromShadows()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping simulation task contains a mapping with an explicit range.");
        and("Filtered users and their projections have items corresponding to various mapping outcome cases");
        setUsersFilterToTask(RANGE_SIMULATION_TASK_OID, "organizationalUnit = \"Marking\"");

        when("Mapping simulation task is run on the resource.");
        rangeMappingTask.rerun(result);

        then("The outcomes correspond to the original values of the employee number on the accounts.");
        final Map<String, String> shadowNameToOidMap = extractShadowOidsAndCorrelationValues(result);
        assertSimulationResult(rangeMappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(4)
                .assertObjectsModified(3);
        assertProcessedShadowsCount(rangeMappingTask.oid, 4)
                .then(assertUnModifiedObjectsCount(1))
                .then(assertModifiedObjectsCount(3))
                .then(assertContainsEventMark(shadowNameToOidMap.get("connor"),
                        SystemObjectsType.MARK_ITEM_VALUE_NOT_CHANGED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("cena"),
                        SystemObjectsType.MARK_ITEM_VALUE_ADDED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("snow"),
                        SystemObjectsType.MARK_ITEM_VALUE_REMOVED.value()))
                .then(assertContainsEventMark(shadowNameToOidMap.get("rambo"),
                        SystemObjectsType.MARK_ITEM_VALUE_MODIFIED.value()));
    }

    @DataProvider
    Object[] nonPreviewExecutionModes() {
        return Arrays.stream(ExecutionModeType.values())
                .filter(mode -> mode != ExecutionModeType.PREVIEW)
                .filter(mode -> mode != ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                .toArray();
    }

    @Test(dataProvider = "nonPreviewExecutionModes")
    void accountsAndUsersExists_runMappingSimulationTaskWithOtherThanPreviewMode_taskShouldFail(
            ExecutionModeType executionMode) throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Mapping task execution mode is set to " + executionMode + ".");
        setExecutionMode(executionMode);

        when("Mapping simulation task with particular mapping configuration is run on the resource.");
        mappingTask.rerunErrorsOk(result);

        then("Task should fail, because it supports only PREVIEW execution mode.");
        mappingTask.doAssert("Mapping task is supported only in PREVIEW mode, thus it should fail.")
                .assertFatalError();
    }

    private void setUsersFilterToTask(String query) throws Exception {
        setUsersFilterToTask(SIMULATION_TASK_OID, query);
    }

    private void setUsersFilterToTask(String taskOid, String query) throws Exception {
        final SearchFilterType filter = new SearchFilterType();
        filter.setText(query);
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_OUTBOUND_MAPPINGS_SIMULATION,
                                OutboundMappingsSimulationWorkDefType.F_FOCUS_OBJECTS))
                        .replace(new ObjectSetType().type(UserType.COMPLEX_TYPE).query(
                                new QueryType().filter(filter)
                        ))
                        .asObjectDelta(taskOid),
                null, getTestTask(), getTestOperationResult()
        );
    }

    private @NotNull Map<String, String> extractShadowOidsAndCorrelationValues(OperationResult result) throws CommonException {
        return this.resource.getAccounts(this, this::listAccounts)
                .shadows(getTestTask(), result)
                .stream()
                .collect(Collectors.toMap(
                        shadow -> ShadowUtil.getSingleStringAttributeValue(shadow, new QName("correlator")),
                        ShadowType::getOid));
    }

    private void setExecutionMode(ExecutionModeType executionMode) throws Exception {
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION,
                                ActivityExecutionModeDefinitionType.F_MODE))
                        .replace(executionMode)
                        .asObjectDelta(SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult());
    }

    private void assertNoProcessedFocuses() throws CommonException {
        assertProcessedObjects(this.mappingTask.oid, "Check number of processed focuses")
                .by().objectType(FocusType.class)
                .assertCount(0);
    }

    private ProcessedObjectsAsserter<ProcessedObjectsAsserter<Void>> assertProcessedShadowsCount(int expectedCount)
            throws CommonException {
        return assertProcessedShadowsCount(this.mappingTask.oid, expectedCount);
    }

    private ProcessedObjectsAsserter<ProcessedObjectsAsserter<Void>> assertProcessedShadowsCount(
            String taskOid, int expectedCount) throws CommonException {
        return assertProcessedObjects(taskOid, "Check processed objects")
                .by().objectType(ShadowType.class)
                .collect("Check number of processed shadows")
                .assertSize(expectedCount);
    }

    private Function<? super ProcessedObjectsAsserter<?>, ? extends ProcessedObjectsAsserter<?>> assertObjectIdentity(
            String givenName) {
        return asserter -> asserter.single().objectBefore()
                // Technically, the given name of course is not the "identity", but in these tests it is enough.
                .assertValues(ShadowType.F_ATTRIBUTES.append("givenName"), givenName)
                .end()
                .end();
    }

    private static ProcessedObjectsAsserter<?> filterModified(ProcessedObjectsAsserter<?> asserter) {
        return asserter.by().state(ObjectProcessingStateType.MODIFIED)
                .collect("Modified objects");
    }

    private static Function<? super ProcessedObjectsAsserter<?>, ? extends ProcessedObjectsAsserter<?>>
            assertModifiedObjectsCount(int expectedNumberOfModifiedObjects) {
        return asserter -> asserter.by().state(ObjectProcessingStateType.MODIFIED)
                .assertCount(expectedNumberOfModifiedObjects)
                .end();
    }

    private static Function<? super ProcessedObjectsAsserter<?>, ? extends ProcessedObjectsAsserter<?>>
            assertUnModifiedObjectsCount(int expectedNumberOfUnmodifiedObjects) {
        return asserter -> asserter.by().state(ObjectProcessingStateType.UNMODIFIED)
                .assertCount(expectedNumberOfUnmodifiedObjects)
                .end();
    }

    private static Function<? super ProcessedObjectsAsserter<?>, ? extends ProcessedObjectsAsserter<?>>
            assertModifiedAttribute(String attribute) {
        return asserter -> asserter.each(po -> po.delta()
                    .assertModifiedExclusive(ShadowType.F_ATTRIBUTES.append(attribute)),
            "Check presence of modified attribute");
    }

    private static Function<? super ProcessedObjectsAsserter<?>, ? extends ProcessedObjectsAsserter<?>>
            assertContainsEventMark(String oid, String markOid) {
        return asserter -> asserter.by().objectOid(oid)
                .find()
                .assertEventMarksOids(markOid)
                .end();
    }

}
