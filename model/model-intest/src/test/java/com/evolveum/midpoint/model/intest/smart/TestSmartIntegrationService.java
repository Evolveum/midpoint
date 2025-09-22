/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.DefaultServiceClientImpl;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Integration tests for the Smart Integration Service implementation.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationService extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    private static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

    private static final int TIMEOUT = 500_000;

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    private static DummyBasicScenario dummyForSuggestCorrelationAndMappings;

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-object-types.xml", "0c59d761-bea9-4342-bbc7-ee0e199d275b",
            "for-suggest-object-types",
            c -> DummyBasicScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-focus-type.xml", "1e97ba6f-90a7-4764-954b-6a29ed5eb597",
            "for-suggest-focus-type",
            c -> DummyBasicScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-correlation-and-mappings.xml", "20d4fca9-bf28-4165-b71b-392172a4b6b2",
            "for-suggest-correlation-and-mappings",
            c -> dummyForSuggestCorrelationAndMappings = DummyBasicScenario.on(c).initialize());

    private static final TestObject<?> USER_JACK = TestObject.file(TEST_DIR, "user-jack.xml", "84d2ff68-9b32-4ef4-b87b-02536fd5e83c");
    private static final TestObject<?> USER_JIM = TestObject.file(TEST_DIR, "user-jim.xml", "8f433649-6cc4-401b-910f-10fa5449f14c");
    private static final TestObject<?> USER_ALICE = TestObject.file(TEST_DIR, "user-alice.xml", "79df4c1f-6480-4eb8-9db7-863e25d5b5fa");
    private static final TestObject<?> USER_BOB = TestObject.file(TEST_DIR, "user-bob.xml", "30cef119-71b6-42b3-9762-5c649b2a2b6a");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS, initTask, initResult);

        initTestObjects(initTask, initResult,
                USER_JACK, USER_JIM, USER_ALICE, USER_BOB);
        createAndLinkAccounts(initTask, initResult);
    }

    private void createAndLinkAccounts(Task initTask, OperationResult initResult) throws Exception {
        var a = dummyForSuggestCorrelationAndMappings.account;
        a.add("jack")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.STATUS.local(), "a")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.TYPE.local(), "e")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.PHONE.local(), "+420-601-040-027");
        linkAccount(USER_JACK, initTask, initResult);
        a.add("jim")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.STATUS.local(), "i")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.PHONE.local(), "+99-123-456-789");
        linkAccount(USER_JIM, initTask, initResult);
        a.add("alice")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.PHONE.local(), "+421-900-111-222")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.STATUS.local(), "a")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.TYPE.local(), "c");
        linkAccount(USER_ALICE, initTask, initResult);
        a.add("bob")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.PHONE.local(), "+421-900-333-444")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.STATUS.local(), "i")
                .addAttributeValues(DummyBasicScenario.Account.AttributeNames.TYPE.local(), "c");
        linkAccount(USER_BOB, initTask, initResult);
    }

    private void linkAccount(TestObject<?> user, Task task, OperationResult result) throws CommonException, IOException {
        var shadow = findShadowRequest()
                .withResource(RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS.getObjectable())
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

    /** Tests the "suggest object types" operation (in an asynchronous way). */
    @Test
    public void test100SuggestObjectTypes() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            //noinspection resource
            var serviceClient = new MockServiceClientImpl(
                    new SiSuggestObjectTypesResponseType()
                            .objectType(new SiSuggestedObjectTypeType()
                                    .kind("account")
                                    .intent("default")
                                    .filter("attributes/ri:type = 'abc'")
                                    .displayName("Default Account")
                                    .description("Catch-all rule for ri:account objects with no specific attribute values available.")),
                    new SiSuggestFocusTypeResponseType()
                            .focusTypeName(UserType.COMPLEX_TYPE));
            smartIntegrationService.setServiceClientSupplier(() -> serviceClient);
        }

        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest object types' operation request");
        var token = smartIntegrationService.submitSuggestObjectTypesOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES.oid, OC_ACCOUNT_QNAME, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestObjectTypesOperationStatus(token, task, result),
                TIMEOUT);

        then("there is at least one suggested object type (we don't care about the actual type here)");
        displayDumpable("response", response);
        assertThat(response).isNotNull();
        assertThat(response.getObjectType()).as("suggested object types collection").isNotEmpty();

        and("response is properly marked regarding AI");
        response.getObjectType().forEach(
                o -> {
                    assertAiProvidedMarkPresent(o,
                            ResourceObjectTypeDefinitionType.F_KIND,
                            ResourceObjectTypeDefinitionType.F_INTENT,
                            ResourceObjectTypeDefinitionType.F_DISPLAY_NAME,
                            ResourceObjectTypeDefinitionType.F_DESCRIPTION,
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_FILTER));
                    assertAiProvidedMarkAbsent(o,
                            ResourceObjectTypeDefinitionType.F_DELINEATION.append(ResourceObjectTypeDelineationType.F_OBJECT_CLASS));
                });
    }

    /** Tests the "suggest focus type" method. */
    @Test
    public void test150SuggestFocusType() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl(
                            new SiSuggestFocusTypeResponseType()
                                    .focusTypeName(UserType.COMPLEX_TYPE)));
        }

        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest focus type' operation request");
        var token = smartIntegrationService.submitSuggestFocusTypeOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE.oid, ACCOUNT_DEFAULT, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestFocusTypeOperationStatus(token, task, result),
                TIMEOUT);

        then("there is a suggested focus type (we don't care about the actual type here)");
        display(response.toString());
        assertThat(response).isNotNull();
        assertThat(response).as("suggested focus type").isNotNull();

        assertAiProvidedMarkPresent(response, FocusTypeSuggestionType.F_FOCUS_TYPE);
    }

    /** Tests the "suggest correlation" operation (in an asynchronous way). */
    @Test
    public void test200SuggestCorrelation() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl(
                            new SiMatchSchemaResponseType()
                                    .attributeMatch(new SiAttributeMatchSuggestionType()
                                            .applicationAttribute(asStringSimple(ICFS_NAME_PATH))
                                            .midPointAttribute(asStringSimple(UserType.F_NAME)))
                                    .attributeMatch(new SiAttributeMatchSuggestionType()
                                            .applicationAttribute(asStringSimple(DummyBasicScenario.Account.AttributeNames.PERSONAL_NUMBER.path()))
                                            .midPointAttribute(asStringSimple(UserType.F_PERSONAL_NUMBER))))); // TODO other matches
        }

        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest correlation' operation request");
        var token = smartIntegrationService.submitSuggestCorrelationOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS.oid, ACCOUNT_DEFAULT, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestCorrelationOperationStatus(token, task, result),
                TIMEOUT);

        then("there is a suggested correlation and an attribute match");
        displayDumpable("response", response);
        assertThat(response).isNotNull();
        var suggestions = response.getSuggestion();
        assertThat(suggestions).as("suggestions").isNotEmpty();
        var suggestion = suggestions.get(0);
        assertThat(suggestion.getCorrelation()).as("suggested correlation").isNotNull();
        assertThat(suggestion.getAttributes()).as("suggested attributes").hasSize(1);
    }

    /** Tests the "suggest mappings" operation (in an asynchronous way). */
    @Test
    public void test300SuggestMappings() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl(
                            new SiMatchSchemaResponseType()
                                    .attributeMatch(new SiAttributeMatchSuggestionType()
                                            .applicationAttribute(asStringSimple(ICFS_NAME_PATH))
                                            .midPointAttribute(asStringSimple(UserType.F_NAME)))
                                    .attributeMatch(new SiAttributeMatchSuggestionType()
                                            .applicationAttribute(asStringSimple(DummyBasicScenario.Account.AttributeNames.PERSONAL_NUMBER.path()))
                                            .midPointAttribute(asStringSimple(UserType.F_PERSONAL_NUMBER))),
                            new SiSuggestMappingResponseType().transformationScript(null),
                            new SiSuggestMappingResponseType().transformationScript(null))); // TODO other matches
        }

        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest mappings' operation request");
        var token = smartIntegrationService.submitSuggestMappingsOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS.oid, ACCOUNT_DEFAULT, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestMappingsOperationStatus(token, task, result),
                TIMEOUT);

        then("there are suggested mappings");
        displayDumpable("response", response);
        assertThat(response).isNotNull();
        assertThat(response.getAttributeMappings()).as("suggested attribute mappings").isNotEmpty();
    }

    /** Tests the handling of an LLM failure in "suggest mappings" operation. */
    @Test
    public void test310SuggestMappingsWithFailure() throws CommonException {
        skipIfRealService();
        smartIntegrationService.setServiceClientSupplier(
                () -> new MockServiceClientImpl(
                        new SiMatchSchemaResponseType()
                                .attributeMatch(new SiAttributeMatchSuggestionType()
                                        .applicationAttribute(asStringSimple(ICFS_NAME_PATH))
                                        .midPointAttribute(asStringSimple(UserType.F_NAME)))
                                .attributeMatch(new SiAttributeMatchSuggestionType()
                                        .applicationAttribute(asStringSimple(DummyBasicScenario.Account.AttributeNames.PERSONAL_NUMBER.path()))
                                        .midPointAttribute(asStringSimple(UserType.F_PERSONAL_NUMBER)))
                                .attributeMatch(new SiAttributeMatchSuggestionType()
                                        .applicationAttribute(asStringSimple(DummyBasicScenario.Account.AttributeNames.TYPE.path()))
                                        .midPointAttribute(asStringSimple(UserType.F_DESCRIPTION)))
                                .attributeMatch(new SiAttributeMatchSuggestionType()
                                        .applicationAttribute(asStringSimple(DummyBasicScenario.Account.AttributeNames.PHONE.path()))
                                        .midPointAttribute(asStringSimple(UserType.F_TELEPHONE_NUMBER))),
                        new RuntimeException("LLM went crazy here"),
                        new SiSuggestMappingResponseType().transformationScript("input.replaceAll('-', '')")));

        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest mappings' operation request");
        var token = smartIntegrationService.submitSuggestMappingsOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_CORRELATION_AND_MAPPINGS.oid, ACCOUNT_DEFAULT, task, result);

        then("returned token is not null");
        assertThat(token).isNotNull();

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestMappingsOperationStatus(token, task, result),
                TIMEOUT);

        then("there are suggested mappings");
        displayDumpable("response", response);
        assertThat(response).isNotNull();
        assertThat(response.getAttributeMappings()).as("suggested attribute mappings").isNotEmpty();

        and("the error is correctly reported");
        var lastStatus = smartIntegrationService.getSuggestMappingsOperationStatus(token, task, result);
        displayDumpable("status (complex)", lastStatus);
        displayValue("result status", lastStatus.getStatus()); // should be PARTIAL_ERROR (but we're not there yet)

        // Just to dump the details
        assertTask(token, "task after")
                .display();
    }

    private void skipIfRealService() {
        skipTestIf(DefaultServiceClientImpl.hasServiceUrlOverride(), "Not applicable with a real service");
    }
}
