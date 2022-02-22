/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.impl.correlator.CorrelationCaseManager;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Here we test the correlation that uses an ID Match implementation (real or dummy one).
 *
 * There are two source systems: `SIS` (Student Information System) and `HR` (Human Resources).
 * They provide accounts using import, live sync, and reconciliation activities.
 */
public abstract class AbstractIdMatchTest extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "000-system-configuration.xml");

    private static final TestResource<FunctionLibraryType> FUNCTION_LIBRARY_IDMATCH =
            new TestResource<>(TEST_DIR, "005-function-library-idmatch.xml", "44e7f86c-604e-4127-8b0f-33bd7310ecb8");
    private static final TestResource<ArchetypeType> ARCHETYPE_PROGRAM =
            new TestResource<>(TEST_DIR, "011-archetype-program.xml", "fbe37f25-1026-4293-ac48-071cc56b1a36");
    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "020-object-template-user.xml", "17f11c7f-3692-4f80-9fec-63f89ee6763e");
    private static final TestResource<OrgType> ORG_PROGRAMS =
            new TestResource<>(TEST_DIR, "030-org-programs.xml", "8e8f024b-c6da-42ba-82f5-d43e514e70eb");
    private static final TestResource<OrgType> ORG_PROGRAM_BIOCH =
            new TestResource<>(TEST_DIR, "034-org-program-bioch.xml", "ed53d2bb-777d-4ef0-82a5-a351b8f7cbce");
    private static final TestResource<OrgType> ORG_PROGRAM_COMP_BIO =
            new TestResource<>(TEST_DIR, "035-org-program-comp-bio.xml", "af93445d-9c1a-4cfb-b89f-2c79de20dc19");
    private static final TestResource<OrgType> ORG_PROGRAM_E_ENG =
            new TestResource<>(TEST_DIR, "036-org-program-e-eng.xml", "54dafa7c-4549-47cc-a909-be410e80e3b4");
    private static final TestResource<OrgType> ORG_PROGRAM_MAT_ENG =
            new TestResource<>(TEST_DIR, "037-org-program-mat-eng.xml", "f664f54d-bf0c-4e49-a65f-1a44f8177994");
    private static final TestResource<OrgType> ORG_PROGRAM_MATH =
            new TestResource<>(TEST_DIR, "038-org-program-math.xml", "b838411b-d556-447c-9f16-f8ba5f31048a");
    private static final TestResource<OrgType> ORG_PROGRAM_SW_ENG =
            new TestResource<>(TEST_DIR, "039-org-program-sw-eng.xml", "56b451f9-42c5-4213-aafb-d93c78f2e75e");
    private static final TestResource<OrgType> ORG_PROGRAM_SW_ENG_DOCTORAL =
            new TestResource<>(TEST_DIR, "040-org-program-sw-eng-doctoral.xml", "edb74c31-f17e-4b80-b255-5115e4b54c60");

    private static final CsvResource RESOURCE_SIS = new CsvResource(TEST_DIR, "resource-sis.xml",
            "21d4788c-15eb-40cc-8ac5-3cd379362ffe", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId,studyProgram");

    private static final TestTask TASK_IMPORT_SIS = new TestTask(TEST_DIR, "task-import-sis.xml",
            "95ebbf1e-9c71-4870-a1fb-dc47ce6856c9", 30000);

    private static final String NS_EXT = "http://example.com/idmatch";
    private static final ItemName EXT_REFERENCE_ID = new ItemName(NS_EXT, "referenceId");

    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired CaseService caseService;

    /** This is the initialized object (retrieved from the repo). */
    private ResourceType resourceSis;

    private UserType john;
    private UserType mary;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(FUNCTION_LIBRARY_IDMATCH, initTask, initResult);
        addObject(ARCHETYPE_PROGRAM, initTask, initResult);
        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);
        addObject(ORG_PROGRAMS, initTask, initResult);
        addObject(ORG_PROGRAM_BIOCH, initTask, initResult);
        addObject(ORG_PROGRAM_COMP_BIO, initTask, initResult);
        addObject(ORG_PROGRAM_E_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_MAT_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_MATH, initTask, initResult);
        addObject(ORG_PROGRAM_SW_ENG, initTask, initResult);
        addObject(ORG_PROGRAM_SW_ENG_DOCTORAL, initTask, initResult);

        RESOURCE_SIS.initialize(initTask, initResult);
        assertSuccess(
                modelService.testResource(RESOURCE_SIS.oid, initTask));
        resourceSis = repositoryService.getObject(ResourceType.class, RESOURCE_SIS.oid, null, initResult).asObjectable();

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
                .display()
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

        mary = findUserByUsername("smith1").asObjectable();
        assertUser(mary, "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .assertAssignments(1)
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                        // TODO extension properties
                    .end()
                .end()
                .singleLink()
                    .resolveTarget()
                        .display();

        john = findUserByUsername("smith2").asObjectable();
        assertUser(john, "John after")
                .display()
                .assertFullName("John Smith")
                .assignments()
                    .assertAssignments(1)
                    .single()
                        .assertTargetOid(ORG_PROGRAM_SW_ENG.oid)
                        // TODO extension properties
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
    public void test110ImportJohnSlightlyModified() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // A slight change in the given name. ID Match should automatically assign the same ID.
        // National ID should be normalized before contacting ID Match.
        RESOURCE_SIS.appendLine("3,Ian,Smith,2004-02-06,040206-1328,math");

        when();
        TASK_IMPORT_SIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(3, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // two existing accounts (Ian, Mary)
                        .assertTransition(LINKED, LINKED, LINKED, null, 2, 0, 0)
                        // account #3 - it is unlinked because the owner exists (but account is not linked yet)
                        .assertTransition(null, UNLINKED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();

        assertUser(findUserByUsername("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        assertUser(findUserByUsername("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(2)
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
    public void test120ImportJohnSingleAmbiguity() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        RESOURCE_SIS.appendLine("4,John,Smith,2004-02-06,040206,e-eng");

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        TASK_IMPORT_SIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(4, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // three existing accounts (Ian, Mary, Jan)
                        .assertTransition(LINKED, LINKED, LINKED, null, 3, 0, 0)
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();

        assertUser(findUserByUsername("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        assertUser(findUserByUsername("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(2)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                .end()
                .assertLinks(2, 0);
        // @formatter:on

        PrismObject<ShadowType> newShadow = findShadowByPrismName("4", resourceSis.asPrismObject(), result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertMatchReferenceId(null) // not matched yet
                .assertHasMatchRequestId();

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), false, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
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

        ShadowType newShadow = findShadowByPrismName("4", resourceSis.asPrismObject(), result).asObjectable();
        CaseType correlationCase = java.util.Objects.requireNonNull(
                correlationCaseManager.findCorrelationCase(newShadow, true, result));
        WorkItemId workItemId = WorkItemId.of(correlationCase.getWorkItem().get(0));

        // TODO adapt when URIs are changed to use OIDs
        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType(prismContext)
                .outcome(SchemaConstants.CORRELATION_OPTION_PREFIX + getReferenceId(john)); // unqualified should be OK here

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when();
        caseService.completeWorkItem(workItemId, output, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        UserType john = findUserByUsername("smith2").asObjectable();
        assertUser(john, "John after")
                .display()
                .assertFullName("John Smith")
                .assignments()
                    .assertAssignments(3)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                    .assertOrg(ORG_PROGRAM_E_ENG.oid)
                .end()
                .assertLinks(3, 0);
        // @formatter:on

        PrismObject<ShadowType> newShadowAfter = findShadowByPrismName("4", resourceSis.asPrismObject(), result);
        assertShadow(newShadowAfter, "after")
                .display()
                .assertSynchronizationSituation(LINKED)
                .assertMatchReferenceId(getReferenceId(john));
        // match request ID may or may not be there

        // @formatter:off
        assertCase(correlationCase.getOid(), "correlation case")
                .display()
                .assertClosed()
                .workItems()
                    .single()
                    .assertClosed();
        // @formatter:on

        displayDumpable("audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);

        // TODO check audit and notification event content
    }

    private String getReferenceId(@NotNull UserType user) {
        return ObjectTypeUtil.getExtensionItemRealValue(user.asPrismObject(), EXT_REFERENCE_ID);
    }

    /**
     * Reimport manually resolved ambiguity introduced in previous tests.
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
                .display()
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
     * Manually resolvable ambiguity (two options) - will be resolved to existing person.
     *
     * A new (different) case should be created.
     */
    @Test(enabled = false)
    public void test150ImportIanTwoOptions() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        RESOURCE_SIS.appendLine("5,Ian,Smith,2004-02-06,0402060000,ian@evolveum.com");

        when();
        TASK_IMPORT_SIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(5, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // four existing accounts (1-4)
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0)
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();
        // @formatter:on

        assertUser(findUserByEmail("ian@evolveum.com"), "Ian after")
                .display()
                .assertName(john.getName().getOrig()) // unchanged
                .assertGivenName("Ian") // unchanged
                .assertEmployeeNumber("4"); // unchanged

        assertUser(findUserByEmail("mary@evolveum.com"), "Mary after")
                .display()
                .assertName(mary.getName().getOrig()) // unchanged
                .assertGivenName("Mary") // unchanged
                .assertEmployeeNumber("2"); // unchanged

        PrismObject<ShadowType> newShadow = findShadowByPrismName("5", resourceSis.asPrismObject(), result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertMatchReferenceId(null) // not matched yet
                .assertHasMatchRequestId();

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

    private @NotNull UserType findUserByEmail(@NotNull String email) throws SchemaException {
        return MiscUtil.extractSingletonRequired(
                repositoryService.searchObjects(
                        UserType.class,
                        queryFor(UserType.class)
                                .item(UserType.F_EMAIL_ADDRESS).eq(email)
                                .build(),
                        null,
                        getTestOperationResult()),
                () -> new AssertionError("Multiple users with email: " + email),
                () -> new AssertionError("No user with email: " + email))
                .asObjectable();
    }
}
