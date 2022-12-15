/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.test.ldap.OpenDJController.OBJECT_CLASS_INETORGPERSON_QNAME;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import org.opends.server.util.LDIFException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AnyResource;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.CommonException;
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

    private static final File INITIAL_LDIF_FILE = new File(TEST_DIR, "initial.ldif");

    private static final String RESOURCE_OPENDJ_OID = "0934922f-0f63-4768-b1b1-eab4275b31d1";

    private static final TestResource<ResourceType> RESOURCE_OPENDJ_TEMPLATE =
            new TestResource<>(TEST_DIR, "resource-opendj-template.xml", "bb554a60-3e83-40e5-be21-ca913ee58a43");

    private static final AnyResource RESOURCE_OPENDJ_1 = createOpenDjResource("resource-opendj-1.xml");

    private static final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class);

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
        objectsCounter.remember(result);

        given("definition with simple `schemaHandling` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_3, task, result);

        when("single account is imported (on foreground, real execution)");
        importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withAssertingSuccess() // The model does not "see" the sync configuration
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated development execution)");
        OperationResult subResult = result.createSubresult("simulated development execution");
        importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withNotAssertingSuccess()
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
                .executeOnForeground(subResult);
        subResult.close();

        then("the import fails");
        assertThatOperationResult(subResult)
                .isFatalError()
                .hasMessageContaining("No name in new object");

        and("no new focus objects are there");
        objectsCounter.assertNoNewObjects(result);
    }

    /**
     * Adding a mapping for `empNo`. Trying to import an account.
     */
    @Test
    public void test130AddEmpNoMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        objectsCounter.remember(result);

        given("definition with mapping for `empNo` is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_4, task, result);

        when("single account is imported (on foreground, real execution)");
        importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        SimulationResult simResult = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .build()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("no deltas as the configuration is not visible");
        assertDeltaCollection(simResult.getSimulatedDeltas(), "simulated production execution")
                .assertSize(0);

        when("single account is imported (on foreground, simulated development execution)");
        SimulationResult simResult2 = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest130SimulatedDeltas(simResult2.getSimulatedDeltas(), "(foreground)");

        when("single account is imported (on background, simulated development execution)");
        String taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
                .execute(result);

        assertTask(taskOid, "simulated production")
                .display();

        if (isNativeRepository()) {
            then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(taskOid, result);
            assertTest130SimulatedDeltas(simulatedDeltas, "(background)");
        }

        and("no new focus objects are there");
        objectsCounter.assertNoNewObjects(result);
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
        objectsCounter.remember(result);

        given("definition with more mappings (one faulty) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_5, task, result);

        when("single account is imported (on foreground, simulated development execution)");
        SimulationResult simResult1 = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest140SimulatedDeltasSingleAccount(simResult1.getSimulatedDeltas(), "(foreground)");

        when("Whoa! Let us run the full import!");
        String taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts()
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .withNotAssertingSuccess()
                .build()
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
        objectsCounter.assertNoNewObjects(result);
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
        objectsCounter.remember(result);

        given("production-mode definition (with a different bug) is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_6, task, result);

        when("Now it must work. Let the import run!");
        String taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts()
                .withNotAssertingSuccess()
                .build()
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
        objectsCounter.assertUserOnlyIncrement(4, result);
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
        objectsCounter.remember(result);

        given("definition with no bugs is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_7, task, result);

        when("single account is imported (on foreground, simulated production execution)");
        SimulationResult simResult1 = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("4")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .build()
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
        String taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts()
                .withNotAssertingSuccess()
                .build()
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
                .assertInternalOperationExecutionCount(OP_CLOCKWORK_RUN, 1);
        // @formatter:on

        and("one new focus object is there");
        objectsCounter.assertUserOnlyIncrement(1, result);
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
        objectsCounter.remember(result);

        given("improved definition is imported and tested");
        reimportAndTestHrResource(RESOURCE_HR_8, task, result);

        and("a testing employee is added");
        RESOURCE_HR_8.append("999,Alice,Test,atest999@evolveum.com,,testing employee");

        when("the testing employee is imported (on foreground, simulated development execution)");
        SimulationResult simResult1 = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("999")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
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
        SimulationResult simResult4 = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("4")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
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
        String simulatedTaskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts()
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
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
                .assertInternalOperationExecutionCount(OP_CLOCKWORK_RUN, 6); // All users went through the clockwork
        // @formatter:on

        and("no new focus object is there");
        objectsCounter.assertNoNewObjects(result);

        when("the resource is switched into production mode");
        putResourceIntoProduction(RESOURCE_HR_OID, result);

        and("testing account is deleted");
        RESOURCE_HR_8.deleteLine("999,.*");

        when("running production import of all accounts");
        String realTaskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withImportingAllAccounts()
                .build()
                .execute(result);

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
                .assertInternalOperationExecutionCount(OP_CLOCKWORK_RUN, 5); // All users went through the clockwork
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
        objectsCounter.remember(result);

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

        then("there are 5 accounts");
        displayCollection("accounts", accounts);
        assertThat(accounts).as("accounts").hasSize(5);

        and("but only single regular one");
        List<PrismObject<ShadowType>> regularAccounts = accounts.stream()
                .filter(account -> !ShadowUtil.isProtected(account))
                .collect(Collectors.toList());
        assertThat(regularAccounts).as("regular accounts").hasSize(1);

        and("kind/intent is OK (defined in the template)");
        for (PrismObject<ShadowType> account : accounts) {
            assertShadow(account, "account")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(INTENT_DEFAULT);
        }
    }

    private void reimportAndTestHrResource(CsvResource resource, Task task, OperationResult result)
            throws CommonException, IOException {
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        resource.initializeAndTest(this, task, result);
    }
}
