/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Testing "very authoritative source". If a value was provided by such a source (via inbound mapping), it should
 * disappear if the source projection is gone.
 *
 * See e.g. MID-7725.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestVeryAuthoritativeSource extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "very-authoritative-source");

    private static final File FILE_SYSTEM_CONFIGURATION = new File(TEST_DIR, "system-configuration.xml");

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-source.xml", "de33fd4e-753a-43a6-bda7-cf1f2576c885", "source");
    private static final DummyTestResource RESOURCE_TARGET = new DummyTestResource(
            TEST_DIR, "resource-target.xml", "fef05e2f-92fc-4171-ba70-c1132ef3bc36", "target");

    private static final TestObject<RoleType> ROLE_TARGET = TestObject.file(
            TEST_DIR, "role-target.xml", "7fbffbc6-e7cf-45a2-b527-e646a8c6afdd");
    private static final TestObject<ObjectTemplateType> TEMPLATE_DELETE_DESCRIPTION = TestObject.file(
            TEST_DIR, "template-delete-description.xml", "987585f0-d457-44d7-8550-d9f1e954dc33");
    private static final TestObject<ObjectTemplateType> TEMPLATE_GLOBAL = TestObject.file(
            TEST_DIR, "template-global.xml", "7223de34-5797-4f71-a4b8-27e33eb11300");
    private static final TestTask TASK_RECONCILIATION_SOURCE = new TestTask(
            TEST_DIR, "task-reconciliation-source.xml", "d76ba51f-3f62-4fed-8141-018a580aa1c4");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ROLE_TARGET, initTask, initResult);
        addObject(TEMPLATE_DELETE_DESCRIPTION, initTask, initResult);
        addObject(TEMPLATE_GLOBAL, initTask, initResult);

        TASK_RECONCILIATION_SOURCE.init(this, initTask, initResult);

        initAndTestDummyResource(RESOURCE_SOURCE, initTask, initResult);
        initAndTestDummyResource(RESOURCE_TARGET, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return FILE_SYSTEM_CONFIGURATION;
    }

    /**
     * There's a source account, linked to a user, resulting in role being assigned, and the target being provisioned.
     *
     * The source disappears. And the user is reconciled.
     *
     * The reconciliation should correctly remove the role and therefore the target account.
     *
     * See MID-7725.
     */
    @Test
    public void test100DiscoverDeletedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("jim source account exists");
        RESOURCE_SOURCE.controller.addAccount("jim");

        when("source is reconciled");
        TASK_RECONCILIATION_SOURCE.rerun(result);

        then("user and target account for jim exist");
        // @formatter:off
        UserType jim = assertUserByUsername("jim", "after 'import'")
                .display()
                .roleMembershipRefs()
                    .assertRole(ROLE_TARGET.oid)
                .end()
                .links()
                    .assertLinks(2, 0)
                    .by()
                        .resourceOid(RESOURCE_SOURCE.oid)
                        .find()
                        .resolveTarget()
                            .display()
                        .end()
                    .end()
                    .by()
                        .resourceOid(RESOURCE_TARGET.oid)
                        .find()
                        .resolveTarget()
                            .display()
                        .end()
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        when("source account is deleted");
        RESOURCE_SOURCE.controller.deleteAccount("jim");

        and("user is reconciled (on foreground)");
        reconcileUser(jim.getOid(), null, task, result);

        then("role and target account should be gone");
        // @formatter:off
        assertUserByUsername("jim", "after deletion of source and reconciliation")
                .display()
                .roleMembershipRefs()
                    .assertNoRole(ROLE_TARGET.oid)
                .end()
                .links()
                    .assertLinks(0, 1)
                .end();
        // @formatter:on
    }
}
