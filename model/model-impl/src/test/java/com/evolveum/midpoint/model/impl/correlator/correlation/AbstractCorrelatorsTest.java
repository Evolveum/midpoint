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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
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
 * The tests are based on {@link #getAccountsFile()} with source data plus expected correlation results.
 * See the description in the file itself.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractCorrelatorsTest extends AbstractInternalModelIntegrationTest {

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired private CorrelationCaseManager correlationCaseManager;
    @Autowired private CorrelationService correlationService;

    /** Used for correlation context construction. */
    private ResourceObjectTypeDefinition resourceObjectTypeDefinition;

    /** Used for correlation context construction. */
    private SystemConfigurationType systemConfiguration;

    private ObjectTemplateType userTemplate;

    /** Correlator instances for configurations loaded from {@link #getCorrelatorFiles()}. */
    private final Map<String, Correlator> correlatorMap = new HashMap<>();

    /** Fetched testing accounts. */
    List<CorrelationTestingAccount> allAccounts;

    protected abstract DummyTestResource getResource();
    protected abstract TestResource<ObjectTemplateType> getUserTemplateResource();
    protected abstract File getAccountsFile();
    protected abstract File getUsersFile();
    protected abstract File[] getCorrelatorFiles();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        this.systemConfiguration = getSystemConfiguration();

        initDummyResource(getResource(), initTask, initResult);
        TestResource<ObjectTemplateType> template = getUserTemplateResource();
        if (template != null) {
            repoAdd(template, initResult);
            userTemplate =
                    repositoryService
                            .getObject(ObjectTemplateType.class, template.oid, null, initResult)
                            .asObjectable();
            setDefaultObjectTemplate(UserType.COMPLEX_TYPE, userTemplate.getOid(), initResult);
        }

        // The import must be raw to preserve "identities" container in items test.
        importObjectsFromFileRaw(getUsersFile(), initTask, initResult);

        CorrelatorTestUtil.addAccountsFromCsvFile(this, getAccountsFile(), getResource());
        allAccounts = CorrelatorTestUtil.getAllAccounts(
                this, getResource(), CorrelationTestingAccount::new, initTask, initResult);

        initDummyIdMatchService();
        instantiateCorrelators(initTask, initResult);

        resourceObjectTypeDefinition =
                findObjectTypeDefinitionRequired(
                        getResource().controller.getRefinedSchema(),
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT);
    }

    abstract void initDummyIdMatchService() throws SchemaException;

    private void instantiateCorrelators(Task task, OperationResult result) throws CommonException, IOException {
        for (File correlatorFile : getCorrelatorFiles()) {
            AbstractCorrelatorType configBean = prismContext.parserFor(correlatorFile)
                    .parseRealValue(AbstractCorrelatorType.class);
            CorrelatorContext<?> correlatorContext =
                    new CorrelatorContext<>(
                            CorrelatorConfiguration.typed(configBean),
                            configBean,
                            null,
                            IdentityManagementConfiguration.of(userTemplate),
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
        ResourceType resource = getResource().getResource().asObjectable();

        SynchronizationPolicy synchronizationPolicy =
                Objects.requireNonNull(
                        SynchronizationPolicyFactory.forKindAndIntent(
                                ShadowKindType.ACCOUNT,
                                SchemaConstants.INTENT_DEFAULT,
                                resource),
                        "no synchronization policy");

        UserType preFocus =
                correlationService.computePreFocus(
                        account.getShadow(),
                        resource,
                        synchronizationPolicy,
                        UserType.class,
                        task,
                        result);

        CorrelationContext context = new CorrelationContext(
                account.getShadow(),
                preFocus,
                resource,
                resourceObjectTypeDefinition,
                userTemplate,
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
