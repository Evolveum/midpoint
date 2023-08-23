/*
 * Copyright (c) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.async;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentWithConstruction;

import java.io.File;
import jakarta.annotation.PreDestroy;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Complex testing of asynchronous provisioning and updating.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAsyncProvisioning extends AbstractStoryTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource RESOURCE_ASYNC_OUTBOUND = TestResource.file(
            TEST_DIR, "resource-async-outbound.xml", "f64a3dc5-3b5e-4bf9-9f46-ed01992984ef");
    private static final TestResource RESOURCE_ASYNC_INBOUND = TestResource.file(
            TEST_DIR, "resource-async-inbound.xml", "6628a329-4b29-4f3a-9339-8fa12c59c38f");

    private static final TestTask TASK_ASYNC_UPDATE = new TestTask(
            TEST_DIR, "task-async-update.xml", "2041e429-8ca9-4f80-a38f-1e3359627e39");
    private static final TestTask TASK_ASYNC_UPDATE_MULTI = new TestTask(
            TEST_DIR, "task-async-update-multi.xml", "c1f5a293-9fc9-4ab4-b497-de8605ee7dc6");
    private static final TestTask TASK_RECOMPUTE_MULTI = new TestTask(
            TEST_DIR, "task-recompute-multi.xml", "8b21b493-c85e-4a77-800f-a9063d1cfe8c");

    private EmbeddedActiveMQ embeddedBroker;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        startEmbeddedBroker();

        // We have to test the resource before async update task is started. See MID-7721.
        RESOURCE_ASYNC_OUTBOUND.initAndTest(this, initTask, initResult);
        RESOURCE_ASYNC_INBOUND.initAndTest(this, initTask, initResult);

        addObject(TASK_ASYNC_UPDATE, initTask, initResult);
    }

    /** We want minimalistic logging here. */
    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        stopEmbeddedBroker();
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100AddJim() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType jim = new UserType()
                .name("jim")
                .fullName("Jim Beam")
                .assignment(
                        createAssignmentWithConstruction(
                                RESOURCE_ASYNC_OUTBOUND.get(), ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT));

        when("object is added and the addition is processed by the async update task");
        addObject(jim, task, result);

        waitForTaskProgress(TASK_ASYNC_UPDATE.oid, 1, 30000, result);

        then("task should be (still) ready");
        assertTask(TASK_ASYNC_UPDATE.oid, "update task after")
                .display()
                // Checking for MID-7721 (though test connection is now done before starting the task, so this shouldn't fail)
                .assertSchedulingState(TaskSchedulingStateType.READY);

        and("'original' user should have the 'outbound' link");
        assertSuccess(result);
        assertUserAfter(jim.getOid())
                .assertAssignments(1)
                .assertLiveLinks(1);

        and("'mirrored' user should be created");
        assertUserAfterByUsername("_jim")
                .assertAssignments(0)
                .assertLiveLinks(1)
                .assertFullName("Jim Beam")
                .assertDescription("This is 'jim' called 'Jim Beam'");
    }

    @Test
    public void test110ModifyJim() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> jim = searchObjectByName(UserType.class, "jim", task, result);
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("Jim BEAM"))
                .asObjectDelta(jim.getOid());

        when();
        executeChanges(delta, null, task, result);

        waitForTaskProgress(TASK_ASYNC_UPDATE.oid, 2, 30000, result);

        then();
        assertSuccess(result);
        assertUserAfter(jim.getOid())
                .assertAssignments(1)
                .assertLiveLinks(1)
                .assertFullName("Jim BEAM");
        assertUserAfterByUsername("_jim")
                .assertAssignments(0)
                .assertLiveLinks(1)
                .assertFullName("Jim BEAM")
                .assertDescription("This is 'jim' called 'Jim BEAM'");
    }

    @Test
    public void test120DeleteJim() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> jim = searchObjectByName(UserType.class, "jim", task, result);

        when();
        deleteObject(UserType.class, jim.getOid(), task, result);

        waitForTaskProgress(TASK_ASYNC_UPDATE.oid, 3, 30000, result);

        then();
        assertSuccess(result);
        assertNoObjectByName(UserType.class, "jim", task, result);
        assertNoObjectByName(UserType.class, "_jim", task, result);
    }

    @Test
    public void test200AddManyUsers() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersAtStart = countUsers(result);

        int users = 30;
        for (int i = 0; i < users; i++) {
            UserType user = new UserType()
                    .name(String.format("user-%06d", i))
                    .fullName(String.format("User %06d", i))
                    .assignment(
                            createAssignmentWithConstruction(
                                    RESOURCE_ASYNC_OUTBOUND.get(), ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT));
            repositoryService.addObject(user.asPrismObject(), null, result);
        }

        suspendTask(TASK_ASYNC_UPDATE.oid);

        when();
        addObject(TASK_RECOMPUTE_MULTI, task, result);
        addObject(TASK_ASYNC_UPDATE_MULTI, task, result);

        waitForTaskCloseOrSuspend(TASK_RECOMPUTE_MULTI.oid, 1800000);
        waitForTaskProgress(
                TASK_ASYNC_UPDATE_MULTI.oid,
                users, () -> countUsers(result) - usersAtStart >= users*2,
                1800000, 5000, result);

        then();
        assertTask(TASK_RECOMPUTE_MULTI.oid, "after")
                .display()
                .rootItemProcessingInformation()
                    .display();
        assertTask(TASK_ASYNC_UPDATE_MULTI.oid, "after")
                .display()
                .rootItemProcessingInformation()
                    .display();
        assertTask(TASK_ASYNC_UPDATE.oid, "after")
                .display()
                .rootItemProcessingInformation()
                    .display();
    }

    private int countUsers(OperationResult result) throws com.evolveum.midpoint.util.exception.SchemaException {
        return repositoryService.countObjects(UserType.class, null, null, result);
    }

    private void startEmbeddedBroker() throws Exception {
        embeddedBroker = new EmbeddedActiveMQ();
        embeddedBroker.start();
    }

    private void stopEmbeddedBroker() throws Exception {
        embeddedBroker.stop();
    }
}
