/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelatorSimulationTask extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "tasks/correlator-simulation");
    private static final String DUMMY_RESOURCE_OID = "8582b8d8-7f29-4dbd-85f6-d674ca40df96";
    private static final File USERS = new File(TEST_DIR, "users.xml");
    private static final File ACCOUNTS = new File(TEST_DIR, "accounts.csv");
    private static final File CORRELATOR_SIMULATION_TASK = new File(TEST_DIR, "task-correlator-simulation.xml");
    private static final String CORRELATOR_SIMULATION_TASK_OID = "04b4b768-3d56-48fd-b339-0586f76e264e";
    private static final File SIMULATION_TASK_WITH_ADDITIONAL_MAPPING = new File(TEST_DIR,
            "simulation-task-with-additional-mapping.xml");
    private static final String SIMULATION_TASK_WITH_ADDITIONAL_MAPPING_OID = "c0caf95e-93ad-445c-bcf8-bc0a9fc7ddec";

    private TestTask correlationTask;
    private DummyTestResource resource;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        CommonInitialObjects.addMarks(this, initTask, initResult);

        this.resource = DummyTestResource.fromFile(TEST_DIR, "dummy-resource.xml",
                DUMMY_RESOURCE_OID, "correlation-test").withAccountsFromCsv(ACCOUNTS);
        this.resource.init(this, initTask, initResult);
        this.correlationTask = TestTask.fromFile(CORRELATOR_SIMULATION_TASK, CORRELATOR_SIMULATION_TASK_OID);

        importObjectsFromFileRaw(USERS, initTask, initResult);
        assertUsers(7);
    }

    @BeforeMethod
    void initObjects() throws Exception {
        this.correlationTask.initWithOverwrite(this, getTestTask(), getTestOperationResult());
    }

    @DataProvider
    Object[][] lifecycleStates() {
        return new Object[][] {
            {"active"},
            {"proposed"}
        };
    }

    @DataProvider
    Object[] nonPreviewExecutionModes() {
        return Arrays.stream(ExecutionModeType.values())
                .filter(mode -> mode != ExecutionModeType.PREVIEW)
                .filter(mode -> mode != ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                .toArray();
    }

    @Test(dataProvider = "lifecycleStates")
    void accountsAndUsersExists_simulateCorrelation_simulationShouldBeDoneWithDefinedCorrelator(String lifecycleState)
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Resource lifecycle state is set to " + lifecycleState + ".");
        setResourceLifeCycleState(lifecycleState);

        when("Correlator simulation task with particular correlator configuration is run on the resource.");
        correlationTask.rerun(result);

        then("Some users should be correlated as shadow's candidate owner.");
        assertSimulationResult(correlationTask.oid, "Assert correlator simulation result.")
                .assertObjectsProcessed(6)
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value(),
                        BigDecimal.valueOf(6))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_FOUND.value(),
                        BigDecimal.valueOf(1))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_CERTAIN.value(),
                        BigDecimal.valueOf(1))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND.value(),
                        BigDecimal.valueOf(4));
    }

    @Test(dataProvider = "nonPreviewExecutionModes")
    void accountsAndUsersExists_runCorrelationTaskWithOtherThanPreviewMode_taskShouldFail(
            ExecutionModeType executionMode) throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Correlation task execution mode is set to " + executionMode + ".");
        setExecutionMode(executionMode);

        when("Correlator simulation task with particular correlator configuration is run on the resource.");
        correlationTask.rerunErrorsOk(result);

        then("Task should fail, because it supports only PREVIEW execution mode.");
        correlationTask.doAssert("Correlation task is supported only in PREVIEW mode, thus it should fail.")
                .assertFatalError();
    }

    @Test
    void accountsAndUsersExists_runCorrelationWithAdditionalMappingOfExistingAttr_simulationShouldFoundCandidateOwners()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Correlation task with additional mapping is used");
        final TestTask correlationTask = TestTask.fromFile(SIMULATION_TASK_WITH_ADDITIONAL_MAPPING,
                SIMULATION_TASK_WITH_ADDITIONAL_MAPPING_OID);
        correlationTask.initWithOverwrite(this, getTestTask(), result);
        final ItemPathType attributeRef = correlationTask.get().asObjectable().getActivity().getWork().getCorrelation()
                .getCorrelators().getAdditionalItemsMappings().get(0).getRef();
        and("Resource contains definition of attribute from additional mapping");
        assertThat(this.resource.get().asObjectable().getSchemaHandling().getObjectType().get(0).getAttribute())
                .as("Check test precondition: Attribute specified in additional mapping is defined on the resource.")
                .map(ItemRefinedDefinitionType::getRef)
                .map(ItemPathType::toString)
                .contains(attributeRef.toString());

        when("Correlation simulation task is run on the resource.");
        correlationTask.rerun(result);

        then("Some users should be correlated as shadow's candidate owner.");
        assertSimulationResult(correlationTask.oid, "Assert correlator simulation result.")
                .assertObjectsProcessed(6)
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value(),
                        BigDecimal.valueOf(6))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_FOUND.value(),
                        BigDecimal.valueOf(5))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_CERTAIN.value(),
                        BigDecimal.valueOf(0))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND.value(),
                        BigDecimal.valueOf(1));
    }

    @Test
    void accountsAndUsersExists_runCorrelationWithAdditionalMappingOfMissingAttr_simulationShouldFoundCandidateOwners()
            throws Exception {
        final OperationResult result = getTestOperationResult();

        given("Correlation task with additional mapping is used");
        final TestTask correlationTask = TestTask.fromFile(SIMULATION_TASK_WITH_ADDITIONAL_MAPPING,
                SIMULATION_TASK_WITH_ADDITIONAL_MAPPING_OID);
        correlationTask.initWithOverwrite(this, getTestTask(), result);
        final ItemPathType attributeRef = correlationTask.get().asObjectable().getActivity().getWork().getCorrelation()
                .getCorrelators().getAdditionalItemsMappings().get(0).getRef();
        and("Resource does not contain definition of attribute from additional mapping");
        removeAttributeDef(attributeRef.getItemPath());
        assertThat(this.resource.get().asObjectable().getSchemaHandling().getObjectType().get(0).getAttribute())
                .as("Check test precondition: Attribute specified in additional mapping is defined on the resource.")
                .map(ItemRefinedDefinitionType::getRef)
                .map(ItemPathType::toString)
                .doesNotContain(attributeRef.toString());

        when("Correlation simulation task is run on the resource.");
        correlationTask.rerun(result);

        then("Some users should be correlated as shadow's candidate owner.");
        assertSimulationResult(correlationTask.oid, "Assert correlator simulation result.")
                .assertObjectsProcessed(6)
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_STATE_CHANGED.value(),
                        BigDecimal.valueOf(6))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_FOUND.value(),
                        BigDecimal.valueOf(5))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_NOT_CERTAIN.value(),
                        BigDecimal.valueOf(0))
                .assertMetricValueByEventMark(SystemObjectsType.MARK_SHADOW_CORRELATION_OWNER_FOUND.value(),
                        BigDecimal.valueOf(1));
    }

    private void removeAttributeDef(ItemPath attributeRef)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        final ResourceObjectTypeDefinitionType objectType = this.resource.get().asObjectable().getSchemaHandling()
                .getObjectType().get(0);
        final long objectTypeId = objectType.getId();
        final ResourceAttributeDefinitionType attrDef = objectType.getAttribute()
                .stream()
                .filter(attr -> attr.getRef().toString().equals(attributeRef.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Can't find attribute with specified ref: " + attributeRef));

        final OperationResult result = getTestOperationResult();
        executeChanges(deltaFor(ResourceType.class)
                .item(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, objectTypeId,
                        ResourceObjectTypeDefinitionType.F_ATTRIBUTE)
                .delete(attrDef.clone())
                .asObjectDelta(DUMMY_RESOURCE_OID),
                null, getTestTask(), result);
        this.resource.reload(result);
    }

    private void setResourceLifeCycleState(String lifecycleState) throws Exception {
        executeChanges(
                deltaFor(ResourceType.class)
                        .item(ResourceType.F_LIFECYCLE_STATE)
                        .replace(lifecycleState)
                        .asObjectDelta(DUMMY_RESOURCE_OID),
                null, getTestTask(), getTestOperationResult());
    }

    private void setExecutionMode(ExecutionModeType executionMode) throws Exception {
        executeChanges(
                deltaFor(TaskType.class)
                        .item(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION,
                                ActivityExecutionModeDefinitionType.F_MODE))
                        .replace(executionMode)
                        .asObjectDelta(CORRELATOR_SIMULATION_TASK_OID),
                null, getTestTask(), getTestOperationResult());
    }
}
