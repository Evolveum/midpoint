package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.activities.ObjectTypeStatisticsComputer;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaService;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;
import static com.evolveum.midpoint.smart.impl.DummyScenario.Account.AttributeNames.*;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(locations = {"classpath:ctx-smart-integration-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMappingsSuggestionOperation extends AbstractSmartIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/mappings-suggestion");

    private static DummyScenario dummyScenario;

    private static final TestObject<UserType> USER1 =
            TestObject.file(TEST_DIR, "user1.xml", "00000000-0000-0000-0000-999000001001");
    private static final TestObject<UserType> USER2 =
            TestObject.file(TEST_DIR, "user2.xml", "00000000-0000-0000-0000-999000001002");
    private static final TestObject<UserType> USER3 =
            TestObject.file(TEST_DIR, "user3.xml", "00000000-0000-0000-0000-999000001003");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-mappings-suggestion.xml", "10000000-0000-0000-0000-999000000002",
            "for-mappings-suggestion", c -> dummyScenario = on(c).initialize());

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private KnownSchemaService knownSchemaService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);
        initAndTestDummyResource(RESOURCE_DUMMY, initTask, initResult);

        initTestObjects(initTask, initResult, USER1, USER2, USER3);

        var a = dummyScenario.account;
        a.add("user1")
                .addAttributeValues(PERSONAL_NUMBER.local(), "11111")
                .addAttributeValues(EMAIL.local(), "user1@acme.com");
        linkAccount(USER1, initTask, initResult);
        a.add("user2")
                .addAttributeValues(PERSONAL_NUMBER.local(), "22222")
                .addAttributeValues(EMAIL.local(), "user2@acme.com");
        linkAccount(USER2, initTask, initResult);
        a.add("user3")
                .addAttributeValues(PERSONAL_NUMBER.local(), "33333")
                .addAttributeValues(EMAIL.local(), "user3@acme.com");
        linkAccount(USER3, initTask, initResult);
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, getTestTask(), getTestOperationResult());
    }

    private void linkAccount(TestObject<?> user, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(RESOURCE_DUMMY.getObjectable())
                .withDefaultAccountType()
                .withNameValue(user.getNameOrig())
                .build().findRequired(task, result);
        executeChanges(
                PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .add(shadow.getRef())
                        .asObjectDelta(user.oid),
                null, task, result);
    }

    private ServiceClient createClient(List<ItemPath> focusPaths, List<ItemPath> shadowPaths, String... scripts) {
        SiMatchSchemaResponseType matchResponse = new SiMatchSchemaResponseType();
        for (int i = 0; i < focusPaths.size(); i++) {
            matchResponse.attributeMatch(
                    new SiAttributeMatchSuggestionType()
                            .applicationAttribute(asStringSimple(shadowPaths.get(i)))
                            .midPointAttribute(asStringSimple(focusPaths.get(i)))
            );
        }

        // Build responses: first schema match, then one suggest-mapping response per provided script
        if (scripts == null || scripts.length == 0) {
            return new MockServiceClientImpl(matchResponse);
        } else {
            List<Object> responses = new ArrayList<>();
            responses.add(matchResponse);
            for (String script : scripts) {
                responses.add(new SiSuggestMappingResponseType().transformationScript(script));
            }
            return new MockServiceClientImpl(responses);
        }
    }

    private void modifyUserReplace(String oid, ItemPath path, Object... newValues) throws Exception {
        executeChanges(
                deltaFor(UserType.class)
                        .item(path)
                        .replace(newValues)
                        .asObjectDelta(oid),
                null, getTestTask(), getTestOperationResult());
    }

    private void modifyShadowReplace(String shadowName, AttrName attr, Object... values) throws Exception {
        dummyScenario.account.getByNameRequired(shadowName)
                .replaceAttributeValues(attr.local(), values);
    }

    private ShadowObjectClassStatisticsType computeStatistics(
            DummyTestResource resource,
            ResourceObjectTypeIdentification typeIdentification,
            Task task,
            OperationResult result) throws CommonException {
        var res = Resource.of(resource.get());
        var typeDefinition = res.getCompleteSchemaRequired().getObjectTypeDefinitionRequired(typeIdentification);
        var computer = new ObjectTypeStatisticsComputer(typeDefinition);
        var shadows = provisioningService.searchShadows(
                res.queryFor(typeIdentification).build(),
                null,
                task, result);
        for (var shadow : shadows) {
            computer.process(shadow.getBean());
        }
        computer.postProcessStatistics();
        return computer.getStatistics();
    }

    @Test
    public void test001AsIsMappingWhenDataIdentical() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        // Personal number is identical on both sides
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                null // No script, triggers "asIs"
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                true,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);

        assertThat(mapping.getExpectedQuality())
                .as("AsIs mapping should have perfect quality")
                .isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Should be asIs (null)")
                .isNull();
    }

    @Test
    public void test002TransformationMappingWhenScriptProvided() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user1", PERSONAL_NUMBER, "1-1-1-1-1");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "2-222-2");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33-3-33");

        refreshShadows();

        String script = "input.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                script
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                true,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getExpectedQuality()).as("Transformed mapping should have perfect quality").isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Should contain a script expression returned by the service")
                .isNotNull();
    }

    @Test
    public void test003InvalidScriptShouldBeIgnored() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user1", PERSONAL_NUMBER, "1-1-1-1-1");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "2-222-2");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33-3-33");

        refreshShadows();

        // Intentionally invalid Groovy (method name misspelled) to trigger evaluation failure
        String invalidScript = "input.repalceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                invalidScript,
                invalidScript
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                true,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Invalid script should result in no mapping being produced")
                .hasSize(0);
    }

    @Test
    public void test004InvalidScriptWithCorrectRetry() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user1", PERSONAL_NUMBER, "1-1-1-1-1");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "2-222-2");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33-3-33");

        refreshShadows();

        // Intentionally invalid Groovy (method name misspelled) to trigger evaluation failure
        String invalidScript = "input.repalceAll('-', '')";
        String validScript = "input.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                invalidScript,
                validScript
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                true,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Invalid script should be corrected with retry mechanism.")
                .hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Expression should still be present. This should be secured with retry mechanism.")
                .isNotNull();
    }

    @Test
    public void test005LowQualityMappingShouldBeSkipped() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user1", PERSONAL_NUMBER, "1-1-1-1-1");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "2-222-2");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33-3-33");

        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "99999");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "88888");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "77777");

        refreshShadows();

        String script = "input.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                script
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                true,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Low quality mapping (below 10% threshold) should be skipped")
                .hasSize(0);
    }

    @Test
    public void test010OutboundAsIsMappingWhenDataIdentical() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(EMAIL.path())
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                false,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);

        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getOutbound().getExpression())
                .as("Outbound asIs should have null expression")
                .isNull();
    }

    @Test
    public void test011OutboundTransformationWhenScriptProvided() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure shadow side has plain numeric personal numbers (previous tests may have dashed)
        modifyShadowReplace("user1", PERSONAL_NUMBER, "11111");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "22222");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33333");

        // Focus has dashed personal numbers, shadow has plain => requires transform for outbound
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "1-1-1-1-1");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "2-222-2");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33-3-33");

        refreshShadows();

        String script = "personalNumber.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                script
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                false,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getOutbound().getExpression())
                .as("Outbound should contain a script expression returned by the service")
                .isNotNull();
    }

    @Test
    public void test012OutboundInvalidScriptShouldBeIgnored() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure shadow side has plain numeric personal numbers
        modifyShadowReplace("user1", PERSONAL_NUMBER, "11111");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "22222");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33333");

        // Keep dashed focus numbers to force script path
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "1-1-1-1-1");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "2-222-2");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33-3-33");

        refreshShadows();

        String invalidScript = "input.repalceAll('-', '')"; // misspelled
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                invalidScript,
                invalidScript
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                false,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Invalid outbound script should result in no mapping being produced")
                .hasSize(0);
    }

    @Test
    public void test013OutboundInvalidScriptWithCorrectRetry() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure shadow side has plain numeric personal numbers
        modifyShadowReplace("user1", PERSONAL_NUMBER, "11111");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "22222");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33333");

        // Keep dashed focus numbers to force script path
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "1-1-1-1-1");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "2-222-2");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33-3-33");

        refreshShadows();

        String invalidScript = "input.replaceAll('-', '')";
        String validScript = "personalNumber.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                invalidScript,
                validScript
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                false,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getOutbound().getExpression()).isNotNull();
    }

    @Test
    public void test014OutboundLowQualityMappingShouldBeSkipped() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Set up focus data with dashed personal numbers
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "1-1-1-1-1");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "2-222-2");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33-3-33");

        // Set up shadow data that doesn't match even after transformation
        modifyShadowReplace("user1", PERSONAL_NUMBER, "99999");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "88888");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "77777");

        refreshShadows();

        // Provide a script that removes dashes, but since data doesn't match, quality will be very low
        String script = "personalNumber.replaceAll('-', '')";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                script
        );

        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                knownSchemaService,
                false,
                task,
                result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Outbound low quality mapping (below 10% threshold) should be skipped")
                .hasSize(0);
    }

    @Test
    public void test030NoSchemaMatchReturnsEmptySuggestion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        // Empty match response
        var mockClient = createClient(List.of(), List.of());
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, true, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).isEmpty();
    }

    @Test
    public void test040MultipleAttributesInboundAsIs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure both sides match for personal number (previous outbound tests changed focus values)
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "11111");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "22222");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33333");
        // And keep shadow values aligned as well
        modifyShadowReplace("user1", PERSONAL_NUMBER, "11111");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "22222");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33333");

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER), ItemPath.create(UserType.F_EMAIL_ADDRESS)),
                List.of(PERSONAL_NUMBER.path(), EMAIL.path()),
                "input", "input"
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, true, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(2);
        assertThat(suggestion.getAttributeMappings())
                .allSatisfy(m -> assertThat(m.getDefinition().getInbound().get(0).getExpression()).isNull());
    }

    @Test
    public void test050IdentityScriptProducesAsIs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refreshShadows();

        String identity = "input";
        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path()),
                identity
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, true, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Identity script must be treated as asIs (null expression)")
                .isNull();
        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
    }

    @Test
    public void test060InboundAllTargetMissingAsIsWithNoSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Remove focus personal numbers => target missing for inbound
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, true, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression()).isNull();
        assertThat(mapping.getExpectedQuality())
                .as("With no comparable samples, expected quality should be null")
                .isNull();
    }

    @Test
    public void test061InboundAllSourceMissingAsIsWithNoSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure focus has data but shadow attributes are missing => source missing for inbound
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "11111");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "22222");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33333");

        // Remove shadow personal numbers
        modifyShadowReplace("user1", PERSONAL_NUMBER);
        modifyShadowReplace("user2", PERSONAL_NUMBER);
        modifyShadowReplace("user3", PERSONAL_NUMBER);

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, true, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings())
                .as("Source data missing should discard mapping")
                .isEmpty();
    }

    @Test
    public void test070OutboundAllSourceMissingAsIsWithNoSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Remove focus personal numbers => source missing for outbound
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER));

        // Ensure shadow has data
        modifyShadowReplace("user1", PERSONAL_NUMBER, "11111");
        modifyShadowReplace("user2", PERSONAL_NUMBER, "22222");
        modifyShadowReplace("user3", PERSONAL_NUMBER, "33333");

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, false, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings())
                .as("Source data missing should discard mapping")
                .isEmpty();
    }

    @Test
    public void test071OutboundAllTargetMissingAsIsWithNoSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure focus has data
        modifyUserReplace(USER1.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "11111");
        modifyUserReplace(USER2.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "22222");
        modifyUserReplace(USER3.oid, ItemPath.create(UserType.F_PERSONAL_NUMBER), "33333");

        // Remove shadow personal numbers => target missing for outbound
        modifyShadowReplace("user1", PERSONAL_NUMBER);
        modifyShadowReplace("user2", PERSONAL_NUMBER);
        modifyShadowReplace("user3", PERSONAL_NUMBER);

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_PERSONAL_NUMBER)),
                List.of(PERSONAL_NUMBER.path())
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);

        var op = MappingsSuggestionOperation.init(
                mockClient, RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, null,
                new MappingsQualityAssessor(expressionFactory), new OwnedShadowsProviderFromResource(),
                knownSchemaService, false, task, result);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getDefinition().getOutbound().getExpression())
                .as("Outbound target data missing should result in asIs mapping")
                .isNull();
        assertThat(mapping.getExpectedQuality())
                .as("With no target data, expected quality should be null")
                .isNull();
    }

}
