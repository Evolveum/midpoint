/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvSimulationReport extends TestCsvReport {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "simulation");

    private static final TestObject<ArchetypeType> ARCHETYPE_BLUE =
            TestObject.file(TEST_DIR, "archetype-blue.xml", "75beee71-edaa-4737-8793-8dd66eb4babf"); // structural
    private static final TestObject<ArchetypeType> ARCHETYPE_MAGENTA =
            TestObject.file(TEST_DIR, "archetype-magenta.xml", "0a7b2a81-d682-4dd0-803d-d47cbee6cc96"); // auxiliary
    private static final TestObject<RoleType> METAROLE_FUNCTION =
            TestObject.file(TEST_DIR, "metarole-function.xml", "");
    private static final TestObject<RoleType> ROLE_TESTER =
            TestObject.file(TEST_DIR, "role-tester.xml", "0d4faf1c-2677-41a4-9026-e0319a335767");
    private static final TestObject<RoleType> ROLE_ADMIN =
            TestObject.file(TEST_DIR, "role-admin.xml", "b9e47537-0d85-4a6c-aeb2-24d4c06db7a6");
    private static final TestObject<RoleType> ROLE_DEVELOPER =
            TestObject.file(TEST_DIR, "role-developer.xml", "d9ed4375-b0af-4cea-ae53-f67ff1e0ca67");
    private static final TestObject<RoleType> ORG_HQ =
            TestObject.file(TEST_DIR, "org-hq.xml", "8692de4f-51b3-472f-9489-5a7bdc12e891");

    private static final int EXISTING_USERS = 10;

    private static final int OBJECT_REPORT_COLUMNS = 11;

    private static final int C_ID = 0;
    private static final int C_OID = 1;
    private static final int C_NAME = 2;
    private static final int C_TYPE = 3;
    private static final int C_ARCHETYPE = 4;
    private static final int C_RESOURCE = 5;
    private static final int C_KIND = 6;
    private static final int C_INTENT = 7;
    private static final int C_TAG = 8;
    private static final int C_STATE = 9;

    // Object-, item-, and value-level
    private static final int C_MARK = 10;

    // Metrics
    private static final int C_M_EVENT_MARK = 10;
    private static final int C_M_CUSTOM_MARK = 11;
    private static final int C_M_SELECTED = 12;
    private static final int C_M_VALUE = 13;

    // Item- and value-level
    private static final int C_ITEM_CHANGED = 11;
    // Item-level
    private static final int C_OLD_VALUES = 12;
    private static final int C_NEW_VALUES = 13;
    private static final int C_VALUES_ADDED = 14;
    private static final int C_VALUES_DELETED = 15;
    private static final int C_I_RELATED_ASSIGNMENT = 16;
    private static final int C_I_RELATED_ASSIGNMENT_ID = 17;
    private static final int C_I_RELATED_ASSIGNMENT_TARGET = 18;
    private static final int C_I_RELATED_ASSIGNMENT_RELATION = 19;
    private static final int C_I_RELATED_ASSIGNMENT_RESOURCE = 20;
    private static final int C_I_RELATED_ASSIGNMENT_KIND = 21;
    private static final int C_I_RELATED_ASSIGNMENT_INTENT = 22;
    // Value-level
    private static final int C_VALUE_STATE = 12;
    private static final int C_VALUE = 13;
    private static final int C_V_RELATED_ASSIGNMENT = 14;
    private static final int C_V_RELATED_ASSIGNMENT_ID = 15;
    private static final int C_V_RELATED_ASSIGNMENT_TARGET = 16;
    private static final int C_V_RELATED_ASSIGNMENT_RELATION = 17;
    private static final int C_V_RELATED_ASSIGNMENT_RESOURCE = 18;
    private static final int C_V_RELATED_ASSIGNMENT_KIND = 19;
    private static final int C_V_RELATED_ASSIGNMENT_INTENT = 20;

    private static final int C_R_OID = 0;
    private static final int C_R_NAME = 1;
    private static final int C_R_IDENTIFIER = 2;
    private static final int C_R_START_TIMESTAMP = 3;
    private static final int C_R_END_TIMESTAMP = 4;
    private static final int C_R_TASK = 5;
    private static final int C_R_PRODUCTION_CONFIGURATION = 6;
    private static final int C_R_BUILT_IN_METRIC = 7;
    private static final int C_R_EVENT_MARK = 8;
    private static final int C_R_CUSTOM_METRIC = 9;
    private static final int C_R_AGGREGATION_FUNCTION = 10;
    private static final int C_R_SC_TYPE = 11;
    private static final int C_R_SC_ARCHETYPE = 12;
    private static final int C_R_SC_RESOURCE = 13;
    private static final int C_R_SC_KIND = 14;
    private static final int C_R_SC_INTENT = 15;
    private static final int C_R_VALUE = 16;
    private static final int C_R_SELECTION_SIZE = 17;
    private static final int C_R_SELECTION_TOTAL_VALUE = 18;
    private static final int C_R_DOMAIN_SIZE = 19;
    private static final int C_R_DOMAIN_TOTAL_VALUE = 20;

    private List<UserType> existingUsers;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // Only for Native repo, as Generic repo does not support simulations
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);

        var resources = repositoryService.searchObjects(ResourceType.class, null, createNoFetchCollection(), initResult);
        display("resources", resources);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        RESOURCE_DUMMY_OUTBOUND.initAndTest(this, initTask, initResult);

        // These do nothing, serving just to check whether simulation records carry the correct archetype information.
        ARCHETYPE_BLUE.init(this, initTask, initResult);
        ARCHETYPE_MAGENTA.init(this, initTask, initResult);

        // Each role creates an account with appropriate group membership.
        // Used to check assignment and associations deltas in simulation reports.
        METAROLE_FUNCTION.init(this, initTask, initResult);
        ROLE_TESTER.init(this, initTask, initResult);
        ROLE_ADMIN.init(this, initTask, initResult);
        ROLE_DEVELOPER.init(this, initTask, initResult);

        // Currently doing nothing, just to report on assignment orgRef.
        ORG_HQ.init(this, initTask, initResult);

        existingUsers = modelObjectCreatorFor(UserType.class)
                .withObjectCount(EXISTING_USERS)
                .withNamePattern("existing-%04d")
                .withCustomizer((u, number) -> addStandardArchetypeAssignments(u))
                .execute(initResult);

        REPORT_SIMULATION_OBJECTS.init(this, initTask, initResult);
        REPORT_SIMULATION_OBJECTS_WITH_METRICS.init(this, initTask, initResult);
        REPORT_SIMULATION_ITEMS_CHANGED.init(this, initTask, initResult);
        REPORT_SIMULATION_VALUES_CHANGED.init(this, initTask, initResult);
        REPORT_SIMULATION_RESULTS.init(this, initTask, initResult);
    }

    private static void addStandardArchetypeAssignments(UserType u) {
        u.getAssignment().add(
                new AssignmentType().targetRef(ARCHETYPE_BLUE.ref()));
        u.getAssignment().add(
                new AssignmentType().targetRef(ARCHETYPE_MAGENTA.ref()));
    }

    /** Checks the most simple "create user" scenario. */
    @Test
    public void test100CreateNewUsers() throws Exception {
        int users = 10;

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("users are created in simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> modelObjectCreatorFor(UserType.class)
                        .withObjectCount(users)
                        .withNamePattern("new-%04d")
                        .withCustomizer((u, number) -> addStandardArchetypeAssignments(u))
                        .execute(result));

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .assertSize(users);

        when("object-level report is created");
        var lines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("it is OK");
        assertCsv(lines, "after")
                .sortBy(C_NAME)
                .display()
                .assertRecords(users)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_NAME, "new-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_RESOURCE, "")
                .assertValue(C_KIND, "")
                .assertValue(C_INTENT, "")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Focus activated")
                .end();

        when("object-level report is created (by metrics)");
        var metricsLines = REPORT_SIMULATION_OBJECTS_WITH_METRICS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("it is OK");
        assertCsv(metricsLines, "after")
                .withNumericColumns(C_ID, C_M_VALUE)
                .sortBy(C_ID, C_M_EVENT_MARK, C_M_CUSTOM_MARK)
                .display()
                .assertRecords(users * 2) // focus activated + special
                .assertColumns(14)
                .record(0,
                        r -> r.assertValue(C_NAME, "new-0000")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Added")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "special")
                                .assertValue(C_M_SELECTED, "false")
                                .assertValue(C_M_VALUE, "0.0"))
                .record(1,
                        r -> r.assertValue(C_NAME, "new-0000")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Added")
                                .assertValue(C_M_EVENT_MARK, "Focus activated")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_SELECTED, "true")
                                .assertValue(C_M_VALUE, "1"))
                .record(14,
                        r -> r.assertValue(C_NAME, "new-0007")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Added")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "special")
                                .assertValue(C_M_SELECTED, "true") // selection expression
                                .assertValue(C_M_VALUE, String.valueOf(7 * 3.14)))
                .end();
    }

    /** Checks simple user modifications. */
    @Test
    public void test110DisableAndRenameUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("users are renamed and optionally disabled in simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> {
                    for (int i = 0; i < existingUsers.size(); i++) {
                        UserType existingUser = existingUsers.get(i);
                        modelService.executeChanges(
                                List.of(deltaFor(UserType.class)
                                        .optimizing()
                                        .item(UserType.F_NAME)
                                        .replace(PolyString.fromOrig(existingUser.getName().getOrig() + "-renamed"))
                                        .item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                                        .old().replace(i%2 == 0 ? ActivationStatusType.DISABLED : null)
                                        .asObjectDelta(existingUser.getOid())),
                                null, task, result);
                    }
                });

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .assertSize(EXISTING_USERS);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .sortBy(C_NAME)
                .display()
                .assertRecords(EXISTING_USERS)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_OID, existingUsers.get(0).getOid())
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed", "Focus deactivated")
                .end()
                .record(1)
                .assertValue(C_OID, existingUsers.get(1).getOid())
                .assertValue(C_NAME, "existing-0001-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed");

        when("object-level report (by metrics) is created");
        var metricsLines = REPORT_SIMULATION_OBJECTS_WITH_METRICS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(metricsLines, "after")
                .withNumericColumns(C_ID, C_M_VALUE)
                .sortBy(C_ID, C_M_EVENT_MARK, C_M_CUSTOM_MARK)
                .display()
                .assertRecords((int) (EXISTING_USERS * 3.5))
                .assertColumns(14)
                .record(0,
                        r -> r.assertValue(C_OID, existingUsers.get(0).getOid())
                                .assertValue(C_NAME, "existing-0000-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "modifications")
                                .assertValue(C_M_SELECTED, "true")
                                .assertValue(C_M_VALUE, "2"))
                .record(1,
                        r -> r.assertValue(C_OID, existingUsers.get(0).getOid())
                                .assertValue(C_NAME, "existing-0000-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "special")
                                .assertValue(C_M_SELECTED, "false")
                                .assertValue(C_M_VALUE, "0.0"))
                .record(2,
                        r -> r.assertValue(C_OID, existingUsers.get(0).getOid())
                                .assertValue(C_NAME, "existing-0000-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "Focus deactivated")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_SELECTED, "true")
                                .assertValue(C_M_VALUE, "1"))
                .record(3,
                        r -> r.assertValue(C_OID, existingUsers.get(0).getOid())
                                .assertValue(C_NAME, "existing-0000-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "Focus renamed")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_SELECTED, "true")
                                .assertValue(C_M_VALUE, "1"))
                .record(4,
                        r -> r.assertValue(C_OID, existingUsers.get(1).getOid())
                                .assertValue(C_NAME, "existing-0001-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "modifications")
                                .assertValue(C_M_SELECTED, "true")
                                .assertValue(C_M_VALUE, "1"))
                .record(5,
                        r -> r.assertValue(C_OID, existingUsers.get(1).getOid())
                                .assertValue(C_NAME, "existing-0001-renamed")
                                .assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "special")
                                .assertValue(C_M_SELECTED, "false")
                                .assertValue(C_M_VALUE, "3.14"));

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .sortBy(C_NAME, C_ITEM_CHANGED)
                .display()
                .assertRecords(15) // 10 changes of name, 5 changes of administrative status
                .record(0)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "activation/administrativeStatus")
                .assertValue(C_OLD_VALUES, "")
                .assertValue(C_NEW_VALUES, "Disabled")
                .assertValue(C_VALUES_ADDED, "Disabled")
                .assertValue(C_VALUES_DELETED, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_OLD_VALUES, "existing-0000")
                .assertValue(C_NEW_VALUES, "existing-0000-renamed")
                .assertValue(C_VALUES_ADDED, "existing-0000-renamed")
                .assertValue(C_VALUES_DELETED, "existing-0000")
                .end();

        when("item-level report is created - all items");
        var itemsLines2 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_INCLUDE_OPERATIONAL_ITEMS, true)
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines2, "after")
                .parse()
                .display()
                .assertRecords((a) -> a.hasSizeGreaterThan(40)); // too many

        when("item-level report is created - 'name' only");
        var itemsLines3 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_PATHS_TO_INCLUDE, UserType.F_NAME.toBean())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines3, "after")
                .parse()
                .display()
                .assertRecords(10);

        when("item-level report is created - 'activation/effectiveStatus' only");
        var itemsLines4 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_PATHS_TO_INCLUDE, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS.toBean())
                .withParameter(PARAM_SHOW_IF_NO_DETAILS, false)
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines4, "after")
                .parse()
                .display()
                .assertRecords(5);

        when("value-level report is created (default)");
        var valuesLines1 = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines1, "after")
                .sortBy(C_NAME, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .display()
                .assertRecords(25) // 20x name (ADD/DELETE), 5x administrativeStatus (ADD)
                .record(0)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "activation/administrativeStatus")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Disabled")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "existing-0000-renamed")
                .end()
                .record(2)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "existing-0000")
                .end();

        when("value-level report is created - all items");
        var valuesLines2 = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_INCLUDE_OPERATIONAL_ITEMS, true)
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines2, "after")
                .parse()
                .display()
                .assertRecords((a) -> a.hasSizeGreaterThan(50)); // too many
    }

    /** Checks whether account add ("link") operation is reported correctly. */
    @Test
    public void test120AddAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account is added (simulated)");
        String userOid = existingUsers.get(0).getOid();
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserAddAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        var processedObjects = assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2)
                .getProcessedObjects();

        String shadowOid = processedObjects.stream()
                .filter(po -> ShadowType.class.equals(po.getType()))
                .map(po -> po.getOid())
                .findFirst().orElseThrow(AssertionError::new);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Projection activated", "Resource object affected");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(2);

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // These assertions are quite fragile; may change if the report changes.
        assertCsv(valuesLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, shadowOid)
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Projection activated", "Resource object affected");
    }

    /** Checks account deletion reporting. */
    @Test
    public void test130DeleteAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("user with account exist");
        String userOid = existingUsers.get(0).getOid();
        executeChanges(
                createModifyUserAddAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get()),
                null, task, result);
        assertSuccess(result);

        when("account is deleted (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserDeleteAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .assertValues(C_MARK, "Projection deactivated", "Resource object affected");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(2);

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // These assertions are quite fragile; may change if the report changes.
        assertCsv(valuesLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "existing-0000")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .assertValues(C_MARK, "Projection deactivated", "Resource object affected");

        then("finally deleting the account");
        executeChanges(
                createModifyUserDeleteAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get()),
                null, task, result);
        assertSuccess(result);
    }

    /** Checks assignment creation + projection creation reporting. */
    @Test
    public void test140AssignAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account is assigned (simulated)");
        String userOid = existingUsers.get(0).getOid();
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserAssignAccount(userOid, RESOURCE_DUMMY_OUTBOUND.oid)),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        var processedObjects = assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2)
                .getProcessedObjects();

        String shadowOid = processedObjects.stream()
                .filter(po -> ShadowType.class.equals(po.getType()))
                .map(po -> po.getOid())
                .findFirst().orElseThrow(AssertionError::new);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                //.assertValue(5, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Projection activated", "Resource object affected");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(3); // assignment, linkRef, shadow

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        assertCsv(valuesLines, "after")
                .assertRecords(3)
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "Focus assignments changed")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, a -> a.startsWith("-> resource-outbound:Account/default"))
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "Focus assignments changed")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, shadowOid)
                .end()
                .record(2)
                .assertValue(C_NAME, "")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Projection activated", "Resource object affected");
    }

    /** Checks reporting of account modifications. */
    @Test
    public void test150ModifyUserWithAssignedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        String userName = getTestNameShort();
        UserType user = new UserType()
                .name(userName)
                .fullName("Jack Sparrow")
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_OUTBOUND.oid, ResourceType.COMPLEX_TYPE)))
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_BLUE.ref()));
        addObject(user.asPrismObject(), task, result);

        when("account is modified via user (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_FULL_NAME)
                        .replace(PolyString.fromOrig("Jackie Sparrow"))
                        .item(UserType.F_LOCALITY)
                        .replace(PolyString.fromOrig("Caribbean"))
                        .asObjectDelta(user.getOid())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .end()
                .record(1)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Modified");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(4); // fullName/location for user, fullname/locality for shadow

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        assertCsv(valuesLines, "after")
                .assertRecords(6)
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_ITEM_CHANGED, "fullName")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Jackie Sparrow")
                .end()
                .record(1)
                .assertValue(C_ITEM_CHANGED, "fullName")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "Jack Sparrow")
                .end()
                .record(2)
                .assertValue(C_ITEM_CHANGED, "locality")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Caribbean")
                .end()
                .record(3)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_ITEM_CHANGED, "attributes/fullname")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Jackie Sparrow")
                .end()
                .record(4)
                .assertValue(C_ITEM_CHANGED, "attributes/fullname")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "Jack Sparrow")
                .end()
                .record(5)
                .assertValue(C_ITEM_CHANGED, "attributes/location")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Caribbean")
                .end();
    }

    /** Checks reporting of assignment and entitlement modifications. */
    @Test
    public void test160ModifyAssignments() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        String userName = getTestNameShort();
        UserType user = new UserType()
                .name(userName)
                .fullName("Jack Sparrow")
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_OUTBOUND.ref())
                                .kind(ShadowKindType.ACCOUNT)
                                .intent("default")))
                .assignment(new AssignmentType().targetRef(ARCHETYPE_BLUE.ref()))
                .assignment(new AssignmentType().targetRef(ROLE_TESTER.ref()))
                .assignment(new AssignmentType().targetRef(ROLE_ADMIN.ref()));
        addObject(user.asPrismObject(), task, result);
        var userReloaded = repositoryService.getObject(UserType.class, user.getOid(), null, result);

        long adminAssignmentId = findAssignmentByTargetRequired(userReloaded, ROLE_ADMIN.oid).getId();
        long testerAssignmentId = findAssignmentByTargetRequired(userReloaded, ROLE_TESTER.oid).getId();
        long dummyAssignmentId = findAssignmentByResourceRequired(userReloaded, RESOURCE_DUMMY_OUTBOUND.oid).getId();

        ItemPath pathTesterAssignment = ItemPath.create(UserType.F_ASSIGNMENT, testerAssignmentId, AssignmentType.F_ORG_REF);
        ItemPath pathDummyAssignment = ItemPath.create(
                UserType.F_ASSIGNMENT, dummyAssignmentId, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when("assignments are modified (simulated)");
        String simulationId = "test160";
        var simulationResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(new AssignmentType().id(adminAssignmentId))
                        .item(pathTesterAssignment)
                        .replace(ORG_HQ.ref())
                        .item(pathDummyAssignment)
                        .replace(ActivationStatusType.DISABLED)
                        .asObjectDelta(user.getOid())),
                TaskExecutionMode.SIMULATED_PRODUCTION,
                defaultSimulationDefinition().identifier(simulationId),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .end()
                .record(1)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Modified");

        when("object-level report (by metrics) is created");
        var metricsLines = REPORT_SIMULATION_OBJECTS_WITH_METRICS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(metricsLines, "after")
                .withNumericColumns(C_ID, C_M_VALUE)
                .sortBy(C_ID, C_M_EVENT_MARK, C_M_CUSTOM_MARK)
                .display()
                .assertRecords(9)
                .assertColumns(14)
                .record(0,
                        r -> r.assertValue(C_TYPE, "UserType")
                                .assertValue(C_STATE, "Modified")
                                .assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "modifications")
                                .assertValue(C_M_VALUE, "3"))
                .record(1,
                        r -> r.assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "special")
                                .assertValue(C_M_VALUE, "0"))
                .record(2,
                        r -> r.assertValue(C_M_EVENT_MARK, "Focus assignments changed")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_VALUE, "1"))
                .record(3,
                        r -> r.assertValue(C_M_EVENT_MARK, "Focus role membership changed")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_VALUE, "1"))
                .record(4,
                        r -> r.assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "association-values-changed")
                                .assertValue(C_M_VALUE, "1"))
                .record(5,
                        r -> r.assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "attribute-modifications")
                                .assertValue(C_M_VALUE, "0"))
                .record(6,
                        r -> r.assertValue(C_M_EVENT_MARK, "")
                                .assertValue(C_M_CUSTOM_MARK, "modifications")
                                .assertValue(C_M_VALUE, "1"))
                .record(7,
                        r -> r.assertValue(C_M_EVENT_MARK, "Projection entitlement changed")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_VALUE, "1"))
                .record(8,
                        r -> r.assertValue(C_M_EVENT_MARK, "Resource object affected")
                                .assertValue(C_M_CUSTOM_MARK, "")
                                .assertValue(C_M_VALUE, "1"));

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // Hacking assignment/[1]/activation/administrativeStatus vs assignment/1/activation/administrativeStatus
        String pathDummyAssignmentAsString = prismContext.toUniformPath(pathDummyAssignment).toString();
        String pathTesterAssignmentAsString = prismContext.toUniformPath(pathTesterAssignment).toString();

        boolean dummyIsFirst = pathDummyAssignmentAsString.compareTo(pathTesterAssignmentAsString) < 0;
        System.out.println("dummy first: " + dummyIsFirst);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED)
                .display()
                .assertRecords(4) // 3x user assignment, 1x shadow association
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_I_RELATED_ASSIGNMENT, "") // no single related assignment
                .end()
                .record(dummyIsFirst ? 1 : 2)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathDummyAssignmentAsString)
                .assertValueNotEmpty(C_I_RELATED_ASSIGNMENT)
                .assertValue(C_I_RELATED_ASSIGNMENT_ID, String.valueOf(dummyAssignmentId))
                .assertValue(C_I_RELATED_ASSIGNMENT_TARGET, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_RELATION, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_I_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_I_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                .record(dummyIsFirst ? 2 : 1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathTesterAssignmentAsString)
                .assertValueNotEmpty(C_I_RELATED_ASSIGNMENT)
                .assertValue(C_I_RELATED_ASSIGNMENT_ID, String.valueOf(testerAssignmentId))
                .assertValue(C_I_RELATED_ASSIGNMENT_TARGET, "tester")
                .assertValue(C_I_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_I_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                .record(3)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_ITEM_CHANGED, "association")
                .end();

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .display()
                .assertRecords(4)
                // "assignment";"DELETED";"-> admin [4]";"-> admin [4]";"4";"admin";"default";"";"";""
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValueNotEmpty(C_VALUE)
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(adminAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "admin")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                // "assignment/[1]/activation/administrativeStatus";"ADDED";"Disabled";"-> resource-outbound:Account/default Disabled [1]";"1";"";"";"resource-outbound";"Account";"default"
                .record(dummyIsFirst ? 1 : 2)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathDummyAssignmentAsString)
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Disabled")
                .assertValueNotEmpty(C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(dummyAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                // "assignment/[3]/orgRef";"ADDED";"hq";"-> tester [3]";"3";"tester";"default";"";"";""
                .record(dummyIsFirst ? 2 : 1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathTesterAssignmentAsString)
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "hq")
                .assertValueNotEmpty(C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(testerAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "tester")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                .record(3)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_ITEM_CHANGED, "association")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "group: admin")
                .end();

        when("result-level report is created (default)");
        var resultsLines = REPORT_SIMULATION_RESULTS.export().execute(result);

        assertCsv(resultsLines, "after")
                .withNumericColumns(C_ID)
                .filter(r -> simulationId.equals(r.get(C_R_IDENTIFIER)))
                .sortBy(C_R_CUSTOM_METRIC, C_R_EVENT_MARK, C_R_BUILT_IN_METRIC)
                .display()
                .allRecords(
                        r -> r.assertValue(C_R_AGGREGATION_FUNCTION, "SELECTION_TOTAL_VALUE"))
                .forRecord(C_R_EVENT_MARK, MARK_FOCUS_ACTIVATED.getNameOrig(),
                        r -> r.assertValue(C_R_SC_TYPE, "UserType")
                                .assertValue(C_R_SC_ARCHETYPE, "blue")
                                .assertValue(C_R_VALUE, "0")
                                .assertValue(C_R_SELECTION_SIZE, "0")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "0")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "0"))
                .forRecord(C_R_EVENT_MARK, MARK_FOCUS_ASSIGNMENT_CHANGED.getNameOrig(),
                        r -> r.assertValue(C_R_SC_TYPE, "UserType")
                                .assertValue(C_R_SC_ARCHETYPE, "blue")
                                .assertValue(C_R_VALUE, "1")
                                .assertValue(C_R_SELECTION_SIZE, "1")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "1")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "1"))
                .forRecord(C_R_EVENT_MARK, MARK_PROJECTION_DEACTIVATED.getNameOrig(),
                        r -> r.assertValue(C_R_SC_TYPE, "ShadowType")
                                .assertValue(C_R_SC_ARCHETYPE, "")
                                .assertValue(C_R_SC_RESOURCE, "resource-outbound")
                                .assertValue(C_R_SC_KIND, "Account")
                                .assertValue(C_R_SC_INTENT, "default")
                                .assertValue(C_R_VALUE, "0")
                                .assertValue(C_R_SELECTION_SIZE, "0")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "0")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "0"))
                .forRecord(C_R_EVENT_MARK, MARK_PROJECTION_ENTITLEMENT_CHANGED.getNameOrig(),
                        r -> r.assertValue(C_R_SC_TYPE, "ShadowType")
                                .assertValue(C_R_SC_ARCHETYPE, "")
                                .assertValue(C_R_SC_RESOURCE, "resource-outbound")
                                .assertValue(C_R_SC_KIND, "Account")
                                .assertValue(C_R_SC_INTENT, "default")
                                .assertValue(C_R_VALUE, "1")
                                .assertValue(C_R_SELECTION_SIZE, "1")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "1")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "1"))
                .forRecord(C_R_CUSTOM_METRIC, "association-values-changed",
                        r -> r.assertValue(C_R_SC_TYPE, "ShadowType")
                                .assertValue(C_R_SC_ARCHETYPE, "")
                                .assertValue(C_R_SC_RESOURCE, "resource-outbound")
                                .assertValue(C_R_SC_KIND, "Account")
                                .assertValue(C_R_SC_INTENT, "default")
                                .assertValue(C_R_VALUE, "1")
                                .assertValue(C_R_SELECTION_SIZE, "1")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "1")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "1"))
                .forRecord(C_R_CUSTOM_METRIC, "attribute-modifications",
                        r -> r.assertValue(C_R_SC_TYPE, "ShadowType")
                                .assertValue(C_R_SC_ARCHETYPE, "")
                                .assertValue(C_R_SC_RESOURCE, "resource-outbound")
                                .assertValue(C_R_SC_KIND, "Account")
                                .assertValue(C_R_SC_INTENT, "default")
                                .assertValue(C_R_VALUE, "0")
                                .assertValue(C_R_SELECTION_SIZE, "0")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "0")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "0"))
                .forRecords(
                        1,
                        r -> "modifications".equals(r.get(C_R_CUSTOM_METRIC))
                                && "ShadowType".equals(r.get(C_R_SC_TYPE)),
                        r -> r.assertValue(C_R_SC_ARCHETYPE, "")
                                .assertValue(C_R_SC_RESOURCE, "resource-outbound")
                                .assertValue(C_R_SC_KIND, "Account")
                                .assertValue(C_R_SC_INTENT, "default")
                                .assertValue(C_R_VALUE, "1")
                                .assertValue(C_R_SELECTION_SIZE, "1")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "1")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "1"))
                .forRecords(
                        1,
                        r -> "modifications".equals(r.get(C_R_CUSTOM_METRIC))
                                && "UserType".equals(r.get(C_R_SC_TYPE)),
                        r -> r.assertValue(C_R_SC_ARCHETYPE, "blue")
                                .assertValue(C_R_VALUE, "3")
                                .assertValue(C_R_SELECTION_SIZE, "1")
                                .assertValue(C_R_SELECTION_TOTAL_VALUE, "3")
                                .assertValue(C_R_DOMAIN_SIZE, "1")
                                .assertValue(C_R_DOMAIN_TOTAL_VALUE, "3"));
    }

    /** Checks handling of REPLACE deltas. */
    @Test
    public void test170ModifyUserUsingReplaceDeltas() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        String userName = getTestNameShort();
        UserType user = new UserType()
                .name(userName)
                .fullName("Jack Sparrow")
                .organization("org1")
                .assignment(new AssignmentType().targetRef(ROLE_ADMIN.ref()));
        addObject(user.asPrismObject(), task, result);
        var userReloaded = repositoryService.getObject(UserType.class, user.getOid(), null, result);

        long adminAssignmentId = findAssignmentByTargetRequired(userReloaded, ROLE_ADMIN.oid).getId();

        when("user is modified by REPLACE deltas (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION)
                        .replace(PolyString.fromOrig("org2"))
                        .item(UserType.F_ASSIGNMENT)
                        .replaceRealValues(
                                List.of(
                                        new AssignmentType()
                                                // This is not quite correct (we should not provide PCV IDs externally)
                                                // but is meant to mislead the change determination algorithm
                                                .id(adminAssignmentId)
                                                .targetRef(ROLE_ADMIN.ref())
                                                .activation(new ActivationType()
                                                        .validFrom("2000-01-01+00:00")),
                                        new AssignmentType().targetRef(ROLE_DEVELOPER.ref())))
                        .asObjectDelta(user.getOid())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display();
                //.assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .end()
                .record(1)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Modified");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED)
                .display()
                .assertRecords(3)
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_OLD_VALUES, a -> a.contains("admin"))
                .assertValue(C_NEW_VALUES,
                        a -> a.contains("developer")
                                .contains("admin")
                                .contains("00"))
                .assertValue(C_VALUES_ADDED, a -> a.contains("developer"))
                .assertValue(C_VALUES_DELETED, "")
                .end()
                .record(1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "organization")
                .assertValue(C_OLD_VALUES, "org1")
                .assertValue(C_NEW_VALUES, "org2")
                .assertValue(C_VALUES_ADDED, "org2")
                .assertValue(C_VALUES_DELETED, "org1")
                .end()
                .record(2)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_ITEM_CHANGED, "association")
                .assertValue(C_OLD_VALUES, "group: admin")
                .assertValue(C_NEW_VALUES,
                        a -> a.contains("group: developer")
                                .contains("group: admin"))
                .assertValue(C_VALUES_ADDED, "group: developer")
                .assertValue(C_VALUES_DELETED, "")
                .end();

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .display()
                .assertRecords(5)
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, a -> a.contains("developer"))
                .end()
                .record(1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "MODIFIED")
                .assertValue(C_VALUE, a -> a.contains("admin"))
                // we have no "old"/"new" value here, so we cannot see what the change was
                .end()
                .record(2)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "organization")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "org2")
                .end()
                .record(3)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "organization")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "org1")
                .end()
                .record(4)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_ITEM_CHANGED, "association")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "group: developer")
                .end();
    }
}
