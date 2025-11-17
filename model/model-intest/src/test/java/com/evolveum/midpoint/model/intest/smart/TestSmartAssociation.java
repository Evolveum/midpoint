/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Smart Integration Association function Service implementation.
 * Validates that the associations suggested by the SmartIntegrationService match a predefined list of expected associations.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartAssociation extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    private static final int TIMEOUT = 500_000;

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    /** Dummy resource containing smart association definitions used for testing. */
    private static final DummyTestResource RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-smart-association-types.xml", "9efbb35d-b2e7-4097-ba5e-3c149f7f932a",
            "ad-smart-association-types",
            c -> DummyAdSmartAssociationsScenario.on(c).initialize()
    );

    private static final String RESOURCE_OID = RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES.oid;

    private static final List<String> ALL_EXPECTED_ASSOCIATIONS = List.of(
            makeAssociationKey("ACCOUNT/default", "ENTITLEMENT/app-group"),
            makeAssociationKey("ACCOUNT/default", "ENTITLEMENT/generic-group"),
            makeAssociationKey("ACCOUNT/default", "ENTITLEMENT/org-group"),
            makeAssociationKey("ACCOUNT/person", "ENTITLEMENT/contract"),
            makeAssociationKey("ENTITLEMENT/app-group", "ENTITLEMENT/generic-group"),
            makeAssociationKey("ENTITLEMENT/app-group", "ENTITLEMENT/org-group"),
            makeAssociationKey("ENTITLEMENT/generic-group", "ENTITLEMENT/app-group"),
            makeAssociationKey("ENTITLEMENT/generic-group", "ENTITLEMENT/org-group"),
            makeAssociationKey("ENTITLEMENT/org-group", "ENTITLEMENT/app-group"),
            makeAssociationKey("ENTITLEMENT/org-group", "ENTITLEMENT/generic-group"),
            makeAssociationKey("ENTITLEMENT/contract", "GENERIC/orgUnit"),
            makeAssociationKey("ENTITLEMENT/expert", "ACCOUNT/default"),
            makeAssociationKey("GENERIC/orgUnit", "ENTITLEMENT/contract"),
            // identity links
            makeAssociationKey("ENTITLEMENT/generic-group", "ENTITLEMENT/generic-group"),
            makeAssociationKey("ENTITLEMENT/org-group", "ENTITLEMENT/org-group"),
            makeAssociationKey("ENTITLEMENT/app-group", "ENTITLEMENT/app-group")
    );

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult, CommonInitialObjects.ARCHETYPE_UTILITY_TASK);
        initAndTestDummyResource(RESOURCE_DUMMY_AD_SMART_ASSOCIATION_TYPES, initTask, initResult);
    }

    @Test
    public void testSmartAssociation_shouldSuggestAllPossibleAssociationsFromReferenceAttributes() throws Exception {
        var suggestions = prepareAndSuggestAssociations();

        assertThat(suggestions)
                .as("Expected non-empty association suggestions")
                .isNotEmpty();

        var actualAssociations = suggestions.stream()
                .map(this::associationToKey)
                .collect(Collectors.toList());

        assertThat(actualAssociations)
                .as("All expected smart associations should be suggested")
                .containsExactlyInAnyOrderElementsOf(ALL_EXPECTED_ASSOCIATIONS);
    }

    @Test
    public void testSmartAssociation_shouldConstructNamesForSuggestedAssociations() throws Exception {
        var suggestions = prepareAndSuggestAssociations();

        var actualAssociationsNames = suggestions.stream()
                .map(a -> a.getDefinition().getName().getLocalPart())
                .collect(Collectors.toList());

        assertThat(actualAssociationsNames)
                .as("All names should be defined")
                .doesNotContainNull()
                .doesNotContain("");

        var suggestion1 = findSuggestion(suggestions, makeAssociationKey("ENTITLEMENT/contract", "GENERIC/orgUnit"));
        assertThat(suggestion1.getDefinition().getName().getLocalPart())
                .as("Suggestion name should be correctly generated")
                .isEqualTo("EntitlementContract-GenericOrgUnit");

        var suggestion2 = findSuggestion(suggestions, makeAssociationKey("ENTITLEMENT/org-group", "ENTITLEMENT/generic-group"));
        assertThat(suggestion2.getDefinition().getName().getLocalPart())
                .as("Suggestion name should be correctly generated")
                .isEqualTo("EntitlementOrggroup-EntitlementGenericgroup");
    }

    @Test
    public void testSmartAssociation_shouldSuggestCorrectInboundAssociationStructure() throws Exception {
        var isInbound = true;
        var suggestions = prepareAndSuggestAssociations(isInbound);

        var suggestion = findSuggestion(suggestions, makeAssociationKey("ACCOUNT/person", "ENTITLEMENT/contract"));

        assertThat(suggestion.getDefinition().getDescription())
                .as("association has non empty description")
                .isNotEmpty();

        // exclude attributes not important to the full assertion
        suggestion.getDefinition().setDescription(null); // don't need to diff

        var actualValue = prettySerialize(suggestion);
        var expectedValue = dedent("""
        <definition>
            <name>ri:AccountPerson-EntitlementContract</name>
            <displayName>AccountPerson-EntitlementContract</displayName>
            <subject>
                <ref>person</ref>
                <objectType>
                    <kind>account</kind>
                    <intent>person</intent>
                </objectType>
                <association>
                    <ref>ri:contract</ref>
                    <sourceAttributeRef>ri:contract</sourceAttributeRef>
                    <inbound>
                        <name>AccountPerson-EntitlementContract-inbound</name>
                        <expression>
                            <associationSynchronization xsi:type="c:AssociationSynchronizationExpressionEvaluatorType">
                                <objectRef>
                                    <correlator/>
                                    <mapping>
                                        <expression>
                                            <shadowOwnerReferenceSearch/>
                                        </expression>
                                        <target>
                                            <path>targetRef</path>
                                        </target>
                                    </mapping>
                                </objectRef>
                                <synchronization>
                                    <reaction>
                                        <situation>unmatched</situation>
                                        <actions>
                                            <addFocusValue/>
                                        </actions>
                                    </reaction>
                                    <reaction>
                                        <situation>matched</situation>
                                        <actions>
                                            <synchronize/>
                                        </actions>
                                    </reaction>
                                </synchronization>
                            </associationSynchronization>
                        </expression>
                    </inbound>
                </association>
            </subject>
            <object>
                <ref>contract</ref>
                <objectType>
                    <kind>entitlement</kind>
                    <intent>contract</intent>
                </objectType>
            </object>
        </definition>
        """);
        assertEqualsMultiline("body of association should be equal (inbound)", expectedValue, actualValue);
    }

    @Test
    public void testSmartAssociation_shouldSuggestCorrectOutboundAssociationStructure() throws Exception {
        var isOutbound = false;
        var suggestions = prepareAndSuggestAssociations(isOutbound);

        var suggestion = findSuggestion(suggestions, makeAssociationKey("ACCOUNT/person", "ENTITLEMENT/contract"));

        assertThat(suggestion.getDefinition().getDescription())
                .as("association has non empty description")
                .isNotEmpty();

        // exclude attributes not important to the full assertion
        suggestion.getDefinition().setDescription(null); // don't need to diff

        var actualValue = prettySerialize(suggestion);
        var expectedValue = dedent("""
        <definition>
            <name>ri:AccountPerson-EntitlementContract</name>
            <displayName>AccountPerson-EntitlementContract</displayName>
            <subject>
                <ref>person</ref>
                <objectType>
                    <kind>account</kind>
                    <intent>person</intent>
                </objectType>
                <association>
                    <ref>ri:contract</ref>
                    <sourceAttributeRef>ri:contract</sourceAttributeRef>
                    <outbound>
                        <name>AccountPerson-EntitlementContract-outbound</name>
                        <strength>strong</strength>
                        <expression>
                            <associationConstruction xsi:type="c:AssociationConstructionExpressionEvaluatorType">
                                <objectRef>
                                    <mapping>
                                        <expression>
                                            <associationFromLink/>
                                        </expression>
                                    </mapping>
                                </objectRef>
                            </associationConstruction>
                        </expression>
                    </outbound>
                </association>
            </subject>
            <object>
                <ref>contract</ref>
                <objectType>
                    <kind>entitlement</kind>
                    <intent>contract</intent>
                </objectType>
            </object>
        </definition>
        """);
        assertEqualsMultiline("body of association should be equal (outbound)", expectedValue, actualValue);
    }

    @Test
    public void testSuggestAssociations_shouldRunOperationAsynchronously() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest associations' operation request");
        var token = smartIntegrationService.submitSuggestAssociationsOperation(RESOURCE_OID, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestAssociationsOperationStatus(token, task, result),
                TIMEOUT);

        then("there are suggested associations");
        displayDumpable("response", response);
        assertThat(response).isNotNull();
        assertThat(response.getAssociation()).as("suggested associations").isNotEmpty();

        when("listing all suggest associations operation statuses for the resource");
        var statuses = smartIntegrationService.listSuggestAssociationsOperationStatuses(RESOURCE_OID, task, result);

        then("the list should contain the submitted token");
        statuses.forEach(s -> displayDumpable("association operation status", s));
        assertThat(statuses)
                .as("statuses list should not be empty")
                .isNotEmpty();

        var tokens = statuses.stream()
                .map(StatusInfo::getToken)
                .collect(Collectors.toSet());

        assertThat(tokens)
                .as("statuses should include the token of the submitted operation")
                .contains(token);
    }

    /**
     * Association suggestion to a string key in a format: subjectKind/subjectIntent=>objectKind/objectIntent.
     * It supports just one object.
     */
    private String associationToKey(AssociationSuggestionType suggestion) {
        var subjectKind = suggestion.getDefinition().getSubject().getObjectType().get(0).getKind();
        var subjectIntent = suggestion.getDefinition().getSubject().getObjectType().get(0).getIntent();
        assertThat(suggestion.getDefinition().getObject())
                .as("Invariant: association has exactly one object") // NOTE: currently supports just one object, but will change when we implement complex associations
                .hasSize(1);
        var objectKind = suggestion.getDefinition().getObject().get(0).getObjectType().get(0).getKind();
        var objectIntent = suggestion.getDefinition().getObject().get(0).getObjectType().get(0).getIntent();
        return makeAssociationKey(subjectKind + "/" + subjectIntent, objectKind + "/" + objectIntent);
    }

    private static String makeAssociationKey(String subjectKindIntent, String objectKindIntent) {
        return subjectKindIntent + "=>" + objectKindIntent;
    }

    private AssociationSuggestionType findSuggestion(List<AssociationSuggestionType> suggestions, String strId) {
        return suggestions.stream()
                .filter(suggestion -> {
                    var subjectKind = suggestion.getDefinition().getSubject().getObjectType().get(0).getKind();
                    var subjectIntent = suggestion.getDefinition().getSubject().getObjectType().get(0).getIntent();
                    var objectKind = suggestion.getDefinition().getObject().get(0).getObjectType().get(0).getKind();
                    var objectIntent = suggestion.getDefinition().getObject().get(0).getObjectType().get(0).getIntent();
                    String associationKey = makeAssociationKey(subjectKind + "/" + subjectIntent, objectKind + "/" + objectIntent);
                    return associationKey.equals(strId);
                })
                .findFirst()
                .orElse(null);
    }

    private List<AssociationSuggestionType> prepareAndSuggestAssociations() throws Exception {
        var isInbound = true;
        return prepareAndSuggestAssociations(isInbound);
    }

    private List<AssociationSuggestionType> prepareAndSuggestAssociations(boolean isInbound) throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_OID, null, task, result);
        displayValueAsXml("Resource: ad-smart-association-types", resource.getValue());

        AssociationsSuggestionType associationsSuggestion= smartIntegrationService.suggestAssociations(RESOURCE_OID, isInbound, task, result);

        displayValueAsXml("Suggested Associations", associationsSuggestion);

        assertThat(associationsSuggestion)
                .as("Association suggestion must not be null")
                .isNotNull();

        return associationsSuggestion.getAssociation();
    }

    /* Dedent whitespaces and trim. */
    private String dedent(String value) {
        return value.stripIndent().trim();
    }

    /* Serialize xml without root tag, pretty formatted. */
    private String prettySerialize(Object value) throws SchemaException {
        var raw = prismContext.xmlSerializer().serializeRealValue(value, SchemaConstants.C_VALUE);
        raw = raw.trim();
        raw = raw.substring(raw.indexOf('\n') + 1, raw.lastIndexOf('\n')); // strip root value container
        raw = dedent(raw);
        return raw;
    }

    private void assertEqualsMultiline(String message, String expected, String actual) {
        // simple assertEquals is convenient to use because it has better idea integration
        // if it fails idea offers to see the diff - "Click to see difference", that doesn't work with assertj
        assertEquals(message, expected, actual);
        // if there are spacing problems it could be asserted with relaxed indentation (but without the nice diffing feature)
        // assertThat(actual).isEqualToNormalizingWhitespace(expected)
    }

}
