/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;

import com.evolveum.midpoint.prism.PrismObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportExportClassic extends TestCsvReport {

    private static final File TEST_DIR = new File("src/test/resources/reports");

    private static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_USERS = new TestResource<>(TEST_DIR,
            "report-object-collection-users.xml", "64e13165-21e5-419a-8d8b-732895109f84");

    private static final int USERS = 50;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(TASK_EXPORT_CLASSIC, initResult);

        createUsers(USERS, initResult);
    }

    @Test
    public void test100ExportUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(REPORT_OBJECT_COLLECTION_USERS.file);
        runExportTask(REPORT_OBJECT_COLLECTION_USERS, result);

        when();

        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);

        then();

        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_USERS.oid);
        basicCheckOutputFile(report, 52, 2, null);
    }
}
