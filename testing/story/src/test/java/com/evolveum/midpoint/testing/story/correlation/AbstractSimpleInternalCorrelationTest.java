/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests internal correlation in the most basic case:
 *
 * 1. Single authoritative resource (HR ~ CSV)
 * 2. Single target resource - presumably, LDAP-like - that is to be correlated (simulated by CSV)
 *
 * Correlation from HR is simple - it is based on `empNo` attribute vs. `employeeNumber` user property.
 *
 * Rules for correlation from target are the following:
 *
 * 1. If employeeNumber is present, it is used as an authoritative source of matching.
 * 2. If e-mail address is present, it is used as an authoritative source of matching.
 * 3. If neither 1 or 2, the surname is used - but any candidate(s) found have to be confirmed by a human operator.
 * 4. If neither 1 or 2, the phone number is used - but any candidate(s) found have to be confirmed by a human operator.
 *
 * Notes:
 *
 * - Candidates from points 3 and 4 are merged together.
 * - E-mail addresses are kept in HR for the simplicity. We assume that _some_ of the accounts
 * on target may have e-mail addresses or employee numbers present.
 */
public abstract class AbstractSimpleInternalCorrelationTest extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "internal/simple");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            TestObject.file(TEST_DIR, "object-template-user.xml", "971bc001-be37-44c3-9c46-d33e761680c9");

    private static final CsvTestResource RESOURCE_HR = new CsvTestResource(TEST_DIR, "resource-hr.xml",
            "e09ffb8a-3f16-4b72-a61c-068f0039b876", "resource-hr.csv");

    static final CsvTestResource RESOURCE_TARGET = new CsvTestResource(TEST_DIR, "resource-target.xml",
            "917e846f-39a5-423e-a63a-b00c3595da37", "resource-target.csv");

    static final CsvTestResource RESOURCE_TARGET_SIMPLIFIED = new CsvTestResource(TEST_DIR, "resource-target-simplified.xml",
            "917e846f-39a5-423e-a63a-b00c3595da37", "resource-target.csv");

    private static final TestTask TASK_IMPORT_HR = new TestTask(TEST_DIR, "task-import-hr.xml",
            "1f484a53-70c6-49d1-ba91-fac3b68eb857", 30000);

    private static final TestTask TASK_IMPORT_TARGET = new TestTask(TEST_DIR, "task-import-target.xml",
            "6613fb95-c5d5-4fed-8c33-10f4e7398247", 30000);

    private static final int HR_ACCOUNTS = 5;
    private static final int TARGET_ACCOUNTS = 6;

    private long firstTargetImportStart;

    abstract CsvTestResource getTargetResource();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);

        RESOURCE_HR.initAndTest(this, initTask, initResult);
        getTargetResource().initAndTest(this, initTask, initResult);

        TASK_IMPORT_HR.init(this, initTask, initResult);
        TASK_IMPORT_TARGET.init(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Import from HR should be straightforward: there's a simple correlation rule,
     * and the HR data are clean, so no surprises there.
     */
    @Test
    public void test100ImportFromHr() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("import task from HR is run");
        TASK_IMPORT_HR.rerun(result);

        then("users should be imported");

        // @formatter:off
        TASK_IMPORT_HR.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(HR_ACCOUNTS, 0, 0);
        // @formatter:on

        assertUserByUsername("smith1", "")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertEmployeeNumber("1")
                .assertEmailAddress("jsmith1@evolveum.com")
                .assertTelephoneNumber("+421-123-456-001")
                .assertLinks(1, 0);
    }

    /**
     * First import from the target. Inbounds are turned off, so no data flow should occur.
     * We do the import in order to correlate the accounts.
     */
    @Test
    public void test200FirstImportFromTarget() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("import task (dry run) from target is run");
        firstTargetImportStart = System.currentTimeMillis();
        TASK_IMPORT_TARGET.rerun(result);
        long firstTargetImportEnd = System.currentTimeMillis();

        then("task is OK, all accounts are processed");

        // @formatter:off
        TASK_IMPORT_TARGET.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(TARGET_ACCOUNTS, 0, 0);
        // @formatter:on

        and("first two users should be correlated (by employee number, by email) and linked");

        assertTargetLinked("smith1", "js1", result);
        assertTargetLinked("smith2", "js2", result);

        and("next two users should be disputed with 1 candidate each (by family name, by phone #)");

        PrismObject<ShadowType> ag3 = getTargetShadow("ag3", result);
        assertShadowAfter(ag3)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertSynchronizationSituation(SynchronizationSituationType.DISPUTED)
                .assertCorrelationStartTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertCorrelationCaseOpenTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertNoCorrelationCaseCloseTimestamp()
                .assertNoCorrelationEndTimestamp();

        assertUserAfterByUsername("green1")
                .assertLinks(1, 0);

        PrismObject<ShadowType> bb4 = getTargetShadow("bb4", result);
        assertShadowAfter(bb4)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertSynchronizationSituation(SynchronizationSituationType.DISPUTED);

        assertUserAfterByUsername("black1")
                .assertLinks(1, 0);

        CaseType ag3case = correlationCaseManager.findCorrelationCase(ag3.asObjectable(), true, result);
        assertCaseAfter(ag3case)
                .assertOpen();

        CaseType bb4case = correlationCaseManager.findCorrelationCase(bb4.asObjectable(), true, result);
        assertCaseAfter(bb4case)
                .assertOpen();

        and("5th user (rb5) should be disputed with 2 candidates (by family name)");

        PrismObject<ShadowType> rb5 = getTargetShadow("rb5", result);
        assertShadowAfter(rb5)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(3) // 2 existing + 1 new
                .assertSynchronizationSituation(SynchronizationSituationType.DISPUTED)
                .assertCorrelationStartTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertCorrelationCaseOpenTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertNoCorrelationCaseCloseTimestamp()
                .assertNoCorrelationEndTimestamp();

        assertUserAfterByUsername("black2")
                .assertLinks(1, 0);

        and("6th user (nn6) should be disputed with 1 candidate (by phone#)");

        PrismObject<ShadowType> nn6 = getTargetShadow("nn6", result);
        assertShadowAfter(nn6)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(2)
                .assertSynchronizationSituation(SynchronizationSituationType.DISPUTED)
                .assertCorrelationStartTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertCorrelationCaseOpenTimestampBetween(firstTargetImportStart, firstTargetImportEnd)
                .assertNoCorrelationCaseCloseTimestamp()
                .assertNoCorrelationEndTimestamp();
    }

    private void assertTargetLinked(String userName, String accountName, OperationResult result) throws CommonException {
        assertShadowAfter(getTargetShadow(accountName, result))
                .assertSynchronizationSituation(SynchronizationSituationType.LINKED);

        assertUserAfterByUsername(userName)
                .assertLinks(2, 0);
    }

    /**
     * Alice (#3) and Bob (#4) are now resolved manually - confirming the candidates.
     */
    @Test
    public void test210ResolveAliceAndBob() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("cases for Alice and Bob");

        CaseType ag3case = getOpenCaseForAccount("ag3", result);
        CaseType bb4case = getOpenCaseForAccount("bb4", result);

        String aliceOid = findUserByUsername("green1").getOid();
        String bobOid = findUserByUsername("black1").getOid();

        when("case for Alice is resolved");

        long aliceCaseResolutionStart = System.currentTimeMillis();
        resolveCase(ag3case, aliceOid, "she is the one", task, result);
        long aliceCaseResolutionEnd = System.currentTimeMillis();

        and("case for Bob is resolved");
        resolveCase(bb4case, bobOid, task, result);

        then("shadows are linked to the users");

        assertTargetLinked("green1", "ag3", result);
        assertTargetLinked("black1", "bb4", result);

        and("alice's case is OK");
        assertCaseAfter(ag3case.getOid())
                .assertClosed();

        and("alice's shadow is OK");
        assertShadowAfter(getTargetShadow("ag3", result))
                .assertCorrelationStartTimestampBetween(firstTargetImportStart, aliceCaseResolutionStart)
                .assertCorrelationCaseOpenTimestampBetween(firstTargetImportStart, aliceCaseResolutionStart)
                .assertCorrelationCaseCloseTimestampBetween(aliceCaseResolutionStart, aliceCaseResolutionEnd)
                .assertCorrelationEndTimestampBetween(aliceCaseResolutionStart, aliceCaseResolutionEnd)
                .assertCorrelationPerformers(USER_ADMINISTRATOR_OID)
                .assertCorrelationComments("she is the one")
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER);
    }

    private @NotNull PrismObject<ShadowType> getTargetShadow(String name, OperationResult result) throws SchemaException {
        return Objects.requireNonNull(
                findShadowByPrismName(name, getTargetResource().get(), result),
                () -> "no target shadow of '" + name + "' was found");
    }

    private @NotNull CaseType getOpenCaseForAccount(String name, OperationResult result) throws CommonException {
        return Objects.requireNonNull(
                correlationCaseManager.findCorrelationCase(
                        getTargetShadow(name, result).asObjectable(),
                        true,
                        result),
                () -> "No open case for account '" + name + "'");
    }

    /**
     * Robert (#5) is resolved manually - to smith2.
     */
    @Test
    public void test220ResolveRobert() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("case for Robert");

        CaseType aCase = getOpenCaseForAccount("rb5", result);
        String userOid = findUserByUsername("black2").getOid();

        when("case for Robert is resolved");

        resolveCase(aCase, userOid, task, result);

        then("shadow is linked to the users");

        assertTargetLinked("black2", "rb5", result);
    }

    /**
     * Nobody (#6) is resolved manually - to no one.
     */
    @Test
    public void test230ResolveNobody() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("case for Nobody");

        CaseType aCase = getOpenCaseForAccount("nn6", result);

        when("case for Nobody is resolved");

        resolveCase(aCase, null, task, result);

        then("shadow is unmatched");

        assertShadowAfter(getTargetShadow("nn6", result))
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(SynchronizationSituationType.UNMATCHED);
    }

    /**
     * Check that the case is updated when import is repeated.
     */
    @Test
    public void test240ReimportingAccount() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new account is added and imported");
        getTargetResource().append("mx1,Martin,Black,,,,");
        importAccountsRequest()
                .withResourceOid(getTargetResource().oid)
                .withNamingAttribute("login")
                .withNameValue("mx1")
                .execute(result);

        PrismObject<ShadowType> firstShadow = findShadowByPrismName("mx1", getTargetResource().get(), result);
        assertShadow(firstShadow, "first")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(3); // 2 existing ones (surname black) + 1 new

        CaseType firstCase = getOpenCaseForAccount("mx1", result);
        assertCase(firstCase, "first")
                .display()
                .assertTargetRef(firstShadow.getOid(), ShadowType.COMPLEX_TYPE);

        //noinspection unchecked
        PrismObject<ShadowType> embeddedFirstShadow =
                (PrismObject<ShadowType>) firstCase.getTargetRef().asReferenceValue().getObject();
        assertThat(embeddedFirstShadow).as("embedded shadow").isNotNull();
        assertShadow(embeddedFirstShadow, "first (embedded)")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(3) // 2 existing ones (surname black) + 1 new
                .attributes()
                .assertValueRaw(new QName(NS_RI, "sn"), "Black");

        when("account is updated and re-imported");
        getTargetResource().replaceLine("mx1,.*", "mx1,Martin,Green,,,,");
        importAccountsRequest()
                .withResourceOid(getTargetResource().oid)
                .withNamingAttribute("login")
                .withNameValue("mx1")
                .execute(result);

        then("case is updated");

        PrismObject<ShadowType> secondShadow = findShadowByPrismName("mx1", getTargetResource().get(), result);
        assertShadow(secondShadow, "second")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(2); // 1 existing one (surname green) + 1 new

        CaseType secondCase = getOpenCaseForAccount("mx1", result);
        assertCase(secondCase, "second")
                .display()
                .assertTargetRef(secondShadow.getOid(), ShadowType.COMPLEX_TYPE); // most probably the same OID as the 1st one

        //noinspection unchecked
        PrismObject<ShadowType> embeddedSecondShadow =
                (PrismObject<ShadowType>) secondCase.getTargetRef().asReferenceValue().getObject();
        assertThat(embeddedSecondShadow).as("embedded shadow").isNotNull();
        assertShadow(embeddedSecondShadow, "second (embedded)")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN)
                .assertPotentialOwnerOptions(2) // 1 existing ones (surname green) + 1 new
                .attributes()
                .assertValueRaw(new QName(NS_RI, "sn"), "Green");
    }
}
