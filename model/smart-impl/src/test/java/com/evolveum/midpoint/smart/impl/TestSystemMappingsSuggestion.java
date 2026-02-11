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
        linkAccount(USER1, initTask, initResult);

        dummyScenario.inetOrgPerson.add("user2")
                .addAttributeValues(CN.local(), "Jane Smith")
                .addAttributeValues(SN.local(), "Smith")
                .addAttributeValues(GIVEN_NAME.local(), "Jane")
                .addAttributeValues(MAIL.local(), "jane@example.com");
        linkAccount(USER2, initTask, initResult);

        dummyScenario.inetOrgPerson.add("user3")
                .addAttributeValues(CN.local(), "Bob Johnson")
                .addAttributeValues(SN.local(), "Johnson")
                .addAttributeValues(GIVEN_NAME.local(), "Bob")
                .addAttributeValues(MAIL.local(), "bob@example.com");
        linkAccount(USER3, initTask, initResult);
    }

    private void refreshShadows() throws Exception {
        provisioningService.searchShadows(
                Resource.of(RESOURCE_LDAP.getObjectable())
                        .queryFor(ACCOUNT_DEFAULT)
                        .build(),
                null, getTestTask(), getTestOperationResult());
    }

    private void linkAccount(TestObject<?> user, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(RESOURCE_LDAP.getObjectable())
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
}
