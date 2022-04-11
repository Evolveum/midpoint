/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests internal correlation in the "medium" case:
 *
 * 1. Three source resources (SIS, HR, External)
 * 2. Personal data are taken from SIS (if present), then from HR (if present), and finally from External.
 *
 * Correlation rules are "as usual" - based on given name, family name, date of birth, and national ID:
 *
 * 1. if family name, date of birth, and national ID match, then the person matches without further questions,
 * 2. if given name, family name, and date of birth match, then the operator should decide,
 * 3. if national ID matches, then the operator should decide.
 */
public class TestInternalCorrelationMedium extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "internal/medium");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<FunctionLibraryType> FUNCTION_LIBRARY_MYLIB =
            new TestResource<>(TEST_DIR, "function-library-mylib.xml", "5471af53-3e47-4143-99af-630de9d56730");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "object-template-user.xml", "297208c0-7928-49e7-8990-131a20fc2dd8");

    private static final CsvResource RESOURCE_SIS = new CsvResource(TEST_DIR, "resource-sis.xml",
            "83de4034-775a-4ead-829b-a4041620d4c2", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId");
    private static final CsvResource RESOURCE_HR = new CsvResource(TEST_DIR, "resource-hr.xml",
            "180b27fe-3529-4e0d-985e-a59f09ffd1cc", "resource-hr.csv",
            "HR_ID,FIRSTN,LASTN,DOB,NATIDENT");
    private static final CsvResource RESOURCE_EXTERNAL = new CsvResource(TEST_DIR, "resource-external.xml",
            "284faaa3-5959-4825-b779-7b9b957230d3", "resource-external.csv",
            "EXT_ID,FIRSTN,LASTN,DOB,NATIDENT"); // schema similar to HR

    private static final TestTask TASK_IMPORT_SIS = new TestTask(TEST_DIR, "task-import-sis.xml",
            "70afdfcc-7dce-44fe-af31-bd504ae8d0e1", 30000);
    private static final TestTask TASK_IMPORT_HR = new TestTask(TEST_DIR, "task-import-hr.xml",
            "02478e69-b439-42a3-a654-f4fabba56dd9", 30000);
    private static final TestTask TASK_IMPORT_EXTERNAL = new TestTask(TEST_DIR, "task-import-external.xml",
            "27fb5306-48fc-452b-8300-b041326a1f1f", 30000);

    private String johnOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);
        addObject(FUNCTION_LIBRARY_MYLIB, initTask, initResult);

        RESOURCE_SIS.initializeAndTest(this, initTask, initResult);
        RESOURCE_HR.initializeAndTest(this, initTask, initResult);
        RESOURCE_EXTERNAL.initializeAndTest(this, initTask, initResult);

        TASK_IMPORT_SIS.initialize(this, initTask, initResult);
        TASK_IMPORT_HR.initialize(this, initTask, initResult);
        TASK_IMPORT_EXTERNAL.initialize(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Imports John from SIS. It's the only user, so no correlation conflicts here.
     */
    @Test
    public void test100ImportJohnFromSis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("John is in SIS as #1");
        RESOURCE_SIS.appendLine("1,John,Smith,2004-02-06,040206/1328");

        when("import task from SIS is run");
        TASK_IMPORT_SIS.rerun(result);

        then("John should be imported");

        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 0);
        // @formatter:on

        johnOid = assertUserAfterByUsername("smith1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertLinks(1, 0)
                .assertExtensionValue("sisId", "1")
                .assertExtensionValue("sisGivenName", "John")
                .assertExtensionValue("sisFamilyName", "Smith")
                .assertExtensionValue("sisDateOfBirth", "2004-02-06")
                .assertExtensionValue("sisNationalId", "040206/1328")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328")
                .getOid();
    }

    /**
     * Imports John from HR. The National ID does not match, so manual correlation is needed.
     */
    @Test
    public void test110ImportJohnFromHrDisputed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("John is in HR as #A1001");
        RESOURCE_HR.appendLine("A1001,John,Smith,2004-02-06,040206/132x");

        when("import task from HR is run");
        TASK_IMPORT_HR.rerun(result);

        then("Correlation case should be created");

        // @formatter:off
        TASK_IMPORT_HR.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 0);
        // @formatter:on

        PrismObject<ShadowType> a1001 = findShadowByPrismName("A1001", RESOURCE_HR.getObject(), result);
        assertShadowAfter(a1001)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(a1001.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();

        assertUserAfterByUsername("smith1")
                .assertLinks(1, 0); // The account should not be linked yet

        when("resolving the case");
        resolveCase(aCase, johnOid, task, result);

        then("John should be updated");
        assertUserAfterByUsername("smith1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertLinks(2, 0)
                .assertExtensionValue("hrId", "A1001")
                .assertExtensionValue("hrGivenName", "John")
                .assertExtensionValue("hrFamilyName", "Smith")
                .assertExtensionValue("hrDateOfBirth", "2004-02-06")
                .assertExtensionValue("hrNationalId", "040206/132x")
                .assertExtensionValue("sisId", "1")
                .assertExtensionValue("sisGivenName", "John")
                .assertExtensionValue("sisFamilyName", "Smith")
                .assertExtensionValue("sisDateOfBirth", "2004-02-06")
                .assertExtensionValue("sisNationalId", "040206/1328")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328");
    }

    /**
     * Imports John from External. Here we match on the HR version of the data (matching National ID but not the date of birth).
     */
    @Test
    public void test120ImportJohnFromExternalDisputed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("John is in External as #X1");
        RESOURCE_EXTERNAL.appendLine("X1,John,Smith,2004-02-26,040206/132x");

        when("import task from EXTERNAL is run");
        TASK_IMPORT_EXTERNAL.rerun(result);

        then("Correlation case should be created");

        // @formatter:off
        TASK_IMPORT_EXTERNAL.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 0);
        // @formatter:on

        PrismObject<ShadowType> x1 = findShadowByPrismName("X1", RESOURCE_EXTERNAL.getObject(), result);
        assertShadowAfter(x1)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(x1.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();

        assertUserAfterByUsername("smith1")
                .assertLinks(2, 0); // The account should not be linked yet

        when("resolving the case");
        resolveCase(aCase, johnOid, task, result);

        then("John should be updated");
        assertUserAfterByUsername("smith1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertLinks(3, 0)
                .assertExtensionValue("externalId", "X1")
                .assertExtensionValue("externalGivenName", "John")
                .assertExtensionValue("externalFamilyName", "Smith")
                .assertExtensionValue("externalDateOfBirth", "2004-02-26")
                .assertExtensionValue("externalNationalId", "040206/132x")
                .assertExtensionValue("hrId", "A1001")
                .assertExtensionValue("hrGivenName", "John")
                .assertExtensionValue("hrFamilyName", "Smith")
                .assertExtensionValue("hrDateOfBirth", "2004-02-06")
                .assertExtensionValue("hrNationalId", "040206/132x")
                .assertExtensionValue("sisId", "1")
                .assertExtensionValue("sisGivenName", "John")
                .assertExtensionValue("sisFamilyName", "Smith")
                .assertExtensionValue("sisDateOfBirth", "2004-02-06")
                .assertExtensionValue("sisNationalId", "040206/1328")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328");
    }
}
