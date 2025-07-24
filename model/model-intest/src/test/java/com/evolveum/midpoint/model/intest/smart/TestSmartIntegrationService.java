/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.DefaultServiceClientImpl;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestFocusTypeResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestObjectTypesResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiSuggestedObjectTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Integration tests for the Smart Integration Service implementation.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationService extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    private static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

    private static final int TIMEOUT = 20000;

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-object-types.xml", "0c59d761-bea9-4342-bbc7-ee0e199d275b",
            "for-suggest-object-types",
            c -> DummyBasicScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE = new DummyTestResource(
            TEST_DIR, "resource-dummy-for-suggest-focus-type.xml", "1e97ba6f-90a7-4764-954b-6a29ed5eb597",
            "for-suggest-focus-type",
            c -> DummyBasicScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE, initTask, initResult);
    }

    /** Tests the "suggest object types" operation (in an asynchronous way). */
    @Test
    public void test100SuggestObjectTypes() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl<>(
                            new SiSuggestObjectTypesResponseType()
                                    .objectType(new SiSuggestedObjectTypeType()
                                            .kind("account")
                                            .intent("default"))));
        }

        when("submitting 'suggest object types' operation request");
        var task = getTestTask();
        var result = task.getResult();

        when("submitting 'suggest object types' operation request");
        var token = smartIntegrationService.submitSuggestObjectTypesOperation(
                RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE.oid, OC_ACCOUNT_QNAME, task, result);

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
    }

    /** Tests the "suggest focus type" method; currently, only the synchronous API is present. */
    @Test
    public void test150SuggestFocusType() throws CommonException {
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // We'll go with the real service client. Hence, this test will not check the actual response; only in rough contours.
        } else {
            smartIntegrationService.setServiceClientSupplier(
                    () -> new MockServiceClientImpl<>(
                            new SiSuggestFocusTypeResponseType()
                                    .focusTypeName(UserType.COMPLEX_TYPE)));
        }

        var task = getTestTask();
        var result = task.getResult();

        when("suggesting focus type");
        var focusType = smartIntegrationService.suggestFocusType(
                RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE.oid, ResourceObjectTypeIdentification.ACCOUNT_DEFAULT, task, result);

        then("the focus type is correct");
        assertSuccess(result);
        assertThat(focusType)
                .as("Focus type")
                .isEqualTo(UserType.COMPLEX_TYPE);
    }
}
