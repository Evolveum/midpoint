/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ENTITLEMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.GENERIC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Smart Integration Association function Service implementation.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartAssociation extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    /** Dummy resource containing smart association definitions used for testing. */
    private static final DummyTestResource RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-smart-association-types.xml", "9efbb35d-b2e7-4097-ba5e-3c149f7f932a",
            "ad-smart-association-types",
            c -> DummyAdSmartAssociationsScenario.on(c).initialize());

    /**
     * Represents the expected structure of a suggested association.
     *
     * @param name         The expected name of the association definition
     * @param sourceKind   The kind of the source object (e.g., ACCOUNT, ENTITLEMENT)
     * @param sourceIntent The intent of the source object
     * @param targetKind   The kind of the target object
     * @param targetIntent The intent of the target object
     */
    private record ExpectedAssociation(
            String name,
            String sourceKind,
            String sourceIntent,
            String targetKind,
            String targetIntent
    ){}

    private record DummyAdSmartScenario (
            Collection<ResourceObjectTypeIdentification> subjectTypeId,
            Collection<ResourceObjectTypeIdentification> objectTypeId,
            List<ExpectedAssociation> expectedAssociations
    ) {}


    private static final DummyAdSmartScenario DUMMY_AD_SMART_SCENARIO_10 = new DummyAdSmartScenario(
            List.of(
                    ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "default"),
                    ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "person"),
                    ResourceObjectTypeIdentification.of(ShadowKindType.ENTITLEMENT, "contract")
            ),
            List.of(
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "app-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "generic-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "org-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "contract"),
                    ResourceObjectTypeIdentification.of(GENERIC, "orgUnit"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "expert")
            ),
            List.of(
                    new ExpectedAssociation("accountDefaultToEntitlementAppgroupGroup",
                            "ACCOUNT", "default", "ENTITLEMENT", "app-group"),
                    new ExpectedAssociation("accountDefaultToEntitlementGenericgroupGroup",
                            "ACCOUNT", "default", "ENTITLEMENT", "generic-group"),
                    new ExpectedAssociation("accountDefaultToEntitlementOrggroupGroup",
                            "ACCOUNT", "default", "ENTITLEMENT", "org-group"),

                    new ExpectedAssociation("accountPersonToEntitlementContractContract",
                            "ACCOUNT", "person", "ENTITLEMENT", "contract"),

                    new ExpectedAssociation("entitlementContractToGenericOrgUnitOrg",
                            "ENTITLEMENT", "contract", "GENERIC", "orgUnit")
            )
    );

    private static final DummyAdSmartScenario DUMMY_AD_SMART_SCENARIO_20 = new DummyAdSmartScenario(
            List.of(
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "app-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "generic-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "org-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "contract"),
                    ResourceObjectTypeIdentification.of(GENERIC, "orgUnit"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "expert")
            ),

            List.of(
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "app-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "generic-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "org-group"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "contract"),
                    ResourceObjectTypeIdentification.of(GENERIC, "orgUnit"),
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "expert")
            ),

            List.of(
                    new ExpectedAssociation("entitlementAppgroupToEntitlementGenericgroupGroup",
                            "ENTITLEMENT", "app-group", "ENTITLEMENT", "generic-group"),
                    new ExpectedAssociation("entitlementAppgroupToEntitlementOrggroupGroup",
                            "ENTITLEMENT", "app-group", "ENTITLEMENT", "org-group"),

                    new ExpectedAssociation("entitlementGenericgroupToEntitlementAppgroupGroup",
                            "ENTITLEMENT", "generic-group", "ENTITLEMENT", "app-group"),
                    new ExpectedAssociation("entitlementGenericgroupToEntitlementOrggroupGroup",
                            "ENTITLEMENT", "generic-group", "ENTITLEMENT", "org-group"),

                    new ExpectedAssociation("entitlementOrggroupToEntitlementAppgroupGroup",
                            "ENTITLEMENT", "org-group", "ENTITLEMENT", "app-group"),
                    new ExpectedAssociation("entitlementOrggroupToEntitlementGenericgroupGroup",
                            "ENTITLEMENT", "org-group", "ENTITLEMENT", "generic-group"),

                    new ExpectedAssociation("genericOrgUnitToEntitlementContractContractOrg",
                            "GENERIC", "orgUnit", "ENTITLEMENT", "contract"),

                    new ExpectedAssociation("entitlementContractToGenericOrgUnitOrg",
                            "ENTITLEMENT", "contract", "GENERIC", "orgUnit")

            )
    );

    private static final DummyAdSmartScenario DUMMY_AD_SMART_SCENARIO_30 = new DummyAdSmartScenario(
            List.of(
                    ResourceObjectTypeIdentification.of(ENTITLEMENT, "expert")
            ),
            List.of(
                    ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "default")
            ),

            List.of(
                    new ExpectedAssociation("entitlementExpertToAccountDefaultExpertReference",
                            "ENTITLEMENT", "expert", "ACCOUNT", "default")
            )
    );


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        initAndTestDummyResource(RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES, initTask, initResult);
    }

    /**
     * Tests the smart association suggestion feature.
     * <p>
     * Validates that the associations suggested by the SmartIntegrationService match
     * a predefined list of expected associations. Each association is validated by name.
     * </p>
     *
     */
    @Test
    public void testSmartAssociationScenario10() throws Exception {

        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                null, task, result);
        displayDumpable("Resource: ad-smart-association-types", resource);

        AssociationsSuggestionType associationsSuggestion= smartIntegrationService.suggestAssociations(
                RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                DUMMY_AD_SMART_SCENARIO_10.subjectTypeId(),
                DUMMY_AD_SMART_SCENARIO_10.objectTypeId(),
                task,
                result);

        displayDumpable("Suggested Associations", associationsSuggestion);

        assertThat(associationsSuggestion)
                .as("Association suggestion must not be null")
                .isNotNull();

        List<AssociationSuggestionType> definitions = associationsSuggestion.getAssociation();

        assertThat(definitions)
                .as("Expected non-empty association suggestions")
                .isNotEmpty();

        Set<String> actualAssociationsSuggestedNames = definitions.stream()
                .map(s -> s.getDefinition().getName().getLocalPart())
                .collect(Collectors.toSet());

        Set<String> expectedAssociationNames = DUMMY_AD_SMART_SCENARIO_10.expectedAssociations().stream()
                .map(ExpectedAssociation::name)
                .collect(Collectors.toSet());

        assertThat(actualAssociationsSuggestedNames)
                .as("All expected smart associations should be suggested")
                .containsExactlyInAnyOrderElementsOf(expectedAssociationNames);


        //TODO (this is only for case when we have single object for association definition)
        // (consider replace it when multiple object will be supported)
        Map<String, ExpectedAssociation> expectedAssociationsMap = DUMMY_AD_SMART_SCENARIO_10.expectedAssociations().stream()
                .collect(Collectors.toMap(ExpectedAssociation::name, ea -> ea));
        definitions.forEach(suggestion -> {
            String name = suggestion.getDefinition().getName().getLocalPart();
            ExpectedAssociation expected = expectedAssociationsMap.get(name);
            assertThat(expected).as("Unexpected association: " + name).isNotNull();
            var subject = suggestion.getDefinition().getSubject().getObjectType().get(0);
            var object = suggestion.getDefinition().getObject().get(0).getObjectType().get(0);
            assertThat(subject.getKind().name()).isEqualTo(expected.sourceKind());
            assertThat(subject.getIntent()).isEqualTo(expected.sourceIntent());
            assertThat(object.getKind().name()).isEqualTo(expected.targetKind());
            assertThat(object.getIntent()).isEqualTo(expected.targetIntent());
        });

    }

    @Test
    public void testSmartAssociationScenario20() throws Exception {

        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                null, task, result);
        displayDumpable("Resource: ad-smart-association-types", resource);

        AssociationsSuggestionType associationsSuggestion= smartIntegrationService.suggestAssociations(
                RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                DUMMY_AD_SMART_SCENARIO_20.subjectTypeId(),
                DUMMY_AD_SMART_SCENARIO_20.objectTypeId(),
                task,
                result);

        displayDumpable("Suggested Associations", associationsSuggestion);

        assertThat(associationsSuggestion)
                .as("Association suggestion must not be null")
                .isNotNull();

        List<AssociationSuggestionType> definitions = associationsSuggestion.getAssociation();

        assertThat(definitions)
                .as("Expected non-empty association suggestions")
                .isNotEmpty();

        Set<String> actualAssociationsSuggestedNames = definitions.stream()
                .map(s -> s.getDefinition().getName().getLocalPart())
                .collect(Collectors.toSet());

        Set<String> expectedAssociationNames = DUMMY_AD_SMART_SCENARIO_20.expectedAssociations().stream()
                .map(ExpectedAssociation::name)
                .collect(Collectors.toSet());

        assertThat(actualAssociationsSuggestedNames)
                .as("All expected smart associations should be suggested")
                .containsExactlyInAnyOrderElementsOf(expectedAssociationNames);


        //TODO (this is only for case when we have single object for association definition)
        // (consider replace it when multiple object will be supported)
        Map<String, ExpectedAssociation> expectedAssociationsMap = DUMMY_AD_SMART_SCENARIO_20.expectedAssociations().stream()
                .collect(Collectors.toMap(ExpectedAssociation::name, ea -> ea));
        definitions.forEach(suggestion -> {
            String name = suggestion.getDefinition().getName().getLocalPart();
            ExpectedAssociation expected = expectedAssociationsMap.get(name);
            assertThat(expected).as("Unexpected association: " + name).isNotNull();
            var subject = suggestion.getDefinition().getSubject().getObjectType().get(0);
            var object = suggestion.getDefinition().getObject().get(0).getObjectType().get(0);
            assertThat(subject.getKind().name()).isEqualTo(expected.sourceKind());
            assertThat(subject.getIntent()).isEqualTo(expected.sourceIntent());
            assertThat(object.getKind().name()).isEqualTo(expected.targetKind());
            assertThat(object.getIntent()).isEqualTo(expected.targetIntent());
        });

    }

    @Test
    public void testSmartAssociationScenario30() throws Exception {

        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                null, task, result);
        displayDumpable("Resource: ad-smart-association-types", resource);

        AssociationsSuggestionType associationsSuggestion= smartIntegrationService.suggestAssociations(
                RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid,
                DUMMY_AD_SMART_SCENARIO_30.subjectTypeId(),
                DUMMY_AD_SMART_SCENARIO_30.objectTypeId(),
                task,
                result);

        displayDumpable("Suggested Associations", associationsSuggestion);

        assertThat(associationsSuggestion)
                .as("Association suggestion must not be null")
                .isNotNull();

        List<AssociationSuggestionType> definitions = associationsSuggestion.getAssociation();

        assertThat(definitions)
                .as("Expected non-empty association suggestions")
                .isNotEmpty();

        Set<String> actualAssociationsSuggestedNames = definitions.stream()
                .map(s -> s.getDefinition().getName().getLocalPart())
                .collect(Collectors.toSet());

        Set<String> expectedAssociationNames = DUMMY_AD_SMART_SCENARIO_30.expectedAssociations().stream()
                .map(ExpectedAssociation::name)
                .collect(Collectors.toSet());

        assertThat(actualAssociationsSuggestedNames)
                .as("All expected smart associations should be suggested")
                .containsExactlyInAnyOrderElementsOf(expectedAssociationNames);


        //TODO (this is only for case when we have single object for association definition)
        // (consider replace it when multiple object will be supported)
        Map<String, ExpectedAssociation> expectedAssociationsMap = DUMMY_AD_SMART_SCENARIO_30.expectedAssociations().stream()
                .collect(Collectors.toMap(ExpectedAssociation::name, ea -> ea));
        definitions.forEach(suggestion -> {
            String name = suggestion.getDefinition().getName().getLocalPart();
            ExpectedAssociation expected = expectedAssociationsMap.get(name);
            assertThat(expected).as("Unexpected association: " + name).isNotNull();
            var subject = suggestion.getDefinition().getSubject().getObjectType().get(0);
            var object = suggestion.getDefinition().getObject().get(0).getObjectType().get(0);
            assertThat(subject.getKind().name()).isEqualTo(expected.sourceKind());
            assertThat(subject.getIntent()).isEqualTo(expected.sourceIntent());
            assertThat(object.getKind().name()).isEqualTo(expected.targetKind());
            assertThat(object.getIntent()).isEqualTo(expected.targetIntent());
        });

    }

}
