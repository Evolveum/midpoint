/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationProperty;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Here we test the correlation that uses an ID Match implementation (real or dummy one).
 *
 * There are two source systems: `SIS` (Student Information System) and `HR` (Human Resources).
 * But only `SIS` is currently implemented here.
 */
public abstract class AbstractMultiAccountsIdMatchCorrelationTest extends AbstractIdMatchCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch/multi-accounts");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "000-system-configuration.xml");

    private static final TestResource<MessageTemplateType> MESSAGE_TEMPLATE_CORRELATION =
            new TestResource<>(TEST_DIR, "002-message-template-correlation.xml", "f5d79bd9-6903-42d9-9562-2e6a38096a26");
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

    private static final TestResource<OrgType> ORG_CORRELATION_OPERATORS =
            new TestResource<>(TEST_DIR, "060-org-correlation-operators.xml", "8d537583-d475-48c4-b23d-5e71e1ef4e2a");

    private static final TestResource<UserType> USER_FRED =
            new TestResource<>(TEST_DIR, "100-user-fred.xml", "75369757-ae32-48ac-86f7-7b24fc687c70");
    private static final TestResource<UserType> USER_ALICE =
            new TestResource<>(TEST_DIR, "110-user-alice.xml", "7c8d1fcc-5033-4275-8396-b3ce02e218a9");

    static final CsvResource RESOURCE_SIS = new CsvResource(TEST_DIR, "resource-sis.xml",
            "21d4788c-15eb-40cc-8ac5-3cd379362ffe", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId,studyProgram");

    static final TestTask TASK_IMPORT_SIS = new TestTask(TEST_DIR, "task-import-sis.xml",
            "95ebbf1e-9c71-4870-a1fb-dc47ce6856c9", 30000);

    private static final TestTask TASK_UPDATE_SIS = new TestTask(TEST_DIR, "task-update-sis.xml",
            "3fd6d6a8-4cd3-4d46-bdca-bec123dbd507", 30000);

    private UserType john;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(MESSAGE_TEMPLATE_CORRELATION, initTask, initResult);
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

        addObject(ORG_CORRELATION_OPERATORS, initTask, initResult);
        addObject(USER_FRED, initTask, initResult);
        addObject(USER_ALICE, initTask, initResult); // approver of correlation operators org - not to be included in the case
        assignOrg(USER_ADMINISTRATOR_OID, ORG_CORRELATION_OPERATORS.oid, initTask, initResult);

        RESOURCE_SIS.initAndTest(this, initTask, initResult);

        TASK_IMPORT_SIS.init(this, initTask, initResult); // importing in closed state
        TASK_UPDATE_SIS.init(this, initTask, initResult); // importing in closed state
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
                    .assertAssignments(1)
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                        // TODO extension properties
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

        assertUser(findUserByUsernameFullRequired("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        assertUser(findUserByUsernameFullRequired("smith2"), "John after")
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
        given("an account having national ID without the last four digits");
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        RESOURCE_SIS.appendLine("4,John,Smith,2004-02-06,040206,e-eng");

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        when("import is run");
        TASK_IMPORT_SIS.rerun(result);

        then("task should complete successfully, with 1 transition to disputed");
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
                        // three existing accounts (Ian, Mary, Jan)
                        .assertTransition(LINKED, LINKED, LINKED, null, 3, 0, 0)
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();

        and("Mary should be unchanged");
        assertUser(findUserByUsernameFullRequired("smith1"), "Mary after")
                .display()
                .assertFullName("Mary Smith")
                .assignments()
                    .single()
                        .assertTargetOid(ORG_PROGRAM_BIOCH.oid)
                    .end()
                .end()
                .assertLinks(1, 0);

        and("John should be unchanged");
        assertUser(findUserByUsernameFullRequired("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(2)
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
                .assertMatchReferenceId(null) // not matched yet
                .assertHasMatchRequestId();

        and("there should be a correlation case for the account");
        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), false, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .displayXml()
                .assertOpen()
                .workItems()
                    .assertWorkItems(2)
                    .forOriginalAssignee(USER_ADMINISTRATOR_OID)
                        .assertDeadlineApproximately("P5D")
                    .end()
                    .forOriginalAssignee(USER_FRED.oid)
                        .assertDeadlineApproximately("P5D")
                    .end();
        // @formatter:on

        and("some audit records and notification events");
        displayDumpable("audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);

        // TODO check audit and notification event content

        and("correlation properties should be correct");
        Collection<CorrelationProperty> properties =
                correlationService
                        .describeCorrelationCase(correlationCase, null, task, result)
                        .getCorrelationProperties()
                        .values();
        displayValue("properties", DebugUtil.debugDump(properties));

        // TODO this will need to be adapted

        CorrelationProperty property = findProperty(properties, "givenName");
        assertThat(property.getDefinition()).as("definition").isNotNull();
        assertThat(property.getItemPath()).as("itemPath").hasToString("givenName");

        // TODO other asserts on correlation case description
    }

    @SuppressWarnings("SameParameterValue")
    private @NotNull CorrelationProperty findProperty(Collection<CorrelationProperty> properties, String name) {
        return MiscUtil.extractSingletonRequired(
                properties.stream()
                        .filter(p -> name.equals(p.getName()))
                        .collect(Collectors.toList()),
                () -> new IllegalStateException("Multiple " + name + " properties"),
                () -> new IllegalStateException("No " + name + " property"));
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

        // TODO adapt when URIs are changed to use OIDs
        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType()
                .outcome(SchemaConstants.CORRELATION_EXISTING_PREFIX + getReferenceId(john)); // unqualified should be OK here

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
                    .assertAssignments(3)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                    .assertOrg(ORG_PROGRAM_E_ENG.oid)
                .end()
                .assertLinks(3, 0);
        // @formatter:on

        PrismObject<ShadowType> newShadowAfter = getShadow("4", result);
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
                    .assertWorkItems(2)
                    .forOriginalAssignee(USER_ADMINISTRATOR_OID).assertClosed().end()
                    .forOriginalAssignee(USER_FRED.oid).assertClosed().end();
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
    public void test150ImportJohnTwoOptions() throws CommonException, IOException {
        OperationResult result = getTestOperationResult();

        given("different national ID, same name and date of birth. (To be consulted with operator.)");
        RESOURCE_SIS.appendLine("6,John,Smith,2004-02-06,040206/8824,sw-eng-doctoral");

        when("the task is executed");
        TASK_IMPORT_SIS.rerun(result);

        then("a case should be created");
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
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

        PrismObject<ShadowType> newShadow = getShadow("6", result);

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
                    .assertWorkItems(2);
        // @formatter:on

        var xml = prismContext.xmlSerializer().serialize(correlationCase.asPrismObject());
        displayValue("Case XML", xml);
    }

    /**
     * Two accounts are updated:
     *
     * 1) account #4 (i.e. "4,John,Smith,2004-02-06,040206,e-eng") is updated to national ID of "111111" and last name of "S",
     * 2) account #6 (i.e. "6,John,Smith,2004-02-06,040206/8824,sw-eng-doctoral") to national ID "222222" and last name of "T".
     *
     * The former is an example of resolved case, the latter of unresolved one.
     *
     * The immediate result for #4 is not visible: it was updated in the ID Match service.
     *
     * But the #6 should be resolved, as the update operation for uncorrelated shadows is implemented (in midPoint)
     * as just another correlation attempt. We check that.
     */
    @Test
    public void test200UpdateTwoAccounts() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("accounts #4 and #6 are updated on SIS");
        modifyNationalId("4", "nationalId", "111111", task, result);
        modifyNationalId("4", "lastName", "S", task, result);
        modifyNationalId("6", "nationalId", "222222", task, result);
        modifyNationalId("6", "lastName", "T", task, result);

        when("update task is run");
        TASK_UPDATE_SIS.rerun(result);

        then("the status of the task should be OK");
        // @formatter:off
        TASK_UPDATE_SIS.assertAfter()
                .assertClosed()
                .assertSuccess();
        // @formatter:on

        and("#6 should be correlated now");
        assertShadowAfter(getShadow("6", result))
                .assertHasMatchReferenceId()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED);

        // TODO case close timestamp should be set (it is not now!)
    }

    private void modifyNationalId(String uid, String attrName, String newValue, Task task, OperationResult result)
            throws CommonException {
        provisioningService.modifyObject(
                ShadowType.class,
                getShadow(uid, result).getOid(),
                deltaFor(ShadowType.class)
                        .item(
                                ItemPath.create(ShadowType.F_ATTRIBUTES, attrName),
                                createStringDefinition(attrName))
                        .replace(newValue)
                        .asItemDeltas(),
                null,
                null,
                task,
                result);
    }

    @SuppressWarnings("SameParameterValue")
    private PrismPropertyDefinition<String> createStringDefinition(String itemName) {
        return prismContext.definitionFactory()
                .createPropertyDefinition(new QName(itemName), DOMUtil.XSD_STRING);
    }

    /**
     * To verify that #4 has been updated, let us correlate another account with the same data.
     * It should got linked to "smith2".
     */
    @Test
    public void test210CorrelateAccountWithID111111() throws CommonException, IOException {
        OperationResult result = getTestOperationResult();

        given("new account (#210) with same data as updated #4");
        RESOURCE_SIS.appendLine("210,John,S,2004-02-06,111111,mat-eng");

        when("import task is run");
        TASK_IMPORT_SIS.rerun(result);

        PrismObject<ShadowType> newShadow = getShadow("210", result);

        then("the account should have EXISTING_OWNER / LINKED");
        assertShadowAfter(newShadow)
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(LINKED);

        and("smith2 should have account #210 linked");
        // @formatter:off
        TASK_IMPORT_SIS.assertAfter()
                .assertClosed()
                .assertSuccess();

        assertUser(findUserByUsernameFullRequired("smith2"), "John after")
                .display()
                .assertFullName("John Smith") // unchanged
                .assignments()
                    .assertAssignments(4)
                    .assertOrg(ORG_PROGRAM_SW_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MATH.oid)
                    .assertOrg(ORG_PROGRAM_E_ENG.oid)
                    .assertOrg(ORG_PROGRAM_MAT_ENG.oid)
                .end()
                .assertLinks(4, 0);
        // @formatter:on
    }

    private PrismObject<ShadowType> getShadow(String name, OperationResult result) throws SchemaException {
        return findShadowByPrismName(name, RESOURCE_SIS.get(), result);
    }
}
