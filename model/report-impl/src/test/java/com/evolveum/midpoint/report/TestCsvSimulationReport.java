/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvSimulationReport extends TestCsvReport {

    private static final int EXISTING_USERS = 10;

    private static final int COLUMNS = 7;

    private static final TestResource<ReportType> REPORT_SIMULATION_BASIC = new TestResource<>(
            TEST_DIR_REPORTS, "report-simulation-basic.xml", "ee0e28ae-2827-4a69-bd5b-35b478cc2f5f");

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

        repoAdd(TASK_EXPORT_CLASSIC, initResult);
        repoAdd(REPORT_SIMULATION_BASIC, initResult);
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

        ReportParameterType parameters = getParameters(
                "simulationResultOid", String.class, simulationResult.getSimulationResultOid());
        var rows = testClassicExport(REPORT_SIMULATION_BASIC, users + 1, COLUMNS, null, parameters);

        // Assuming nice sequential ordering of processed records (may require specifying it in the report)
        assertCsv(rows, "after")
                .removeStandardFooter()
                .parse()
                .display()
                .row(0)
                .assertValue(2, "new-0000")
                .assertValue(4, "Added")
                .assertValue(5, "UserType")
                .assertValues(6, "Focus activated")
                .end();
    }

    @Test
    public void test110DisableAndRenameUsers() throws Exception {
        int users = 10;

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
                                        .item(UserType.F_NAME)
                                        .replace(PolyString.fromOrig(existingUser.getName().getOrig() + "-renamed"))
                                        .item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                                        .replace(i%2 == 0 ? ActivationStatusType.DISABLED : null)
                                        .asObjectDelta(existingUser.getOid())),
                                null, task, result);
                    }
                });

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .assertSize(EXISTING_USERS);

        ReportParameterType parameters = getParameters(
                "simulationResultOid", String.class, simulationResult.getSimulationResultOid());
        var rows = testClassicExport(REPORT_SIMULATION_BASIC, users + 1, COLUMNS, null, parameters);

        // Assuming nice sequential ordering of processed records (may require specifying it in the report)
        assertCsv(rows, "after")
                .removeStandardFooter()
                .parse()
                .display()
                .row(0)
                .assertValue(2, "existing-0000-renamed")
                .assertValue(3, existingUsers.get(0).getOid())
                .assertValue(4, "Modified")
                .assertValue(5, "UserType")
                .assertValues(6, "Focus renamed", "Focus deactivated")
                .end()
                .row(1)
                .assertValue(2, "existing-0001-renamed")
                .assertValue(3, existingUsers.get(1).getOid())
                .assertValue(4, "Modified")
                .assertValue(5, "UserType")
                .assertValues(6, "Focus renamed");
    }
}
