/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportImportClassic extends TestCsvReport {

    private static final TestObject<TaskType> TASK_IMPORT_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "task-import.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f12");

    private static final TestObject<ReportType> REPORT_IMPORT_USERS_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-import-object-collection-with-view.xml", "2b77aa2e-dd86-4842-bcf5-762c8a9a85de");
    private static final TestObject<ReportType> REPORT_REIMPORT_USERS_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a851a");
    private static final TestObject<ReportType> REPORT_IMPORT_WITH_SCRIPT_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-with-import-script.xml", "2b44aa2e-dd86-4842-bcf5-762c8c4a851a");

    private static final String REPORT_DATA_TEST100_OID = "2b77aa2e-dd86-4842-bcf5-762c8a9a8588";
    private static final String REPORT_DATA_TEST101_OID = "2b77aa2e-dd86-4842-bcf5-762c8a9a8589";
    private static final String REPORT_DATA_TEST102_OID = "3b77aa2e-dd86-4842-bcf5-762c8a9a8588";

    private static final String IMPORT_USERS_FILE_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/import/import-users.csv";
    private static final String IMPORT_MODIFY_FILE_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/import/import-modify-user.csv";

    // Needed by the import (to proceed without warnings)
    private static final File ROLE_END_USER_FILE = new File(TEST_DIR_COMMON, "role-end-user.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        repoAdd(TASK_IMPORT_CLASSIC, initResult);
        repoAddObjectFromFile(ROLE_END_USER_FILE, RepoAddOptions.createOverwrite(), false, initResult);
    }

    @Test(priority = 100)
    public void test100ImportUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ReportDataType reportData = new ReportDataType();
        PolyStringType name = new PolyStringType(getTestNameShort());
        name.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(getTestNameShort()));
        reportData.setName(name);
        reportData.setOid(REPORT_DATA_TEST100_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_IMPORT_USERS_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(IMPORT_USERS_FILE_PATH);

        addObject(reportData.asPrismObject());
        addObject(OBJECT_COLLECTION_ALL_USERS_WITH_VIEW, task, result);
        addObject(REPORT_IMPORT_USERS_CLASSIC, task, result);
        runImportTask(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST100_OID, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();

        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_IMPORT_CLASSIC_TASK.value());

        PrismObject<UserType> user1 = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", user1);
        assertEquals(ActivationStatusType.ENABLED, getValueOrNull(() -> user1.asObjectable().getActivation().getAdministrativeStatus()));
        assertEquals("2020-07-07T00:00:00.000+02:00", getValueOrNull(() -> user1.asObjectable().getActivation().getValidFrom().toString()));
        assertEquals("sub1", getValueOrNull(() -> user1.asObjectable().getSubtype().get(0)));
        assertEquals("sub22", getValueOrNull(() -> user1.asObjectable().getSubtype().get(1)));
        assertEquals("Test import: test_NICK", getValueOrNull(() -> user1.asObjectable().getNickName().getOrig()));
        assertEquals("00000000-0000-0000-0000-000000000008", getValueOrNull(() -> user1.asObjectable().getAssignment().get(0).getTargetRef().getOid()));
        assertEquals("00000000-0000-0000-0000-000000000004", getValueOrNull(() -> user1.asObjectable().getAssignment().get(1).getTargetRef().getOid()));

        PrismObject<UserType> user2 = searchObjectByName(UserType.class, "testUser02");
        assertNotNull("User testUser02 was not created", user2);
        assertEquals(ActivationStatusType.ENABLED, getValueOrNull(() -> user2.asObjectable().getActivation().getAdministrativeStatus()));
        assertEquals("2020-07-07T00:00:00.000+02:00", getValueOrNull(() -> user2.asObjectable().getActivation().getValidFrom().toString()));
        assertTrue(user2.asObjectable().getSubtype().isEmpty());
        assertEquals("Test import: test_NICK2", getValueOrNull(() -> user2.asObjectable().getNickName().getOrig()));
        assertTrue(user2.asObjectable().getAssignment().isEmpty());

        PrismObject<UserType> user3 = searchObjectByName(UserType.class, "testUser03");
        assertNotNull("User testUser03 was not created", user3);
        assertEquals(ActivationStatusType.ENABLED, getValueOrNull(() -> user3.asObjectable().getActivation().getAdministrativeStatus()));
        assertEquals("2020-07-07T00:00:00.000+02:00", getValueOrNull(() -> user3.asObjectable().getActivation().getValidFrom().toString()));
        assertEquals("sub31", getValueOrNull(() -> user3.asObjectable().getSubtype().get(0)));
        assertEquals("Test import: test_NICK3", getValueOrNull(() -> user3.asObjectable().getNickName().getOrig()));
        assertTrue(user3.asObjectable().getAssignment().isEmpty());
    }

    @Test(priority = 101)
    public void test101ExportAndImportUser() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addTask(TASK_EXPORT_CLASSIC, result);

        addObject(OBJECT_COLLECTION_ALL_USERS, task, result);
        addObject(REPORT_REIMPORT_USERS_CLASSIC, task, result);
        runExportTaskClassic(REPORT_REIMPORT_USERS_CLASSIC, result);
        PrismObject<UserType> oldWill = getObject(UserType.class, USER_WILL.oid);

        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);
        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess();

        modifyObjectReplaceProperty(ReportType.class, REPORT_REIMPORT_USERS_CLASSIC.oid,
                ItemPath.create(ReportType.F_BEHAVIOR, ReportBehaviorType.F_DIRECTION), task, result, DirectionTypeType.IMPORT);
        deleteObject(UserType.class, USER_WILL.oid);

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_EXPORT_CLASSIC.oid);
        File outputFile = findReportOutputFile(reportTask, result);

        ReportDataType reportData = new ReportDataType();
        PolyStringType name = new PolyStringType(getTestNameShort());
        name.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(getTestNameShort()));
        reportData.setName(name);
        reportData.setOid(REPORT_DATA_TEST101_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_REIMPORT_USERS_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(outputFile.getAbsolutePath());
        removeLastLine(outputFile.getAbsolutePath());
        addObject(reportData.asPrismObject());
        runImportTask(REPORT_REIMPORT_USERS_CLASSIC, REPORT_DATA_TEST101_OID, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();

        assertThat(outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName())))
                .isTrue();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<UserType> newWill = searchObjectByName(UserType.class, "will");
        assertNotNull("User will was not created", newWill);
        assertThat(newWill.asObjectable().getTelephoneNumber()).isNull();
        assertEquals(oldWill.asObjectable().getGivenName(), newWill.asObjectable().getGivenName());
        assertEquals(oldWill.asObjectable().getFamilyName(), newWill.asObjectable().getFamilyName());
        assertEquals(oldWill.asObjectable().getFullName(), newWill.asObjectable().getFullName());
        assertEquals(oldWill.asObjectable().getEmailAddress(), newWill.asObjectable().getEmailAddress());
    }

    /** This removes the last line with subscription appeal. */
    private void removeLastLine(String absolutePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(absolutePath), Charset.defaultCharset());
        lines.remove(lines.size() - 1);
        FileUtils.writeLines(new File(absolutePath), Charset.defaultCharset().name(), lines);
    }

    @Test(dependsOnMethods = { "test100ImportUsers" }, priority = 102)
    public void test102ImportWithImportScript() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> testUser02 = searchObjectByName(UserType.class, "testUser02");
        assertNotNull("User testUser02 was not created", testUser02);
        assertTrue(testUser02.asObjectable().getAssignment().isEmpty());

        PrismObject<UserType> testUser01 = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", testUser01);
        assertThat(testUser01.asObjectable().getAssignment()).hasSize(2);

        PrismObject<UserType> jack = searchObjectByName(UserType.class, "jack");
        assertNotNull("User jack was not created", jack);
        assertNull(jack.asObjectable().getAssignment().get(0).getActivation().getValidFrom());
        assertNull(jack.asObjectable().getAssignment().get(0).getActivation().getValidTo());

        ReportDataType reportData = new ReportDataType();
        PolyStringType name = new PolyStringType(getTestNameShort());
        name.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(getTestNameShort()));
        reportData.setName(name);
        reportData.setOid(REPORT_DATA_TEST102_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_IMPORT_WITH_SCRIPT_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(IMPORT_MODIFY_FILE_PATH);

        addObject(reportData.asPrismObject());
        addObject(REPORT_IMPORT_WITH_SCRIPT_CLASSIC, task, result);
        runImportTask(REPORT_IMPORT_WITH_SCRIPT_CLASSIC, REPORT_DATA_TEST102_OID, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();

        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = format.parse("2018-01-01");
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        XMLGregorianCalendar validFrom = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        date = format.parse("2018-05-01");
        cal.setTime(date);
        XMLGregorianCalendar validTo = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

        testUser02 = searchObjectByName(UserType.class, "testUser02");
        assertNotNull("User testUser02 was not created", testUser02);
        assertEquals("00000000-0000-0000-0000-000000000004", testUser02.asObjectable().getAssignment().get(0).getTargetRef().getOid());
        assertEquals(validFrom, testUser02.asObjectable().getAssignment().get(0).getActivation().getValidFrom());
        assertEquals(validTo, testUser02.asObjectable().getAssignment().get(0).getActivation().getValidTo());

        testUser01 = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", testUser01);
        assertThat(testUser01.asObjectable().getAssignment()).hasSize(1);
        assertEquals("00000000-0000-0000-0000-000000000008", testUser01.asObjectable().getAssignment().get(0).getTargetRef().getOid());

        jack = searchObjectByName(UserType.class, "jack");
        assertNotNull("User jack was not created", jack);
        assertEquals("00000000-0000-0000-0000-000000000004", jack.asObjectable().getAssignment().get(0).getTargetRef().getOid());
        assertEquals(validFrom, jack.asObjectable().getAssignment().get(0).getActivation().getValidFrom());
        assertEquals(validTo, jack.asObjectable().getAssignment().get(0).getActivation().getValidTo());
    }

    private void runImportTask(TestObject<ReportType> reportResource, String reportDataOid, OperationResult result) throws CommonException {
        changeImportReport(reportResource, reportDataOid);
        rerunTask(TASK_IMPORT_CLASSIC.oid, result);
    }

    private Object getValueOrNull(Supplier<Object> function) {
        try {
            return function.get();
        } catch (Exception e) {
            return null;
        }
    }

    private void changeImportReport(TestObject<ReportType> reportResource, String reportDataOid) throws CommonException {
        changeTaskReport(reportResource,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_IMPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_REF
                ),
                TASK_IMPORT_CLASSIC);
        Task task = getTestTask();
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(reportDataOid);
        modifyObjectReplaceReference(TaskType.class,
                TASK_IMPORT_CLASSIC.oid,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_IMPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_DATA_REF
                ),
                task,
                task.getResult(),
                ref
        );
    }
}
