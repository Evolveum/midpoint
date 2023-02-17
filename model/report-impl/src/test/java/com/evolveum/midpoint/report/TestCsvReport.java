/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReport extends EmptyReportIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(USER_WILL, initTask, initResult);
        addObject(USER_JACK, initTask, initResult);
    }

    List<String> basicCheckOutputFile(PrismObject<TaskType> task, int expectedRows, int expectedColumns, String lastLine)
            throws IOException, SchemaException, ObjectNotFoundException {

        if (expectedRows != DONT_COUNT_ROWS) {
            // +1 for the final line with the subscription appeal
            expectedRows += 1;
        }

        List<String> lines = getLinesOfOutputFile(task, getTestOperationResult());

        if (expectedRows != DONT_COUNT_ROWS && lines.size() != expectedRows) {
            fail("Unexpected count of rows of csv report. Expected: " + expectedRows + ", Actual: " + lines.size());
        }

        if (expectedRows == DONT_COUNT_ROWS && lines.size() < 2) {
            fail("Unexpected count of rows of csv report. Expected: more as one, Actual: " + lines.size());
        }

        int actualColumns = getNumberOfColumns(lines);
        if (actualColumns != expectedColumns) {
            fail("Unexpected count of columns of csv report. Expected: " + expectedColumns + ", Actual: " + actualColumns);
        }

        // Last content line before the subscription appeal, hence -2, not -1.
        String lastRealLine = lines.get(lines.size() - 2);
        if (StringUtils.isNoneEmpty(lastLine) && !lastRealLine.equals(lastLine)) {
            fail("Unexpected last line of csv report. Expected: '" + lastLine + "', Actual: '" + lastRealLine + "'");
        }
        return lines;
    }

    private int getNumberOfColumns(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Couldn't find line of report");
        }
        return lines.get(0).split(";").length;
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return new FileFormatConfigurationType()
                .type(FileFormatTypeType.CSV);
    }

    void assertNotificationMessage(TestObject<ReportType> reportTestResource) {
        assertNotificationMessage(reportTestResource.getObjectable(), "text/csv");
    }

    void testClassicExport(
            TestObject<ReportType> reportResource, int expectedRows, int expectedColumns,
            String lastLine, ReportParameterType parameters) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        if (parameters != null) {
            modifyObjectReplaceContainer(TaskType.class,
                    TASK_EXPORT_CLASSIC.oid,
                    ItemPath.create(TaskType.F_ACTIVITY,
                            ActivityDefinitionType.F_WORK,
                            WorkDefinitionsType.F_REPORT_EXPORT,
                            ClassicReportImportWorkDefinitionType.F_REPORT_PARAM),
                    task,
                    result,
                    parameters);
        }

        dummyTransport.clearMessages();

        when();
        runExportTaskClassic(reportResource, result);
        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);

        then();
        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value());

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_EXPORT_CLASSIC.oid);
        basicCheckOutputFile(reportTask, expectedRows, expectedColumns, lastLine);

        assertNotificationMessage(reportResource);
    }

    void testClassicExport(TestObject<ReportType> reportResource,
            int expectedRows, int expectedColumns, String lastLine)
            throws Exception {
        testClassicExport(reportResource, expectedRows, expectedColumns, lastLine, null);
    }
}
