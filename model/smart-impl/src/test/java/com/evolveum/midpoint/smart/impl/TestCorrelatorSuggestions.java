package com.evolveum.midpoint.smart.impl;


import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

import org.assertj.core.data.Offset;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.*;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelatorSuggestions extends AbstractSmartIntegrationTest {
    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/correlator-evaluator");

    private static DummyScenario dummyScenario;
    private static TypeOperationContext ctx;

    private static final TestObject<UserType> USER1 = TestObject.file(TEST_DIR, "user1.xml", "00000000-0000-0000-0000-990000000001");
    private static final TestObject<UserType> USER2 = TestObject.file(TEST_DIR, "user2.xml", "00000000-0000-0000-0000-990000000002");
    private static final TestObject<UserType> USER3 = TestObject.file(TEST_DIR, "user3.xml", "00000000-0000-0000-0000-990000000003");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-correlator-evaluator.xml", "10000000-0000-0000-0000-990000000001",
                    "for-correlator-evaluator", c -> dummyScenario = on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);
        initAndTestDummyResource(RESOURCE_DUMMY, initTask, initResult);

        ctx = TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, initTask, initResult);

        // Init focus objects
        initTestObjects(initTask, initResult, USER1, USER2, USER3);
        // Init shadow objects
        createShadows();
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(ctx.resource)
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null,
                getTestTask(),
                getTestOperationResult()
        );
    }

    private void createShadows() throws Exception {
        var a = dummyScenario.account;
        a.add("account1")
                .addAttributeValues(PERSONAL_NUMBER.local(), "11111")
                .addAttributeValues(EMAIL.local(), "user1@acme.com");
        a.add("account2")
                .addAttributeValues(PERSONAL_NUMBER.local(), "22222")
                .addAttributeValues(EMAIL.local(), "user2@acme.com");
        a.add("account3")
                .addAttributeValues(PERSONAL_NUMBER.local(), "33333")
                .addAttributeValues(EMAIL.local(), "user3@acme.com");

        refreshShadows();
    }

    private void modifyUserReplace(String oid, ItemPath path, Object... newValues) throws Exception {
        executeChanges(
                deltaFor(UserType.class)
                        .item(path)
                        .replace(newValues)
                        .asObjectDelta(oid),
                null, getTestTask(), getTestOperationResult());
    }

    private void modifyUserAdd(String oid, ItemPath path, Object... addValues) throws Exception {
        executeChanges(
                deltaFor(UserType.class)
                        .item(path)
                        .add(addValues)
                        .asObjectDelta(oid),
                null, getTestTask(), getTestOperationResult());
    }

    @Test
    public void test010UniquePersonalNumberCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        List<CorrelationSuggestionOperation.CorrelatorSuggestion> suggestions = List.of(suggestion);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, suggestions);
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for unique personalNumber correlation should be 1.")
                .isEqualTo(1.0);
    }

    @Test
    public void test020AmbiguousEmailCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER2.oid, UserType.F_EMAIL_ADDRESS, "user1@acme.com");

        ItemPath focusPath = ItemPath.create(UserType.F_EMAIL_ADDRESS);
        ItemPath shadowPath = EMAIL.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for ambiguous and empty email correlation should be low.")
                .isEqualTo(0.33, Offset.offset(0.01));
    }

    @Test
    public void test030IncompletePersonalNumberCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER);

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for incomplete personalNumber correlation should be 2/3")
                .isEqualTo(0.66, Offset.offset(0.01));
    }

    @Test
    public void test040MultiValuedAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Add a second value to USER1 personalNumber, making it multi-valued
        modifyUserAdd(USER1.oid, UserType.F_EMAIL, new EmailAddressType().value("user1@acme.com"));
        modifyUserAdd(USER1.oid, UserType.F_EMAIL, new EmailAddressType().value("user2@acme.com"));

        ItemPath focusPath = ItemPath.create(UserType.F_EMAIL);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for multi-valued attribute correlation should be -1.0")
                .isEqualTo(-1.0);
    }

    @Test
    public void test050ResourceOnlyAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ItemPath focusPath = null;
        ItemPath shadowPath = EMAIL.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for resource-only attribute correlation should be 0.5")
                .isEqualTo(0.5);
    }

    @Test
    public void test060FocusOnlyAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "11111");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "22222");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "33333");

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = null;
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for focus-only attribute correlation should be below 0.5")
                .isLessThanOrEqualTo(0.5);
    }

    @Test
    public void test070AllMissingFocusAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Remove personalNumber from all users
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER);
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER);
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER);

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for all-missing focus attribute should be 0.")
                .isEqualTo(0.0);
    }

    @Test
    public void test080NoUniquenessAllSameValue() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Set the same personal number to all users
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "SAME");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "SAME");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "SAME");

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = PERSONAL_NUMBER.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0))
                .as("Score for correlation with no uniqueness (all same value) should be 0.0")
                .isEqualTo(0.0);
    }

    @Test
    public void test090NoSuggestions() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of());
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores)
                .as("No suggestions should yield empty score list")
                .isEmpty();
    }

    @Test
    public void test100NullSuggestion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, Arrays.asList(
                new CorrelationSuggestionOperation.CorrelatorSuggestion(null, null, null)
        ));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0))
                .as("Suggestion with both paths null should be -1.0")
                .isEqualTo(-1.0);
    }

    @Test
    public void test110MaxAmbiguityAllShadowsMatchAllFocuses() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Set all users to have same email, and all shadows to have that email
        modifyUserReplace(USER1.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");
        modifyUserReplace(USER2.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");
        modifyUserReplace(USER3.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");

        var a = dummyScenario.account;
        a.getByName("account1").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");
        a.getByName("account2").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");
        a.getByName("account3").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");

        refreshShadows();

        ItemPath focusPath = ItemPath.create(UserType.F_EMAIL_ADDRESS);
        ItemPath shadowPath = EMAIL.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        // When all shadows match all focuses, the ambiguity penalty should bring the score down to zero
        assertThat(scores.get(0))
                .as("Score for max ambiguity (all shadows point to all focuses) should be 0.0")
                .isEqualTo(0.0);
    }

    @Test
    public void test120MultiSuggestionList() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Prepare two suggestions: one good (personal number), one bad (all same email)
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "101");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "102");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "103");

        modifyUserReplace(USER1.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");
        modifyUserReplace(USER2.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");
        modifyUserReplace(USER3.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");

        // Make corresponding shadow attributes
        var a = dummyScenario.account;
        a.getByName("account1").replaceAttributeValue(PERSONAL_NUMBER.local(), "101");
        a.getByName("account2").replaceAttributeValue(PERSONAL_NUMBER.local(), "102");
        a.getByName("account3").replaceAttributeValue(PERSONAL_NUMBER.local(), "103");
        a.getByName("account1").replaceAttributeValue(EMAIL.local(), "common@acme.com");
        a.getByName("account2").replaceAttributeValue(EMAIL.local(), "common@acme.com");
        a.getByName("account3").replaceAttributeValue(EMAIL.local(), "common@acme.com");

        refreshShadows();

        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion1 =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(
                        ItemPath.create(UserType.F_PERSONAL_NUMBER), PERSONAL_NUMBER.path(), null);

        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion2 =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(
                        ItemPath.create(UserType.F_EMAIL_ADDRESS), EMAIL.path(), null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(ctx, List.of(suggestion1, suggestion2));
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(2);
        assertThat(scores.get(0))
                .as("Score for unique personalNumber correlation should be 1.0")
                .isEqualTo(1.0);
        assertThat(scores.get(1))
                .as("Score for all-same email correlation should be 0.0")
                .isEqualTo(0.0);
    }

}
