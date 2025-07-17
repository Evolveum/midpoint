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

import com.evolveum.axiom.concepts.CheckedSupplier;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Integration tests for the Smart Integration Service implementation.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationService extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

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

        //addDummyObjects(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_OBJECT_TYPES, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_FOR_SUGGEST_FOCUS_TYPE, initTask, initResult);

        if (System.getProperty(MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE) == null) {
            // For tests without a real service, we have to use a mock service client.
            smartIntegrationService.setServiceClientSupplier(MockServiceClientImpl::new);
        }
    }

    @Test
    public void test100SuggestObjectTypes() throws CommonException {
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

    @SuppressWarnings("SameParameterValue")
    private <T> T waitForFinish(
            CheckedSupplier<SmartIntegrationService.StatusInformation<T>, CommonException> statusInformationSupplier,
            long timeout) throws CommonException {

        var checker = new Checker() {

            SmartIntegrationService.StatusInformation<T> lastStatusInformation;

            @Override
            public boolean check() throws CommonException {
                lastStatusInformation = statusInformationSupplier.get();
                return lastStatusInformation.status() != OperationResultStatus.IN_PROGRESS;
            }

            @Override
            public void timeout() {
                fail("Timeout while waiting for the operation to finish. Last status: " + lastStatusInformation);
            }
        };

        IntegrationTestTools.waitFor("Waiting for the operation to finish", checker, timeout, 500);
        if (checker.lastStatusInformation.status() != OperationResultStatus.SUCCESS) {
            fail("Operation did not finish successfully. Last status: " + checker.lastStatusInformation);
        }
        return checker.lastStatusInformation.result();
    }

    @Test
    public void test150SuggestFocusType() throws CommonException {
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
