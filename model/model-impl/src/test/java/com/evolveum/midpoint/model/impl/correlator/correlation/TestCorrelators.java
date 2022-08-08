/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import com.evolveum.concepts.func.FailableConsumer;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.api.identities.IndexingConfiguration;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorTestUtil;
import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchCorrelatorFactory;
import com.evolveum.midpoint.model.test.idmatch.DummyIdMatchServiceImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated testing of individual correlators.
 *
 * The tests are based on "accounts file" with source data plus expected correlation results.
 * See the description in the file itself.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelators extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation");

    private static final DummyTestResource RESOURCE_DUMMY_CORRELATION = new DummyTestResource(
            TEST_DIR, "resource-dummy-correlation.xml",
            "4a7f6b3e-64cc-4cd9-b5ba-64ecc47d7d10", "correlation", CorrelatorTestUtil::createAttributeDefinitions);

    /** Names, date of birth, and national ID are indexed using the default (i.e., polystring norm) algorithm. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_DEFAULT_INDEXING = new TestResource<>(
            TEST_DIR, "user-template-default-indexing.xml", "204f3615-bcd7-430d-93ec-c36f1db1dccd");

    /** Names, date of birth, and national ID are indexed using their original value. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_ORIGINAL_INDEXING = new TestResource<>(
            TEST_DIR, "user-template-original-indexing.xml", "c3c93da0-d17e-4926-8208-8441ba745381");

    // TODO
    private static final File FILE_USERS_TRADITIONAL = new File(TEST_DIR, "users-traditional.xml");
    private static final File FILE_USERS_ITEMS = new File(TEST_DIR, "users-items.xml");

    private static final File FILE_ACCOUNTS_EMP = new File(TEST_DIR, "accounts-emp.csv");
    private static final TestCorrelator CORRELATOR_EMP = new TestCorrelator(new File(TEST_DIR, "correlator-emp.xml"));

    private static final File FILE_ACCOUNTS_EMP_FN = new File(TEST_DIR, "accounts-emp-fn.csv");
    private static final TestCorrelator CORRELATOR_EMP_FN = new TestCorrelator(new File(TEST_DIR, "correlator-emp-fn.xml"));

    private static final File FILE_ACCOUNTS_EMP_FN_OPT = new File(TEST_DIR, "accounts-emp-fn-opt.csv");
    private static final TestCorrelator CORRELATOR_EMP_FN_OPT = new TestCorrelator(new File(TEST_DIR, "correlator-emp-fn-opt.xml"));

    private static final File FILE_ACCOUNTS_OWNER = new File(TEST_DIR, "accounts-owner.csv");
    private static final TestCorrelator CORRELATOR_OWNER = new TestCorrelator(new File(TEST_DIR, "correlator-owner.xml"));

    private static final File FILE_ACCOUNTS_OWNER_REF = new File(TEST_DIR, "accounts-owner-ref.csv");
    private static final TestCorrelator CORRELATOR_OWNER_REF = new TestCorrelator(new File(TEST_DIR, "correlator-owner-ref.xml"));

    private static final File FILE_ACCOUNTS_ID_MATCH = new File(TEST_DIR, "accounts-id-match.csv");
    private static final TestCorrelator CORRELATOR_ID_MATCH = new TestCorrelator(new File(TEST_DIR, "correlator-id-match.xml"));

    private static final File FILE_ACCOUNTS_BY_NAME_DEFAULT = new File(TEST_DIR, "accounts-by-name-default.csv");
    private static final TestCorrelator CORRELATOR_BY_NAME_DEFAULT =
            new TestCorrelator(
                    new File(TEST_DIR, "correlator-by-name-default.xml"),
                    USER_TEMPLATE_DEFAULT_INDEXING);

    private static final File FILE_ACCOUNTS_BY_NAME_ORIGINAL = new File(TEST_DIR, "accounts-by-name-original.csv");
    private static final TestCorrelator CORRELATOR_BY_NAME_ORIGINAL =
            new TestCorrelator(
                    new File(TEST_DIR, "correlator-by-name-original.xml"),
                    USER_TEMPLATE_ORIGINAL_INDEXING);

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired private CorrelationCaseManager correlationCaseManager;
    @Autowired private CorrelationService correlationService;
    @Autowired private IdMatchCorrelatorFactory idMatchCorrelatorFactory;

    /** Used by the `id-match` correlator instead of real ID Match Service. */
    private final DummyIdMatchServiceImpl dummyIdMatchService = new DummyIdMatchServiceImpl();

    /** Used for correlation context construction. */
    private ResourceObjectTypeDefinition resourceObjectTypeDefinition;

    /** Used for correlation context construction. */
    private SystemConfigurationType systemConfiguration;

    /** To avoid useless deleting + reloading the users. */
    private File currentlyUsedUsersFile;

    private String currentlyUsedTemplateOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        this.systemConfiguration = getSystemConfiguration();

        initDummyResource(RESOURCE_DUMMY_CORRELATION, initTask, initResult);
        resourceObjectTypeDefinition =
                findObjectTypeDefinitionRequired(
                        RESOURCE_DUMMY_CORRELATION.controller.getRefinedSchema(),
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT);
    }

    @Test
    public void test100CorrelateOnEmployeeNumber() throws Exception {
        executeTest(CORRELATOR_EMP, FILE_USERS_TRADITIONAL, FILE_ACCOUNTS_EMP);
    }

    @Test
    public void test110CorrelateOnEmployeeNumberConfirmingByFamilyName() throws Exception {
        executeTest(CORRELATOR_EMP_FN, FILE_USERS_TRADITIONAL, FILE_ACCOUNTS_EMP_FN);
    }

    @Test
    public void test120CorrelateOnEmployeeNumberConfirmingByFamilyNameExceptForSingleResult() throws Exception {
        executeTest(CORRELATOR_EMP_FN_OPT, FILE_USERS_TRADITIONAL, FILE_ACCOUNTS_EMP_FN_OPT);
    }

    @Test
    public void test150CorrelateUsingEmployeeNumberAsOwnerOidWithFullObject() throws Exception {
        executeTest(CORRELATOR_OWNER, FILE_USERS_TRADITIONAL, FILE_ACCOUNTS_OWNER);
    }

    @Test
    public void test160CorrelateUsingEmployeeNumberAsOwnerOidWithReference() throws Exception {
        executeTest(CORRELATOR_OWNER_REF, FILE_USERS_TRADITIONAL, FILE_ACCOUNTS_OWNER_REF);
    }

    @Test
    public void test190CorrelateUsingIdMatchService() throws Exception {
        executeTest(
                CORRELATOR_ID_MATCH,
                FILE_USERS_TRADITIONAL,
                FILE_ACCOUNTS_ID_MATCH,
                accounts -> {
                    // We need a specific record in our ID Match service.
                    ShadowType ian1 = CorrelatorTestUtil.findAccount(accounts, 1).getShadow();
                    dummyIdMatchService.addRecord("1", ian1.getAttributes(), "9481", null);
                    idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
                });
    }

    @Test
    public void test200CorrelateByNameDefault() throws Exception {
        skipIfNotNativeRepository();
        executeTest(CORRELATOR_BY_NAME_DEFAULT, FILE_USERS_ITEMS, FILE_ACCOUNTS_BY_NAME_DEFAULT);
    }

    @Test
    public void test210CorrelateByNameOriginal() throws Exception {
        skipIfNotNativeRepository();
        executeTest(CORRELATOR_BY_NAME_ORIGINAL, FILE_USERS_ITEMS, FILE_ACCOUNTS_BY_NAME_ORIGINAL);
    }

    @NotNull
    private CorrelationContext createCorrelationContext(
            CorrelationTestingAccount account, TestCorrelator correlator, Task task, OperationResult result)
            throws CommonException {
        ResourceType resource = RESOURCE_DUMMY_CORRELATION.getResource().asObjectable();

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

        return new CorrelationContext(
                account.getShadow(),
                preFocus,
                resource,
                resourceObjectTypeDefinition,
                correlator.getUserTemplate(),
                systemConfiguration, task);
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

    private void executeTest(TestCorrelator correlator, File usersFile, File accountsFile)
            throws ConflictException, EncryptionException, CommonException, IOException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException {
        executeTest(correlator, usersFile, accountsFile, null);
    }

    private void executeTest(
            TestCorrelator correlator,
            File usersFile,
            File accountsFile,
            FailableConsumer<List<CorrelationTestingAccount>, CommonException> additionalInitializer)
            throws CommonException, IOException, ConflictException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException, EncryptionException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("object template is set up");
        TestResource<ObjectTemplateType> userTemplateResource = correlator.userTemplateResource;
        String userTemplateOid = userTemplateResource != null ? userTemplateResource.oid : null;
        if (!Objects.equals(userTemplateOid, currentlyUsedTemplateOid)) {
            if (userTemplateResource != null && userTemplateResource.getObject() == null) {
                repoAdd(userTemplateResource, result);
                userTemplateResource.reload(result);
            }
            System.out.println("Setting user template OID (in system config) to be " + userTemplateOid);
            setDefaultObjectTemplate(UserType.COMPLEX_TYPE, userTemplateOid, result);
            currentlyUsedTemplateOid = userTemplateOid;
            currentlyUsedUsersFile = null; // We need to invalidate the users, as the stored form depends on the template
        }

        and("users are there");
        if (!usersFile.equals(currentlyUsedUsersFile)) {
            deleteUsers(result);
            importObjectsFromFileNotRaw(usersFile, task, result);
            currentlyUsedUsersFile = usersFile;

            displayAllUsersFull();
        }

        and("accounts are loaded");
        RESOURCE_DUMMY_CORRELATION.controller.getDummyResource().clear();
        CorrelatorTestUtil.addAccountsFromCsvFile(this, accountsFile, RESOURCE_DUMMY_CORRELATION);
        var accounts = CorrelatorTestUtil.getAllAccounts(
                this, RESOURCE_DUMMY_CORRELATION, CorrelationTestingAccount::new, task, result);

        if (additionalInitializer != null) {
            additionalInitializer.accept(accounts);
        }

        and("correlator is initialized");
        initializeCorrelator(correlator, task, result);

        for (CorrelationTestingAccount account : accounts) {
            String prefix = "correlating account #" + account.getNumber() + ": ";

            given(prefix + "correlation context is created");
            CorrelationContext context = createCorrelationContext(account, correlator, task, result);

            when(prefix + "correlation is done");
            CorrelationResult correlationResult = correlator.instance.correlate(context, result);

            then(prefix + "correlation result is OK");
            assertCorrelationResult(correlationResult, account, result);
        }
    }

    private void deleteUsers(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, null, null, result);
        for (PrismObject<UserType> user : users) {
            String oid = user.getOid();
            if (!USER_ADMINISTRATOR_OID.equals(oid)) {
                repositoryService.deleteObject(UserType.class, oid, result);
            }
        }
    }

    private void initializeCorrelator(TestCorrelator correlator, Task task, OperationResult result)
            throws CommonException, IOException {
        AbstractCorrelatorType configBean = prismContext.parserFor(correlator.file)
                .parseRealValue(AbstractCorrelatorType.class);
        CorrelatorContext<?> correlatorContext =
                new CorrelatorContext<>(
                        CorrelatorConfiguration.typed(configBean),
                        configBean,
                        null,
                        IdentityManagementConfiguration.of(correlator.getUserTemplate()),
                        IndexingConfiguration.of(correlator.getUserTemplate()),
                        systemConfiguration);
        correlator.instance = correlatorFactoryRegistry.instantiateCorrelator(
                correlatorContext, task, result);
    }

    /** Definition of the correlator and its instance. */
    static class TestCorrelator {
        @NotNull private final File file;
        @Nullable private final TestResource<ObjectTemplateType> userTemplateResource; // loaded on startup
        private Correlator instance; // set on initialization

        TestCorrelator(@NotNull File file) {
            this(file, null);
        }

        TestCorrelator(@NotNull File file, @Nullable TestResource<ObjectTemplateType> userTemplateResource) {
            this.file = file;
            this.userTemplateResource = userTemplateResource;
        }

        ObjectTemplateType getUserTemplate() {
            return userTemplateResource != null ? userTemplateResource.getObjectable() : null;
        }
    }
}
