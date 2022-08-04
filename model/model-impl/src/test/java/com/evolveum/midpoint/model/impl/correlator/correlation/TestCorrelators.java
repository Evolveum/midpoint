/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelatorFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated testing of individual correlators.
 *
 * The tests are based on {@link #FILE_ACCOUNTS} with source data plus expected correlation results.
 * See the description in the file itself.
 *
 * Correlation cases: tests if they are created (or not), but does not check their content.
 * This is done in {@link TestExpressionCorrelator}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelators extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR =
            new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation/correlators");

    private static final DummyTestResource RESOURCE_DETERMINISTIC = new DummyTestResource(
            TEST_DIR, "resource-dummy-correlation.xml",
            "4a7f6b3e-64cc-4cd9-b5ba-64ecc47d7d10", "correlation", CorrelatorTestUtil::createAttributeDefinitions);

    /**
     * Contains data for executing the tests. Please see comments in the file itself.
     */
    private static final File FILE_ACCOUNTS = new File(TEST_DIR, "accounts.csv");

    /**
     * Users against which we correlate the accounts.
     */
    private static final File FILE_USERS = new File(TEST_DIR, "users.xml");

    private static final File[] CORRELATOR_FILES = {
            new File(TEST_DIR, "correlator-emp.xml"),
            new File(TEST_DIR, "correlator-emp-fn.xml"),
            new File(TEST_DIR, "correlator-emp-fn-opt.xml"),
            new File(TEST_DIR, "correlator-owner.xml"),
            new File(TEST_DIR, "correlator-owner-ref.xml"),
            new File(TEST_DIR, "correlator-id-match.xml")
    };

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired private IdMatchCorrelatorFactory idMatchCorrelatorFactory;
    @Autowired private CorrelationCaseManager correlationCaseManager;

    /** Used for correlation context construction. */
    private ResourceObjectTypeDefinition resourceObjectTypeDefinition;

    /** Used for correlation context construction. */
    private SystemConfigurationType systemConfiguration;

    /** Used by the `id-match` correlator instead of real ID Match Service. */
    private final DummyIdMatchServiceImpl dummyIdMatchService = new DummyIdMatchServiceImpl();

    /** Correlator instances for configurations loaded from {@link #CORRELATOR_FILES}. */
    private final Map<String, Correlator> correlatorMap = new HashMap<>();

    /** Fetched testing accounts. */
    private List<CorrelationTestingAccount> allAccounts;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        this.systemConfiguration = getSystemConfiguration();

        initDummyResource(RESOURCE_DETERMINISTIC, initTask, initResult);

        importObjectsFromFileNotRaw(FILE_USERS, initTask, initResult);
        CorrelatorTestUtil.addAccountsFromCsvFile(this, FILE_ACCOUNTS, RESOURCE_DETERMINISTIC);
        allAccounts = CorrelatorTestUtil.getAllAccounts(
                this, RESOURCE_DETERMINISTIC, CorrelationTestingAccount::new, initTask, initResult);

        initDummyIdMatchService();
        instantiateCorrelators(initTask, initResult);

        resourceObjectTypeDefinition =
                findObjectTypeDefinitionRequired(
                        RESOURCE_DETERMINISTIC.controller.getRefinedSchema(),
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT);
    }

    /**
     * We need specific records in our ID Match service.
     */
    private void initDummyIdMatchService() throws SchemaException {
        ShadowType ian200 = CorrelatorTestUtil.findAccount(allAccounts, 200).getShadow();
        dummyIdMatchService.addRecord("200", ian200.getAttributes(), "9481", null);
        idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
    }

    private void instantiateCorrelators(Task task, OperationResult result) throws CommonException, IOException {
        for (File correlatorFile : CORRELATOR_FILES) {
            AbstractCorrelatorType configBean = prismContext.parserFor(correlatorFile)
                    .parseRealValue(AbstractCorrelatorType.class);
            CorrelatorContext<?> correlatorContext =
                    new CorrelatorContext<>(
                            CorrelatorConfiguration.typed(configBean),
                            configBean,
                            null,
                            IdentityManagementConfiguration.of(null),
                            systemConfiguration);
            Correlator correlator = correlatorFactoryRegistry.instantiateCorrelator(
                    correlatorContext, task, result);
            correlatorMap.put(configBean.getName(), correlator);
        }
    }

    /**
     * Sequentially processes all accounts, pushing them to correlator and checking its response.
     */
    @Test
    public void test100ProcessAccounts() throws CommonException {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        for (CorrelationTestingAccount account : allAccounts) {
            processAccount(account, task, result);
        }
    }

    private void processAccount(CorrelationTestingAccount account, Task task, OperationResult result)
            throws CommonException {
        when("correlating account #" + account.getNumber());

        String correlatorName = Objects.requireNonNull(
                account.getCorrelator(), "no correlator specified");
        Correlator correlator = Objects.requireNonNull(
                correlatorMap.get(correlatorName), () -> "unknown correlator " + correlatorName);

        UserType preFocus = new UserType();
        MatchingUtil.copyAttributes(preFocus, account.getShadow());

        CorrelationContext context = new CorrelationContext(
                account.getShadow(),
                preFocus,
                RESOURCE_DETERMINISTIC.getResource().asObjectable(),
                resourceObjectTypeDefinition,
                null,
                systemConfiguration, task);

        then("correlating account #" + account.getNumber());

        CorrelationResult correlationResult = correlator.correlate(context, result);
        assertCorrelationResult(correlationResult, account, result);
    }

    private void assertCorrelationResult(
            CorrelationResult correlationResult, CorrelationTestingAccount account, OperationResult result)
            throws SchemaException {

        displayDumpable("Correlation result", correlationResult);

        assertThat(correlationResult.getSituation())
                .as("correlation result status")
                .isEqualTo(account.getExpectedCorrelationSituation());

        if (correlationResult.getSituation() == CorrelationSituationType.EXISTING_OWNER) {
            ObjectType realOwner = correlationResult.getOwner();
            assertThat(realOwner).as("correlated owner").isNotNull();
            String expectedOwnerName = account.getExpectedOwnerName();
            assertThat(realOwner.getName().getOrig()).as("owner name").isEqualTo(expectedOwnerName);
        }

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(account.getShadow(), false, result);
        if (account.shouldCorrelationCaseExist()) {
            assertThat(correlationCase).as("correlation case").isNotNull();
            displayDumpable("Correlation case", correlationCase);
        } else {
            assertThat(correlationCase).as("correlation case").isNull();
        }
    }
}
