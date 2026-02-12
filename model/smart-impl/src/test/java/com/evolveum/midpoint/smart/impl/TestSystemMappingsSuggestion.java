/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.impl.scoring.MappingsQualityAssessor;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.smart.impl.mappings.heuristics.HeuristicRuleMatcher;
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
import static com.evolveum.midpoint.smart.impl.DummyScenario.InetOrgPerson.AttributeNames.*;
import static com.evolveum.midpoint.smart.impl.DummyScenario.on;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LDAP inetOrgPerson mapping suggestions with well-known schema detection.
 * Focuses on system-provided mappings, duplicate detection, and quality assessment
 * in the context of LDAP schema.
 */
@ContextConfiguration(locations = {"classpath:ctx-smart-integration-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSystemMappingsSuggestion extends AbstractSmartIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "smart/mappings-suggestion");

    private static DummyScenario dummyScenario;
    private static DummyScenario dummyScenarioAd;

    private static final TestObject<UserType> USER1 =
            TestObject.file(TEST_DIR, "user1.xml", "00000000-0000-0000-0000-999000001001");
    private static final TestObject<UserType> USER2 =
            TestObject.file(TEST_DIR, "user2.xml", "00000000-0000-0000-0000-999000001002");
    private static final TestObject<UserType> USER3 =
            TestObject.file(TEST_DIR, "user3.xml", "00000000-0000-0000-0000-999000001003");

    private static final DummyTestResource RESOURCE_LDAP = new DummyTestResource(
            TEST_DIR, "resource-ldap-for-mappings-suggestion.xml", "10000000-0000-0000-0000-999000000003",
            "ldap-for-mappings-suggestion", c -> {
                dummyScenario = on(c);
                dummyScenario.inetOrgPerson.initialize();
            });

    private static final DummyTestResource RESOURCE_AD = new DummyTestResource(
            TEST_DIR, "resource-ad-for-mappings-suggestion.xml", "10000000-0000-0000-0000-999000000004",
            "ad-for-mappings-suggestion", c -> {
                dummyScenarioAd = on(c);
                dummyScenarioAd.initializeAd();
            });

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private WellKnownSchemaService wellKnownSchemaService;
    @Autowired private HeuristicRuleMatcher heuristicRuleMatcher;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, CommonInitialObjects.SERVICE_ORIGIN_ARTIFICIAL_INTELLIGENCE);
        initAndTestDummyResource(RESOURCE_LDAP, initTask, initResult);

        initTestObjects(initTask, initResult, USER1, USER2, USER3);

        dummyScenario.inetOrgPerson.add("user1")
                .addAttributeValues(CN.local(), "John Doe")
                .addAttributeValues(SN.local(), "Doe")
                .addAttributeValues(GIVEN_NAME.local(), "John")
                .addAttributeValues(MAIL.local(), "john@example.com");
        linkAccount(USER1, RESOURCE_LDAP, initTask, initResult);

        dummyScenario.inetOrgPerson.add("user2")
                .addAttributeValues(CN.local(), "Jane Smith")
                .addAttributeValues(SN.local(), "Smith")
                .addAttributeValues(GIVEN_NAME.local(), "Jane")
                .addAttributeValues(MAIL.local(), "jane@example.com");
        linkAccount(USER2, RESOURCE_LDAP, initTask, initResult);

        dummyScenario.inetOrgPerson.add("user3")
                .addAttributeValues(CN.local(), "Bob Johnson")
                .addAttributeValues(SN.local(), "Johnson")
                .addAttributeValues(GIVEN_NAME.local(), "Bob")
                .addAttributeValues(MAIL.local(), "bob@example.com");
        linkAccount(USER3, RESOURCE_LDAP, initTask, initResult);

        initAndTestDummyResource(RESOURCE_AD, initTask, initResult);

        dummyScenarioAd.adUser.add("user1")
                .addAttributeValues(DummyScenario.AdUser.AttributeNames.SAM_ACCOUNT_NAME.local(), "user1")
                .addAttributeValues(DummyScenario.AdUser.AttributeNames.CN.local(), "John Doe")
                .addAttributeValues(DummyScenario.AdUser.AttributeNames.SN.local(), "Doe")
                .addAttributeValues(DummyScenario.AdUser.AttributeNames.GIVEN_NAME.local(), "John")
                .addAttributeValues(DummyScenario.AdUser.AttributeNames.MAIL.local(), "john@example.com");
        linkAccount(USER1, RESOURCE_AD, initTask, initResult);
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(RESOURCE_LDAP.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, getTestTask(), getTestOperationResult());
    }

    private void linkAccount(TestObject<?> user, DummyTestResource resource, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(resource.getObjectable())
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

    private void modifyShadowReplace(String shadowName, com.evolveum.midpoint.test.AttrName attr, Object... values) throws Exception {
        dummyScenario.inetOrgPerson.getByNameRequired(shadowName)
                .replaceAttributeValues(attr.local(), values);
    }

    @Test
    public void test100SystemMappingsFromLdapWithPerfectMatch() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_FULL_NAME, PolyString.fromOrig("John Doe"));
        modifyUserReplace(USER2.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Jane Smith"));
        modifyUserReplace(USER3.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Bob Johnson"));

        refreshShadows();

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                true,
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        assertThat(suggestion.getAttributeMappings())
                .as("LDAP system mappings should be present")
                .isNotEmpty();

        var cnMapping = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("cn"))
                .findFirst();

        assertThat(cnMapping).isPresent();

        assertThat(SmartMetadataUtil.isMarkedAsSystemProvided(cnMapping.get().asPrismContainerValue()))
                .as("CN mapping should be marked as system-provided")
                .isTrue();
    }

    @Test
    public void test110DuplicateDetectionSameTargetBoth() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_FULL_NAME, PolyString.fromOrig("John Doe"));
        modifyUserReplace(USER2.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Jane Smith"));
        modifyUserReplace(USER3.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Bob Johnson"));

        modifyShadowReplace("user1", CN, "John Doe - Different");
        modifyShadowReplace("user2", CN, "Jane Smith");
        modifyShadowReplace("user3", CN, "Bob Johnson");

        modifyShadowReplace("user1", DISPLAY_NAME, "John Doe");
        modifyShadowReplace("user2", DISPLAY_NAME, "Jane Smith");
        modifyShadowReplace("user3", DISPLAY_NAME, "Bob Johnson");

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_FULL_NAME)),
                List.of(DISPLAY_NAME.path()),
                null, null, null, null, null, null, null
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                true,
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        var fullNameMappings = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getInbound() != null
                        && !m.getDefinition().getInbound().isEmpty()
                        && m.getDefinition().getInbound().get(0).getTarget().getPath().toString().contains("fullName"))
                .toList();

        assertThat(fullNameMappings)
                .as("Should have two fullName mapping.s")
                .hasSize(2);
    }

    @Test
    public void test112SystemProvidedPreferredWhenQualityEqual() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_FULL_NAME, PolyString.fromOrig("John Doe"));
        modifyUserReplace(USER2.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Different"));
        modifyUserReplace(USER3.oid, UserType.F_FULL_NAME, PolyString.fromOrig("Another"));

        modifyShadowReplace("user1", CN, "John Doe");
        modifyShadowReplace("user2", CN, "Different Value");
        modifyShadowReplace("user3", CN, "Another Value");

        refreshShadows();

        var mockClient = createClient(
                List.of(ItemPath.create(UserType.F_FULL_NAME)),
                List.of(CN.path()),
                null, null, null, null, null, null, null
        );
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                true,
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        var cnMappings = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("cn"))
                .toList();

        assertThat(cnMappings)
                .as("Should have exactly one cn mapping")
                .hasSize(1);

        var cnMapping = cnMappings.get(0);

        assertThat(SmartMetadataUtil.isMarkedAsSystemProvided(cnMapping.asPrismContainerValue()))
                .as("System-provided should be preferred when quality is similar")
                .isTrue();
    }

    @Test
    public void test120DeduplicationAgainstAcceptedSuggestion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_TELEPHONE_NUMBER, "123-456-7890");
        modifyUserReplace(USER2.oid, UserType.F_TELEPHONE_NUMBER, "123-456-7891");
        modifyUserReplace(USER3.oid, UserType.F_TELEPHONE_NUMBER, "123-456-7892");

        modifyShadowReplace("user1", TELEPHONE_NUMBER, "123-456-7890");
        modifyShadowReplace("user2", TELEPHONE_NUMBER, "123-456-7891");
        modifyShadowReplace("user3", TELEPHONE_NUMBER, "123-456-7892");

        refreshShadows();

        var acceptedSuggestionPaths = List.of(ItemPath.create(UserType.F_TELEPHONE_NUMBER));

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                true,
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, true, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, acceptedSuggestionPaths);

        var telephoneMappings = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().contains("telephoneNumber"))
                .toList();

        assertThat(telephoneMappings)
                .as("Mapping for telephoneNumber should be excluded because it was already accepted in GUI")
                .isEmpty();
    }

    @Test
    public void test130OutboundDnMappingSuggestion() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user3", DISTINGUISHED_NAME, "uid=user3,ou=People,dc=example,dc=com");

        refreshShadows();

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                false, // outbound
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, false, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        assertThat(suggestion.getAttributeMappings())
                .as("Outbound system mappings should be present")
                .isNotEmpty();

        var dnMapping = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("dn"))
                .findFirst();

        assertThat(dnMapping)
                .as("DN outbound mapping should be present")
                .isPresent();

        var outboundMapping = dnMapping.get().getDefinition().getOutbound();
        assertThat(outboundMapping)
                .as("DN mapping should have outbound configuration")
                .isNotNull();

        var expression = outboundMapping.getExpression();
        assertThat(expression)
                .as("DN mapping should have expression")
                .isNotNull();

        assertThat(expression.getDescription())
                .as("DN mapping expression should have description")
                .contains("Compose DN");

        var scriptEvaluator = expression.getExpressionEvaluator().stream()
                .filter(e -> e.getValue() instanceof ScriptExpressionEvaluatorType)
                .map(e -> (ScriptExpressionEvaluatorType) e.getValue())
                .findFirst();

        assertThat(scriptEvaluator)
                .as("DN mapping should have script expression evaluator")
                .isPresent();

        String script = scriptEvaluator.get().getCode();
        assertThat(script)
                .as("Script should use basic.composeDnWithSuffix function")
                .contains("basic.composeDnWithSuffix");
        assertThat(script)
                .as("Script should use 'uid' as RDN type")
                .contains("'uid'");
        assertThat(script)
                .as("Script should use 'name' as RDN value")
                .contains("name");
        assertThat(script)
                .as("Script should use extracted suffix from sample DNs")
                .contains("ou=People,dc=example,dc=com");
        assertThat(script)
                .as("Script should NOT use resource config fallback")
                .doesNotContain("basic.getResourceIcfConfigurationPropertyValue");

        assertThat(SmartMetadataUtil.isMarkedAsSystemProvided(dnMapping.get().asPrismContainerValue()))
                .as("DN mapping should be marked as system-provided")
                .isTrue();

        assertThat(dnMapping.get().getExpectedQuality())
                .as("DN mapping should have quality assessed (script executed successfully)")
                .isNotNull()
                .isEqualTo(1.0f);
    }

    @Test
    public void test132OutboundDnMappingNotSuggestedWhenNoSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyShadowReplace("user1", DISTINGUISHED_NAME);
        modifyShadowReplace("user2", DISTINGUISHED_NAME);
        modifyShadowReplace("user3", DISTINGUISHED_NAME);

        refreshShadows();

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                false, // outbound
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_LDAP.oid, ACCOUNT_DEFAULT, false, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        var dnMapping = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("dn"))
                .findFirst();

        assertThat(dnMapping)
                .as("DN mapping should NOT be suggested when no DN found in samples (no resource config fallback)")
                .isEmpty();
    }

    @Test
    public void test140OutboundAdDnMappingSuggestionWithCn() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER1.oid, UserType.F_FULL_NAME, PolyString.fromOrig("John Doe"));

        dummyScenarioAd.adUser.getByNameRequired("user1")
                .replaceAttributeValues(DummyScenario.AdUser.AttributeNames.DISTINGUISHED_NAME.local(), 
                        "cn=John Doe,ou=Users,dc=example,dc=com");

        provisioningService.searchShadows(
                Resource.of(RESOURCE_AD.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, task, result);

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_AD.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                false, // outbound
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_AD.oid, ACCOUNT_DEFAULT, false, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        var dnMapping = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("distinguishedName"))
                .findFirst();

        assertThat(dnMapping)
                .as("AD distinguishedName outbound mapping should be present")
                .isPresent();

        var outboundMapping = dnMapping.get().getDefinition().getOutbound();
        assertThat(outboundMapping).isNotNull();

        var expression = outboundMapping.getExpression();
        assertThat(expression).isNotNull();

        var scriptEvaluator = expression.getExpressionEvaluator().stream()
                .filter(e -> e.getValue() instanceof ScriptExpressionEvaluatorType)
                .map(e -> (ScriptExpressionEvaluatorType) e.getValue())
                .findFirst();

        assertThat(scriptEvaluator).isPresent();

        String script = scriptEvaluator.get().getCode();
        assertThat(script)
                .as("Script should use basic.composeDnWithSuffix function")
                .contains("basic.composeDnWithSuffix");
        assertThat(script)
                .as("Script should use 'cn' as RDN type for AD")
                .contains("'cn'");
        assertThat(script)
                .as("Script should use 'fullName' as RDN value")
                .contains("fullName");
        assertThat(script)
                .as("Script should use extracted suffix from AD DN samples")
                .contains("ou=Users,dc=example,dc=com");

        assertThat(SmartMetadataUtil.isMarkedAsSystemProvided(dnMapping.get().asPrismContainerValue()))
                .as("DN mapping should be marked as system-provided")
                .isTrue();
    }

    @Test
    public void test142OutboundAdDnMappingNotSuggestedWithoutSamples() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyScenarioAd.adUser.getByNameRequired("user1")
                .replaceAttributeValues(DummyScenario.AdUser.AttributeNames.DISTINGUISHED_NAME.local());

        provisioningService.searchShadows(
                Resource.of(RESOURCE_AD.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, task, result);

        var mockClient = createClient(List.of(), List.of(), null, null, null, null, null, null, null);
        TestServiceClientFactory.mockServiceClient(clientFactoryMock, mockClient);
        var ctx = TypeOperationContext.init(mockClient, RESOURCE_AD.oid, ACCOUNT_DEFAULT, null, task, result);

        var op = MappingsSuggestionOperation.init(
                ctx,
                new MappingsQualityAssessor(expressionFactory),
                new OwnedShadowsProviderFromResource(),
                wellKnownSchemaService,
                heuristicRuleMatcher,
                false, // outbound
                true);

        var match = smartIntegrationService.computeSchemaMatch(RESOURCE_AD.oid, ACCOUNT_DEFAULT, false, task, result);
        MappingsSuggestionType suggestion = op.suggestMappings(result, match, null);

        var dnMapping = suggestion.getAttributeMappings().stream()
                .filter(m -> m.getDefinition() != null
                        && m.getDefinition().getRef() != null
                        && m.getDefinition().getRef().toString().endsWith("distinguishedName"))
                .findFirst();

        assertThat(dnMapping)
                .as("AD DN mapping should NOT be suggested when no DN found in samples")
                .isEmpty();
    }
}
