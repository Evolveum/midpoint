/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ID Match correlation in the "medium" case:
 *
 * 1. Three source resources (SIS, HR, External)
 * 2. Personal data are taken from SIS (if present), then from HR (if present), and finally from External.
 */
public abstract class AbstractMediumIdMatchCorrelationTest extends AbstractIdMatchCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch/medium");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            TestObject.file(TEST_DIR, "object-template-user.xml", "bf275746-f2ce-4ae3-9e91-0c40e26422b7");

    public static final CsvTestResource RESOURCE_SIS = new CsvTestResource(TEST_DIR, "resource-sis.xml",
            "773991ae-4853-4e88-9cfc-b10bec750f3b", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId");
    public static final CsvTestResource RESOURCE_HR = new CsvTestResource(TEST_DIR, "resource-hr.xml",
            "084dfbfa-c465-421b-a2ac-2ab3afbf20ff", "resource-hr.csv",
            "HR_ID,FIRSTN,LASTN,DOB,NATIDENT");
    public static final CsvTestResource RESOURCE_EXTERNAL = new CsvTestResource(TEST_DIR, "resource-external.xml",
            "106c248c-ce69-4274-845f-7fb391e1545a", "resource-external.csv",
            "EXT_ID,FIRSTN,LASTN,DOB,NATIDENT"); // schema similar to HR

    private static final TestTask TASK_IMPORT_SIS = new TestTask(TEST_DIR, "task-import-sis.xml",
            "d5b49ed0-5916-4371-a7d8-67d55597c31b", 30000);
    private static final TestTask TASK_IMPORT_HR = new TestTask(TEST_DIR, "task-import-hr.xml",
            "d2e64047-6b10-49fd-98d0-af7b57228a52", 30000);
    private static final TestTask TASK_IMPORT_EXTERNAL = new TestTask(TEST_DIR, "task-import-external.xml",
            "aa7cb0bc-50ed-4f83-90ab-cb2e72f4ac2c", 30000);

    private String johnOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);

        RESOURCE_SIS.initAndTest(this, initTask, initResult);
        RESOURCE_HR.initAndTest(this, initTask, initResult);
        RESOURCE_EXTERNAL.initAndTest(this, initTask, initResult);

        TASK_IMPORT_SIS.init(this, initTask, initResult);
        TASK_IMPORT_HR.init(this, initTask, initResult);
        TASK_IMPORT_EXTERNAL.init(this, initTask, initResult);
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

        johnOid = assertUserAfter(findUserByUsernameFullRequired("smith1"))
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328")
                .assertLinks(1, 0)
                .identities()
                    .fromResource(RESOURCE_SIS.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-06")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/1328")
                    .end()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "john")
                        .assertNormalizedItem("familyName.polyStringNorm", "smith")
                        .assertNormalizedItem("dateOfBirth.polyStringNorm", "20040206")
                        .assertNormalizedItem("nationalId.polyStringNorm", "0402061328")
                    .end()
                .end()
                .getOid();
        // @formatter:on
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

        PrismObject<ShadowType> a1001 = findShadowByPrismName("A1001", RESOURCE_HR.get(), result);
        assertShadowAfter(a1001)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(a1001.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();
        assertCase(aCase, "case")
                .displayXml();

        assertUserAfterByUsername("smith1")
                .assertLinks(1, 0); // The account should not be linked yet

        when("resolving the case");
        resolveCase(aCase, johnOid, task, result);

        then("John should be updated");
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(findUserByUsernameFullRequired("smith1"))
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328")
                .assertLinks(2, 0)
                .identities()
                    .fromResource(RESOURCE_SIS.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-06")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/1328")
                    .end()
                    .fromResource(RESOURCE_HR.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-06")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/132")
                    .end()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "john")
                        .assertNormalizedItem("familyName.polyStringNorm", "smith")
                        .assertNormalizedItem("dateOfBirth.polyStringNorm", "20040206")
                        .assertNormalizedItem("nationalId.polyStringNorm", "0402061328", "040206132")
                    .end()
                .end();
        // @formatter:on
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

        PrismObject<ShadowType> x1 = findShadowByPrismName("X1", RESOURCE_EXTERNAL.get(), result);
        assertShadowAfter(x1)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(x1.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();
        assertCase(aCase, "case")
                .displayXml();

        assertUserAfterByUsername("smith1")
                .assertLinks(2, 0); // The account should not be linked yet

        when("resolving the case");
        resolveCase(aCase, johnOid, task, result);

        then("John should be updated");
        // @formatter:off
        assertUserAfter(findUserByUsernameFullRequired("smith1"))
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328")
                .assertLinks(3, 0)
                .identities()
                    .fromResource(RESOURCE_SIS.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-06")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/1328")
                    .end()
                    .fromResource(RESOURCE_HR.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-06")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/132")
                    .end()
                    .fromResource(RESOURCE_EXTERNAL.oid, ACCOUNT, INTENT_DEFAULT, null)
                        .assertDataItem(UserType.F_GIVEN_NAME, PolyString.fromOrig("John"))
                        .assertDataItem(UserType.F_FAMILY_NAME, PolyString.fromOrig("Smith"))
                        .assertDataItem(PATH_DATE_OF_BIRTH, "2004-02-26")
                        .assertDataItem(PATH_NATIONAL_ID, "040206/132")
                    .end()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "john")
                        .assertNormalizedItem("familyName.polyStringNorm", "smith")
                        .assertNormalizedItem("dateOfBirth.polyStringNorm", "20040206", "20040226")
                        .assertNormalizedItem("nationalId.polyStringNorm", "0402061328", "040206132")
                    .end()
                .end();
        // @formatter:on
    }
}
