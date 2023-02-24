/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvSimulationReport extends TestCsvReport {

    private static final int EXISTING_USERS = 10;

    private static final int OBJECT_REPORT_COLUMNS = 6;

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

        CommonInitialObjects.addMarks(this, initTask, initResult);

        existingUsers = modelObjectCreatorFor(UserType.class)
                .withObjectCount(EXISTING_USERS)
                .withNamePattern("existing-%04d")
                .execute(initResult);

        REPORT_SIMULATION_OBJECTS.init(this, initTask, initResult);
        REPORT_SIMULATION_OBJECTS_BY_MARKS.init(this, initTask, initResult);
        REPORT_SIMULATION_ITEMS_CHANGED.init(this, initTask, initResult);
        REPORT_SIMULATION_VALUES_CHANGED.init(this, initTask, initResult);
    }

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
                .sortBy(2)
                .display()
                .assertRecords(users)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(2, "new-0000")
                .assertValue(3, "UserType")
                .assertValue(4, "Added")
                .assertValues(5, "Focus activated")
                .end();

        when("object-level report is created (by marks)");
        var byMarksLines = REPORT_SIMULATION_OBJECTS_BY_MARKS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("it is OK");
        assertCsv(byMarksLines, "after")
                .sortBy(2)
                .display()
                .assertRecords(users)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(2, "new-0000")
                .assertValue(3, "UserType")
                .assertValue(4, "Added")
                .assertValues(5, "Focus activated")
                .end();
    }

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
                .sortBy(2)
                .display()
                .assertRecords(EXISTING_USERS)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(1, existingUsers.get(0).getOid())
                .assertValue(2, "existing-0000-renamed")
                .assertValue(3, "UserType")
                .assertValue(4, "Modified")
                .assertValues(5, "Focus renamed", "Focus deactivated")
                .end()
                .record(1)
                .assertValue(1, existingUsers.get(1).getOid())
                .assertValue(2, "existing-0001-renamed")
                .assertValue(3, "UserType")
                .assertValue(4, "Modified")
                .assertValues(5, "Focus renamed");

        when("object-level report (by marks) is created");
        var byMarksLines = REPORT_SIMULATION_OBJECTS_BY_MARKS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(byMarksLines, "after")
                .sortBy(2, 5)
                .display()
                .assertRecords((int) (EXISTING_USERS * 1.5))
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(1, existingUsers.get(0).getOid())
                .assertValue(2, "existing-0000-renamed")
                .assertValue(3, "UserType")
                .assertValue(4, "Modified")
                .assertValues(5, "Focus deactivated")
                .end()
                .record(1)
                .assertValue(1, existingUsers.get(0).getOid())
                .assertValue(2, "existing-0000-renamed")
                .assertValue(3, "UserType")
                .assertValue(4, "Modified")
                .assertValues(5, "Focus renamed")
                .end()
                .record(2)
                .assertValue(1, existingUsers.get(1).getOid())
                .assertValue(2, "existing-0001-renamed")
                .assertValue(3, "UserType")
                .assertValue(4, "Modified")
                .assertValues(5, "Focus renamed");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .sortBy(2, 6)
                .display()
                .assertRecords(15) // 10 changes of name, 5 changes of administrative status
                .record(0)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(6, "activation/administrativeStatus")
                .assertValue(7, "") // before
                .assertValue(8, "Disabled") // after
                .assertValue(9, "Disabled") // added
                .assertValue(10, "") // deleted
                .end()
                .record(1)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(6, "name")
                .assertValue(7, "existing-0000") // before
                .assertValue(8, "existing-0000-renamed") // after
                .assertValue(9, "existing-0000-renamed") // added
                .assertValue(10, "existing-0000") // deleted
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
                .assertRecords((a) -> a.hasSizeGreaterThan(50)); // too many

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
                .sortBy(2, 6, 7, 8)
                .display()
                .assertRecords(25) // 20x name (ADD/DELETE), 5x administrativeStatus (ADD)
                .record(0)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(6, "activation/administrativeStatus")
                .assertValue(7, "ADDED")
                .assertValue(8, "Disabled")
                .end()
                .record(1)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(6, "name")
                .assertValue(7, "ADDED")
                .assertValue(8, "existing-0000-renamed")
                .end()
                .record(2)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(6, "name")
                .assertValue(7, "DELETED")
                .assertValue(8, "existing-0000")
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
}
