/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.schema.TaskExecutionMode.SIMULATED_DEVELOPMENT;
import static com.evolveum.midpoint.schema.TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;
import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.test.ldap.OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType.UNMODIFIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.PROTECTED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.test.asserter.ProcessedObjectAsserter;
import com.evolveum.midpoint.model.test.asserter.ProcessedObjectsAsserter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.jetbrains.annotations.NotNull;
import org.opends.server.util.LDIFException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.model.test.util.SynchronizationRequest.SynchronizationRequestBuilder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AnyTestResource;
import com.evolveum.midpoint.test.CsvTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Here we check comprehensive "First steps" scenario.
 *
 * The test methods here are chained: they depend on each other, forming one of possible "first steps" uses of midPoint.
 *
 * General idea:
 *
 * . Gradually evolving HR CSV resource in development (`proposed`) and then production (`active`) mode - see `test1xx`
 * . Gradually connecting OpenDJ resource - see `test2xx`
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

    private static final int INITIAL_HR_ACCOUNTS = 6;

    private static final String RESOURCE_HR_OID = "a1864c6e-b154-4384-bc7f-0b0c92379c3f";

    private static final CsvTestResource RESOURCE_HR_100 = createHrResource("resource-hr-100.xml");
    private static final CsvTestResource RESOURCE_HR_110 = createHrResource("resource-hr-110.xml");
    private static final CsvTestResource RESOURCE_HR_120 = createHrResource("resource-hr-120.xml");
    private static final CsvTestResource RESOURCE_HR_130 = createHrResource("resource-hr-130.xml");
    private static final CsvTestResource RESOURCE_HR_140 = createHrResource("resource-hr-140.xml");
    private static final CsvTestResource RESOURCE_HR_150 = createHrResource("resource-hr-150.xml");
    private static final CsvTestResource RESOURCE_HR_160 = createHrResource("resource-hr-160.xml");
    private static final CsvTestResource RESOURCE_HR_170 = createHrResource("resource-hr-170.xml");

    private static final File INITIAL_LDIF_FILE = new File(TEST_DIR, "initial.ldif");

    private static final int PROTECTED_OPENDJ_ACCOUNTS = 4;
    private static final int REGULAR_INITIAL_OPENDJ_ACCOUNTS = 9;
    private static final int ALL_INITIAL_OPENDJ_ACCOUNTS = PROTECTED_OPENDJ_ACCOUNTS + REGULAR_INITIAL_OPENDJ_ACCOUNTS;

    private static final String RESOURCE_OPENDJ_OID = "0934922f-0f63-4768-b1b1-eab4275b31d1";

    private static final TestObject<ResourceType> RESOURCE_OPENDJ_TEMPLATE =
            TestObject.file(TEST_DIR, "resource-opendj-template.xml", "bb554a60-3e83-40e5-be21-ca913ee58a43");

    private static final AnyTestResource RESOURCE_OPENDJ_200 = createOpenDjResource("resource-opendj-200.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_210 = createOpenDjResource("resource-opendj-210.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_220 = createOpenDjResource("resource-opendj-220.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_240 = createOpenDjResource("resource-opendj-240.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_250 = createOpenDjResource("resource-opendj-250.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_260 = createOpenDjResource("resource-opendj-260.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_270 = createOpenDjResource("resource-opendj-270.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_280 = createOpenDjResource("resource-opendj-280.xml");
    private static final AnyTestResource RESOURCE_OPENDJ_290 = createOpenDjResource("resource-opendj-290.xml");
    private AnyTestResource currentOpenDjResource = RESOURCE_OPENDJ_200;

    private static final ObjectsCounter focusCounter = new ObjectsCounter(FocusType.class);

    private static final String NAME_JSMITH1 = "jsmith1";
    private static final String NAME_JSMITH2 = "jsmith2";
    private static final String NAME_AGREEN3 = "agreen3";
    private static final String NAME_RBLACK = "rblack";
    private static final String NAME_BOB = "bob";
    private static final String NAME_EMPNO_6 = "empNo:6";

    private static final String DN_JSMITH1 = "uid=jsmith1,ou=People,dc=example,dc=com";
    private static final String DN_JSMITH2 = "uid=jsmith2,ou=People,dc=example,dc=com";
    private static final String DN_AGREEN3 = "uid=agreen3,ou=People,dc=example,dc=com";
    private static final String DN_RBLACK = "uid=rblack,ou=People,dc=example,dc=com";
    private static final String DN_BOB = "uid=bob,ou=People,dc=example,dc=com";
    private static final String DN_TESLA = "uid=tesla,ou=People,dc=example,dc=com";
    private static final String DN_HACKER = "uid=hacker,ou=People,dc=example,dc=com";
    private static final String DN_ADMIN = "uid=admin,ou=People,dc=example,dc=com";
    private static final String DN_JUNIOR1 = "uid=junior1,ou=People,dc=example,dc=com";
    private static final String DN_EMPNO_6 = "uid=empNo:6,ou=People,dc=example,dc=com";

    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired CaseManager caseManager;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    private static CsvTestResource createHrResource(String fileName) {
        return new CsvTestResource(TEST_DIR, fileName, RESOURCE_HR_OID, "hr.csv");
    }

    private static AnyTestResource createOpenDjResource(String fileName) {
        return AnyTestResource.file(TEST_DIR, fileName, RESOURCE_OPENDJ_OID);
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

    /**
     * Ref: _Step I: Connect; point 6 "I may now list the accounts in the resource through midPoint to see which data I have"_
     *
     * We get some data from HR by providing the most simple definition (`hr-1`) - no `schemaHandling` there, `proposed` state.
     */
    @Test
    public void test100FirstResourceDefinition() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("first definition is imported and tested");
        RESOURCE_HR_100.initAndTest(this, task, result);

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_HR_100.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        then("there are all accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(INITIAL_HR_ACCOUNTS);

        and("there is no known kind/intent");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.UNKNOWN)
                    .assertIntent(SchemaConstants.INTENT_UNKNOWN);
        }
    }

    /**
     * Adding dummy `schemaHandling` - just defining the `account/default` type.
     *
     * Ref: _Step I: Connect_ - but no corresponding item is there (now)
     *
     * Setting `account/default` is not mentioned in the guide. But it perhaps should be. Without defining
     * the object type we are not able to specify any mappings, correlation rules, nor synchronization reactions.
     */
    @Test
    public void test110AddDummySchemaHandling() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("definition with simple `schemaHandling` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_110, task, result);

        when("accounts are retrieved");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_HR_110.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .build(),
                null, task, result);

        then("there are all accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(INITIAL_HR_ACCOUNTS);

        and("they are classified as account/default");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(SchemaConstants.INTENT_DEFAULT);
        }
    }

    /**
     * Ref: _Step I: Connect; point 12 "Set situations and reactions"_
     *
     * Adding a reaction for `UNMATCHED` situation. Trying to import an account.
     * It should fail (in development simulation mode), because there are no mappings yet, so no user name is provided.
     */
    @Test
    public void test120AddUnmatchedReaction() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with simple `schemaHandling` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_120, task, result);

        when("single account is imported (on foreground, real execution)");
        importHrAccountRequest("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration -> hence no failure
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        importHrAccountRequest("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration -> hence no failure
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
     * Ref: _Step I: Connect; part "Start import user phase (repeat until satisfied)"_
     *
     * Adding a mapping for `empNo`. Trying to import an account. It should succeed with simulated user ADD delta.
     */
    @Test
    public void test130AddEmpNoMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with mapping for `empNo` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_130, task, result);

        when("single account is imported (on foreground, real execution)");
        importHrAccountRequest("1")
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        TestSimulationResult simResult = importHrAccountRequest("1")
                .simulatedProduction()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("no deltas as the configuration is not visible");
        assertProcessedObjects(simResult, "simulated production")
                .assertSize(0); // no clockwork

        when("single account is imported (on foreground, simulated development execution)");
        TestSimulationResult simResult2 = importHrAccountRequest("1")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("simulation result contains a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest130SimulationResult(simResult2);

        when("single account is imported (on background, simulated development execution)");
        String taskOid = importHrAccountRequest("1")
                .simulatedDevelopment()
                .execute(result);

        assertTask(taskOid, "simulated development")
                .display();

        then("simulation result contains a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest130SimulationResult(
                getTaskSimResult(taskOid, result));

        and("no new focus objects are there");
        focusCounter.assertNoNewObjects(result);
    }

    private void assertTest130SimulationResult(TestSimulationResult simResult) throws CommonException {
        // @formatter:off
        assertProcessedObjects(simResult)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .assertState(ObjectProcessingStateType.ADDED)
                    .delta()
                        .objectToAdd()
                            .asFocus()
                                .assertName("empNo:1")
                                .assertLinks(1, 0)
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .assertState(ObjectProcessingStateType.MODIFIED)
                    .delta()
                        .assertNoRealResourceObjectModifications();
        // @formatter:on
    }

    /**
     * Ref: _Step I: Connect; part "Start import user phase (repeat until satisfied)"_
     *
     * Adding other mappings, with broken `note` -> `description` mapping.
     * Trying single-user import and then full (simulated) import.
     */
    @Test
    public void test140AddAllMappingWithBrokenDescriptionMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with more mappings (one faulty) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_140, task, result);

        when("single account is imported (on foreground, simulated development execution)");
        TestSimulationResult simResult1 = importHrAccountRequest("1")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest140SimulatedDeltasSingleAccount(simResult1, "(foreground)");

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
                        .assertCommitted(INITIAL_HR_ACCOUNTS - 1, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(INITIAL_HR_ACCOUNTS - 1, 1, 0)
                        .assertLastFailureObjectName("5");
        // @formatter:on

        then("processed objects info is OK");
        // @formatter:off
        assertProcessedObjects(taskOid, "")
                .display()
                .assertSize((INITIAL_HR_ACCOUNTS - 1) * 2 + 1) // N user ADD, N shadow MODIFY, 1 shadow unmodified
                .by().state(UNMODIFIED).find()
                    .assertName("5"); // User does not exist, shadow is not modified because of the exception
        // @formatter:on

        and("no new focus objects are there");
        focusCounter.assertNoNewObjects(result);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTest140SimulatedDeltasSingleAccount(TestSimulationResult simResult, String message) throws CommonException {
        // @formatter:off
        assertProcessedObjects(simResult, "simulated development execution: " + message)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .delta()
                        .objectToAdd()
                            .asUser()
                                .assertName("empNo:1")
                                .assertGivenName("John")
                                .assertFamilyName("Smith")
                                .assertEmailAddress("jsmith1@evolveum.com")
                                .assertTelephoneNumber("+421-123-456-001")
                                .assertLinks(1, 0)
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertNoRealResourceObjectModifications();
        // @formatter:on
    }

    /**
     * Ref: _Step I: Connect; part "Start import user phase (repeat until satisfied)"_
     *
     * The bug is fixed, another one is introduced.
     * In blissful ignorance we run the full import (switching the resource to the production mode).
     */
    @Test
    public void test150FixTheBugIntroduceAnother() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("production-mode definition (with a different bug) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_150, task, result);

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
                        .assertCommitted(INITIAL_HR_ACCOUNTS - 1, 1, 0)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(INITIAL_HR_ACCOUNTS - 1, 1, 0)
                        .assertLastFailureObjectName("4");
        // @formatter:on

        and("four new focus objects are there");
        focusCounter.assertUserOnlyIncrement(INITIAL_HR_ACCOUNTS - 1, result);
        assertUserByUsername("empNo:1", "after")
                .display()
                .assertName("empNo:1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertEmailAddress("jsmith1@evolveum.com")
                .assertTelephoneNumber("+421-123-456-001")
                .assertLinks(1, 0);
        assertNoUserByUsername("empNo:4");
    }

    /**
     * Ref: _Step I: Connect; part "Start import user phase (repeat until satisfied)"_
     *
     * Finally we were able to fix the bug without introducing another one.
     * Let us be more humble and simulate the import first.
     */
    @Test
    public void test160FixAllBugs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("definition with no bugs is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_160, task, result);

        when("single account is imported (on foreground, simulated production execution)");
        TestSimulationResult simResult1 = importHrAccountRequest("4")
                .simulatedProduction()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        // @formatter:off
        assertProcessedObjects(simResult1, "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .delta()
                        .objectToAdd()
                            .asUser()
                                .assertName("empNo:4")
                                .assertGivenName("Robert")
                                .assertFamilyName("Black")
                                .assertEmailAddress("rblack4@evolveum.com")
                                .assertTelephoneNumber("00421-123-456-004")
                                .assertLinks(1, 0)
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertNoRealResourceObjectModifications();
        // @formatter:on

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
                        .assertCommitted(INITIAL_HR_ACCOUNTS, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, INITIAL_HR_ACCOUNTS - 1, 0, 0)
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
     * Ref: _Step I: Connect; part "Start import user phase (repeat until satisfied)"_
     *
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
        reimportAndTestHrResource(RESOURCE_HR_170, task, result);

        and("a testing employee is added");
        RESOURCE_HR_170.append("999,Alice,Test,atest999@evolveum.com,,testing employee");

        when("the testing employee is imported (on foreground, simulated development execution)");
        TestSimulationResult simResult1 = importHrAccountRequest("999")
                .simulatedDevelopment() // because resource is `proposed` now
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        // @formatter:off
        var shadowOid999 = assertProcessedObjects(simResult1, "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find()
                    .delta()
                        .objectToAdd()
                            .asUser()
                                .assertName("empNo:999")
                                .assertEmployeeNumber("999")
                                .assertGivenName("Alice")
                                .assertFamilyName("Test")
                                .assertEmailAddress("atest999@evolveum.com")
                                .assertTelephoneNumber(null)
                                .assertLinks(1, 0)
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertNoRealResourceObjectModifications()
                        .getOid();
        // @formatter:on

        assertRepoShadow(shadowOid999, "test account after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED)
                .assertSynchronizationSituationDescriptionUpdatedButNotFull();

        when("existing employee 4 is imported (on foreground, simulated development execution)");
        TestSimulationResult simResult4 = importHrAccountRequest("4")
                .simulatedDevelopment()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("there is a single user MODIFY delta");
        // @formatter:off
        assertProcessedObjects(simResult4, "simulated production execution")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertModified(
                                UserType.F_EMPLOYEE_NUMBER, // the effect of the newly added mapping
                                UserType.F_METADATA)
                    .end();
        // @formatter:on

        assertShadow(findShadowByPrismName("4", RESOURCE_HR_170.get(), result), "shadow 4 after")
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
                        .assertCommitted(INITIAL_HR_ACCOUNTS + 1, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, INITIAL_HR_ACCOUNTS, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end()
                .end()
                .assertClockworkRunCount(INITIAL_HR_ACCOUNTS + 1); // All users went through the clockwork
        // @formatter:on

        and("no new focus object is there");
        focusCounter.assertNoNewObjects(result);

        when("the resource is switched into production mode");
        putResourceIntoProduction(RESOURCE_HR_OID, result);

        and("testing account is deleted");
        RESOURCE_HR_170.deleteLine("999,.*");

        when("running production import of all accounts");
        String realTaskOid = importAllHrAccountsRequest().execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(realTaskOid, "full import")
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(INITIAL_HR_ACCOUNTS, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, INITIAL_HR_ACCOUNTS, 0, 0)
                        .assertTransitions(1)
                    .end()
                .end()
                .assertClockworkRunCount(INITIAL_HR_ACCOUNTS); // All users went through the clockwork
        // @formatter:on

        and("employeeNumber is set");
        assertUserAfterByUsername("empNo:5")
                .assertEmployeeNumber("5");
    }

    /**
     * Ref: _Step I Connect; part "Connect target system (AD)"_
     *
     * We simply create OpenDJ from template and try to read its content.
     */
    @Test
    public void test200AddOpenDj() throws CommonException, IOException, URISyntaxException, LDIFException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("OpenDJ is started and initialized");
        openDJController.startCleanServer();
        openDJController.addEntriesFromLdifFile(INITIAL_LDIF_FILE);

        and("template and the first version of the resource are imported");
        importObject(RESOURCE_OPENDJ_TEMPLATE, task, result);
        RESOURCE_OPENDJ_200.initAndTest(this, task, result);

        when("OpenDJ content is listed");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_OPENDJ_200.get())
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

    /**
     * Ref: _Step II: Clean up; points 1 "review/update correlation" and 2 "run reconciliation with target system"_
     *
     * We are going to define the correlation on OpenDJ resource. Let us start with `employeeNumber`, if present.
     */
    @Test
    public void test210OpenDjCorrelationOnEmpNo() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_210, task, result);

        when("the reconciliation is run in shadow-simulated development mode");
        String taskOid = reconcileAllOpenDjAccountsRequest()
                .withTaskExecutionMode(SIMULATED_SHADOWS_DEVELOPMENT)
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "simulated task after")
                .display()
                .activityState(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
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
                .activityState(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(0, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                    .end()
                .end()
                .assertClockworkRunCount(0); // no reactions are there
        // @formatter:on

        and("there are 'classification' processed objects");
        Function<
                ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>>,
                ProcessedObjectAsserter<ObjectType, ProcessedObjectsAsserter<Void>>> noOwner = po ->
                po.assertEventMarks(MARK_SHADOW_CORRELATION_STATE_CHANGED)
                        .assertSynchronizationSituationChangedTo(UNMATCHED)
                        .assertCorrelationSituationChangedTo(CorrelationSituationType.NO_OWNER);

        assertProcessedObjects(taskOid, "")
                .display()
                .by().objectOid(getJSmith1OpenDjShadow().getOid()).find(po ->
                        po.assertEventMarks(MARK_SHADOW_CORRELATION_STATE_CHANGED)
                                .assertSynchronizationSituationChangedTo(UNLINKED)
                                .assertCorrelationSituationChangedTo(CorrelationSituationType.EXISTING_OWNER)
                                .assertResultingOwnerChangedTo(getUserRef("empNo:1")))
                .by().objectOid(getJSmith2OpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getAGreenOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getRBlackOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getBobOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getTeslaOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getHackerOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getAdminOpenDjShadow().getOid()).find(noOwner)
                .by().objectOid(getJunior1OpenDjShadow().getOid()).find(noOwner);

        when("the reconciliation is run in simulated development mode");
        String taskOid2 = reconcileAllOpenDjAccountsRequest()
                .withTaskExecutionMode(SIMULATED_DEVELOPMENT)
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid2, "simulated task after")
                .display()
                .activityState(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
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
                .activityState(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(0, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                    .end()
                .end()
                .assertClockworkRunCount(0); // no reactions are there
        // @formatter:on

        assertShadow(getJSmith1OpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(UNLINKED);

        and("there are no processed objects");
        assertProcessedObjects(taskOid2, "")
                .display()
                .assertSize(0);
    }

    /**
     * Ref: _Step II: Clean up; points 1 "review/update correlation" and 2 "run reconciliation with target system"_
     *
     * As previous test ({@link #test210OpenDjCorrelationOnEmpNo()}) but using import instead of reconciliation.
     * This is not covered by the "first steps" document but we want to check that import works as well.
     */
    @Test
    public void test215OpenDjCorrelationOnEmpNoUsingImport() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        when("the import is run in shadow-simulated development mode");
        String taskOid = importAllOpenDjAccountsRequest()
                .withTaskExecutionMode(SIMULATED_SHADOWS_DEVELOPMENT)
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
                        .assertTransition(UNLINKED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, REGULAR_INITIAL_OPENDJ_ACCOUNTS - 1, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(3)
                    .end()
                .end()
                .assertClockworkRunCount(0); // no reactions are there
        // @formatter:on

        assertProcessedObjects(taskOid, "")
                .display();
        // TODO check that only timestamps are to be changed

        when("the import is run in simulated development mode");
        String taskOid2 = importAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid2, "simulated task after")
                .display()
                .rootActivityState()
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS, 0, PROTECTED_OPENDJ_ACCOUNTS)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(UNLINKED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, REGULAR_INITIAL_OPENDJ_ACCOUNTS - 1, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(3)
                    .end()
                .end()
                .assertClockworkRunCount(0); // no reactions are there
        // @formatter:on

        assertShadow(getJSmith1OpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER)
                .assertSynchronizationSituation(UNLINKED);

        and("there are no simulation deltas");
        assertProcessedObjects(taskOid2, "")
                .display()
                .assertSize(0);
    }

    /**
     * Ref: _Step II: Clean up; points 1 "review/update correlation" and 2 "run reconciliation with target system"_
     *
     * Improving the correlation rules to include email and name. Testing the reconciliation.
     */
    @Test
    public void test220OpenDjCorrelationFinal() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_220, task, result);

        when("the reconciliation is run in shadow-simulated development mode");
        String taskOid = reconcileAllOpenDjAccountsRequest()
                .withTaskExecutionMode(SIMULATED_SHADOWS_DEVELOPMENT)
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "simulated task after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS, 0, PROTECTED_OPENDJ_ACCOUNTS)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(UNLINKED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, 5, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(5)
                    .end()
                .end()
                .assertClockworkRunCount(0); // still no reactions
        // @formatter:on

        and("there are some processed objects");
        assertProcessedObjects(taskOid, "")
                .display();
        // TODO

        when("the reconciliation is run in simulated development mode");
        String taskOid2 = reconcileAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid2, "simulated task after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS, 0, PROTECTED_OPENDJ_ACCOUNTS)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(UNLINKED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, UNLINKED, UNLINKED, null, 1, 0, 0)
                        .assertTransition(UNMATCHED, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, 5, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(5)
                    .end()
                .end()
                .assertClockworkRunCount(0); // still no reactions
        // @formatter:on

        String oidUser1 = assertUserByUsername("empNo:1", "after")
                .assertLinks(1, 0) // No OpenDJ link (no execution)
                .getOid();
        assertShadow(getJSmith1OpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER) // by empNo
                .assertPotentialOwnerOptions(1)
                .assertCandidateOwners(oidUser1)
                .assertResultingOwner(oidUser1)
                .assertSynchronizationSituation(UNLINKED);

        String oidUser2 = assertUserByUsername("empNo:2", "after")
                .assertLinks(1, 0) // No OpenDJ link (no execution)
                .getOid();
        assertShadow(getJSmith2OpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.EXISTING_OWNER) // by mail
                .assertPotentialOwnerOptions(1)
                .assertCandidateOwners(oidUser2)
                .assertResultingOwner(oidUser2)
                .assertSynchronizationSituation(UNLINKED);

        String oidUser3 = assertUserByUsername("empNo:3", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        assertShadow(getAGreenOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN) // by name (1 candidate)
                .assertPotentialOwnerOptions(2)
                .assertCandidateOwners(oidUser3)
                .assertResultingOwner(null)
                .assertSynchronizationSituation(DISPUTED);

        String oidUser4 = assertUserByUsername("empNo:4", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        String oidUser5 = assertUserByUsername("empNo:5", "after")
                .assertLinks(1, 0) // No OpenDJ link (no correlation)
                .getOid();
        assertShadow(getRBlackOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN) // by name (2 candidates)
                .assertPotentialOwnerOptions(3)
                .assertCandidateOwners(oidUser4, oidUser5)
                .assertResultingOwner(null)
                .assertSynchronizationSituation(DISPUTED);

        assertShadow(getBobOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER) // alternate name -> no candidates found
                .assertSynchronizationSituation(UNMATCHED);

        assertShadow(getTeslaOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED);

        assertShadow(getHackerOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED);

        assertShadow(getAdminOpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED);

        assertShadow(getJunior1OpenDjShadow(), "shadow after")
                .display()
                .assertCorrelationSituation(CorrelationSituationType.NO_OWNER)
                .assertSynchronizationSituation(UNMATCHED);

        focusCounter.assertNoNewObjects(result);

        and("there are no simulation deltas");
        assertProcessedObjects(taskOid2, "")
                .display()
                .assertSize(0);

        and("there are no correlation (nor other) cases as the reconciliation was simulated");
        assertNoRepoObjects(CaseType.class);
    }

    /**
     * Ref: _Step II: Clean up; (Optional) Account marking phase_
     *
     * We review and mark unmatched shadows:
     *
     * - `tesla`: as "do not touch"
     * - `hacker`: as "decommission later"
     * - `admin`: as protected
     * - `junior1`: as "correlate later"
     */
    @Test
    public void test230MarkUnmatchedAccounts() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        when("marking shadows");
        markShadow(getTeslaOpenDjShadow().getOid(), MARK_DO_NOT_TOUCH.oid, task, result);
        markShadow(getHackerOpenDjShadow().getOid(), MARK_DECOMMISSION_LATER.oid, task, result);
        markShadow(getAdminOpenDjShadow().getOid(), MARK_PROTECTED.oid, task, result);
        markShadow(getJunior1OpenDjShadow().getOid(), MARK_CORRELATE_LATER.oid, task, result);

        when("the reconciliation is run in simulated development mode");
        String taskOid = reconcileAllOpenDjAccountsRequest()
                .simulatedDevelopment()
                .execute(result);

        then("the task skips marked accounts");
        // @formatter:off
        assertTask(taskOid, "after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 8)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(UNLINKED, UNLINKED, UNLINKED, null, 2, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, 1, 0, 0)
                        .assertTransition(DISPUTED, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransition(UNMATCHED, null, null, PROTECTED, 0, 0, 4)
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
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_240, task, result);

        when("import task is run (real execution)");
        String taskOid = reconcileAllOpenDjAccountsRequest().execute(result);

        then("task is OK");
        // @formatter:off
        assertTask(taskOid, "after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 8)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(UNLINKED, UNLINKED, LINKED, null, 2, 0, 0)
                        .assertTransition(UNMATCHED, UNMATCHED, UNMATCHED, null, 1, 0, 0)
                        .assertTransition(DISPUTED, DISPUTED, DISPUTED, null, 2, 0, 0)
                        .assertTransition(UNMATCHED, null, null, PROTECTED, 0, 0, 4)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(5)
                    .end()
                .end()
                .assertClockworkRunCount(2); // Two unlinked->linked accounts
        // @formatter:on

        when("Unmatched 'bob' and disputed 'agreen3' are linked manually");
        linkOpenDjAccount(DN_BOB, "empNo:5", task, result);
        linkOpenDjAccount(DN_AGREEN3, "empNo:3", task, result);

        and("Disputed 'rblack' is linked by resolving the case");
        CaseType aCase = findCorrelationCase(DN_RBLACK, result);
        caseManager.completeWorkItem(
                WorkItemId.of(aCase.getWorkItem().get(0)),
                new AbstractWorkItemOutputType().outcome(
                        OwnerOptionIdentifier.forExistingOwner(
                                        findUserRequired("empNo:4").getOid())
                                .getStringValue()),
                null, task, result);

        then("Users are correctly linked");
        assertUserLinked("empNo:1", DN_JSMITH1);
        assertUserLinked("empNo:2", DN_JSMITH2);
        assertUserLinked("empNo:3", DN_AGREEN3);
        assertUserLinked("empNo:4", DN_RBLACK);
        assertUserLinked("empNo:5", DN_BOB);

        focusCounter.assertNoNewObjects(result);
    }

    /**
     * Ref: _Username import phase_
     *
     * We import usernames from OpenDJ to midPoint. (No outbound name mapping yet.)
     *
     * Missing feature: _Correlation: Candidate Identifier_ (empty usernames)
     * https://docs.evolveum.com/midpoint/methodology/first-steps/solution/#correlation-candidate-identifier
     *
     * Workaround:
     *
     * . We use HR-provided identifiers as temporary user names
     * . Inbound name mapping for OpenDJ has a condition that the existing name was temporary at the start (this is a ugly hack)
     */
    @Test
    public void test250ImportUsernames() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_250, task, result);

        when("previewing the inbound username mapping on single user before running import (foreground, prod sim)");
        TestSimulationResult simResult = importOpenDjAccountRequest(DN_JSMITH1)
                .simulatedProduction()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        then("the user name should change (simulated)");
        assertSimulationResultAfter(simResult);
        // @formatter:off
        assertProcessedObjects(simResult, "single account simulated import")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertNotModifiedExcept(UserType.F_NAME, UserType.F_METADATA)
                        .assertPolyStringModification(UserType.F_NAME, "empNo:1", NAME_JSMITH1)
                    .end()
                .end()
                .by().objectType(ShadowType.class).state(UNMODIFIED)
                    .assertCount(2)
                    .assertNames("1", DN_JSMITH1) // these shadows were processed but not modified
                .end()
                .assertSize(3);
        // @formatter:on

        when("import the single user before running the whole import");
        importOpenDjAccountRequest(DN_JSMITH1).executeOnForeground(result);

        then("user name should really change");
        assertUserByUsername(NAME_JSMITH1, "after")
                .display();

        when("previewing the all-accounts import");
        long before = System.currentTimeMillis();
        String simTaskOid = reconcileAllOpenDjAccountsRequest()
                .simulatedProduction()
                .execute(result);
        long after = System.currentTimeMillis();

        then("task is OK");
        // @formatter:off
        assertTask(simTaskOid, "simulated task after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .display()
                        .assertCommitted(REGULAR_INITIAL_OPENDJ_ACCOUNTS - 4, 0, PROTECTED_OPENDJ_ACCOUNTS + 4)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, 5, 0, 0)
                        .assertTransition(UNMATCHED, null, null, PROTECTED, 0, 0, 4)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, PROTECTED_OPENDJ_ACCOUNTS)
                        .assertTransitions(3)
                    .end()
                .end()
                .assertClockworkRunCount(5);
        // @formatter:on

        and("there is exactly one simulation result");
        Task simTask = taskManager.getTaskPlain(simTaskOid, result);
        Set<String> simulationResultOids =
                new HashSet<>(
                        List.of(
                                getSimulationResultOid(simTask, ActivityPath.empty()),
                                getSimulationResultOid(simTask, RECONCILIATION_OPERATION_COMPLETION_PATH),
                                getSimulationResultOid(simTask, RECONCILIATION_RESOURCE_OBJECTS_PATH),
                                getSimulationResultOid(simTask, RECONCILIATION_REMAINING_SHADOWS_PATH)));
        assertThat(simulationResultOids)
                .as("simulation result OIDs in root activity and sub-activities")
                .hasSize(1);

        and("there are is a simulation result but no deltas");
        TestSimulationResult taskSimResult = getTaskSimResult(simTaskOid, result);
        SimulationResultType simResultBean = taskSimResult.getSimulationResultBean(result);
        long simStartTs = XmlTypeConverter.toMillis(simResultBean.getStartTimestamp());
        assertSimulationResult(simResultBean, "after")
                .display()
                .assertStartTimestampBetween(before, after)
                .assertEndTimestampBetween(simStartTs, after)
                .assertMetricValueByEventMark(CommonInitialObjects.MARK_FOCUS_RENAMED.oid, BigDecimal.valueOf(4));
        // TODO other metrics, also classified ones

        // @formatter:off
        assertProcessedObjects(taskSimResult, "simulation deltas")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).assertCount(4).end()
                .by().objectType(UserType.class).state(UNMODIFIED).find()
                .assertName("jsmith1")
                .end()
                .by().objectType(ShadowType.class).assertCount(10).end() // 5 source, 5 target
                .assertSize(15);
        // @formatter:on

        when("executing the all-accounts reconciliation");
        reconcileAllOpenDjAccountsRequest().execute(result);

        then("linked users now have correct names");
        assertUserByUsername(NAME_JSMITH2, "after").display();
        assertUserByUsername(NAME_AGREEN3, "after").display();
        assertUserByUsername(NAME_RBLACK, "after").display();
        assertUserByUsername(NAME_BOB, "after").display();
        assertUserByUsername(NAME_EMPNO_6, "after").display(); // this one has no OpenDJ account
    }

    /**
     * Ref: _Username import phase_
     *
     * Here we add the outbound "name" mapping for OpenDJ resource. (Development mode.)
     * The first attempt is faulty - the mapping is wrong.
     */
    @Test
    public void test260OutboundUsernamesWrong() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("resource definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_260, task, result);

        when("trying the import (development simulation)");
        OperationResult result1 = result.createSubresult("import");
        TestSimulationResult simResult = importOpenDjAccountRequest(DN_JSMITH1)
                .simulatedDevelopment()
                .withNotAssertingSuccess()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result1);
        result1.close();

        then("the operation should result in failure");
        display("result", result1);
        assertThatOperationResult(result1)
                .isPartialError()
                .hasMessageContaining("String 'jsmith1' is not a DN");

        and("there should be no processed objects");
        assertProcessedObjects(simResult, "single account simulated import")
                .display()
                .by().state(UNMODIFIED)
                    // These are not modified, because there is a failure
                    .assertNames(NAME_JSMITH1, "1", DN_JSMITH1)
                .end()
                .assertSize(3);
    }

    /**
     * Ref: _Username import phase_
     *
     * Here we fix the outbound "name" mapping for OpenDJ resource. (Still development mode.)
     * We check that there are no changes for existing accounts and that the new account would be created with the correct DN.
     */
    @Test
    public void test270OutboundUsernamesFixed() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("resource definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_270, task, result);

        when("trying the single-account import (development simulation)");
        TestSimulationResult simResult = importOpenDjAccountRequest(DN_JSMITH1)
                .simulatedDevelopment()
                .executeOnForegroundSimulated(defaultSimulationDefinition(), task, result);

        and("there should be no deltas, as the DN matches");
        assertProcessedObjects(simResult, "single account simulated import")
                .display()
                .by().state(UNMODIFIED)
                    .assertNames(NAME_JSMITH1, "1", DN_JSMITH1) // not modified because the DN matches
                .end()
                .assertSize(3);

        when("trying the all-accounts import (development simulation)");
        String simTaskOid = importAllOpenDjAccountsRequest() // TODO switch to reconciliation
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        assertTask(simTaskOid, "simulated task after")
                .assertClockworkRunCount(5);

        and("there should be no processed objects, as all the DN match");
        assertProcessedObjects(simTaskOid, "")
                .display()
                .assertSize(3 * 5); // TODO

        when("trying to create the account for John Johnson");
        TestSimulationResult simResult2 =
                executeDeltasInDevelopmentSimulationMode(
                        List.of(createOpenDjAssignmentDelta(NAME_EMPNO_6)),
                        defaultSimulationDefinition(),
                        task, result);

        then("deltas are OK");
        // @formatter:off
        assertProcessedObjects(simResult2, "account creation")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertModified(
                                UserType.F_ASSIGNMENT,
                                UserType.F_LINK_REF,
                                UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find()
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER, DN_EMPNO_6)
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).state(UNMODIFIED).find()
                    .assertName("6")
                .end()
                .assertSize(3);
        // @formatter:on

        focusCounter.assertNoNewObjects(result);
    }

    /**
     * Ref: _Attribute correlation (?) phase_
     *
     * Here we analyze what would midPoint change in OpenDJ, should the outbound mappings be in production mode.
     */
    @Test
    public void test280CorrelateOpenDjAttributes() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        focusCounter.remember(result);

        given("resource definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_280, task, result);

        when("checking outbound mappings by creating the account for John Johnson");
        TestSimulationResult simResult =
                executeDeltasInDevelopmentSimulationMode(
                        List.of(createOpenDjAssignmentDelta(NAME_EMPNO_6)),
                        defaultSimulationDefinition(),
                        task, result);

        then("deltas are OK");
        // @formatter:off
        assertProcessedObjects(simResult, "account creation")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .delta()
                        .assertModified(
                                UserType.F_ASSIGNMENT,
                                UserType.F_LINK_REF,
                                UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find()
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER, DN_EMPNO_6)
                                    .assertValue(QNAME_EMPLOYEE_NUMBER, "6")
                                    .assertNoAttribute(QNAME_MAIL)
                                    .assertValue(QNAME_GIVEN_NAME, "John")
                                    .assertValue(QNAME_SN, "Johnson")
                                    .assertValue(QNAME_CN, "John Johnson")
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).state(UNMODIFIED).find()
                    .assertName("6")
                .end()
                .assertSize(3);
        // @formatter:on

        when("trying the all-accounts import (development simulation)");
        String simTaskOid = importAllOpenDjAccountsRequest() // TODO switch to reconciliation
                .simulatedDevelopment()
                .execute(result);

        then("task is OK");
        assertTask(simTaskOid, "simulated task after")
                .assertClockworkRunCount(5);

        and("task processed objects are OK");
        // @formatter:off
        assertProcessedObjects(simTaskOid, "import")
                .display()
                .by().objectType(UserType.class).objectOid(getUserOid(NAME_JSMITH1)).find()
                    .assertName(NAME_JSMITH1)
                    .assertState(UNMODIFIED) // all is set
                .end()
                .by().objectType(ShadowType.class).objectOid(getHrShadowOid("1")).find()
                    .assertName("1")
                    .assertState(UNMODIFIED)
                .end()
                .by().objectType(ShadowType.class).objectOid(getOpenDjShadowOid(DN_JSMITH1)).find()
                    .assertName(DN_JSMITH1)
                    .assertState(UNMODIFIED) // all is set
                .end()

                .by().objectType(UserType.class).objectOid(getUserOid(NAME_JSMITH2)).find()
                    .assertName(NAME_JSMITH2)
                    .delta()
                        .assertModifiedExclusive(UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).objectOid(getHrShadowOid("2")).find()
                    .assertName("2")
                    .assertState(UNMODIFIED)
                .end()
                .by().objectType(ShadowType.class).objectOid(getOpenDjShadowOid(DN_JSMITH2)).find()
                    .assertName(DN_JSMITH2)
                    .delta()
                        .assertModification(PATH_EMPLOYEE_NUMBER, null, "2")
                        .assertModifiedExclusive(PATH_EMPLOYEE_NUMBER, ShadowType.F_METADATA)
                    .end()
                .end()

                .by().objectType(UserType.class).objectOid(getUserOid(NAME_AGREEN3)).find()
                    .assertName(NAME_AGREEN3)
                    .delta()
                        .assertModifiedExclusive(UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).objectOid(getHrShadowOid("3")).find()
                    .assertName("3")
                    .assertState(UNMODIFIED)
                .end()
                .by().objectType(ShadowType.class).objectOid(getOpenDjShadowOid(DN_AGREEN3)).find()
                    .assertName(DN_AGREEN3)
                    .delta()
                        .assertModification(PATH_EMPLOYEE_NUMBER, null, "3")
                        .assertModification(PATH_MAIL, null, "agreen3@evolveum.com")
                        .assertModifiedExclusive(PATH_EMPLOYEE_NUMBER, PATH_MAIL, ShadowType.F_METADATA)
                    .end()
                .end()

                .by().objectType(UserType.class).objectOid(getUserOid(NAME_RBLACK)).find()
                    .assertName(NAME_RBLACK)
                    .delta()
                        .assertModifiedExclusive(UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).objectOid(getHrShadowOid("4")).find()
                    .assertName("4")
                    .assertState(UNMODIFIED)
                .end()
                .by().objectType(ShadowType.class).objectOid(getOpenDjShadowOid(DN_RBLACK)).find()
                    .assertName(DN_RBLACK)
                    .delta()
                        .assertModification(PATH_EMPLOYEE_NUMBER, null, "4")
                        .assertModification(PATH_MAIL, null, "rblack4@evolveum.com")
                        .assertModifiedExclusive(PATH_EMPLOYEE_NUMBER, PATH_MAIL, ShadowType.F_METADATA)
                    .end()
                .end()

                .by().objectType(UserType.class).objectOid(getUserOid(NAME_BOB)).find()
                    .assertName(NAME_BOB)
                    .delta()
                        .assertModifiedExclusive(UserType.F_METADATA)
                    .end()
                .end()
                .by().objectType(ShadowType.class).objectOid(getHrShadowOid("5")).find()
                    .assertName("5")
                    .assertState(UNMODIFIED)
                .end()
                .by().objectType(ShadowType.class).objectOid(getOpenDjShadowOid(DN_BOB)).find()
                    .assertName(DN_BOB)
                    .delta()
                        .assertModification(PATH_EMPLOYEE_NUMBER, null, "5")
                        .assertModification(PATH_MAIL, null, "rblack5@evolveum.com")
                        .assertModification(PATH_GIVEN_NAME, "Bob", "Robert")
                        .assertModification(PATH_CN, "Bob Black", "Robert Black")
                        .assertModifiedExclusive(PATH_EMPLOYEE_NUMBER, PATH_MAIL, PATH_GIVEN_NAME, PATH_CN, ShadowType.F_METADATA)
                    .end()
                .end()

                .assertSize(15);
        // @formatter:on

        // TODO report the following
        //  - How many accounts would be created, changed, deleted -- can be seen from the deltas
        //  - How many accounts are marked "decommissioned", "to be reviewed", "protected" etc. (outbounds are ignored for them)
        //     -- can be seen from the policy situations; but note: these are not synchronized at all (i.e. not only outbounds are ignored)
        //  - Which attributes will be changed and how many changes (e.g. attribute givenName will be changed in 200 accounts;
        //    attribute dn will be changed in 20 accounts), sorted descending -- can be seen from the deltas
        //  - Table of changes to be made
    }

    /**
     * Ref: _Attribute correlation (?) phase_
     *
     * We are (almost) happy, so let us do the following:
     *
     * - mark shadow "bob" as "do not touch" (TODO not implemented yet)
     * - put all mappings into production mode
     * - run the synchronization task in the production execution mode
     */
    @Test
    public void test290PutOpenDjOutboundsIntoProduction() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource definition is imported and tested");
        reimportAndTestOpenDjResource(RESOURCE_OPENDJ_290, task, result);

        when("running the all-accounts import (production)");
        String taskOid = importAllOpenDjAccountsRequest().execute(result); // TODO switch to reconciliation

        then("task is OK");
        assertTask(taskOid, "simulated task after")
                // TODO check some statistics
                .assertClockworkRunCount(5);

        and("shadows are updated correctly");
        assertUserAndOpenDjShadow(
                NAME_JSMITH1, DN_JSMITH1, "John", "Smith", "jsmith1@evolveum.com", "1");
        assertUserAndOpenDjShadow(
                NAME_JSMITH2, DN_JSMITH2, "John", "Smith", "jsmith2@evolveum.com", "2");
        assertUserAndOpenDjShadow(
                NAME_AGREEN3, DN_AGREEN3, "Alice", "Green", "agreen3@evolveum.com", "3");
        assertUserAndOpenDjShadow(
                NAME_RBLACK, DN_RBLACK, "Robert", "Black", "rblack4@evolveum.com", "4");
        assertUserAndOpenDjShadow(
                NAME_BOB, DN_BOB, "Robert", "Black", "rblack5@evolveum.com", "5");
    }

    private void assertUserAndOpenDjShadow(
            String userName, String dn, String givenName, String familyName, String mail, String employeeNumber)
            throws CommonException {
        String shadowOid = assertUserByUsername(userName, "after")
                .assertGivenName(givenName)
                .assertFamilyName(familyName)
                .assertEmailAddress(mail)
                .assertEmployeeNumber(employeeNumber)
                .links()
                .by().resourceOid(RESOURCE_OPENDJ_OID).find().getOid();
        assertShadow(getShadowModel(shadowOid), "after")
                .attributes()
                .assertValue(QNAME_DN, dn)
                .assertValue(QNAME_GIVEN_NAME, givenName)
                .assertValue(QNAME_SN, familyName)
                .assertValue(QNAME_CN, givenName + " " + familyName)
                .assertValue(QNAME_MAIL, mail)
                .assertValue(QNAME_EMPLOYEE_NUMBER, employeeNumber);
    }

    private void assertUserLinked(String username, String dn) throws CommonException {
        UserType user = findUserRequired(username).asObjectable();
        ShadowType shadow = findOpenDjShadow(dn);
        assertUser(user, "")
                .links()
                .by().resourceOid(RESOURCE_OPENDJ_OID).dead(false).find()
                .assertOid(shadow.getOid());
        assertShadow(shadow, "")
                .assertSynchronizationSituation(LINKED);
    }

    @SuppressWarnings("SameParameterValue")
    private @NotNull CaseType findCorrelationCase(String dn, OperationResult result) throws SchemaException {
        return MiscUtil.requireNonNull(
                correlationCaseManager.findCorrelationCase(
                        findOpenDjShadowRequired(dn), true, result),
                () -> "No correlation case for " + dn);
    }

    private void linkOpenDjAccount(String accountName, String userName, Task task, OperationResult result)
            throws CommonException {
        executeChanges(
                createOpenDjLinkDelta(userName, accountName, result),
                null, task, result);
    }

    private ObjectDelta<ObjectType> createOpenDjLinkDelta(String userName, String accountName, OperationResult result)
            throws CommonException {
        ObjectReferenceType linkRef =
                ObjectTypeUtil.createObjectRef(
                        MiscUtil.requireNonNull(
                                findShadowByPrismName(accountName, currentOpenDjResource.get(), result),
                                () -> "no shadow named " + accountName));
        String userOid = findUserRequired(userName).getOid();
        return deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(linkRef)
                .asObjectDelta(userOid);
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectDelta<ObjectType> createOpenDjAssignmentDelta(String userName)
            throws CommonException {
        String userOid = findUserRequired(userName).getOid();
        return deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .construction(
                                new ConstructionType()
                                        .resourceRef(RESOURCE_OPENDJ_OID, ResourceType.COMPLEX_TYPE)))
                .asObjectDelta(userOid);
    }

    private String getHrShadowOid(String name) throws CommonException {
        return findHrShadowRequired(name).getOid();
    }

    private String getOpenDjShadowOid(String dn) throws CommonException {
        return findOpenDjShadowRequired(dn).getOid();
    }

    private String getUserOid(String name) {
        try {
            return findUserRequired(name).getOid();
        } catch (CommonException e) {
            throw new AssertionError(e);
        }
    }

    private ObjectReferenceType getUserRef(String name) {
        return ObjectTypeUtil.createObjectRef(getUserOid(name), ObjectTypes.USER);
    }

    private PrismObject<UserType> findUserRequired(String userName) throws CommonException {
        return MiscUtil.requireNonNull(
                findUserByUsername(userName),
                () -> "no user named " + userName);
    }

    private void reimportAndTestHrResource(CsvTestResource resource, Task task, OperationResult result)
            throws CommonException, IOException {
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        resource.initAndTest(this, task, result);
    }

    private void reimportAndTestOpenDjResource(AnyTestResource resource, Task task, OperationResult result)
            throws CommonException, IOException {
        deleteObject(ResourceType.class, RESOURCE_OPENDJ_OID, task, result);
        resource.initAndTest(this, task, result);
        currentOpenDjResource = resource;
    }

    private SynchronizationRequestBuilder importHrAccountRequest(String name) {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue(name);
    }

    private SynchronizationRequestBuilder importAllHrAccountsRequest() {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withProcessingAllAccounts();
    }

    @SuppressWarnings("SameParameterValue")
    private SynchronizationRequestBuilder importOpenDjAccountRequest(String dn) {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_OPENDJ_OID)
                .withNamingAttribute(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER)
                .withNameValue(dn);
    }

    private SynchronizationRequestBuilder importAllOpenDjAccountsRequest() {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_OPENDJ_OID)
                .withProcessingAllAccounts();
    }

    private SynchronizationRequestBuilder reconcileAllOpenDjAccountsRequest() {
        return reconcileAccountsRequest()
                .withResourceOid(RESOURCE_OPENDJ_OID)
                .withProcessingAllAccounts();
    }

    private ShadowType findHrShadowRequired(String name) throws SchemaException {
        return MiscUtil.requireNonNull(
                findHrShadow(name),
                () -> "No HR shadow " + name);
    }

    private ShadowType findHrShadow(String name) throws SchemaException {
        return asObjectable(
                findShadowByPrismName(name, RESOURCE_HR_100.get(), getTestOperationResult()));
    }

    private ShadowType findOpenDjShadowRequired(String dn) throws SchemaException {
        return MiscUtil.requireNonNull(
                findOpenDjShadow(dn),
                () -> "No OpenDJ shadow " + dn);
    }

    private ShadowType findOpenDjShadow(String dn) throws SchemaException {
        return asObjectable(
                findShadowByPrismName(dn, RESOURCE_OPENDJ_210.get(), getTestOperationResult()));
    }

    private ShadowType getJSmith1OpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_JSMITH1);
    }

    private ShadowType getJSmith2OpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_JSMITH2);
    }

    private ShadowType getAGreenOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_AGREEN3);
    }

    private ShadowType getRBlackOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_RBLACK);
    }

    private ShadowType getBobOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_BOB);
    }

    private ShadowType getTeslaOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_TESLA);
    }

    private ShadowType getHackerOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_HACKER);
    }

    private ShadowType getAdminOpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_ADMIN);
    }

    private ShadowType getJunior1OpenDjShadow() throws SchemaException {
        return findOpenDjShadow(DN_JUNIOR1);
    }
}
