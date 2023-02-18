/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Common superclass for "empty" report integration tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class EmptyReportIntegrationTest extends AbstractModelIntegrationTest {

    static final int DONT_COUNT_ROWS = -1;

    static final String DIR_REPORTS = "reports";
    static final File TEST_DIR_REPORTS = new File("src/test/resources/" + DIR_REPORTS);
    static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    private static final File EXPORT_DIR = new File("target/midpoint-home/export");

    static final TestObject<ReportType> REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN = TestObject.file(TEST_DIR_REPORTS,
            "report-audit-collection-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85bc");
    static final TestObject<ReportType> REPORT_AUDIT_COLLECTION_WITH_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-audit-collection-with-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85cd");
    static final TestObject<ReportType> REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-audit-collection-with-double-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85f9");
    static final TestObject<ReportType> REPORT_AUDIT_COLLECTION_WITH_CONDITION = TestObject.file(TEST_DIR_REPORTS,
            "report-audit-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85fa");
    static final TestObject<ReportType> REPORT_AUDIT_COLLECTION_EMPTY = TestObject.file(TEST_DIR_REPORTS,
            "report-audit-collection-empty.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85aa");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ab");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85de");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-double-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ef");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_FILTER = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-filter.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ac");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-filter-and-basic-collection.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ad");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_CONDITION = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a851a");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_EMPTY = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-empty.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85af");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-filter-and-basic-collection-without-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ae");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_PARAM = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-param.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ee");
    static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-subreport-param.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a85ed");
    static final TestObject<ReportType> REPORT_USER_LIST = TestObject.file(TEST_DIR_REPORTS,
            "report-user-list.xml", "00000000-0000-0000-0000-000000000110");
    static final TestObject<ReportType> REPORT_USER_LIST_SCRIPT = TestObject.file(TEST_DIR_REPORTS,
            "report-user-list-script.xml", "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80");
    static final TestObject<ReportType> REPORT_DASHBOARD_WITH_DEFAULT_COLUMN = TestObject.file(TEST_DIR_REPORTS,
            "report-dashboard-with-default-column.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a8582");
    static final TestObject<ReportType> REPORT_DASHBOARD_WITH_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-dashboard-with-view.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a8533");
    static final TestObject<ReportType> REPORT_DASHBOARD_WITH_TRIPLE_VIEW = TestObject.file(TEST_DIR_REPORTS,
            "report-dashboard-with-triple-view.xml", "2b87aa2e-dd86-4842-bcf5-76200a9a8533");
    static final TestObject<ReportType> REPORT_DASHBOARD_EMPTY = TestObject.file(TEST_DIR_REPORTS,
            "report-dashboard-empty.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a8eee");
    static final TestObject<ReportType> REPORT_SUBREPORT_AS_ROW_USERS = TestObject.file(TEST_DIR_REPORTS,
            "report-subreport-as-row-users.xml", "a9934d64-5e6b-4d3e-9526-e334883fff34");
    static final TestObject<ReportType> REPORT_SUBREPORT_AUDIT = TestObject.file(TEST_DIR_REPORTS,
            "report-subreport-audit.xml", "44026fc7-c73d-4210-91c3-e5d10391c02b");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_AUDIT_RECORDS = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-audit-records.xml", "00000000-0000-0000-0001-000000001234");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_AUDIT_RECORDS_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-audit-records-with-view.xml", "11000000-0000-0000-0001-000000001234");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_AUDIT_EMPTY = TestObject.file(TEST_DIR_COMMON,
            "object-collection-audit-empty.xml", "11000000-0000-0000-0001-000000aa1234");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-user.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb266");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-user-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb266");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_ROLES = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-role.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb255");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_ROLES_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-role-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb255");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_BASIC_FILTER = TestObject.file(TEST_DIR_COMMON,
            "object-collection-basic-filter.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb299");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_EMPTY = TestObject.file(TEST_DIR_COMMON,
            "object-collection-empty.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb201");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_RESOURCE = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-resource.xml", "00000000-0000-0000-0001-000000000006");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_RESOURCE_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-resource-with-view.xml", "11000000-0000-0000-0001-000000000006");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_ASSIGNMENT_HOLDER = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-assignment-holder.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb775");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_ASSIGNMENT_HOLDER_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-assignment-holder-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb775");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_TASK = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-task.xml", "00000000-0000-0000-0001-000000000007");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_ALL_TASK_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-all-task-with-view.xml", "11000000-0000-0000-0001-000000000007");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_SHADOW_OF_RESOURCE = TestObject.file(TEST_DIR_COMMON,
            "object-collection-shadow-of-resource.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb244");
    static final TestObject<ObjectCollectionType> OBJECT_COLLECTION_SHADOW_OF_RESOURCE_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "object-collection-shadow-of-resource-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb244");

    static final TestObject<ObjectCollectionType> DASHBOARD_DEFAULT_COLUMNS = TestObject.file(TEST_DIR_COMMON,
            "dashboard-default-columns.xml", "00000000-0000-0000-0001-000000001231");
    static final TestObject<ObjectCollectionType> DASHBOARD_WITH_VIEW = TestObject.file(TEST_DIR_COMMON,
            "dashboard-with-view.xml", "00000000-0000-0000-0001-000000661231");
    static final TestObject<ObjectCollectionType> DASHBOARD_WITH_TRIPLE_VIEW = TestObject.file(TEST_DIR_COMMON,
            "dashboard-with-triple-view.xml", "00000000-0000-0000-0001-000022661231");
    static final TestObject<ObjectCollectionType> DASHBOARD_EMPTY = TestObject.file(TEST_DIR_COMMON,
            "dashboard-empty.xml", "00000000-0000-0000-0001-000000aa1231");

    static final TestObject<ObjectCollectionType> USER_WILL = TestObject.file(TEST_DIR_COMMON,
            "user-will.xml", "c0c010c0-d34d-b33f-f00d-111111111122");
    static final TestObject<ObjectCollectionType> USER_JACK = TestObject.file(TEST_DIR_COMMON,
            "user-jack.xml", "c0c010c0-d34d-b33f-f00d-111111111111");

    static final TestTask TASK_EXPORT_CLASSIC = TestTask.file(TEST_DIR_REPORTS,
            "task-export.xml", "d3a13f2e-a8c0-4f8c-bbf9-e8996848bddf");

    private static final TestObject<ArchetypeType> ARCHETYPE_TASK_REPORT_EXPORT_CLASSIC = TestObject.file(TEST_DIR_COMMON,
            "archetype-task-report-export-classic.xml", "00000000-0000-0000-0000-000000000511");
    private static final TestObject<ArchetypeType> ARCHETYPE_TASK_REPORT_EXPORT_DISTRIBUTED = TestObject.file(TEST_DIR_COMMON,
            "archetype-task-report-export-distributed.xml", "00000000-0000-0000-0000-000000000512");
    private static final TestObject<ArchetypeType> ARCHETYPE_TASK_REPORT_IMPORT_CLASSIC = TestObject.file(TEST_DIR_COMMON,
            "archetype-task-report-import-classic.xml", "00000000-0000-0000-0000-000000000510");

    private static final File USER_ADMINISTRATOR_FILE = new File(TEST_DIR_COMMON, "user-administrator.xml");
    private static final File ROLE_SUPERUSER_FILE = new File(TEST_DIR_COMMON, "role-superuser.xml");
    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR_COMMON, "system-configuration.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);
    }

    private void commonInitialization(OperationResult initResult)
            throws CommonException, EncryptionException, IOException {
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RepoAddOptions.createOverwrite(), false, initResult);

        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }
        modelService.postInit(initResult);

        PrismObject<UserType> userAdministrator =
                repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        login(userAdministrator);

        repoAdd(ARCHETYPE_TASK_REPORT_EXPORT_CLASSIC, initResult);
        repoAdd(ARCHETYPE_TASK_REPORT_EXPORT_DISTRIBUTED, initResult);
        repoAdd(ARCHETYPE_TASK_REPORT_IMPORT_CLASSIC, initResult);

        activityBasedTaskHandler.setAvoidAutoAssigningArchetypes(false); // We test auto-assigning of archetypes here
    }

    void createUsers(int users, Task initTask, OperationResult initResult) throws CommonException {
        for (int i = 0; i < users; i++) {
            UserType user = new UserType()
                    .name(String.format("u%06d", i))
                    .givenName(String.format("GivenNameU%06d", i))
                    .familyName(String.format("FamilyNameU%06d", i))
                    .fullName(String.format("FullNameU%06d", i))
                    .emailAddress(String.format("EmailU%06d@test.com", i));
            addObject(user.asPrismObject(), initTask, initResult);
        }
        System.out.printf("%d users created", users);
    }

    void changeTaskReport(TestObject<ReportType> reportResource, ItemPath reportRefPath, TestObject<TaskType> taskResource) throws CommonException {
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

    void runExportTaskClassic(TestObject<ReportType> reportResource, OperationResult result) throws CommonException {
        runExportTask(TASK_EXPORT_CLASSIC, reportResource, result);
    }

    void runExportTask(TestObject<TaskType> testTask, TestObject<ReportType> testResource, OperationResult result)
            throws CommonException {
        modifyObjectReplaceContainer(ReportType.class,
                testResource.oid,
                ReportType.F_FILE_FORMAT,
                getTestTask(),
                result,
                getFileFormatConfiguration());
        changeTaskReport(testResource,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        getWorkDefinitionType(),
                        ClassicReportImportWorkDefinitionType.F_REPORT_REF),
                testTask);
        rerunTask(testTask.oid, result);
    }

    protected ItemName getWorkDefinitionType() {
        return WorkDefinitionsType.F_REPORT_EXPORT;
    }

    protected abstract FileFormatConfigurationType getFileFormatConfiguration();

    void assertNotificationMessage(ReportType report, String expectedContentType) {
        displayDumpable("dummy transport", dummyTransport);

        String reportName = report.getName().getOrig();
        assertSingleDummyTransportMessageContaining(DIR_REPORTS, "Report: " + reportName);

        Message message = dummyTransport.getMessages("dummy:" + DIR_REPORTS).get(0);
        List<NotificationMessageAttachmentType> attachments = message.getAttachments();
        assertThat(attachments).as("notification message attachments").hasSize(1);
        NotificationMessageAttachmentType attachment = attachments.get(0);
        assertThat(attachment.getContentType()).as("attachment content type").isEqualTo(expectedContentType);
    }
}
