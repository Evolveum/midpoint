/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMappingSimulationTask extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "tasks/mapping-simulation");
    private static final String DUMMY_RESOURCE_OID = "f499e126-f235-4141-82eb-15df1d9c2adc";
    private static final File USERS = new File(TEST_DIR, "users.xml");
    private static final File ACCOUNTS = new File(TEST_DIR, "accounts.csv");
    private static final File SIMULATION_TASK = new File(TEST_DIR, "task-mapping-simulation.xml");
    private static final String SIMULATION_TASK_OID = "b2bbb2d5-cad2-421a-965d-31bed71e0191";
    private static final ItemName.WithoutPrefix ACCOUNT_CORRELATION_ATTRIBUE = ItemName.from(SchemaConstants.NS_RI,
            "correlator");

    private TestTask mappingTask;
    private DummyTestResource resource;
    private List<PrismObject<UserType>> users;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        CommonInitialObjects.addMarks(this, initTask, initResult);

        this.resource = DummyTestResource.fromFile(TEST_DIR, "dummy-resource.xml", DUMMY_RESOURCE_OID, "mapping-test")
                .withAccountsFromCsv(ACCOUNTS);
        this.resource.init(this, initTask, initResult);
        this.mappingTask = TestTask.fromFile(SIMULATION_TASK, SIMULATION_TASK_OID);
        this.users = repoAddObjectsFromFile(USERS, UserType.class, initResult);
        assertUsers(9); // Including one admin.

        final Collection<PrismObject<ShadowType>> linkedAccounts = this.resource.getAccounts(this, this::listAccounts)
                .linkWithUsers(this.users, delta -> this.executeChanges(delta, null, initTask, initResult), initTask,
                        initResult)
                .onAttributes(ACCOUNT_CORRELATION_ATTRIBUE, UserType.F_NAME);
        assertThat(linkedAccounts).as("Check test precondition: Linked accounts count").hasSize(6);

    }

    @BeforeMethod
    void initObjects() throws Exception {
        this.mappingTask.initWithOverwrite(this, getTestTask(), getTestOperationResult());
    }

    @Test
    void accountIsLinkedToUser_simulateMappingAlsoWithExistingMappings_allMappingsShouldBeEvaluated()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("One account is linked with a user");
        and("Object type definition in resource contains one inbound mapping");
        and("Mapping simulation task contains one explicitly defined mapping");
        and("Mapping simulation task is configured to include existing mappings");
        includeExistingMappings(true);

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("All shadows with their owners should be processed.");
        and("One object should be modified.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                .assertObjectsProcessed(2)
                .assertObjectsModified(1);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 1);
        assertProcessedShadowsCount(processedObjects, 1);
        assertModifiedObjectsCount(processedObjects, 1)
                .assertItemModificationsCount(2);
    }

    @DataProvider
    Object[] existingMappingsExclusionValues() {
        return new Object[]{null, false};
    }

    @Test(dataProvider = "existingMappingsExclusionValues")
    void accountIsLinkedToUser_simulateMappingButExcludeExistingOnes_existingMappingsShouldNotBeEvaluated(
            Boolean excludeExistingMappingsOption) throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Accounts are linked with a user");
        and("Object type definition in resource contains one inbound mapping");
        and("Mapping simulation task contains one explicitly defined mapping");
        and("Mapping simulation task is configured to exclude existing mappings");
        includeExistingMappings(excludeExistingMappingsOption);

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("Only delta from explicit mapping should be present. Existing mappings should not evaluate to any delta.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(2)
                .assertObjectsModified(1);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 1);
        assertProcessedShadowsCount(processedObjects, 1);
        final String userWithExpectedChange = this.users.stream()
                .filter(user -> user.getName().getOrig().equals("smith1"))
                .findFirst()
                .map(PrismObject::getOid)
                .orElseGet(() -> Assertions.fail("Expected user does not exist."));
        assertModifiedObjectsCount(processedObjects, 1)
                .assertItemModificationsCount(1)
                .assertContainsEventMark(userWithExpectedChange, SystemObjectsType.MARK_ITEM_VALUE_ADDED.value());
    }

    @Test
    void accountsAreLinkedToUser_simulateMappingWithDifferentOutcomes_eventMarksShouldBeSetAccordinglyToMappingOutcome()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Accounts are linked with a user");
        and("Object type definition in resource does not contain any inbound mapping");
        and("Mapping simulation task contains one explicitly defined mapping");
        final String intent = "marks-test";
        and("Mapping simulation task is configured to use object type with \"" + intent + "\" intent");
        setObjectTypeIntent(intent);

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("processed objects should contain event marks corresponding to the change");
        final Map<String, String> usersNameToOidMap = this.users.stream()
                .collect(Collectors.toMap(user -> user.getName().getOrig(), PrismObject::getOid));
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(8)
                .assertObjectsModified(3);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        final ProcessedObjectsAsserter processedFocusesAsserter = assertProcessedFocusesCount(processedObjects, 4);
        processedFocusesAsserter
                .assertUnModifiedObjectsCount(1)
                .assertContainsEventMark(usersNameToOidMap.get("connor"),
                        SystemObjectsType.MARK_ITEM_VALUE_NOT_CHANGED.value());
        processedFocusesAsserter
                .assertModifiedObjectsCount(3)
                .assertItemModificationsCount(3) // Each focus should have one item changed
                .assertContainsEventMark(usersNameToOidMap.get("cena"),
                        SystemObjectsType.MARK_ITEM_VALUE_ADDED.value())
                .assertContainsEventMark(usersNameToOidMap.get("snow"),
                        SystemObjectsType.MARK_ITEM_VALUE_REMOVED.value())
                .assertContainsEventMark(usersNameToOidMap.get("rambo"),
                        SystemObjectsType.MARK_ITEM_VALUE_MODIFIED.value());
    }


    @Test
    void linkedAccountHasObjectTypeWithInheritance_simulateMappingButExcludeExistingOnes_existingMappingsShouldNotBeEvaluated()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Accounts are linked with a user");
        and("Object type definition in resource contains one inbound mapping defined in its super type");
        and("Mapping simulation task contains one explicitly defined mapping");
        and("Mapping simulation task is configured to exclude existing mappings");
        includeExistingMappings(false);
        final String intent = "exclusion-test";
        and("Mapping simulation task is configured to use object type with \"" + intent + "\" intent");
        setObjectTypeIntent(intent);

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("Only delta from explicit mapping should be present. Existing mappings should not evaluate to any delta.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(2)
                .assertObjectsModified(1);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 1);
        assertProcessedShadowsCount(processedObjects, 1);
        assertModifiedObjectsCount(processedObjects, 1)
                .assertItemModificationsCount(1);
    }

    @Test
    void oneAccountLinkedOneAccountCorrelated_simulateMapping_linkedAndCorrelatedAccountsAndFocusesShouldBeProcessed()
            throws CommonException {
        final Task task = getTestTask();
        final OperationResult result = getTestOperationResult();

        given("One account is linked with a user");
        and("One account is correlated but not linked with a user");
        final Collection<PrismObject<ShadowType>> correlatedShadows = this.resource.getAccounts(this, this::listAccounts)
                .correlateWithUsers(this.users, delta -> this.executeChanges(delta, null, task, result), task, result)
                .onAttributes(ACCOUNT_CORRELATION_ATTRIBUE, UserType.F_FAMILY_NAME);
        assertThat(correlatedShadows).as("Check test precondition: Correlated accounts count").hasSize(1);
        and("There is some mapping to simulate, either explicitly defined in work def or already present in resource.");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("All shadows with their owners (linked or correlated) should be processed.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                .assertObjectsProcessed(4);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 2)
                // There are two processed focuses, each should have one processed shadow.
                .assertContainsProjectionRecords(1, 1);
        assertProcessedShadowsCount(processedObjects, 2)
                .assertContainsLinkedFocus();
    }

    @DataProvider
    Object[] nonPreviewExecutionModes() {
        return Arrays.stream(ExecutionModeType.values())
                .filter(mode -> mode != ExecutionModeType.PREVIEW)
                .filter(mode -> mode != ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                .toArray();
    }

    @Test(dataProvider = "nonPreviewExecutionModes")
    void accountsAndUsersExists_runCorrelationTaskWithOtherThanPreviewMode_taskShouldFail(
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

    private void setExecutionMode(ExecutionModeType executionMode) throws Exception {
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION,
                                ActivityExecutionModeDefinitionType.F_MODE))
                        .replace(executionMode)
                        .asObjectDelta(SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult());
    }

    private void includeExistingMappings(Boolean include) throws Exception {
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_MAPPINGS, MappingWorkDefinitionType.F_INCLUDE_EXISTING_MAPPINGS))
                        .replace(include)
                        .asObjectDelta(SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult()
        );
    }

    private void setObjectTypeIntent(String intent) throws Exception {
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK,
                                WorkDefinitionsType.F_MAPPINGS, MappingWorkDefinitionType.F_RESOURCE_OBJECTS,
                                ResourceObjectSetType.F_INTENT))
                        .replace(intent)
                        .asObjectDelta(SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult()
        );
    }

    private static ProcessedObjectsAsserter assertProcessedFocusesCount(
            List<? extends ProcessedObject<?>> processedObjects, int expectedCount) {
        return new ProcessedObjectsAsserter(
                assertThat(processedObjects)
                        .filteredOn(ProcessedObject::isFocus)
                        .as("Check number of processed focuses")
                        .hasSize(expectedCount),
                "processed focuses");
    }

    private static ProcessedObjectsAsserter assertProcessedShadowsCount(
            List<? extends ProcessedObject<?>> processedObjects, int expectedCount) {
        return new ProcessedObjectsAsserter(
                assertThat(processedObjects)
                        .filteredOn(ProcessedObject::isShadow)
                        .as("Check number of processed shadows")
                        .hasSize(expectedCount),
                "processed shadows");
    }

    private static ProcessedObjectsAsserter assertModifiedObjectsCount(
            List<? extends ProcessedObject<?>> processedObjects, int expectedNumberOfModifiedObjects) {
        return new ProcessedObjectsAsserter(
                assertThat(processedObjects)
                        .filteredOn(ProcessedObject::isModification)
                        .as("Check number of modified objects")
                        .hasSize(expectedNumberOfModifiedObjects),
                "modified objects");
    }

    private static final class ProcessedObjectsAsserter {
        private final ListAssert<? extends ProcessedObject<?>>  objectsAsserter;
        private final String currentObjectsDescription;

        private ProcessedObjectsAsserter(ListAssert<? extends ProcessedObject<?>> objectsAsserter,
                String currentObjectsDescription) {
            this.objectsAsserter = objectsAsserter;
            this.currentObjectsDescription = currentObjectsDescription;
        }

        void assertContainsProjectionRecords(Integer... expectedCount) {
            this.objectsAsserter
                    .map(ProcessedObject::toBean)
                    .as("Check links with projection")
                    .map(SimulationResultProcessedObjectType::getProjectionRecords)
                    .contains(expectedCount);
        }

        void assertContainsLinkedFocus() {
            this.objectsAsserter
                    .map(ProcessedObject::toBean)
                    .as("Check links with projection")
                    .map(SimulationResultProcessedObjectType::getFocusRecordId)
                    .doesNotContainNull();
        }

        ProcessedObjectsAsserter assertItemModificationsCount(int expectedNumberOfModifications) {
            this.objectsAsserter
                    .map(ProcessedObject::getDelta)
                    .as("Check number of modified items in all %s", this.currentObjectsDescription)
                    .doesNotContainNull()
                    .flatMap(delta -> delta.getModifications())
                    .hasSize(expectedNumberOfModifications);
            return this;
        }

        ProcessedObjectsAsserter assertContainsEventMark(String objectOid, String markOid) {
            this.objectsAsserter
                    .filteredOn(object -> object.getOid().equals(objectOid))
                    .flatMap(ProcessedObject::getMatchingEventMarksOids)
                    .as("Check event marks on %s with oid %s", this.currentObjectsDescription, objectOid)
                    .contains(markOid);
            return this;
        }

        ProcessedObjectsAsserter assertUnModifiedObjectsCount(int expectedNumberOfModifiedObjects) {
            return new ProcessedObjectsAsserter(
                    this.objectsAsserter
                            .filteredOn(Predicate.not(ProcessedObject::isModification))
                            .as("Check number of unmodified %s objects", this.currentObjectsDescription)
                            .hasSize(expectedNumberOfModifiedObjects),
                    "unmodified " + this.currentObjectsDescription);
        }

        ProcessedObjectsAsserter assertModifiedObjectsCount(int expectedNumberOfModifiedObjects) {
            return new ProcessedObjectsAsserter(
                    this.objectsAsserter
                            .filteredOn(ProcessedObject::isModification)
                            .as("Check number of modified %s objects", this.currentObjectsDescription)
                            .hasSize(expectedNumberOfModifiedObjects),
                    "modified " + this.currentObjectsDescription);
        }

    }

}
