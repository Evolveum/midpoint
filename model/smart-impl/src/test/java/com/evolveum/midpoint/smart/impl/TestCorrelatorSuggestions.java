package com.evolveum.midpoint.smart.impl;


import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
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
import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.*;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceItemDefinitionType.F_INBOUND;

import static com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType.F_OBJECT_TYPE;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCorrelatorSuggestions extends AbstractSmartIntegrationTest {
    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/correlator-evaluator");

    private static DummyScenario dummyScenario;
    private static Task task;
    private static OperationResult result;

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

        initTestObjects(initTask, initResult, USER1, USER2, USER3);

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
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null,
                getTestTask(),
                getTestOperationResult()
        );
    }

    private ServiceClient createClient(List<ItemPath> focusPaths, List<ItemPath> shadowPaths) {
        if (focusPaths.size() != shadowPaths.size()) {
            throw new IllegalArgumentException("focusPaths and shadowPaths must have the same size");
        }
        SiMatchSchemaResponseType matchResponse = new SiMatchSchemaResponseType();
        for (int i = 0; i < focusPaths.size(); i++) {
            matchResponse.attributeMatch(
                    new SiAttributeMatchSuggestionType()
                            .applicationAttribute(asStringSimple(shadowPaths.get(i)))
                            .midPointAttribute(asStringSimple(focusPaths.get(i)))
            );
        }
        return new MockServiceClientImpl(
                matchResponse,
                new SiSuggestMappingResponseType().transformationScript(null)
        );
    }

    private void modifyUserReplace(String oid, ItemPath path, Object... newValues) throws Exception {
        executeChanges(
                deltaFor(UserType.class)
                        .item(path)
                        .replace(newValues)
                        .asObjectDelta(oid),
                null, getTestTask(), getTestOperationResult());
    }

    @Test
    public void test001MultiValuedAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        ItemPath focusPath = ItemPath.create(UserType.F_EMAIL);
        ItemPath shadowPath = EMAIL.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(
                TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result),
                List.of(suggestion)
        );
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for multi-valued attribute correlation should be -1.0")
                .isEqualTo(-1.0);
    }

    @Test
    public void test002ResourceOnlyAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        ItemPath focusPath = null;
        ItemPath shadowPath = EMAIL.path();
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(
                TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result),
                List.of(suggestion)
        );
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for resource-only attribute correlation should be 0.5")
                .isEqualTo(0.5);
    }

    @Test
    public void test003FocusOnlyAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "11111");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "22222");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "33333");

        refreshShadows();

        ItemPath focusPath = ItemPath.create(UserType.F_PERSONAL_NUMBER);
        ItemPath shadowPath = null;
        CorrelationSuggestionOperation.CorrelatorSuggestion suggestion =
                new CorrelationSuggestionOperation.CorrelatorSuggestion(focusPath, shadowPath, null);

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(
                TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result),
                List.of(suggestion)
        );
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for focus-only attribute correlation should be below 0.5")
                .isLessThanOrEqualTo(0.5);
    }

    @Test
    public void test004NoSuggestions() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(
                TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result),
                List.of()
        );
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores)
                .as("No suggestions should yield empty score list")
                .isEmpty();
    }

    @Test
    public void test005NullSuggestion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        CorrelatorEvaluator evaluator = new CorrelatorEvaluator(
                TypeOperationContext.init(new MockServiceClientImpl(), RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null, task, result),
                Arrays.asList(new CorrelationSuggestionOperation.CorrelatorSuggestion(null, null, null))
        );
        List<Double> scores = evaluator.evaluateSuggestions(result);

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0))
                .as("Suggestion with both paths null should be -1.0")
                .isEqualTo(-1.0);
    }


    @Test
    public void test010UniquePersonalNumberCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

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

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(EMAIL.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

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

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for incomplete personalNumber correlation should be 2/3")
                .isEqualTo(0.66, Offset.offset(0.01));
    }


    @Test
    public void test040AllMissingFocusAttributeCorrelationScore() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Remove personalNumber from all users
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER);
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER);
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER);

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

        assertThat(scores).hasSize(1);
        double score = scores.get(0);
        assertThat(score)
                .as("Score for all-missing focus attribute should be 0.")
                .isEqualTo(0.0);
    }


    @Test
    public void test050NoUniquenessAllSameValue() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "SAME");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "SAME");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "SAME");

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0))
                .as("Score for correlation with no uniqueness (all same value) should be 0.0")
                .isEqualTo(0.0);
    }

    @Test
    public void test060MaxAmbiguityAllShadowsMatchAllFocuses() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");
        modifyUserReplace(USER2.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");
        modifyUserReplace(USER3.oid, UserType.F_EMAIL_ADDRESS, "ambiguous@acme.com");

        var a = dummyScenario.account;
        a.getByName("account1").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");
        a.getByName("account2").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");
        a.getByName("account3").replaceAttributeValue(EMAIL.local(), "ambiguous@acme.com");

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(EMAIL.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0))
                .as("Score for max ambiguity (all shadows point to all focuses) should be 0.0")
                .isEqualTo(0.0);
    }

    @Test
    public void test070MultiSuggestionList() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "101");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "102");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "103");
        modifyUserReplace(USER1.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");
        modifyUserReplace(USER2.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");
        modifyUserReplace(USER3.oid, UserType.F_EMAIL_ADDRESS, "common@acme.com");

        var a = dummyScenario.account;
        a.getByName("account1").replaceAttributeValue(PERSONAL_NUMBER.local(), "101");
        a.getByName("account2").replaceAttributeValue(PERSONAL_NUMBER.local(), "102");
        a.getByName("account3").replaceAttributeValue(PERSONAL_NUMBER.local(), "103");
        a.getByName("account1").replaceAttributeValue(EMAIL.local(), "common@acme.com");
        a.getByName("account2").replaceAttributeValue(EMAIL.local(), "common@acme.com");
        a.getByName("account3").replaceAttributeValue(EMAIL.local(), "common@acme.com");

        refreshShadows();
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER), ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(PERSONAL_NUMBER.path(), EMAIL.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);
        List<Double> scores = suggestions.getSuggestion().stream().map(CorrelationSuggestionType::getQuality).toList();

        assertThat(scores).hasSize(2);
        assertThat(scores.get(0))
                .as("Score for unique personalNumber correlation should be 1.0")
                .isEqualTo(1.0);
        assertThat(scores.get(1))
                .as("Score for all-same email correlation should be 0.0")
                .isEqualTo(0.0);
    }

    @Test
    public void test080FindExistingInboundMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY.oid, null, result).asObjectable();

        ResourceObjectTypeDefinitionType accountType = resource.getSchemaHandling().getObjectType().stream()
                .filter(o -> ResourceObjectTypeIdentification.of(o.getKind(), o.getIntent()).equals(ACCOUNT_DEFAULT))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find ACCOUNT_DEFAULT type"));

        ResourceAttributeDefinitionType attr = accountType.getAttribute().stream()
                .filter(a -> PERSONAL_NUMBER.local().equals(asStringSimple(a.getRef().getItemPath())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("personalNumber attr not found"));

        attr.getInbound().add(new InboundMappingType()
                .target(new VariableBindingDefinitionType()
                        .path(ItemPath.create(UserType.F_PERSONAL_NUMBER).toBean())));

        executeChanges(List.of(PrismContext.get().deltaFor(ResourceType.class)
                                .item(ResourceType.F_SCHEMA_HANDLING)
                                .replace(CloneUtil.clone(resource.getSchemaHandling()))
                                .asObjectDelta(RESOURCE_DUMMY.oid)),
                null, task, result);


        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER), ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(PERSONAL_NUMBER.path(), EMAIL.path())
        );
        smartIntegrationService.setServiceClientSupplier(() -> mockClient);

        var suggestions = smartIntegrationService.suggestCorrelation(
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null, task, result);

        List<CorrelationSuggestionType> suggestionList = suggestions.getSuggestion();

        assertThat(suggestionList).hasSize(2);

        // Find the suggestion for personalNumber
        CorrelationSuggestionType personalNumberSuggestion = suggestionList.stream()
                .filter(s -> {
                    // Assuming getFocusPath returns the ItemPath or its string representation
                    String focusPath = s.getCorrelation().getCorrelators().getItems().get(0).getItem().get(0).getRef().getItemPath().toString();
                    return focusPath != null && focusPath.endsWith("personalNumber");
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Suggestion for personalNumber not found"));

        assertThat(personalNumberSuggestion.getAttributes())
                .as("PersonalNumber should be treated as existing mapping (no AI inbound mapping suggested)")
                .isEmpty();

    }

}
