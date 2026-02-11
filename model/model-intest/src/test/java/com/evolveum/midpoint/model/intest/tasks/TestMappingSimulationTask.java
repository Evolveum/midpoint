/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.model.impl.correlator.TestingAccount;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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

    private TestTask mappingTask;
    private Collection<ShadowType> accounts;
    private List<ShadowType> unlinkedAccounts;
    private Collection<UserType> users;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        CommonInitialObjects.addMarks(this, initTask, initResult);

        final DummyTestResource resource = new DummyTestResource(TEST_DIR, "dummy-resource.xml", DUMMY_RESOURCE_OID,
                "mapping-test",
                // FIXME I guess we should not use correlator test util. Maybe move the method to more generic class.
                CorrelatorTestUtil::createAttributeDefinitions);
        resource.init(this, initTask, initResult);
        this.mappingTask = TestTask.fromFile(SIMULATION_TASK, SIMULATION_TASK_OID);
        // FIXME Fix similarly as above fixme.
        CorrelatorTestUtil.addAccountsFromCsvFile(this, ACCOUNTS, resource);
        // FIXME, once again, bellow method should probably goes to some other place. Maybe DummyTestAccount.
        this.accounts = CorrelatorTestUtil.getAllAccounts(this, resource, TestingAccount::new, initTask, initResult)
                .stream()
                .map(TestingAccount::getShadow)
                .toList();
        this.unlinkedAccounts = new ArrayList<>();

        final List<PrismObject<UserType>> users = repoAddObjectsFromFile(USERS, UserType.class, initResult);
        assertUsers(4);
        this.users = users.stream().map(user -> user.asObjectable()).toList();

        final Map<String, @NotNull UserType> usersByName = users.stream()
                .map(user -> user.asObjectable())
                .collect(Collectors.toMap(user -> user.getName().getOrig(), user -> user));
        for (ShadowType account : this.accounts) {
            final String matchingFieldValue = ShadowUtil.getAttributeValue(account, ItemName.from(NS_RI, "correlator"));
            if (usersByName.containsKey(matchingFieldValue)) {
                linkAccount(account, usersByName.get(matchingFieldValue), initTask, initResult);
            } else {
                this.unlinkedAccounts.add(account);
            }
        }
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
        and("Resource contains one inbound mapping");
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
        final List<? extends ProcessedObject<?>> modifiedObjects = assertAndGetModifiedObjects(processedObjects, 1);
        assertItemModificationsCount(modifiedObjects.get(0), 2);
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
        and("Resource contains one inbound mapping");
        and("Mapping simulation task contains one explicitly defined mapping");
        and("Mapping simulation task is configured to exclude existing mappings");
        includeExistingMappings(excludeExistingMappingsOption);

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("Only delta from explicit mapping should be present. Existing mappings should not evaluate to any delta.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result.")
                .assertObjectsProcessed(2) // 6 accounts, 6 users
                .assertObjectsModified(1);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 1);
        assertProcessedShadowsCount(processedObjects, 1);
        final List<? extends ProcessedObject<?>> modifiedObjects = assertAndGetModifiedObjects(processedObjects, 1);
        assertItemModificationsCount(modifiedObjects.get(0), 1);
    }

    @Test
    void oneAccountLinkedOneAccountCorrelated_simulateMapping_linkedAndCorrelatedAccountsAndFocusesShouldBeProcessed()
            throws CommonException {
        final OperationResult result = getTestOperationResult();

        given("One account is linked with a user");
        and("One account is correlated but not linked with a user");
        correlateAccountWithUserWithoutProjection(this.unlinkedAccounts.get(0), getTestTask(), result);
        and("There is some mapping to simulate, either explicitly defined in work def or already present in resource.");

        when("Mapping simulation task is run on the resource.");
        mappingTask.rerun(result);

        then("All shadows with their owners (linked or correlated) should be processed.");
        assertSimulationResult(mappingTask.oid, "Assert mapping simulation result metrics.")
                .assertObjectsProcessed(4);
        final List<? extends ProcessedObject<?>> processedObjects = getTaskSimResult(this.mappingTask.oid,
                result).getProcessedObjects(result);
        assertProcessedFocusesCount(processedObjects, 2);
        assertProcessedShadowsCount(processedObjects, 2);
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
                                WorkDefinitionsType.F_MAPPING, MappingWorkDefinitionType.F_MAPPINGS,
                                SimulatedMappingsType.F_INCLUDE_EXISTING_MAPPINGS))
                        .replace(include)
                        .asObjectDelta(SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult()
        );
    }

    private void linkAccount(ShadowType shadow, UserType user, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .add(ObjectTypeUtil.createObjectRef(shadow.getOid(), ObjectTypes.SHADOW))
                        .asObjectDelta(user.getOid()),
                null, task, result);
    }

    private void correlateAccountWithUserWithoutProjection(ShadowType account, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        final UserType userWithoutProjection = this.users.stream()
                .filter(user -> user.getLinkRef() == null || user.getLinkRef().isEmpty())
                .findAny()
                .orElseThrow(() -> new AssertionError("Can't find any user without a projection."));

        executeChanges(
                deltaFor(ShadowType.class)
                        .item(ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_RESULTING_OWNER))
                        .replace(MiscSchemaUtil.createObjectReference(userWithoutProjection.asPrismObject(),
                                UserType.class))
                        .asObjectDelta(account.getOid()),
                null, task, result
        );
    }

    private static void assertProcessedFocusesCount(List<? extends ProcessedObject<?>> processedObjects,
            int expectedCount) {
        assertThat(countProcessedObjects(processedObjects, ProcessedObject::isFocus))
                .as("Check number of processed focuses")
                .isEqualTo(expectedCount);
    }

    private static void assertProcessedShadowsCount(List<? extends ProcessedObject<?>> processedObjects,
            int expectedCount) {
        assertThat(countProcessedObjects(processedObjects, ProcessedObject::isShadow))
                .as("Check number of processed shadows")
                .isEqualTo(expectedCount);
    }

    private static @NotNull List<? extends ProcessedObject<?>> assertAndGetModifiedObjects(
            List<? extends ProcessedObject<?>> processedObjects, int expectedNumberOfModifiedObjects) {
        final List<? extends ProcessedObject<?>> modifiedObjects = processedObjects.stream()
                .filter(ProcessedObject::isModification)
                .toList();
        assertThat(modifiedObjects.size())
                .as("Check number of modified objects")
                .isEqualTo(expectedNumberOfModifiedObjects);
        return modifiedObjects;
    }

    private static void assertItemModificationsCount(ProcessedObject<?> processedObject,
            int expectedNumberOfModifications) {
        final long modifiedItemsCount = Objects.requireNonNull(processedObject.getDelta()).getModifications().size();
        assertThat(modifiedItemsCount)
                .as("Check number of modified items in the object")
                .isEqualTo(expectedNumberOfModifications);
    }

    private static long countProcessedObjects(List<? extends ProcessedObject<?>> objects,
            Predicate<ProcessedObject<?>> filter) {
        return objects.stream()
                .filter(filter)
                .count();
    }
}
