/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.UNCERTAIN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Here we test the correlation that uses the internal correlator.
 *
 * Contrary to {@link AbstractSimpleInternalCorrelationTest} here we allow a user to have multiple accounts on the source
 * resource (`SIS`). They are mapped into assignments. There is an algorithm to find the "authoritative" identity
 * that provides the authoritative personal data for the user.
 *
 * RUNS ON NATIVE REPOSITORY ONLY!
 */
public class TestInternalCorrelationMultiAccounts extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "internal/multi-accounts");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "000-system-configuration.xml");

    private static final TestResource<FunctionLibraryType> FUNCTION_LIBRARY_IDMATCH =
            new TestResource<>(TEST_DIR, "005-function-library.xml", "c4b1f289-e45a-4788-9c53-00601e1c8ed2");
    private static final TestResource<ArchetypeType> ARCHETYPE_PROGRAM =
            new TestResource<>(TEST_DIR, "011-archetype-program.xml", "cc5626d8-1347-46b9-971d-f03dc129991e");
    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_PERSON =
            new TestResource<>(TEST_DIR, "020-object-template-person.xml", "81268f26-d4e5-4a32-8318-4b0f7b0c9a20");
    private static final TestResource<ArchetypeType> ARCHETYPE_PERSON =
            new TestResource<>(TEST_DIR, "021-archetype-person.xml", "eaf32546-3584-46e9-bdb8-2efbd7ecac47");
    private static final TestResource<OrgType> ORG_PROGRAMS =
            new TestResource<>(TEST_DIR, "030-org-programs.xml", "d62065fb-651a-453f-9884-67d271e1fc2b");
    private static final TestResource<OrgType> ORG_PROGRAM_BIOCH =
            new TestResource<>(TEST_DIR, "034-org-program-bioch.xml", "233e78b7-9db8-4503-8d71-6ebf60d9329b");
    private static final TestResource<OrgType> ORG_PROGRAM_COMP_BIO =
            new TestResource<>(TEST_DIR, "035-org-program-comp-bio.xml", "083de604-23fd-4d1f-82e0-6c66b448440d");
    private static final TestResource<OrgType> ORG_PROGRAM_E_ENG =
            new TestResource<>(TEST_DIR, "036-org-program-e-eng.xml", "ed0b9007-c851-4686-80aa-d9970032e0cb");
    private static final TestResource<OrgType> ORG_PROGRAM_MAT_ENG =
            new TestResource<>(TEST_DIR, "037-org-program-mat-eng.xml", "d025526d-8025-44de-a5ed-9d8068beed47");
    private static final TestResource<OrgType> ORG_PROGRAM_MATH =
            new TestResource<>(TEST_DIR, "038-org-program-math.xml", "d0bbb2a8-3835-46dd-850f-f6c4ec034de0");
    private static final TestResource<OrgType> ORG_PROGRAM_SW_ENG =
            new TestResource<>(TEST_DIR, "039-org-program-sw-eng.xml", "0406dab5-3e86-451e-acc3-3ccfda892115");
    private static final TestResource<OrgType> ORG_PROGRAM_SW_ENG_DOCTORAL =
            new TestResource<>(TEST_DIR, "040-org-program-sw-eng-doctoral.xml", "474c95ce-59f0-4104-8a64-f3d0406234f0");

    private static final CsvResource RESOURCE_SIS = new CsvResource(TEST_DIR, "resource-sis.xml",
            "afb142f9-2218-491a-8b99-ce5713ca424d", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId,studyProgram");

    private static final TestTask TASK_IMPORT_SIS = new TestTask(TEST_DIR, "task-import-sis.xml",
            "805b1477-edbd-48db-bb34-2710e4dbeed4", 30000);

    private UserType john;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(FUNCTION_LIBRARY_IDMATCH, initTask, initResult);
        addObject(ARCHETYPE_PROGRAM, initTask, initResult);
        addObject(OBJECT_TEMPLATE_PERSON, initTask, initResult);
        addObject(ARCHETYPE_PERSON, initTask, initResult);
        addObject(ORG_PROGRAMS, initTask, initResult);
        addObject(ORG_PROGRAM_BIOCH, initTask, initResult);
        addObject(ORG_PROGRAM_COMP_BIO, initTask, initResult);
        addObject(ORG_PROGRAM_E_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_MAT_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_MATH, initTask, initResult);
        addObject(ORG_PROGRAM_SW_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_SW_ENG_DOCTORAL, initTask, initResult);

        RESOURCE_SIS.initializeAndTest(this, initTask, initResult);
        TASK_IMPORT_SIS.initialize(this, initTask, initResult); // importing in closed state
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * First import: No ambiguities.
     *
     * Here we import two persons, with no ambiguities. Both should correlated to distinct new users.
     */
    @Test
    public void test100ImportNoAmbiguities() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        RESOURCE_SIS.appendLine("1,Mary,Smith,2006-04-10,060410/1993,bioch");
        RESOURCE_SIS.appendLine("2,John,Smith,2004-02-06,040206/1328,sw-eng");

        when();
        TASK_IMPORT_SIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(2, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNMATCHED, LINKED, null, 2, 0, 0)
                        .assertTransitions(1)
                    .end();

        UserType mary = findUserByUsernameFullRequired("smith1").asObjectable();
        assertUser(mary, "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .assertAssignments(2)
                    .by().targetType(OrgType.COMPLEX_TYPE).find()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .singleLink()
                    .resolveTarget()
                        .display();

        john = findUserByUsernameFullRequired("smith2").asObjectable();
        assertUser(john, "John after")
                .display()
                .assertFullName("John Smith")
                .assignments()
                    .assertAssignments(2)
                    .by().targetType(OrgType.COMPLEX_TYPE).find()
                        .assertTargetOid(ORG_PROGRAM_SW_ENG.oid)
                    .end()
                .end()
                .singleLink()
                    .resolveTarget()
                        .display();
        // @formatter:on
    }

    /**
     * Automatically resolved ambiguities.
     *
     * Adding a third person. The existing two should be synchronized without correlation,
     * and the third one should be correlated to existing owner.
     */
    @Test
    public void test110ImportJohnSlightlyModified() throws CommonException, IOException, PreconditionViolationException {
        given();
        OperationResult result = getTestOperationResult();

        // A slight change in the given name. ID Match should automatically assign the same ID.
        // National ID should be normalized before contacting ID Match.
        RESOURCE_SIS.appendLine("3,Ian,Smith,2004-02-06,040206-1328,math");

        when();
        var taskOid = importSingleAccountRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("3")
                .execute(result);

        then();
        // @formatter:off
        assertTask(taskOid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // account #3 - it is unlinked because the owner exists (but account is not linked yet)
                        .assertTransition(null, UNLINKED, LINKED, null, 1, 0, 0)
                        .assertTransitions(1)
                    .end();

        assertUser(findUserByUsernameFullRequired("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .assertAssignments(2)
                    .by().targetType(OrgType.COMPLEX_TYPE).find()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        assertUser(findUserByUsernameFullRequired("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(3)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                .end()
                .assertLinks(2, 0);
        // @formatter:on
    }

    /**
     * Manually resolvable (single) ambiguity - will be resolved to the same person.
     *
     * A case should be created.
     */
    @Test
    public void test120ImportJohnSingleAmbiguity() throws CommonException, IOException, PreconditionViolationException {
        given();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        // Here we intentionally use given name of Ian, that is NOT authoritative (i.e. it's present only
        // in an assignment).
        RESOURCE_SIS.appendLine("4,Ian,Smith,2004-02-06,040206,e-eng");

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when("import is run");
        var taskOid = importSingleAccountRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("4")
                .execute(result);

        then("task should complete successfully, with 1 transition to disputed");
        // @formatter:off
        assertTask(taskOid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(1, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(1)
                    .end();

        and("Mary should be unchanged");
        assertUser(findUserByUsernameFullRequired("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .assertAssignments(2)
                    .by().targetType(OrgType.COMPLEX_TYPE).find()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        and("John should be unchanged");
        assertUser(findUserByUsernameFullRequired("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(3)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                .end()
                .assertLinks(2, 0);
        // @formatter:on

        and("there should be a new shadow with DISPUTED state");
        PrismObject<ShadowType> newShadow = getShadow("4", result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertPotentialOwnerOptions(2);

        and("there should be a correlation case for the account");
        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), false, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .displayXml()
                .assertOpen()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on

        displayValue("correlation case", prismContext.xmlSerializer().serializeRealValue(correlationCase));

        displayDumpable("audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);

        // TODO check audit and notification event content
    }

    /**
     * Resolve the case created in the previous test. (By confirming it is the same person.)
     *
     * The user should be updated, because the import is executed immediately.
     */
    @Test
    public void test130ResolveAmbiguityAsExisting() throws CommonException {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType newShadow = getShadow("4", result).asObjectable();
        CaseType correlationCase = java.util.Objects.requireNonNull(
                correlationCaseManager.findCorrelationCase(newShadow, true, result));
        WorkItemId workItemId = WorkItemId.of(correlationCase.getWorkItem().get(0));

        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                .outcome(
                        OwnerOptionIdentifier.forExistingOwner(john.getOid()).getStringValue());

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        caseService.completeWorkItem(workItemId, output, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        UserType john = findUserByUsernameFullRequired("smith2").asObjectable();
        assertUser(john, "John after")
                .display()
                .assertFullName("John Smith")
                .assignments()
                    .assertAssignments(4)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                    .assertOrg(ORG_PROGRAM_E_ENG.oid)
                .end()
                .assertLinks(3, 0);
        // @formatter:on

        PrismObject<ShadowType> newShadowAfter = getShadow("4", result);
        assertShadow(newShadowAfter, "after")
                .display()
                .assertSynchronizationSituation(LINKED);

        // @formatter:off
        assertCase(correlationCase.getOid(), "correlation case")
                .display()
                .assertClosed()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on

        displayDumpable("audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);

        // TODO check audit and notification event content
    }

    /**
     * Reimport manually resolved ambiguity introduced in previous tests. (By running the whole import task.)
     * It should be no-op, because the account was already imported.
     */
    @Test
    public void test140ReimportJohnSingleAmbiguity() throws CommonException {
        given();
        OperationResult result = getTestOperationResult();

        when();
        TASK_IMPORT_SIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(4, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // three imported accounts (Ian, Mary, Jan) + resolved account (reimport was done when correlating)
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0)
                        .assertTransitions(1)
                    .end();
        // @formatter:on
    }

    /**
     * Manually resolvable ambiguity (two options) - will be resolved to new person.
     *
     * A new (different) case should be created.
     */
    @Test
    public void test150ImportJohnTwoOptions() throws CommonException, IOException, PreconditionViolationException {
        OperationResult result = getTestOperationResult();

        given("different national ID, same name and date of birth. (To be consulted with operator.)");
        RESOURCE_SIS.appendLine("6,John,Smith,2004-02-06,040206/8824,sw-eng-doctoral");

        when("the task is executed");
        var taskOid = importSingleAccountRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("6")
                .execute(result);

        then("a case should be created");
        // @formatter:off
        assertTask(taskOid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(1, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(1)
                    .end();
        // @formatter:on

        PrismObject<ShadowType> newShadow = getShadow("6", result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertPotentialOwnerOptions(2);

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), true, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .assertOpen()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on

        var xml = prismContext.xmlSerializer().serialize(correlationCase.asPrismObject());
        displayValue("Case XML", xml);
    }

    /**
     * Resolve the case created in the previous test. (By telling it's a different person.)
     */
    @Test
    public void test160ResolveAmbiguityAsNone() throws CommonException {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType newShadow = getShadow("6", result).asObjectable();
        CaseType correlationCase = java.util.Objects.requireNonNull(
                correlationCaseManager.findCorrelationCase(newShadow, true, result));
        WorkItemId workItemId = WorkItemId.of(correlationCase.getWorkItem().get(0));

        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                .outcome(
                        OwnerOptionIdentifier.forNoOwner().getStringValue());

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        caseService.completeWorkItem(workItemId, output, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        UserType john = findUserByUsernameFullRequired("smith3").asObjectable();
        assertUser(john, "New john after")
                .display()
                .assertFullName("John Smith")
                .assignments()
                    .assertAssignments(2)
                    .assertOrg(ORG_PROGRAM_SW_ENG_DOCTORAL.oid)
                .end()
                .assertLinks(1, 0);
        // @formatter:on

        PrismObject<ShadowType> newShadowAfter = getShadow("6", result);
        assertShadow(newShadowAfter, "after")
                .display()
                .assertSynchronizationSituation(LINKED);

        assertCase(correlationCase.getOid(), "correlation case")
                .display()
                .assertClosed();

        displayDumpable("audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);

        // TODO check audit and notification event content
    }

    /**
     * Here we import an account that matches only the national ID. We resolve it on the spot.
     */
    @Test
    public void test170ImportMatchingNationalIdOnly() throws CommonException, IOException, PreconditionViolationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("same national ID, different name");
        RESOURCE_SIS.appendLine("7,Jim,Sanchez,2004-02-06,040206/8824,math");

        when("the task is executed");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("7")
                .execute(result);

        and("shadow is disputed with 2 options");
        PrismObject<ShadowType> newShadow = getShadow("7", result);

        assertShadow(newShadow, "after")
                .display()
                .assertCorrelationSituation(UNCERTAIN)
                .assertSynchronizationSituation(DISPUTED)
                .assertPotentialOwnerOptions(2);

        and("case is open");
        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), true, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .assertOpen()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on

        when("the case is resolved to 'none'");
        caseService.completeWorkItem(
                WorkItemId.of(correlationCase.getWorkItem().get(0)),
                new AbstractWorkItemOutputType()
                        .outcome(
                                OwnerOptionIdentifier.forNoOwner().getStringValue()),
                task,
                result);

        then("new user (sanchez1) is created");
        assertSuccess(result);

        // @formatter:off
        UserType jim = findUserByUsernameFullRequired("sanchez1").asObjectable();
        assertUser(jim, "after")
                .display()
                .assertFullName("Jim Sanchez")
                .assignments()
                    .assertAssignments(2)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                .end()
                .assertLinks(1, 0);
        // @formatter:on
    }

    private PrismObject<ShadowType> getShadow(String name, OperationResult result) throws SchemaException {
        return findShadowByPrismName(name, RESOURCE_SIS.getObject(), result);
    }
}
