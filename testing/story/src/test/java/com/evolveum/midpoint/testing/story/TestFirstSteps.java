/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.CsvResource;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.testng.annotations.Test;

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

    private static final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class);

    private static CsvResource createHrResource(String fileName) {
        return new CsvResource(TEST_DIR, fileName, RESOURCE_HR_OID, "hr.csv");
    }

    private static final ItemName ATTR_EMP_NO = new ItemName(NS_RI, "empNo");

    @Override
    protected void startResources() throws Exception {
        //openDJController.startCleanServer(); // later
    }

    @AfterClass
    public static void stopResources() {
        //openDJController.stop();
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
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        RESOURCE_HR_2.initializeAndTest(this, task, result);

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
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        RESOURCE_HR_3.initializeAndTest(this, task, result);

        when("single account is imported (on foreground, real execution)");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withAssertSuccess(true) // The model does not "see" the sync configuration
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withAssertSuccess(true) // The model does not "see" the sync configuration
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated development execution)");
        OperationResult subResult = result.createSubresult("simulated development execution");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withAssertSuccess(false)
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
        deleteObject(ResourceType.class, RESOURCE_HR_OID, task, result);
        RESOURCE_HR_4.initializeAndTest(this, task, result);

        when("single account is imported (on foreground, real execution)");
        importSingleAccountRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .build()
                .executeOnForeground(result);

        when("single account is imported (on foreground, simulated production execution)");
        SimulationResult simResult = importSingleAccountRequest()
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
        SimulationResult simResult2 = importSingleAccountRequest()
                .withResourceOid(RESOURCE_HR_OID)
                .withNamingAttribute(ATTR_EMP_NO)
                .withNameValue("1")
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                .build()
                .executeOnForegroundSimulated(getDefaultSimulationConfiguration(), task, result);

        then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
        assertTest130SimulatedDeltas(simResult2.getSimulatedDeltas(), "(foreground)");

        if (isNativeRepository()) {
            when("single account is imported (on background, simulated development execution)");
            String firstTaskOid = importSingleAccountRequest()
                    .withResourceOid(RESOURCE_HR_OID)
                    .withNamingAttribute(ATTR_EMP_NO)
                    .withNameValue("1")
                    .withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT)
                    .build()
                    .execute(result);

            assertTask(firstTaskOid, "simulated production")
                    .display();

            then("there is a single user ADD delta plus not substantial shadow MODIFY delta");
            Collection<ObjectDelta<?>> simulatedDeltas = getTaskSimDeltas(firstTaskOid, result);
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
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .assertNotModifiedPaths(
                        ShadowType.F_ATTRIBUTES, ShadowType.F_CREDENTIALS, ShadowType.F_AUXILIARY_OBJECT_CLASS);
        // @formatter:on
    }
}
