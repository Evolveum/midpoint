/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;

/**
 * Testing mapping chaining (eventually in all contexts - in template, in assignments, etc).
 */
@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingChaining extends AbstractMappingTest {

    public static final File TEST_DIR = new File("src/test/resources/mapping/chaining");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
    private static final TestObject<?> USER_TEMPLATE = TestObject.file(TEST_DIR, "user-template.xml", "c983791f-10c7-410c-83bd-71ef85174505");

    private static final TestObject<?> ROLE_MASTER = TestObject.file(TEST_DIR, "role-master.xml", "c7968dad-0711-4e45-8846-b6c28fc8c71a");
    private static final TestObject<?> USER_LEGALL = TestObject.file(TEST_DIR, "user-legall.xml", "db3875df-5f5e-4379-bca5-d1349eee5033");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_TEMPLATE, initResult);
        repoAdd(ROLE_MASTER, initResult);
        repoAdd(USER_LEGALL, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * MID-6135. (Computation of delta from upstream mapping was not correct, misleading the downstream one.)
     */
    @Test
    public void test100RecomputeUser() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();

        recomputeUser(USER_LEGALL.oid, task, result);

        then();

        assertSuccess(result);
        assertUser(USER_LEGALL.oid, "user after")
                .assertAssignments(0)
                .assertExtensionValue("booleanFlag", false);
    }
}
