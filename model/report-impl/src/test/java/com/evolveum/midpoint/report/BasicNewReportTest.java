/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author skublik
 */

public abstract class BasicNewReportTest extends AbstractReportIntegrationTest {

    public static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
    public static final File COLLECTION_ROLE_FILE = new File(COMMON_DIR, "object-collection-all-role.xml");
    public static final File COLLECTION_RESOURCE_FILE = new File(COMMON_DIR, "object-collection-all-resource.xml");
    public static final File COLLECTION_TASK_FILE = new File(COMMON_DIR, "object-collection-all-task.xml");
    public static final File COLLECTION_USER_FILE = new File(COMMON_DIR, "object-collection-all-user.xml");
    public static final File COLLECTION_AUDIT_FILE = new File(COMMON_DIR, "object-collection-all-audit-records.xml");
    public static final File COLLECTION_ASSIGNMENT_HOLDER_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder.xml");
    public static final File COLLECTION_SHADOW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource.xml");
    public static final File DASHBOARD_DEFAULT_COLUMNS_FILE = new File(COMMON_DIR, "dashboard-default-columns.xml");
    public static final File DASHBOARD_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-view.xml");
    public static final File COLLECTION_ROLE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-role-with-view.xml");
    public static final File COLLECTION_RESOURCE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-resource-with-view.xml");
    public static final File COLLECTION_TASK_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-task-with-view.xml");
    public static final File COLLECTION_USER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-user-with-view.xml");
    public static final File COLLECTION_AUDIT_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-audit-records-with-view.xml");
    public static final File COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder-with-view.xml");
    public static final File COLLECTION_SHADOW_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource-with-view.xml");
    public static final File DASHBOARD_TRIPLE_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-triple-view.xml");

    public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        DummyResourceContoller dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        PrismObject<ResourceType> resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        dummyResourceCtl.setResource(resourceDummy);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, initTask, initResult);
        importObjectFromFile(COLLECTION_ROLE_FILE, initResult);
        importObjectFromFile(COLLECTION_USER_FILE, initResult);
        importObjectFromFile(COLLECTION_RESOURCE_FILE, initResult);
        importObjectFromFile(COLLECTION_TASK_FILE, initResult);
        importObjectFromFile(COLLECTION_AUDIT_FILE, initResult);
        importObjectFromFile(COLLECTION_ASSIGNMENT_HOLDER_FILE, initResult);
        importObjectFromFile(COLLECTION_SHADOW_FILE, initResult);
        importObjectFromFile(DASHBOARD_DEFAULT_COLUMNS_FILE, initResult);
        importObjectFromFile(DASHBOARD_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_ROLE_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_USER_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_RESOURCE_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_TASK_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_AUDIT_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_SHADOW_WITH_VIEW_FILE, initResult);
        importObjectFromFile(DASHBOARD_TRIPLE_VIEW_FILE, initResult);
    }

    @Test
    public void test001CreateDashboardReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, getDashboardReportWithDefaultColumnOid());
        runReport(report, false);
        checkHtmlOutputFile(report);
    }

    @Test
    public void test002CreateDashboardReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, getDashboardReportWithViewOid());
        runReport(report, false);
        checkHtmlOutputFile(report);
    }

    @Test
    public void test003CreateDashboardReportWithTripleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, getDashboardReportWithTripleViewOid());
        runReport(report, false);
        checkHtmlOutputFile(report);
    }

    protected abstract String getDashboardReportWithTripleViewOid();
    protected abstract String getDashboardReportWithViewOid();
    protected abstract String getDashboardReportWithDefaultColumnOid();

    protected PrismObject<TaskType> runReport(PrismObject<ReportType> report, boolean errorOk) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reportManager.runReport(report, null, task, result);

        assertInProgress(result);

        display("Background task (running)", task);

        waitForTaskFinish(task.getOid(), false, DEFAULT_TASK_WAIT_TIMEOUT, errorOk);

        // THEN
        then();
        PrismObject<TaskType> finishedTask = getTask(task.getOid());
        display("Background task (finished)", finishedTask);

        return finishedTask;
    }

    protected void checkHtmlOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        File outputFile = findOutputFile(report);
        displayValue("Found report file", outputFile);
        assertNotNull("No output file for "+report, outputFile);
        List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
        displayValue("Report content ("+lines.size()+" lines)", String.join("\n", lines));
        outputFile.renameTo(new File(outputFile.getParentFile(), "processed-"+outputFile.getName()));

        if (lines.size() < 10) {
            fail("Html report CSV too short ("+lines.size()+" lines)");
        }
    }

    protected File findOutputFile(PrismObject<ReportType> report) {
        String filePrefix = report.getName().getOrig();
        File[] matchingFiles = EXPORT_DIR.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(filePrefix);
            }
        });
        if (matchingFiles.length == 0) {
            return null;
        }
        if (matchingFiles.length > 1) {
            throw new IllegalStateException("Found more than one output files for "+report+": "+ Arrays.toString(matchingFiles));
        }
        return matchingFiles[0];
    }
}
