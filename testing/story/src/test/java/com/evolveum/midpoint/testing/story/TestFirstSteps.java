/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.POLICY_SITUATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.PROTECTED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.test.ldap.OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.opends.server.util.LDIFException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.model.test.util.ImportAccountsRequest.ImportAccountsRequestBuilder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AnyResource;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Here we check comprehensive "First steps" scenario.
 *
 * The test methods here are chained: they depend on each other, forming one of possible "first steps" uses of midPoint.
 *
 * General idea:
 *
 * . Gradually evolving HR CSV resource in development (`proposed`) mode - see `test1xx`
 * . TODO
 *
 * Planned improvements:
 *
 * . Treating bad data in HR (~ shadow marks)
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestFirstSteps extends AbstractStoryTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "first-steps");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String RESOURCE_HR_OID = "a1864c6e-b154-4384-bc7f-0b0c92379c3f";

    private static final CsvResource RESOURCE_HR_1 = createHrResource("resource-hr-1.xml");
    private static final CsvResource RESOURCE_HR_2 = createHrResource("resource-hr-2.xml");
    private static final CsvResource RESOURCE_HR_3 = createHrResource("resource-hr-3.xml");
    private static final CsvResource RESOURCE_HR_4 = createHrResource("resource-hr-4.xml");
    private static final CsvResource RESOURCE_HR_5 = createHrResource("resource-hr-5.xml");
    private static final CsvResource RESOURCE_HR_6 = createHrResource("resource-hr-6.xml");
    private static final CsvResource RESOURCE_HR_7 = createHrResource("resource-hr-7.xml");
    private static final CsvResource RESOURCE_HR_8 = createHrResource("resource-hr-8.xml");

    private static final int PROTECTED_OPENDJ_ACCOUNTS = 4;
    private static final int REGULAR_INITIAL_OPENDJ_ACCOUNTS = 9;
    private static final int ALL_INITIAL_OPENDJ_ACCOUNTS = PROTECTED_OPENDJ_ACCOUNTS + REGULAR_INITIAL_OPENDJ_ACCOUNTS;
    private static final File INITIAL_LDIF_FILE = new File(TEST_DIR, "initial.ldif");

    private static final String RESOURCE_OPENDJ_OID = "0934922f-0f63-4768-b1b1-eab4275b31d1";

    private static final TestResource<ResourceType> RESOURCE_OPENDJ_TEMPLATE =
            new TestResource<>(TEST_DIR, "resource-opendj-template.xml", "bb554a60-3e83-40e5-be21-ca913ee58a43");

    private static final AnyResource RESOURCE_OPENDJ_1 = createOpenDjResource("resource-opendj-1.xml");
    private static final AnyResource RESOURCE_OPENDJ_2 = createOpenDjResource("resource-opendj-2.xml");
    private static final AnyResource RESOURCE_OPENDJ_3 = createOpenDjResource("resource-opendj-3.xml");
    private static final AnyResource RESOURCE_OPENDJ_4 = createOpenDjResource("resource-opendj-4.xml");
    private AnyResource currentOpenDjResource = RESOURCE_OPENDJ_1;

    private static final ObjectsCounter focusCounter = new ObjectsCounter(FocusType.class);
    private static final String DN_JSMITH1 = "uid=jsmith1,ou=People,dc=example,dc=com";
    private static final String DN_JSMITH2 = "uid=jsmith2,ou=People,dc=example,dc=com";
    private static final String DN_AGREEN3 = "uid=agreen3,ou=People,dc=example,dc=com";
    private static final String DN_RBLACK = "uid=rblack,ou=People,dc=example,dc=com";
    private static final String DN_BOB = "uid=bob,ou=People,dc=example,dc=com";
    private static final String DN_TESLA = "uid=tesla,ou=People,dc=example,dc=com";
    private static final String DN_HACKER = "uid=hacker,ou=People,dc=example,dc=com";
    private static final String DN_ADMIN = "uid=admin,ou=People,dc=example,dc=com";
    private static final String DN_JUNIOR1 = "uid=junior1,ou=People,dc=example,dc=com";

    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired CaseManager caseManager;

    private static CsvResource createHrResource(String fileName) {
        return new CsvResource(TEST_DIR, fileName, RESOURCE_HR_OID, "hr.csv");
    }

    private static AnyResource createOpenDjResource(String fileName) {
        return new AnyResource(TEST_DIR, fileName, RESOURCE_OPENDJ_OID);
    }

    private static final ItemName ATTR_EMP_NO = new ItemName(NS_RI, "empNo");

    @AfterClass
    public static void stopResources() {
        if (openDJController.isRunning()) {
            openDJController.stop();
        }
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    /**
     * We get some data from HR by providing the most simple definition (`hr-1`) - no `schemaHandling` there, `proposed` state.
     */
    @Test
    public void test100FirstResourceDefinition() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("first definition is imported and tested");
        RESOURCE_HR_1.initializeAndTest(this, task, result);

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_HR_1.object)
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        then("there are 5 accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(5);

        and("there is no known kind/intent");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.UNKNOWN)
                    .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        }
    }

    /**
     * Adding dummy `schemaHandling` - just defining the `account/default` type.
     */
    @Test
    public void test110AddDummySchemaHandling() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("definition with simple `schemaHandling` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_2, task, result);

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_HR_2.object)
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        then("there are 5 accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(5);

        and("they are classified as account/default");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(SchemaConstants.INTENT_DEFAULT);
        }
    }

    /**
     * Adding a reaction for `UNMATCHED` situation. Trying to import an account.
     */
    @Test
    public void test120AddUnmatchedReaction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with simple `schemaHandling` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_3, task, result);

        when("single account is imported (on foreground, real execution)");
        importHrAccountRequest("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        importHrAccountRequest("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration
                .simulatedProduction()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated development execution)");
        OperationResult subResult = result.createSubresult("simulated development execution");
        importHrAccountRequest("1")
                .withNotAssertingSuccess()
                .simulatedDevelopment()
                .executeOnForeground(subResult);
        subResult.close();

        then("the import fails");
        assertThatOperationResult(subResult)
                .isFatalError()
                .hasMessageContaining("No name in new object");

        and("no new focus objects are there");
        focusCounter.assertNoNewObjects(result);
    }

    /**
     * Adding a mapping for `empNo`. Trying to import an account.
     */
    @Test
    public void test130AddEmpNoMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with mapping for `empNo` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_4, task, result);

        when("single account is imported (on foreground, real execution)");
        importHrAccountRequest("1")
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        SimulationResult simResult = importHrAccountRequest("1")
                .simulatedProduction()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("no deltas as the configuration is not visible");
        assertDeltaCollection(simResult.getSimulatedDeltas(), "simulated production execution")
                .assertSize(0);

        when("single account is imported (on foreground, simulated development execution)");
        SimulationResult simResult2 = importHrAccountRequest("1")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest130SimulatedDeltas(simResult2.getSimulatedDeltas(), "(foreground)");

        when("single account is imported (on background, simulated development execution)");
        String taskOid = importHrAccountRequest("1")
                .simulatedDevelopment()
                .execute(result);

        assertTask(taskOid, "simulated production")
                .display();

        if (isNativeRepository()) {
            then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(taskOid, result);
            assertTest130SimulatedDeltas(simulatedDeltas, "(background)");
        }

        and("no new focus objects are there");
        focusCounter.assertNoNewObjects(result);
    }

    private void assertTest130SimulatedDeltas(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, "simulated development execution: " + message)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .objectToAdd()
                        .asFocus()
                            .assertName("1")
                            .assertLinks(1, 0)
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .assertNoRealResourceObjectModifications();
        // @formatter:on
    }

    /**
     * Adding other mappings, with broken `note` -> `description` mapping.
     * Trying single-user import and then full (simulated) import.
     */
    @Test
    public void test140AddAllMappingWithBrokenDescriptionMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with more mappings (one faulty) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_5, task, result);

        when("single account is imported (on foreground, simulated development execution)");
        SimulationResult simResult1 = importHrAccountRequest("1")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest140SimulatedDeltasSingleAccount(simResult1.getSimulatedDeltas(), "(foreground)");

        when("Whoa! Let us run the full import!");
        String taskOid = importAllHrAccountsRequest()
                .simulatedDevelopment()
                .withNotAssertingSuccess()
                .execute(result);

        // @formatter:off
        assertTask(taskOid, "simulated production")
                .assertPartialError()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(4, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(4, 1, 0)
                        .assertLastSuccessObjectName("4")
                        .assertLastFailureObjectName("5");
        // @formatter:on

        if (isNativeRepository()) {
            then("there should be some deltas there");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(taskOid, result);
            assertDeltaCollection(simulatedDeltas, "simulated development execution (background)")
                    .display()
                    .assertSize(8); // 4 user ADD, 4 shadow MODIFY
            // TODO assert also some information on processed objects
        }

        and("no new focus objects are there");
        focusCounter.assertNoNewObjects(result);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTest140SimulatedDeltasSingleAccount(Collection<ObjectDelta<?>> simulatedDeltas, String message) {
        // @formatter:off
        assertDeltaCollection(simulatedDeltas, "simulated development execution: " + message)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .objectToAdd()
                        .asUser()
                            .assertName("1")
                            .assertGivenName("John")
                            .assertFamilyName("Smith")
                            .assertEmailAddress("jsmith1@evolveum.com")
                            .assertTelephoneNumber("+421-123-456-001")
                            .assertLinks(1, 0)
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .assertNoRealResourceObjectModifications();
        // @formatter:on
    }

    /**
     * The bug is fixed, another one is introduced.
     * In blissful ignorance we run the full import (switching the resource to the production mode).
     */
    @Test
    public void test150FixTheBugIntroduceAnother() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("production-mode definition (with a different bug) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_6, task, result);

        when("Now it must work. Let the import run!");
        String taskOid = importAllHrAccountsRequest()
                .withNotAssertingSuccess()
                .execute(result);

        // @formatter:off
        assertTask(taskOid, "production")
                .assertPartialError()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(4, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(4, 1, 0)
                        .assertLastSuccessObjectName("5")
                        .assertLastFailureObjectName("4");
        // @formatter:on

        and("four new focus objects are there");
        focusCounter.assertUserOnlyIncrement(4, result);
        assertUserByUsername("1", "after")
                .display()
                .assertName("1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertEmailAddress("jsmith1@evolveum.com")
                .assertTelephoneNumber("+421-123-456-001")
                .assertLinks(1, 0);
        assertNoUserByUsername("4");
    }

    /**
     * Finally we were able to fix the bug without introducing another one.
     * Let us be more humble and simulate the import first.
     */
    @Test
    public void test160FixAllBugs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with no bugs is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_7, task, result);

        when("single account is imported (on foreground, simulated production execution)");
        SimulationResult simResult1 = importHrAccountRequest("4")
                .simulatedProduction()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertDeltaCollection(simResult1.getSimulatedDeltas(), "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .objectToAdd()
                        .asUser()
                            .assertName("4")
                            .assertGivenName("Robert")
                            .assertFamilyName("Black")
                            .assertEmailAddress("rblack4@evolveum.com")
                            .assertTelephoneNumber("00421-123-456-004")
                            .assertLinks(1, 0)
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .assertNoRealResourceObjectModifications();

        when("Will the full import run this time?");
        String taskOid = importAllHrAccountsRequest()
                .withNotAssertingSuccess()
                .execute(result);

        // @formatter:off
        assertTask(taskOid, "full import")
                .assertSuccess()
                .display()
                .rootActivityState()
                    .progress()
                        .display()
                        // Even if there is no synchronization reaction, the processing is considered to be successful (for now)
                        .assertCommitted(5, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end()
                .end()
                // Only single user has a synchronization reaction defined - TODO there should be a better way how to check this
                .assertClockworkRunCount(1);
        // @formatter:on

        and("one new focus object is there");
        focusCounter.assertUserOnlyIncrement(1, result);
    }

    /**
     * Going towards more serious solution: we add mapping from `empNo` to `employeeNumber` along with enabling correlation
     * on this attribute/property, make `empNo` -> `name` mapping weak (anticipating taking usernames from LDAP),
     * and add all necessary synchronization reactions.
     *
     * We put the resource into development mode to test everything before committing the changes.
     *
     * After the simulation, we put the resource into `active` state and run the import again.
     */
    @Test
    public void test170AddEmployeeNumberMappingAndCorrelationAndReactions() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_8, task, result);

        and("a testing employee is added");
        RESOURCE_HR_8.append("999,Alice,Test,atest999@evolveum.com,,testing employee");

        when("the testing employee is imported (on foreground, simulated development execution)");
        SimulationResult simResult1 = importHrAccountRequest("999")
                .simulatedDevelopment() // because resource is `proposed` now
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        // @formatter:off
        var shadowOid999 = assertDeltaCollection(simResult1.getSimulatedDeltas(), "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .objectToAdd()
                        .asUser()
                            .assertName("999")
                            .assertEmployeeNumber("999")
                            .assertGivenName("Alice")
                            .assertFamilyName("Test")
                            .assertEmailAddress("atest999@evolveum.com")
                            .assertTelephoneNumber(null)
                            .assertLinks(1, 0)
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .assertNoRealResourceObjectModifications()
                .getOid();
        // @formatter:on

        assertRepoShadow(shadowOid999, "test account after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(null); // Not updated because of the simulated execution.

        when("existing employee 4 is imported (on foreground, simulated development execution)");
        SimulationResult simResult4 = importHrAccountRequest("4")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        // @formatter:off
        assertDeltaCollection(simResult4.getSimulatedDeltas(), "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertModifiedPaths(
                            UserType.F_EMPLOYEE_NUMBER, // the effect of the newly added mapping
                            PATH_METADATA_MODIFY_CHANNEL,
                            PATH_METADATA_MODIFY_TIMESTAMP,
                            PATH_METADATA_MODIFIER_REF,
                            PATH_METADATA_MODIFY_TASK_REF,
                            PATH_METADATA_MODIFY_APPROVER_REF,
                            PATH_METADATA_MODIFY_APPROVAL_COMMENT)
                .end();
        // @formatter:on

        assertShadow(findShadowByPrismName("4", RESOURCE_HR_8.getObject(), result), "shadow 4 after")
                .display()
                .assertSynchronizationSituation(LINKED);
                // The correlation situation is still "NO_OWNER". It is not updated after the user is linked. Is that OK?

        when("let us try simulate import of all accounts");
        String simulatedTaskOid = importAllHrAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        // @formatter:off
        assertTask(simulatedTaskOid, "full simulated import")
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(6, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, 5, 0, 0)
                        .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end()
                .end()
                .assertClockworkRunCount(6); // All users went through the clockwork
        // @formatter:on

        and("no new focus object is there");
        focusCounter.assertNoNewObjects(result);

        when("the resource is switched into production mode");
        putResourceIntoProduction(RESOURCE_HR_OID, result);

        and("testing account is deleted");
        RESOURCE_HR_8.deleteLine("999,.*");

        when("running production import of all accounts");
        String realTaskOid = importAllHrAccountsRequest().execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(realTaskOid, "full import")
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, 5, 0, 0)
                        .assertTransitions(1)
                    .end()
                .end()
                .assertClockworkRunCount(5); // All users went through the clockwork
        // @formatter:on

        and("employeeNumber is set");
        assertUserAfterByUsername("5")
                .assertEmployeeNumber("5");
    }

    /** We simply create OpenDJ from template and try to read its content. */
    @Test
    public void test200AddOpenDj() throws CommonException, IOException, URISyntaxException, LDIFException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("OpenDJ is started and initialized");
        openDJController.startCleanServer();
        openDJController.addEntriesFromLdifFile(INITIAL_LDIF_FILE);

        and("template and the first version of the resource are imported");
        importObjectFromFile(RESOURCE_OPENDJ_TEMPLATE.file, task, result);
        RESOURCE_OPENDJ_1.initializeAndTest(this, task, result);

        when("OpenDJ content is listed");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_OPENDJ_1.object)
                        .queryFor(OBJECT_CLASS_INETORGPERSON_QNAME)
                        .build(),
                null, task, result);

        then("the number of accounts is expected");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(ALL_INITIAL_OPENDJ_ACCOUNTS);

        and("the number of regular accounts is expected");
        List<PrismObject<ShadowType>> regularAccounts = accounts.stream()
                .filter(account -> !ShadowUtil.isProtected(account))
                .collect(Collectors.toList());
        assertThat(regularAccounts).as("regular accounts").hasSize(REGULAR_INITIAL_OPENDJ_ACCOUNTS);

        and("kind/intent is OK for all accounts (it is defined in the template)");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(INTENT_DEFAULT);
        }
    }

    /** We are going to define the correlation on OpenDJ resource. Let us start with `employeeNumber`, if present. */
    @Test
    public void test210OpenDjCorrelationOnEmpNo() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_2, task, result);

        when("the import is run in simulated development mode"); // TODO later - reconciliation
        String taskOid = importAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "simulated task after")
                .display()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS, 0, PROTECTED_OPENDJ_ACCOUNTS)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, REGULAR_INITIAL_OPENDJ_ACCOUNTS - 1, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(3)
                    .end()
                .end()
                .assertClockworkRunCount(0); // no reactions are there
        // @formatter:on

        assertShadow(getJSmith1OpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(null); // no real execution was there

        if (isNativeRepository()) {
            and("there are no simulation deltas");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(taskOid, result);
            assertDeltaCollection(simulatedDeltas, "simulation deltas")
                    .display()
                    .assertSize(0);
        }
    }

    /** Improving the correlation rules to include email and name. Testing the import. */
    @Test
    public void test220OpenDjCorrelationFinal() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_3, task, result);

        when("the import is run in simulated development mode"); // TODO later - reconciliation
        String taskOid = importAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "simulated task after")
                .display()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS, 0, PROTECTED_OPENDJ_ACCOUNTS)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNLINKED, UNLINKED, null, 2, 0, 0)
                        .assertTransition(null, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 5, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(4)
                    .end()
                .end()
                .assertClockworkRunCount(0); // still no reactions
        // @formatter:on

        String oidUser1 = assertUserByUsername("1", "after")
                .assertLinks(1, 0) // No OpenDJ link (no execution)
                .getOid();
        assertShadow(getJSmith1OpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER) // by empNo
                .assertPotentialOwnerOptions(1)
                .assertCandidateOwners(oidUser1)
                .assertResultingOwner(oidUser1)
                .assertSynchronizationSituation(null);

        String oidUser2 = assertUserByUsername("2", "after")
                .assertLinks(1, 0) // No OpenDJ link (no execution)
                .getOid();
        assertShadow(getJSmith2OpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER) // by mail
                .assertPotentialOwnerOptions(1)
                .assertCandidateOwners(oidUser2)
                .assertResultingOwner(oidUser2)
                .assertSynchronizationSituation(null);

        String oidUser3 = assertUserByUsername("3", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        assertShadow(getAGreenOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN) // by name (1 candidate)
                .assertPotentialOwnerOptions(2)
                .assertCandidateOwners(oidUser3)
                .assertResultingOwner(null)
                .assertSynchronizationSituation(null);

        String oidUser4 = assertUserByUsername("4", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        String oidUser5 = assertUserByUsername("5", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        assertShadow(getRBlackOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN) // by name (2 candidates)
                .assertPotentialOwnerOptions(3)
                .assertCandidateOwners(oidUser4, oidUser5)
                .assertResultingOwner(null)
                .assertSynchronizationSituation(null);

        assertShadow(getBobOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER) // alternate name -> no candidates found
                .assertSynchronizationSituation(null);

        assertShadow(getTeslaOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(null);

        assertShadow(getHackerOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(null);

        assertShadow(getAdminOpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(null);

        assertShadow(getJunior1OpenDjShadow(result), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(null);

        focusCounter.assertNoNewObjects(result);

        if (isNativeRepository()) {
            and("there are no simulation deltas");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(taskOid, result);
            assertDeltaCollection(simulatedDeltas, "simulation deltas")
                    .display()
                    .assertSize(0);
        }
    }

    /**
     * Ref: _(Optional) Account marking phase_
     *
     * We review and mark unmatched shadows:
     *
     * - `tesla`: as "keep but do not touch" (legacy)
     * - `hacker`: as "investigate and delete" (illegal)
     * - `admin`: as protected
     * - `junior1`: as "please add to HR" (pending)
     *
     * *HIGHLY EXPERIMENTAL* Just to implement "Account marking phase" in the First Steps Solution Notes document.
     */
    @Test
    public void test230MarkUnmatchedAccounts() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        when("marking shadows with respective policy situations");
        addShadowPolicySituation(
                getTeslaOpenDjShadow(result).getOid(),
                TEST_POLICY_SITUATION_LEGACY,
                result);
        addShadowPolicySituation(
                getHackerOpenDjShadow(result).getOid(),
                TEST_POLICY_SITUATION_ILLEGAL,
                result);
        addShadowPolicySituation(
                getAdminOpenDjShadow(result).getOid(),
                MODEL_POLICY_SITUATION_PROTECTED_SHADOW,
                result);
        addShadowPolicySituation(
                getJunior1OpenDjShadow(result).getOid(),
                TEST_POLICY_SITUATION_PENDING,
                result);

        when("the import is run in simulated development mode");
        String taskOid = importAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("the task skips marked accounts");
        // @formatter:off
        assertTask(taskOid, "after")
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 8)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNLINKED, UNLINKED, null, 2, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 1, 0, 0)
                        .assertTransition(null, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS + 1)
                        .assertTransition(null, null, null, POLICY_SITUATION, 0, 0, 3)
                        .assertTransitions(5)
                    .end();
        // @formatter:on

        focusCounter.assertNoNewObjects(result);
    }

    /**
     * Ref: _Link accounts phase_
     *
     * We want to link accounts but still without executing any mappings (inbound / outbound).
     *
     * . Two unlinked shadows will be linked automatically by the synchronization reaction.
     * . One unmatched shadow will be linked manually.
     * . One of two disputed shadows will be linked manually, the other one by resolving the respective correlation case.
     */
    @Test
    public void test240LinkAccounts() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_4, task, result);

        when("import task is run (real execution)");
        String taskOid = importAllOpenDjAccountsRequest().execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "simulated task after")
                .display()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 8)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNLINKED, LINKED, null, 2, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 1, 0, 0)
                        .assertTransition(null, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS + 1)
                        .assertTransition(null, null, null, POLICY_SITUATION, 0, 0, 3)
                        .assertTransitions(5)
                    .end()
                .end()
                .assertClockworkRunCount(2); // Two unlinked->linked accounts
        // @formatter:on

        when("Unmatched 'bob' and disputed 'agreen3' are linked manually");
        linkOpenDjAccount(DN_BOB, "5", task, result);
        linkOpenDjAccount(DN_AGREEN3, "3", task, result);

        and("Disputed 'rblack' is linked by resolving the case");
        CaseType aCase = findCorrelationCase(DN_RBLACK, result);
        caseManager.completeWorkItem(
                WorkItemId.of(aCase.getWorkItem().get(0)),
                new AbstractWorkItemOutputType().outcome(
                        OwnerOptionIdentifier.forExistingOwner(
                                        findUserRequired("4").getOid())
                                .getStringValue()),
                null, task, result);

        then("Users are correctly linked");
        assertUserLinked("1", DN_JSMITH1, result);
        assertUserLinked("2", DN_JSMITH2, result);
        assertUserLinked("3", DN_AGREEN3, result);
        assertUserLinked("4", DN_RBLACK, result);
        assertUserLinked("5", DN_BOB, result);

        focusCounter.assertNoNewObjects(result);
    }

    private void assertUserLinked(String username, String dn, OperationResult result) throws CommonException {
        UserType user = findUserRequired(username).asObjectable();
        ShadowType shadow = findOpenDjShadow(dn, result);
        assertUser(user, "")
                .links()
                .by().resourceOid(RESOURCE_OPENDJ_OID).dead(false).find()
                .assertOid(shadow.getOid());
        assertShadow(shadow, "")
                .assertSynchronizationSituation(LINKED);
    }

    private @NotNull CaseType findCorrelationCase(String dn, OperationResult result) throws SchemaException {
        return MiscUtil.requireNonNull(
                correlationCaseManager.findCorrelationCase(
                        findOpenDjShadowRequired(dn, result), true, result),
                () -> "No correlation case for " + dn);
    }

    private void linkOpenDjAccount(String accountName, String userName, Task task, OperationResult result) throws CommonException {
        ObjectReferenceType linkRef =
                ObjectTypeUtil.createObjectRef(
                        MiscUtil.requireNonNull(
                                findShadowByPrismName(accountName, currentOpenDjResource.object, result),
                                () -> "no shadow named " + accountName));
        String userOid = findUserRequired(userName).getOid();

        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .add(linkRef)
                        .asObjectDelta(userOid),
                null, task, result);
    }

    private PrismObject<UserType> findUserRequired(String userName) throws CommonException {
        return MiscUtil.requireNonNull(
                findUserByUsername(userName),
                () -> "no user named " + userName);
    }

    private void reimportAndTestHrResource(CsvResource resource, Task task, OperationResult result)
            throws CommonException, IOException {
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        resource.initializeAndTest(this, task, result);
    }

    private void reimportAndTestOpenDjResource(AnyResource resource, Task task, OperationResult result)
            throws CommonException, IOException {
        deleteObject(ResourceType.class, RESOURCE_OPENDJ_OID, task, result);
        resource.initializeAndTest(this, task, result);
        currentOpenDjResource = resource;
    }

    private ImportAccountsRequestBuilder importHrAccountRequest(String name) {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue(name);
    }

    private ImportAccountsRequestBuilder importAllHrAccountsRequest() {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts();
    }

    private ImportAccountsRequestBuilder importAllOpenDjAccountsRequest() {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_OPENDJ_OID)
                .withImportingAllAccounts();
    }

    private ShadowType findOpenDjShadowRequired(String dn, OperationResult result) throws SchemaException {
        return MiscUtil.requireNonNull(
                findOpenDjShadow(dn, result),
                () -> "No OpenDJ shadow " + dn);
    }

    private ShadowType findOpenDjShadow(String dn, OperationResult result) throws SchemaException {
        return asObjectable(
                findShadowByPrismName(dn, RESOURCE_OPENDJ_2.object, result));
    }

    private ShadowType getJSmith1OpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_JSMITH1, result);
    }

    private ShadowType getJSmith2OpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_JSMITH2, result);
    }

    private ShadowType getAGreenOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_AGREEN3, result);
    }

    private ShadowType getRBlackOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_RBLACK, result);
    }

    private ShadowType getBobOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_BOB, result);
    }

    private ShadowType getTeslaOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_TESLA, result);
    }

    private ShadowType getHackerOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_HACKER, result);
    }

    private ShadowType getAdminOpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_ADMIN, result);
    }

    private ShadowType getJunior1OpenDjShadow(OperationResult result) throws SchemaException {
        return findOpenDjShadow(DN_JUNIOR1, result);
    }
}
