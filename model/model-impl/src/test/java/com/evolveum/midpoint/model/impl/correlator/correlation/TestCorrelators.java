/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import static com.evolveum.midpoint.model.impl.correlator.correlation.TestCorrelators.DescriptionMode.*;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;

import static org.assertj.core.api.Assertions.offset;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CandidateDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationPropertyValuesDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.Match;
import com.evolveum.midpoint.model.impl.correlation.TemplateCorrelationConfigurationImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.concepts.func.FailableConsumer;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.CorrelationService.CorrelationCaseDescriptionOptions;
import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
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
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Isolated testing of individual correlators.
 *
 * The tests are based on "accounts file" with source data plus expected correlation results. The requirements are:
 *
 * . The `uid` has to be a pure integer. The accounts are processed in the order of their `uid`.
 * . The `expCandidates` column describes the expected candidates as returned from the correlator.
 * . The `expResult` column describes the result from the correlation service:
 * .. `_none` means that no matching
 * .. `_uncertain` means that the correlator couldn't decide
 * .. a name is a name of a specific user
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelators extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation");

    /** The vehicle for correlator testing. Contains the accounts that are correlated. */
    private static final DummyTestResource RESOURCE_DUMMY_CORRELATION = new DummyTestResource(
            TEST_DIR, "resource-dummy-correlation.xml",
            "4a7f6b3e-64cc-4cd9-b5ba-64ecc47d7d10", "correlation", CorrelatorTestUtil::createAttributeDefinitions);

    // The following templates are used for testing the smart correlator (in various settings).

    /** Names, date of birth, and national ID are indexed using the default (i.e., polystring norm) algorithm. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_DEFAULT_INDEXING = new TestResource<>(
            TEST_DIR, "user-template-default-indexing.xml", "204f3615-bcd7-430d-93ec-c36f1db1dccd");

    /** Names, date of birth, and national ID are indexed using their original value. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_ORIGINAL_INDEXING = new TestResource<>(
            TEST_DIR, "user-template-original-indexing.xml", "c3c93da0-d17e-4926-8208-8441ba745381");

    /** Names, date of birth, and national ID are indexed in various (complex) ways. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_COMPLEX = new TestResource<>(
            TEST_DIR, "user-template-complex.xml", "dc393b43-e125-4ebf-987d-366c57120e96");

    /** TODO. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_MATCHING_RULES_MAIN = new TestResource<>(
            TEST_DIR, "user-template-matching-rules-main.xml", "e18dc8f8-0e88-4a4d-afb0-3248c704599a");

    /** TODO. */
    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_MATCHING_RULES_CHILD = new TestResource<>(
            TEST_DIR, "user-template-matching-rules-child.xml", "a9759cd0-d3e1-4aa6-9c26-54e879b4994c");

    /** Used for 1xx tests (filter, expression, and ID Match correlators). */
    private static final File FILE_USERS_TRADITIONAL = new File(TEST_DIR, "users-traditional.xml");

    /** Used for majority of 2xx tests (items a.k.a. smart correlator). */
    private static final File FILE_USERS_SMART_BASIC = new File(TEST_DIR, "users-smart-basic.xml");

    /** Used for "matching rules" test. */
    private static final File FILE_USERS_SMART_MATCHING_RULES = new File(TEST_DIR, "users-smart-matching-rules.xml");

    @Autowired private CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired private CorrelationServiceImpl correlationService;
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

        // This object should be there "in the background". It is referenced from the main template for matching rules.
        repoAdd(USER_TEMPLATE_MATCHING_RULES_CHILD, initResult);
    }

    /**
     * Correlate solely on the basis of `employeeNumber` (legacy correlator).
     */
    @Test
    public void test100FilterByEmployeeNumber() throws Exception {
        executeTest("filter-by-employee-number", FILE_USERS_TRADITIONAL);
    }

    /**
     * Correlates on `employeeNumber`, confirms on `familyName`.
     */
    @Test
    public void test110FilterByEmployeeNumberConfirmByFamilyName() throws Exception {
        executeTest("filter-by-employee-number-confirm-by-family-name", FILE_USERS_TRADITIONAL);
    }

    /**
     * Correlates on `employeeNumber`, confirms on `familyName`.
     * Skipping the confirmation if there's only a single candidate.
     */
    @Test
    public void test120FilterByEmployeeNumberConfirmByFamilyNameExceptForSingleResult() throws Exception {
        executeTest("filter-by-employee-number-confirm-by-family-name-except-for-single-result", FILE_USERS_TRADITIONAL);
    }

    /**
     * Tests "owner expression" correlator. A bit hacked: Uses `employeeNumber` as owner OID.
     */
    @Test
    public void test150OwnerExpression() throws Exception {
        executeTest("owner-expression",
                FILE_USERS_TRADITIONAL,
                DESCRIBE_ONLY, // We skip the explanation because "owner" correlator does not support it.
                null);
    }

    /**
     * Tests "ownerRef expression" correlator. A bit hacked: Uses `employeeNumber` as owner OID.
     */
    @Test
    public void test160OwnerRefExpression() throws Exception {
        executeTest(
                "owner-ref-expression",
                FILE_USERS_TRADITIONAL,
                DESCRIBE_ONLY, // We skip the explanation because "owner" correlator does not support it.
                null);
    }

    /**
     * Tests ID Match correlator.
     */
    @Test
    public void test190IdMatch() throws Exception {
        executeTest(
                "id-match",
                FILE_USERS_TRADITIONAL,
                FULL,
                accounts -> {
                    // We need a specific record in our ID Match service.
                    ShadowType ian1 = CorrelatorTestUtil.findAccount(accounts, 1).getShadow();
                    dummyIdMatchService.addRecord("1", ian1.getAttributes(), "9481", null);
                    idMatchCorrelatorFactory.setServiceOverride(dummyIdMatchService);
                });
    }

    /**
     * Correlating by given name and family name, using default indexing.
     */
    @Test
    public void test200SmartByNameWithDefaultIndexing() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-by-name-with-default-indexing",
                FILE_USERS_SMART_BASIC,
                USER_TEMPLATE_DEFAULT_INDEXING);
    }

    /**
     * Correlating by given name and family name, using "original value" indexing.
     */
    @Test
    public void test210SmartByNameWithOriginalIndexing() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-by-name-with-original-indexing",
                FILE_USERS_SMART_BASIC,
                USER_TEMPLATE_ORIGINAL_INDEXING);
    }

    /**
     * Correlating by given name and family name, using fuzzy search with default (gradual) confidence.
     */
    @Test
    public void test220SmartByNameFuzzy() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-by-name-fuzzy",
                FILE_USERS_SMART_BASIC,
                USER_TEMPLATE_DEFAULT_INDEXING);
    }

    /**
     * Correlating by given name and family name, using fuzzy search with confidence fixed to 1.
     */
    @Test
    public void test225SmartByNameFuzzyFixedConfidence() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-by-name-fuzzy-fixed-confidence",
                FILE_USERS_SMART_BASIC,
                USER_TEMPLATE_DEFAULT_INDEXING);
    }

    /**
     * Complex smart correlation scenario with multiple rules.
     */
    @Test
    public void test230SmartMultiRuleComplex() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-multi-rule-complex",
                FILE_USERS_SMART_BASIC,
                USER_TEMPLATE_COMPLEX);

        // Just for completeness, let us check the normalizations
        // @formatter:off
        assertUserAfter(findUserByUsernameFullRequired("smith1"))
                .identities()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "john", "ian")
                        .assertNormalizedItem("familyName.norm", "smith")
                        .assertNormalizedItem("familyName.orig", "Smith")
                        .assertNormalizedItem("familyName.polyStringNorm.prefix3", "smi")
                        .assertNormalizedItem("nationalId.digits", "0402061111");
        // @formatter:on
    }

    /**
     * Tests whether matching rules are applied correctly.
     * Also, tests whether "include" directives in object templates are respected.
     *
     * See the CSV file for the description of items to correlate.
     */
    @Test
    public void test250SmartWithMatchingRules() throws Exception {
        skipIfNotNativeRepository();
        executeTest(
                "smart-with-matching-rules",
                FILE_USERS_SMART_MATCHING_RULES,
                USER_TEMPLATE_MATCHING_RULES_MAIN);

        // Just for completeness, let us check the normalizations
        // @formatter:off
        assertUserAfter(findUserByUsernameFullRequired("smith1"))
                .identities()
                    .normalizedData()
                        .assertNormalizedItem("familyName.polyStringNorm", "smith");
        // @formatter:on
    }

    @SuppressWarnings("SameParameterValue")
    private void executeTest(String name, File usersFile)
            throws ConflictException, EncryptionException, CommonException, IOException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException {
        executeTest(
                correlator(name),
                usersFile,
                accountsFile(name),
                FULL,
                null);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeTest(String name, File usersFile, TestResource<ObjectTemplateType> template)
            throws ConflictException, EncryptionException, CommonException, IOException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException {
        executeTest(
                correlator(name, template),
                usersFile,
                accountsFile(name),
                FULL,
                null);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeTest(String name, File usersFile, TestResource<ObjectTemplateType> template, DescriptionMode mode)
            throws ConflictException, EncryptionException, CommonException, IOException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException {
        executeTest(
                correlator(name, template),
                usersFile,
                accountsFile(name),
                mode,
                null);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeTest(
            String name,
            File usersFile,
            DescriptionMode descriptionMode,
            FailableConsumer<List<CorrelationTestingAccount>, CommonException> additionalInitializer)
            throws CommonException, IOException, ConflictException, SchemaViolationException,
            InterruptedException, ObjectAlreadyExistsException, EncryptionException {
        executeTest(
                correlator(name),
                usersFile,
                accountsFile(name),
                descriptionMode,
                additionalInitializer);
    }

    private void executeTest(
            TestCorrelator correlator,
            File usersFile,
            File accountsFile,
            DescriptionMode descriptionMode,
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
                ObjectTemplateType expanded = archetypeManager.getExpandedObjectTemplate(userTemplateResource.oid, result);
                userTemplateResource.object = expanded.asPrismObject();
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
            displayDumpable("account", account);
            CorrelationContext correlationContext = createCorrelationContext(account, correlator, task, result);

            when(prefix + "correlation is done (using a correlator)");
            CorrelationResult correlationResult = correlator.instance.correlate(correlationContext, result);

            then(prefix + "correlation result is OK");
            assertCorrelationResult(correlationResult, account);

            when(prefix + "correlation is done (using CorrelationService)");
            CompleteCorrelationResult completeCorrelationResult =
                    correlationService.correlate(
                            correlator.correlatorContext,
                            correlationContext,
                            result);

            then(prefix + "correlation result is OK");
            assertCompleteCorrelationResult(completeCorrelationResult, account);

            when(prefix + "case description is requested");
            CorrelationCaseDescription<?> description =
                    describeCorrelationCase(
                            correlator.correlatorContext, correlationContext, completeCorrelationResult, descriptionMode, task, result);

            then(prefix + "case description is OK");
            displayDumpable("case description", description);
            assertCorrelationDescription(description, descriptionMode, account);
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
        correlator.correlatorContext =
                new CorrelatorContext<>(
                        CorrelatorConfiguration.typed(configBean),
                        configBean,
                        getSynchronizationPolicy().getCorrelationDefinition(), // it is OK that there's no correlator info here
                        TemplateCorrelationConfigurationImpl.of(correlator.getUserTemplate()),
                        systemConfiguration);
        correlator.instance = correlatorFactoryRegistry.instantiateCorrelator(
                correlator.correlatorContext, task, result);
    }

    @NotNull
    private CorrelationContext createCorrelationContext(
            CorrelationTestingAccount account, TestCorrelator correlator, Task task, OperationResult result)
            throws CommonException {
        ResourceType resource = RESOURCE_DUMMY_CORRELATION.getResource().asObjectable();

        SynchronizationPolicy synchronizationPolicy = getSynchronizationPolicy();

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

    private @NotNull SynchronizationPolicy getSynchronizationPolicy() throws SchemaException, ConfigurationException {
        return requireNonNull(
                SynchronizationPolicyFactory.forKindAndIntent(
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT,
                        RESOURCE_DUMMY_CORRELATION.getResource().asObjectable()),
                "no synchronization policy");
    }

    private void assertCorrelationResult(
            CorrelationResult correlationResult, CorrelationTestingAccount account) {
        displayDumpable("Correlation result", correlationResult);
        assertCandidateOwnersMap(
                account.getExpectedCandidateOwners(false),
                correlationResult.getCandidateOwnersMap());
    }

    private void assertCompleteCorrelationResult(
            CompleteCorrelationResult completeResult, CorrelationTestingAccount account) {

        displayDumpable("Correlation result", completeResult);

        assertThat(completeResult.getSituation())
                .as("correlation result status")
                .isEqualTo(account.getExpectedCorrelationSituation());

        if (completeResult.getSituation() == CorrelationSituationType.EXISTING_OWNER) {
            ObjectType realOwner = completeResult.getOwner();
            assertThat(realOwner).as("correlated owner").isNotNull();
            String expectedOwnerName = account.getExpectedOwnerName();
            assertThat(realOwner.getName().getOrig()).as("owner name").isEqualTo(expectedOwnerName);
        }

        assertCandidateOwnersMap(
                account.getExpectedCandidateOwners(true),
                completeResult.getCandidateOwnersMap());
    }

    private void assertCandidateOwnersMap(
            Collection<TestCandidateOwner> expectedOwnerOptions, CandidateOwnersMap completeResult) {
        Set<TestCandidateOwner> realOwnerOptions = getRealOwnerOptions(completeResult);
        assertThat(realOwnerOptions)
                .as("owner options")
                .containsExactlyInAnyOrderElementsOf(expectedOwnerOptions);
    }

    private @NotNull Set<TestCandidateOwner> getRealOwnerOptions(@NotNull CandidateOwnersMap candidateOwnersMap) {
        Set<TestCandidateOwner> candidateOwnerSet = new HashSet<>();
        for (CandidateOwner candidateOwner : candidateOwnersMap.values()) {
            candidateOwnerSet.add(
                    TestCandidateOwner.of(candidateOwner));
        }
        return candidateOwnerSet;
    }

    private CorrelationCaseDescription<?> describeCorrelationCase(
            CorrelatorContext<?> correlatorContext,
            CorrelationContext correlationContext,
            CompleteCorrelationResult completeCorrelationResult,
            DescriptionMode descriptionMode,
            Task task,
            OperationResult result) throws CommonException {
        ResourceObjectOwnerOptionsType optionsBean = completeCorrelationResult.getOwnerOptions();
        if (optionsBean == null || descriptionMode == NONE) {
            System.out.println("No options (or description mode is NONE), skipping testing the correlation description");
            return null;
        } else {
            CorrelationCaseDescriptionOptions options =
                    new CorrelationCaseDescriptionOptions().explain(descriptionMode != DESCRIBE_ONLY);
            return correlationService.describeCorrelationCase(
                    correlatorContext, correlationContext, optionsBean.getOption(), options, task, result);
        }
    }

    private void assertCorrelationDescription(
            CorrelationCaseDescription<?> description, DescriptionMode descriptionMode, CorrelationTestingAccount account) {
        if (description == null) {
            return;
        }

        List<ExpectedMatches> expectedCandidateMatches = account.getExpectedMatches();
        List<TestCandidateOwner> expectedCandidates = account.getExpectedCandidateOwners(true);
        if (descriptionMode == FULL) {
            assertThat(description.getCandidates())
                    .as("candidates in description")
                    .hasSize(expectedCandidates.size());
            for (int i = 0; i < expectedCandidates.size(); i++) {
                TestCandidateOwner expectedCandidate = expectedCandidates.get(i);
                ExpectedMatches expectedMatches = expectedCandidateMatches.size() > i ? expectedCandidateMatches.get(i) : null;
                CandidateDescription<?> candidateDescription =
                        MiscUtil.extractSingletonRequired(
                                description.getCandidates().stream()
                                        .filter(c -> expectedCandidate.getName().equals(c.getObject().getName().getOrig()))
                                        .collect(Collectors.toList()),
                                () -> new AssertionError("Multiple candidates found for " + expectedCandidate),
                                () -> new AssertionError("No candidates found for " + expectedCandidate));
                assertThat(candidateDescription.getConfidence())
                        .as("candidate confidence (in description)")
                        .isEqualTo(expectedCandidate.getConfidence(), offset(TestCandidateOwner.EPSILON));
                System.out.println("Confidence is OK for " + candidateDescription);
                if (expectedMatches != null) {
                    expectedMatches.getMatches().forEach(
                            (path, match) -> assertMatch(candidateDescription, path, match));
                    System.out.println(expectedMatches.getMatches().size() + " item(s) matches are OK for " + candidateDescription);
                }

                LocalizableMessage message =
                        requireNonNull(candidateDescription.getExplanation(), "no explanation")
                                .toLocalizableMessage();
                String messageText = localizationService.translate(message, Locale.US);
                displayValue("explanation as message", messageText);
            }
        }
    }

    private void assertMatch(CandidateDescription<?> candidateDescription, ItemPath path, Match expectedMatch) {
        CorrelationPropertyValuesDescription propertyDesc = candidateDescription.getProperties().get(path);
        assertThat(propertyDesc).as("property description for " + path).isNotNull();
        assertThat(propertyDesc.getMatch()).as("match for " + path).isEqualTo(expectedMatch);
    }

    private static File accountsFile(String name) {
        return new File(TEST_DIR, name + ".csv");
    }

    private static TestCorrelator correlator(String name) {
        return correlator(name, null);
    }

    private static TestCorrelator correlator(String name, TestResource<ObjectTemplateType> template) {
        return new TestCorrelator(
                new File(TEST_DIR, name + ".xml"),
                template);
    }

    enum DescriptionMode {
        FULL, DESCRIBE_ONLY, NONE
    }

    /** Definition of the correlator and its instance. */
    static class TestCorrelator {
        @NotNull private final File file;
        @Nullable private final TestResource<ObjectTemplateType> userTemplateResource; // loaded on start of test execution
        private CorrelatorContext<?> correlatorContext;
        private Correlator instance; // set on initialization

        TestCorrelator(@NotNull File file, @Nullable TestResource<ObjectTemplateType> userTemplateResource) {
            this.file = file;
            this.userTemplateResource = userTemplateResource;
        }

        ObjectTemplateType getUserTemplate() {
            return userTemplateResource != null ? userTemplateResource.getObjectable() : null;
        }
    }
}
