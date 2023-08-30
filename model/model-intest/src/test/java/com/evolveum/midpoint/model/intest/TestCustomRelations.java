/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests behavior of custom (non-standard) relations.
 * Currently not part of automated test suites.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCustomRelations extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/custom-relations");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<UserType> USER_ABRAHAM = TestObject.file(TEST_DIR, "user-abraham.xml", "855e276c-65d4-49ac-98eb-e871b737bd84");
    private static final TestObject<UserType> USER_ISAAC = TestObject.file(TEST_DIR, "user-isaac.xml", "020a0294-7fb7-4d43-845f-dfcd8843bec0");

    private static final String NS_CUSTOM = "http://custom";
    private static final QName CHILD = new QName(NS_CUSTOM, "child");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_ISAAC, initResult);
        repoAdd(USER_ABRAHAM, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test(enabled = false) // We do not support user-user relations other than delegation one.
    public void test100AddParent() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // @formatter:off
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                    .add(ObjectTypeUtil.createAssignmentTo(USER_ABRAHAM.get(), CHILD))
                .asObjectDelta(USER_ISAAC.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);

        then();
        assertUser(USER_ISAAC.oid, "after")
                .display();
    }
}
