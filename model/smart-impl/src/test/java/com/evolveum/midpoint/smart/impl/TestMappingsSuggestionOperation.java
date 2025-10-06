package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
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

    private MappingsQualityAssessor qualityAssessor() {
        return new MappingsQualityAssessor(expressionFactory);
    }

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
                null, getTestTask(), getTestOperationResult());
    }

    private ServiceClient createClient(List<ItemPath> focusPaths, List<ItemPath> shadowPaths, String script) {
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
                new SiSuggestMappingResponseType().transformationScript(script)
        );
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

        var op = MappingsSuggestionOperation.init(
                mockClient,
                RESOURCE_DUMMY.oid,
                ACCOUNT_DEFAULT,
                null,
                qualityAssessor(),
                task,
                result);

        MappingsSuggestionType suggestion = op.suggestMappings(result);
        assertThat(suggestion.getAttributeMappings()).hasSize(1);
        AttributeMappingsSuggestionType mapping = suggestion.getAttributeMappings().get(0);

        assertThat(mapping.getExpectedQuality())
                .as("AsIs mapping should have perfect quality")
                .isEqualTo(1.0f);
        assertThat(mapping.getDefinition().getInbound().get(0).getExpression())
                .as("Should be asIs (null)")
                .isNull();
    }

}
