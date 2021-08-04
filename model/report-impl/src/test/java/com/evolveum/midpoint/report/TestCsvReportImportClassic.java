/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Supplier;

import static org.testng.AssertJUnit.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportImportClassic extends EmptyReportIntegrationTest {

    private static final File TEST_DIR_REPORTS = new File("src/test/resources/reports");
    private static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    private static final File EXPORT_DIR = new File("target/midpoint-home/export");

    private static final TestResource<TaskType> TASK_IMPORT_USERS_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "task-import-users-classic-with-view.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f12");
    private static final TestResource<TaskType> TASK_REIMPORT_USER_WILL = new TestResource<>(TEST_DIR_REPORTS,
            "task-reimport-user-will.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f19");
    private static final TestResource<TaskType> TASK_REIMPORT_EXPORT_USER_WILL = new TestResource<>(TEST_DIR_REPORTS,
            "task-reimport-export-user-will.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f29");
    private static final TestResource<TaskType> TASK_IMPORT_WITH_SCRIPT = new TestResource<>(TEST_DIR_REPORTS,
            "task-import-with-script.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f14");

    private static final TestResource<ReportType> REPORT_IMPORT_USERS_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "report-import-object-collection-with-view.xml", "2b77aa2e-dd86-4842-bcf5-762c8a9a85de");
    private static final TestResource<ReportType> REPORT_REIMPORT_USERS_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "report-object-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a851a");
    private static final TestResource<ReportType> REPORT_IMPORT_WITH_SCRIPT_CLASSIC = new TestResource<>(TEST_DIR_REPORTS,
            "report-with-import-script.xml", "2b44aa2e-dd86-4842-bcf5-762c8c4a851a");

    private static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS_WITH_VIEW = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-user-with-view.xml", "11b1f98e-f587-4b9f-b92b-72e251dbb266");
    private static final TestResource<ObjectCollectionType> OBJECT_COLLECTION_ALL_USERS = new TestResource<>(TEST_DIR_COMMON,
            "object-collection-all-user.xml", "72b1f98e-f587-4b9f-b92b-72e251dbb266");

    private static final TestResource<ObjectCollectionType> USER_WILL = new TestResource<>(TEST_DIR_COMMON,
            "user-will.xml", "c0c010c0-d34d-b33f-f00d-111111111122");
    private static final TestResource<ObjectCollectionType> USER_JACK = new TestResource<>(TEST_DIR_COMMON,
            "user-jack.xml", "c0c010c0-d34d-b33f-f00d-111111111111");

    private static final String REPORT_DATA_TEST100_OID = "2b77aa2e-dd86-4842-bcf5-762c8a9a8588";
    private static final String REPORT_DATA_TEST101_OID = "2b77aa2e-dd86-4842-bcf5-762c8a9a8589";
    private static final String REPORT_DATA_TEST102_OID = "3b77aa2e-dd86-4842-bcf5-762c8a9a8588";

    private static final String IMPORT_USERS_FILE_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/import/import-users.csv";
    public static final String IMPORT_MODIFY_FILE_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/import/import-modify-user.csv";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
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
        reportData.asPrismObject().setPrismContext(prismContext);
        reportData.setOid(REPORT_DATA_TEST100_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_IMPORT_USERS_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(IMPORT_USERS_FILE_PATH);

        addObject(reportData.asPrismObject());
        addObject(OBJECT_COLLECTION_ALL_USERS_WITH_VIEW.file);
        addObject(REPORT_IMPORT_USERS_CLASSIC.file);
        addTask(TASK_IMPORT_USERS_CLASSIC, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_USERS_CLASSIC.oid);

        then();

        assertTask(TASK_IMPORT_USERS_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<UserType> user1 = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", user1);
        assertEquals(ActivationStatusType.ENABLED, getValueOrNull(() -> user1.asObjectable().getActivation().getAdministrativeStatus()));
        assertEquals("2020-07-07T00:00:00.000+02:00", getValueOrNull(() -> user1.asObjectable().getActivation().getValidFrom().toString()));
        assertEquals("sub1", getValueOrNull(() -> user1.asObjectable().getSubtype().get(0)));
        assertEquals("sub22", getValueOrNull(() -> user1.asObjectable().getSubtype().get(1)));
        assertEquals("Test import: test_NICK", getValueOrNull(() ->  user1.asObjectable().getNickName().getOrig()));
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

        addObject(USER_WILL, task, result);
        addObject(OBJECT_COLLECTION_ALL_USERS, task, result);
        addObject(REPORT_REIMPORT_USERS_CLASSIC, task, result);
        addTask(TASK_REIMPORT_EXPORT_USER_WILL, result);
        PrismObject<UserType> oldWill = getObject(UserType.class, USER_WILL.oid);

        waitForTaskCloseOrSuspend(TASK_REIMPORT_EXPORT_USER_WILL.oid);
        assertTask(TASK_REIMPORT_EXPORT_USER_WILL.oid, "after")
                .assertSuccess();

        modifyObjectReplaceProperty(ReportType.class, REPORT_REIMPORT_USERS_CLASSIC.oid,
                ItemPath.create(ReportType.F_BEHAVIOR, ReportBehaviorType.F_DIRECTION), task, result, DirectionTypeType.IMPORT);
        deleteObject(UserType.class, USER_WILL.oid);

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_REIMPORT_USERS_CLASSIC.oid);
        File outputFile = findOutputFile(report);

        ReportDataType reportData = new ReportDataType();
        PolyStringType name = new PolyStringType(getTestNameShort());
        name.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(getTestNameShort()));
        reportData.setName(name);
        reportData.asPrismObject().setPrismContext(prismContext);
        reportData.setOid(REPORT_DATA_TEST101_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_REIMPORT_USERS_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(outputFile.getAbsolutePath());
        addObject(reportData.asPrismObject());
        addTask(TASK_REIMPORT_USER_WILL, result);

        when();

        waitForTaskCloseOrSuspend(TASK_REIMPORT_USER_WILL.oid);

        then();

        outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName()));
        assertTask(TASK_REIMPORT_USER_WILL.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<UserType> newWill = searchObjectByName(UserType.class, "will");
        assertNotNull("User will was not created", newWill);
        assertEquals(null, newWill.asObjectable().getTelephoneNumber());
        assertEquals(oldWill.asObjectable().getGivenName(), newWill.asObjectable().getGivenName());
        assertEquals(oldWill.asObjectable().getFamilyName(), newWill.asObjectable().getFamilyName());
        assertEquals(oldWill.asObjectable().getFullName(), newWill.asObjectable().getFullName());
        assertEquals(oldWill.asObjectable().getEmailAddress(), newWill.asObjectable().getEmailAddress());
    }

    @Test(dependsOnMethods = {"test100ImportUsers"}, priority = 102)
    public void test102ImportWithImportScript() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_JACK, task, result);

        PrismObject<UserType> testUser02 = searchObjectByName(UserType.class, "testUser02");
        assertNotNull("User testUser02 was not created", testUser02);
        assertTrue(testUser02.asObjectable().getAssignment().isEmpty());

        PrismObject<UserType> testUser01 = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", testUser01);
        assertTrue(testUser01.asObjectable().getAssignment().size() == 2);

        PrismObject<UserType> jack = searchObjectByName(UserType.class, "jack");
        assertNotNull("User jack was not created", jack);
        assertNull(jack.asObjectable().getAssignment().get(0).getActivation().getValidFrom());
        assertNull(jack.asObjectable().getAssignment().get(0).getActivation().getValidTo());

        ReportDataType reportData = new ReportDataType();
        PolyStringType name = new PolyStringType(getTestNameShort());
        name.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(getTestNameShort()));
        reportData.setName(name);
        reportData.asPrismObject().setPrismContext(prismContext);
        reportData.setOid(REPORT_DATA_TEST102_OID);
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(REPORT_IMPORT_WITH_SCRIPT_CLASSIC.oid);
        reportData.setParentRef(ref);
        reportData.setFilePath(IMPORT_MODIFY_FILE_PATH);

        addObject(reportData.asPrismObject());
        addObject(REPORT_IMPORT_WITH_SCRIPT_CLASSIC, task, result);
        addTask(TASK_IMPORT_WITH_SCRIPT, result);

        when();

        waitForTaskCloseOrSuspend(TASK_IMPORT_WITH_SCRIPT.oid);

        then();

        assertTask(TASK_IMPORT_WITH_SCRIPT.oid, "after")
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
        assertTrue(testUser01.asObjectable().getAssignment().size() == 1);
        assertEquals("00000000-0000-0000-0000-000000000008", testUser01.asObjectable().getAssignment().get(0).getTargetRef().getOid());

        jack = searchObjectByName(UserType.class, "jack");
        assertNotNull("User jack was not created", jack);
        assertEquals("00000000-0000-0000-0000-000000000004", jack.asObjectable().getAssignment().get(0).getTargetRef().getOid());
        assertEquals(validFrom, jack.asObjectable().getAssignment().get(0).getActivation().getValidFrom());
        assertEquals(validTo, jack.asObjectable().getAssignment().get(0).getActivation().getValidTo());
    }

    private Object getValueOrNull(Supplier<Object> function){
        try {
            return function.get();
        } catch (Exception e) {
            return null;
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
            throw new IllegalStateException("Found more than one output files for " + report + ": " + Arrays.toString(matchingFiles));
        }
        return matchingFiles[0];
    }
}
