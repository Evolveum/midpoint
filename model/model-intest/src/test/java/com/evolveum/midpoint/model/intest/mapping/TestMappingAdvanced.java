/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import java.io.File;

import com.evolveum.midpoint.test.PredefinedTestMethodTracing;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Various advanced tests related to mappings.
 *
 * NOT a subclass of AbstractMappingTest.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingAdvanced extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/mapping/advanced");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> USER_TEMPLATE = new TestResource<>(TEST_DIR, "user-template.xml", "bf0cf9b7-4c38-4ff4-afc6-c9cc9bc08490");

    private static final TestResource<UserType> USER_FRANZ = new TestResource<>(TEST_DIR, "user-franz.xml", "ac42084a-d780-4d32-ac02-bcc49bdc747b");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_TEMPLATE, initResult);

        repoAdd(USER_FRANZ, initResult); // intentionally not via full processing

        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100RecomputeFranz() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        recomputeUser(USER_FRANZ.oid, task, result);

        then();
        assertUserAfter(USER_FRANZ.oid)
                .assertDescription("2") // from 0 to 1 in the first projector run, from 1 to 2 in the second
                .assertOrganizations("O2"); // should be from O0 to O1, then from O1 to O2 leading to [O2] ... but the real output is [O1, O2]
    }
}
