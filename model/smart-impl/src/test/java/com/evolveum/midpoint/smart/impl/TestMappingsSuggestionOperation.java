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
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
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
                task,
                result);

        var statistics = computeStatistics(RESOURCE_DUMMY, ACCOUNT_DEFAULT, task, result);
        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, statistics, match);
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

        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "1-1-1-1-1");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "2-222-2");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "33-3-33");

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
                task,
                result);

        var statistics = computeStatistics(RESOURCE_DUMMY, ACCOUNT_DEFAULT, task, result);
        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, statistics, match);
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

        // Ensure data requires transformation so the microservice is queried
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "1-1-1-1-1");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "2-222-2");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "33-3-33");

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
                task,
                result);

        var statistics = computeStatistics(RESOURCE_DUMMY, ACCOUNT_DEFAULT, task, result);
        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, statistics, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Invalid script should result in no mapping being produced")
                .hasSize(0);
    }

    @Test
    public void test004InvalidScriptWithCorrectRetry() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Ensure data requires transformation so the microservice is queried
        modifyUserReplace(USER1.oid, UserType.F_PERSONAL_NUMBER, "1-1-1-1-1");
        modifyUserReplace(USER2.oid, UserType.F_PERSONAL_NUMBER, "2-222-2");
        modifyUserReplace(USER3.oid, UserType.F_PERSONAL_NUMBER, "33-3-33");

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
                task,
                result);

        var statistics = computeStatistics(RESOURCE_DUMMY, ACCOUNT_DEFAULT, task, result);
        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_DUMMY.oid, ACCOUNT_DEFAULT, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, statistics, match);

        assertThat(suggestion.getAttributeMappings())
                .as("Invalid script should be corrected with retry mechanism.")
                .hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);
        assertThat(mapping.getExpectedQuality()).isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Expression should still be present. This should be secured with retry mechanism.")
                .isNotNull();
    }

}
