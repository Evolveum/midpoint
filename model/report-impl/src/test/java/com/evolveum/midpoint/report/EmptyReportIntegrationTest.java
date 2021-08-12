/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.report.AbstractReportIntegrationTest.*;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Common superclass for "empty" report integration tests.
 *
 * VERY EXPERIMENTAL
 *
 * TODO reconsider
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class EmptyReportIntegrationTest extends AbstractModelIntegrationTest {

    static final File TEST_DIR_REPORTS = new File("src/test/resources/reports");
    static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    private static final File EXPORT_DIR = new File("target/midpoint-home/export");

    //    private static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_USERS = new TestResource<>(TEST_DIR_REPORTS,
//            "report-object-collection-users.xml", "64e13165-21e5-419a-8d8b-732895109f84");
    static final TestResource<ReportType> REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN = new TestResource<>(TEST_DIR_REPORTS,
            "report-audit-collection-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85bc");
    static final TestResource<ReportType> REPORT_AUDIT_COLLECTION_WITH_VIEW = new TestResource<>(TEST_DIR_REPORTS,
            "report-audit-collection-with-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85cd");
    static final TestResource<ReportType> REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW = new TestResource<>(TEST_DIR_REPORTS,
            "report-audit-collection-with-double-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85fg");
    static final TestResource<ReportType> REPORT_AUDIT_COLLECTION_WITH_CONDITION = new TestResource<>(TEST_DIR_REPORTS,
            "report-audit-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85rr");
    static final TestResource<ReportType> REPORT_AUDIT_COLLECTION_EMPTY = new TestResource<>(TEST_DIR_REPORTS,
            "report-audit-collection-empty.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85qf");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ab");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_VIEW = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85de");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-double-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ef");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_FILTER = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-filter.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85gh");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-filter-and-basic-collection.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85hi");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_CONDITION = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a851a");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_EMPTY = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-empty.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85sq");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-filter-and-basic-collection-without-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85r7");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_PARAM = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-param.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ew");
    static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-subreport-param.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85qq");
    static final TestResource<ReportType> REPORT_USER_LIST = new TestResource<>(TEST_DIR_REPORTS,
            "report-user-list.xml", "00000000-0000-0000-0000-000000000110");
    static final TestResource<ReportType> REPORT_USER_LIST_SCRIPT = new TestResource<>(TEST_DIR_REPORTS,
            "report-user-list-script.xml", "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80");
    static final TestResource<ReportType> REPORT_DASHBOARD_WITH_DEFAULT_COLUMN = new TestResource<>(TEST_DIR_REPORTS,
            "report-dashboard-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a8582");

    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_AUDIT_RECORDS = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-audit-records.xml", "00000000-0000-0000-0001-000000001234");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_AUDIT_RECORDS_WITH_VIEW = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-audit-records-with-view.xml", "11000000-0000-0000-0001-000000001234");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_AUDIT_EMPTY = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-audit-empty.xml", "11000000-0000-0000-0001-000000gh1234");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-user.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb266");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_ROLES = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-role.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb255");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS_WITH_VIEW = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-user-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb266");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_BASIC_FILTER = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-basic-filter.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb299");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_EMPTY = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-empty.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb201");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_RESOURCE = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-resource.xml", "00000000-0000-0000-0001-000000000006");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_ASSIGNMENT = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-assignment-holder.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb775");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_TASK = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-task.xml", "00000000-0000-0000-0001-000000000007");
    static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_SHADOW_OF_RESOURCE = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-shadow-of-resource.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb244");

    static final TestResource<ObjectCollectionType> DASHBOARD_DEFAULT_COLUMNS = new TestResource<>(TEST_DIR_COMMON,
            "dashboard-default-columns.xml", "00000000-0000-0000-0001-000000001231");

    static final TestResource<ObjectCollectionType> USER_WILL = new TestResource<>(TEST_DIR_COMMON,
            "user-will.xml", "c0c010c0-d34d-b33f-f00d-111111111122");
    static final TestResource<ObjectCollectionType> USER_JACK = new TestResource<>(TEST_DIR_COMMON,
            "user-jack.xml", "c0c010c0-d34d-b33f-f00d-111111111111");

    static final TestResource<TaskType> TASK_EXPORT_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "task-export.xml", "d3a13f2e-a8c0-4f8c-bbf9-e8996848bddf");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);
    }

    // TODO deduplicate
    void commonInitialization(OperationResult initResult)
            throws CommonException, EncryptionException, IOException {
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RepoAddOptions.createOverwrite(), false, initResult);

        modelService.postInit(initResult);
        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        login(userAdministrator);
    }

    void createUsers(int users, OperationResult initResult) throws CommonException {
        for (int i = 0; i < users; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format("u%06d", i))
                    .givenName(String.format("GivenNameU%06d", i))
                    .familyName(String.format("FamilyNameU%06d", i))
                    .fullName(String.format("FullNameU%06d", i))
                    .emailAddress(String.format("EmailU%06d@test.com", i));
            repositoryService.addObject(user.asPrismObject(), null, initResult);
        }
        System.out.printf("%d users created", users);
    }

    List<String> getLinesOfOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ParseException {
        File outputFile = findOutputFile(report);
        displayValue("Found report file", outputFile);
        assertNotNull("No output file for " + report, outputFile);
        List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
        displayValue("Report content (" + lines.size() + " lines)", String.join("\n", lines));
        outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName()));
        return lines;
    }

    File findOutputFile(PrismObject<ReportType> report) throws ParseException {
        String filePrefix = report.getName().getOrig();
        File[] matchingFiles = EXPORT_DIR.listFiles((dir, name) -> name.startsWith(filePrefix));
        if (matchingFiles.length == 0) {
            return null;
        }
        if (matchingFiles.length > 1) {
            Date date = null;
            for (File file : matchingFiles) {
                String name = file.getName();
                String stringDate = name.substring(0, name.lastIndexOf("."))
                        .substring(name.substring(0, name.lastIndexOf(" ")).lastIndexOf(" "));

                Date fileDate = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss.SSS").parse(stringDate);
                if (date == null || date.before(fileDate)){
                    date = fileDate;
                }
            }
            if (date == null) {
                throw new IllegalStateException(
                        "Found more than one output files for " + report + ": " + Arrays.toString(matchingFiles));
            }
        }
        return matchingFiles[0];
    }

    void changeTaskReport(TestResource<ReportType> reportResource, ItemPath reportRefPath, TestResource<TaskType> taskResource) throws CommonException{
        Task task = getTestTask();
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(reportResource.oid);
        modifyObjectReplaceReference(TaskType.class,
                taskResource.oid,
                reportRefPath,
                task,
                task.getResult(),
                ref
                );
    }

    void runExportTask(TestResource<ReportType> reportResource, OperationResult result) throws CommonException {
        modifyObjectReplaceContainer(ReportType.class,
                reportResource.oid,
                ReportType.F_FILE_FORMAT,
                getTestTask(),
                result,
                getFileFormatConfiguration()
        );
        changeTaskReport(reportResource,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_EXPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_REF
                        ),
                TASK_EXPORT_CLASSIC);
        rerunTask(TASK_EXPORT_CLASSIC.oid, result);
    }

    protected abstract FileFormatConfigurationType getFileFormatConfiguration();
}
