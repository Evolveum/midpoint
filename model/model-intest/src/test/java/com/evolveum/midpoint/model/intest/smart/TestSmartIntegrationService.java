/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Integration tests for the Smart Integration Service implementation.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationService extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart");

    @Autowired private SmartIntegrationService smartIntegrationService;

    private static DummyBasicScenario basicScenario;

    private static final DummyTestResource RESOURCE_DUMMY_BASIC = new DummyTestResource(
            TEST_DIR, "resource-dummy-basic.xml", "1e97ba6f-90a7-4764-954b-6a29ed5eb597", "basic",
            c -> basicScenario = DummyBasicScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_BASIC, initTask, initResult);
    }

    @Test
    public void test100SuggestFocusType() {
        var task = getTestTask();
        var result = task.getResult();

        when("suggesting focus type");
        var focusType = smartIntegrationService.suggestFocusType(
                RESOURCE_DUMMY_BASIC.oid, ResourceObjectTypeIdentification.ACCOUNT_DEFAULT, task, result);

        then("the focus type is correct");
        assertSuccess(result);
        assertThat(focusType)
                .as("Focus type")
                .isEqualTo(UserType.COMPLEX_TYPE);
    }
}
