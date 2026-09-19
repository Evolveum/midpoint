/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

/**
 * Tests XLSX import for classic report tasks.
 *
 * Similar to {@link TestCsvReportImportClassic}; the input files are the CSV test resources converted to XLSX
 * (multivalues as line breaks within a cell). Also checks that the format of the report data object takes
 * precedence over the report configuration, and that wrong input is rejected.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestXlsxReportImportClassic extends EmptyReportIntegrationTest {

    private static final TestObject<TaskType> TASK_IMPORT_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "task-import.xml", "ebc7b177-7ce1-421b-8fb2-94ecd6980f12");

    private static final TestObject<ReportType> REPORT_IMPORT_USERS_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-import-object-collection-with-view.xml", "2b77aa2e-dd86-4842-bcf5-762c8a9a85de");
    private static final TestObject<ReportType> REPORT_REIMPORT_USERS_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-with-condition.xml", "2b44aa2e-dd86-4842-bcf5-762c8a9a851a");
    private static final TestObject<ReportType> REPORT_IMPORT_WITH_SCRIPT_CLASSIC = TestObject.file(TEST_DIR_REPORTS,
            "report-with-import-script.xml", "2b44aa2e-dd86-4842-bcf5-762c8c4a851a");

    private static final String REPORT_DATA_TEST100_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8100";
    private static final String REPORT_DATA_TEST110_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8110";
    private static final String REPORT_DATA_TEST120_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8120";
    private static final String REPORT_DATA_TEST130_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8130";
    private static final String REPORT_DATA_TEST140_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8140";
    private static final String REPORT_DATA_TEST200_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8200";
    private static final String REPORT_DATA_TEST210_OID = "7c11aa2e-dd86-4842-bcf5-762c8a9a8210";

    private static final File IMPORT_USERS_CSV_FILE = new File(MidPointTestConstants.TEST_RESOURCES_PATH, "import/import-users.csv");
    private static final File IMPORT_MODIFY_CSV_FILE = new File(MidPointTestConstants.TEST_RESOURCES_PATH, "import/import-modify-user.csv");

    /** Where the XLSX input files are generated. */
    private static final File XLSX_DIR = new File("target/xlsx-import");

    private static final File ROLE_END_USER_FILE = new File(TEST_DIR_COMMON, "role-end-user.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(USER_JACK, initTask, initResult);
        repoAdd(TASK_IMPORT_CLASSIC, initResult);
        repoAddObjectFromFile(ROLE_END_USER_FILE, RepoAddOptions.createOverwrite(), false, initResult);
        assertTrue(XLSX_DIR.isDirectory() || XLSX_DIR.mkdirs());
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return new FileFormatConfigurationType()
                .type(FileFormatTypeType.XLSX)
                .xlsx(new XlsxFileFormatType().multivalueDelimiter("\n"));
    }

    /**
     * Report has no file format type (CSV by default). The XLSX format of the report data object wins.
     * The XLSX configuration of the report (line break as multivalue delimiter) is still applied.
     */
    @Test(priority = 100)
    public void test100ImportUsers() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        File xlsxFile = csvToXlsx(IMPORT_USERS_CSV_FILE, "import-users.xlsx");
        addObject(createReportData(REPORT_DATA_TEST100_OID, REPORT_IMPORT_USERS_CLASSIC, xlsxFile, FileFormatTypeType.XLSX).asPrismObject());
        addObject(OBJECT_COLLECTION_ALL_USERS_WITH_VIEW, task, result);
        addObject(REPORT_IMPORT_USERS_CLASSIC, task, result);
        modifyObjectReplaceContainer(ReportType.class, REPORT_IMPORT_USERS_CLASSIC.oid, ReportType.F_FILE_FORMAT,
                task, result, new FileFormatConfigurationType().xlsx(new XlsxFileFormatType().multivalueDelimiter("\n")));

        when();
        runImportTask(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST100_OID, result);
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
        assertTrue(user2.asObjectable().getSubtype().isEmpty());
        assertEquals("Test import: test_NICK2", getValueOrNull(() -> user2.asObjectable().getNickName().getOrig()));
        assertTrue(user2.asObjectable().getAssignment().isEmpty());

        PrismObject<UserType> user3 = searchObjectByName(UserType.class, "testUser03");
        assertNotNull("User testUser03 was not created", user3);
        assertEquals("sub31", getValueOrNull(() -> user3.asObjectable().getSubtype().get(0)));
        assertEquals("Test import: test_NICK3", getValueOrNull(() -> user3.asObjectable().getNickName().getOrig()));
        assertTrue(user3.asObjectable().getAssignment().isEmpty());
    }

    /** Round trip: export users to XLSX, delete one, import the file back using the report's XLSX format. */
    @Test(priority = 110)
    public void test110ExportAndImportUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_WILL, task, result);
        addTask(TASK_EXPORT_CLASSIC, result);
        addObject(OBJECT_COLLECTION_ALL_USERS, task, result);
        addObject(REPORT_REIMPORT_USERS_CLASSIC, task, result);
        runExportTaskClassic(REPORT_REIMPORT_USERS_CLASSIC, result);
        UserType oldWill = getObject(UserType.class, USER_WILL.oid).asObjectable();
        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);
        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess();

        modifyObjectReplaceProperty(ReportType.class, REPORT_REIMPORT_USERS_CLASSIC.oid,
                ItemPath.create(ReportType.F_BEHAVIOR, ReportBehaviorType.F_DIRECTION), task, result, DirectionTypeType.IMPORT);
        deleteObject(UserType.class, USER_WILL.oid);

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_EXPORT_CLASSIC.oid);
        File outputFile = findReportOutputFile(reportTask, result);
        assertThat(outputFile.getName()).endsWith(".xlsx");
        removeLastRow(outputFile); // the subscription appeal

        addObject(createReportData(REPORT_DATA_TEST110_OID, REPORT_REIMPORT_USERS_CLASSIC, outputFile, null).asPrismObject());

        when();
        runImportTask(REPORT_REIMPORT_USERS_CLASSIC, REPORT_DATA_TEST110_OID, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<UserType> newWillUser = searchObjectByName(UserType.class, "will");
        assertNotNull("User will was not created", newWillUser);
        UserType newWill = newWillUser.asObjectable();
        assertEquals("123456", newWill.getPersonalNumber());
        assertEquals(oldWill.getFullName(), newWill.getFullName());
        assertEquals(oldWill.getEmailAddress(), newWill.getEmailAddress());
    }

    /** No view: column names come from the header row of the sheet and feed the import script. */
    @Test(dependsOnMethods = { "test100ImportUsers" }, priority = 120)
    public void test120ImportWithImportScript() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertThat(searchObjectByName(UserType.class, "testUser02").asObjectable().getAssignment()).isEmpty();
        assertThat(searchObjectByName(UserType.class, "testUser01").asObjectable().getAssignment()).hasSize(2);

        File xlsxFile = csvToXlsx(IMPORT_MODIFY_CSV_FILE, "import-modify-user.xlsx");
        addObject(createReportData(REPORT_DATA_TEST120_OID, REPORT_IMPORT_WITH_SCRIPT_CLASSIC, xlsxFile, FileFormatTypeType.XLSX).asPrismObject());
        addObject(REPORT_IMPORT_WITH_SCRIPT_CLASSIC, task, result);

        when();
        runImportTask(REPORT_IMPORT_WITH_SCRIPT_CLASSIC, REPORT_DATA_TEST120_OID, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        XMLGregorianCalendar validFrom = toXmlCalendar("2018-01-01");
        XMLGregorianCalendar validTo = toXmlCalendar("2018-05-01");

        UserType testUser02 = searchObjectByName(UserType.class, "testUser02").asObjectable();
        assertEquals("00000000-0000-0000-0000-000000000004", testUser02.getAssignment().get(0).getTargetRef().getOid());
        assertEquals(validFrom, testUser02.getAssignment().get(0).getActivation().getValidFrom());
        assertEquals(validTo, testUser02.getAssignment().get(0).getActivation().getValidTo());

        UserType testUser01 = searchObjectByName(UserType.class, "testUser01").asObjectable();
        assertThat(testUser01.getAssignment()).hasSize(1);
        assertEquals("00000000-0000-0000-0000-000000000008", testUser01.getAssignment().get(0).getTargetRef().getOid());

        UserType jack = searchObjectByName(UserType.class, "jack").asObjectable();
        assertEquals("00000000-0000-0000-0000-000000000004", jack.getAssignment().get(0).getTargetRef().getOid());
        assertEquals(validFrom, jack.getAssignment().get(0).getActivation().getValidFrom());
        assertEquals(validTo, jack.getAssignment().get(0).getActivation().getValidTo());
    }

    /** Configured multivalue delimiter: the cells keep the CSV commas and are split on them. */
    @Test(dependsOnMethods = { "test120ImportWithImportScript" }, priority = 130)
    public void test130ImportUsersWithCommaDelimiter() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        for (String name : List.of("testUser01", "testUser02", "testUser03")) {
            deleteObject(UserType.class, searchObjectByName(UserType.class, name).getOid());
        }
        modifyObjectReplaceContainer(ReportType.class, REPORT_IMPORT_USERS_CLASSIC.oid, ReportType.F_FILE_FORMAT,
                task, result, new FileFormatConfigurationType()
                        .type(FileFormatTypeType.XLSX)
                        .xlsx(new XlsxFileFormatType().multivalueDelimiter(",")));
        File xlsxFile = csvToXlsx(IMPORT_USERS_CSV_FILE, "import-users-comma.xlsx", ",");
        addObject(createReportData(REPORT_DATA_TEST130_OID, REPORT_IMPORT_USERS_CLASSIC, xlsxFile, null).asPrismObject());

        when();
        runImportTask(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST130_OID, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();

        UserType user1 = searchObjectByName(UserType.class, "testUser01").asObjectable();
        assertThat(user1.getSubtype()).containsExactly("sub1", "sub22");
        assertThat(user1.getAssignment()).hasSize(2);
        UserType user3 = searchObjectByName(UserType.class, "testUser03").asObjectable();
        assertThat(user3.getSubtype()).containsExactly("sub31");
    }

    /** Without a configured delimiter a cell is one value, even if it contains line breaks. */
    @Test(dependsOnMethods = { "test130ImportUsersWithCommaDelimiter" }, priority = 140)
    public void test140ImportWithoutDelimiterKeepsCellWhole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        deleteObject(UserType.class, searchObjectByName(UserType.class, "testUser03").getOid());
        modifyObjectReplaceContainer(ReportType.class, REPORT_IMPORT_USERS_CLASSIC.oid, ReportType.F_FILE_FORMAT,
                task, result, new FileFormatConfigurationType().type(FileFormatTypeType.XLSX));
        File xlsxFile = writeXlsx("import-user-no-delimiter.xlsx", List.of(
                List.of("Name (Collection)", "Administrative status", "Valid from", "Nick", "AssignmentOid", "Subtype"),
                List.of("testUser03", "enabled", "2020-07-07T00:00:00.000+02:00", "test_NICK3", "", "sub31\nsub32")));
        addObject(createReportData(REPORT_DATA_TEST140_OID, REPORT_IMPORT_USERS_CLASSIC, xlsxFile, null).asPrismObject());

        when();
        runImportTask(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST140_OID, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_CLASSIC.oid);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display();
        UserType user3 = searchObjectByName(UserType.class, "testUser03").asObjectable();
        assertThat(user3.getSubtype()).containsExactly("sub31\nsub32");
    }

    /** HTML cannot be imported; the task must fail before touching any object. */
    @Test(priority = 200)
    public void test200RejectHtmlInput() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyObjectReplaceContainer(ReportType.class, REPORT_IMPORT_USERS_CLASSIC.oid, ReportType.F_FILE_FORMAT,
                task, result, new FileFormatConfigurationType().type(FileFormatTypeType.HTML));
        addObject(createReportData(REPORT_DATA_TEST200_OID, REPORT_IMPORT_USERS_CLASSIC, IMPORT_USERS_CSV_FILE, null).asPrismObject());
        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        when();
        changeImportReport(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST200_OID);
        rerunTaskErrorsOk(TASK_IMPORT_CLASSIC.oid, result);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertFatalError()
                .display();
        assertEquals(usersBefore, repositoryService.countObjects(UserType.class, null, null, result));
    }

    /** An XLSX file declared as CSV must not be parsed into garbage objects. */
    @Test(priority = 210)
    public void test210RejectXlsxDeclaredAsCsv() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyObjectReplaceContainer(ReportType.class, REPORT_IMPORT_USERS_CLASSIC.oid, ReportType.F_FILE_FORMAT,
                task, result, new FileFormatConfigurationType().type(FileFormatTypeType.CSV));
        File xlsxFile = csvToXlsx(IMPORT_USERS_CSV_FILE, "import-users-as-csv.xlsx");
        addObject(createReportData(REPORT_DATA_TEST210_OID, REPORT_IMPORT_USERS_CLASSIC, xlsxFile, FileFormatTypeType.CSV).asPrismObject());
        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        when();
        changeImportReport(REPORT_IMPORT_USERS_CLASSIC, REPORT_DATA_TEST210_OID);
        rerunTaskErrorsOk(TASK_IMPORT_CLASSIC.oid, result);

        then();
        assertTask(TASK_IMPORT_CLASSIC.oid, "after")
                .assertFatalError()
                .display();
        assertEquals(usersBefore, repositoryService.countObjects(UserType.class, null, null, result));
    }

    private ReportDataType createReportData(String oid, TestObject<ReportType> report, File file, FileFormatTypeType fileFormat) {
        String name = getTestNameShort();
        PolyStringType polyName = new PolyStringType(name);
        polyName.setNorm(prismContext.getDefaultPolyStringNormalizer().normalize(name));
        return new ReportDataType()
                .oid(oid)
                .name(polyName)
                .parentRef(new ObjectReferenceType().oid(report.oid))
                .filePath(file.getAbsolutePath())
                .fileFormat(fileFormat);
    }

    private File csvToXlsx(File csvFile, String xlsxName) throws IOException {
        return csvToXlsx(csvFile, xlsxName, "\n");
    }

    /**
     * Converts a semicolon-separated CSV test resource to an XLSX file with the same rows.
     * Values separated by comma (the CSV multivalue delimiter) are joined by the given delimiter within the cell.
     */
    private File csvToXlsx(File csvFile, String xlsxName, String multivalueDelimiter) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setQuote('"').build();
        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), StandardCharsets.UTF_8);
                CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord record : parser) {
                rows.add(record.stream().map(value -> value.replace(",", multivalueDelimiter)).toList());
            }
        }
        return writeXlsx(xlsxName, rows);
    }

    private File writeXlsx(String xlsxName, List<List<String>> rows) throws IOException {
        File xlsxFile = new File(XLSX_DIR, xlsxName);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("import");
            int rowIndex = 0;
            for (List<String> values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    row.createCell(i).setCellValue(values.get(i));
                }
            }
            try (FileOutputStream out = new FileOutputStream(xlsxFile)) {
                workbook.write(out);
            }
        }
        return xlsxFile;
    }

    private void removeLastRow(File xlsxFile) throws IOException {
        XSSFWorkbook workbook;
        try (FileInputStream in = new FileInputStream(xlsxFile)) {
            workbook = new XSSFWorkbook(in);
        }
        try (workbook) {
            Sheet sheet = workbook.getSheetAt(0);
            sheet.removeRow(sheet.getRow(sheet.getLastRowNum()));
            try (FileOutputStream out = new FileOutputStream(xlsxFile)) {
                workbook.write(out);
            }
        }
    }

    private static XMLGregorianCalendar toXmlCalendar(String date) throws Exception {
        Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(parsed);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    private void runImportTask(TestObject<ReportType> reportResource, String reportDataOid, OperationResult result) throws CommonException {
        changeImportReport(reportResource, reportDataOid);
        rerunTask(TASK_IMPORT_CLASSIC.oid, result);
    }

    private void changeImportReport(TestObject<ReportType> reportResource, String reportDataOid) throws CommonException {
        changeTaskReport(reportResource,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_IMPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_REF),
                TASK_IMPORT_CLASSIC);
        Task task = getTestTask();
        modifyObjectReplaceReference(TaskType.class,
                TASK_IMPORT_CLASSIC.oid,
                ItemPath.create(TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_REPORT_IMPORT,
                        ClassicReportImportWorkDefinitionType.F_REPORT_DATA_REF),
                task,
                task.getResult(),
                new ObjectReferenceType().oid(reportDataOid));
    }

    private Object getValueOrNull(Supplier<Object> function) {
        try {
            return function.get();
        } catch (Exception e) {
            return null;
        }
    }
}
