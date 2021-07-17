/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportImportClassic extends EmptyReportIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reports");

    private static final TestResource<TaskType> TASK_IMPORT_USERS_CLASSIC = new TestResource<>(TEST_DIR,
            "task-import-users-classic.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f12");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test100ImportUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addTask(TASK_IMPORT_USERS_CLASSIC, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_USERS_CLASSIC.oid);

        then();

        assertTask(TASK_IMPORT_USERS_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        // TODO assert the resulting file
    }
}
