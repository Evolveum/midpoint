/*
 * Copyright (c) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.async;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentWithConstruction;

import java.io.File;
import javax.annotation.PreDestroy;

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
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Complex testing of asynchronous provisioning and updating.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AsynchronousProvisioningTest extends AbstractStoryTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ResourceType> RESOURCE_ASYNC_OUTBOUND = new TestResource<>(TEST_DIR, "resource-async-outbound.xml", "f64a3dc5-3b5e-4bf9-9f46-ed01992984ef");
    private static final TestResource<ResourceType> RESOURCE_ASYNC_INBOUND = new TestResource<>(TEST_DIR, "resource-async-inbound.xml", "6628a329-4b29-4f3a-9339-8fa12c59c38f");

    private static final TestResource<TaskType> TASK_ASYNC_UPDATE = new TestResource<>(TEST_DIR, "task-async-update.xml", "2041e429-8ca9-4f80-a38f-1e3359627e39");

    protected EmbeddedActiveMQ embeddedBroker;

    private PrismObject<ResourceType> resourceOutbound;
    private PrismObject<ResourceType> resourceInbound;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        startEmbeddedBroker();

        resourceOutbound = importAndGetObjectFromFile(ResourceType.class, RESOURCE_ASYNC_OUTBOUND.file, RESOURCE_ASYNC_OUTBOUND.oid, initTask, initResult);
        resourceInbound = importAndGetObjectFromFile(ResourceType.class, RESOURCE_ASYNC_INBOUND.file, RESOURCE_ASYNC_INBOUND.oid, initTask, initResult);

        addObject(TASK_ASYNC_UPDATE, initTask, initResult);
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
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        assertSuccess(modelService.testResource(RESOURCE_ASYNC_OUTBOUND.oid, task));
        assertSuccess(modelService.testResource(RESOURCE_ASYNC_INBOUND.oid, task));
    }

    @Test
    public void test100AddJim() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType jim = new UserType(prismContext)
                .name("jim")
                .fullName("Jim Beam")
                .assignment(
                        createAssignmentWithConstruction(
                                resourceOutbound, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, prismContext));

        when();
        addObject(jim.asPrismObject(), task, result);

        waitForTaskProgress(TASK_ASYNC_UPDATE.oid, 1, 30000, result);

        then();
        assertSuccess(result);
        assertUserAfter(jim.getOid())
                .assertLinks(2)
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
                .assertLinks(2)
                .assertDescription("This is 'jim' called 'Jim BEAM'");
    }

    @Test
    public void test120DeleteJimOutbound() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> jim = searchObjectByName(UserType.class, "jim", task, result);
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).replace()
                .asObjectDelta(jim.getOid());

        when();
        executeChanges(delta, null, task, result);

        waitForTaskProgress(TASK_ASYNC_UPDATE.oid, 3, 30000, result);

        then();
        assertSuccess(result);
        assertUserAfter(jim.getOid())
                .assertAssignments(0)
                .assertLinks(0);
    }

    private void startEmbeddedBroker() throws Exception {
        embeddedBroker = new EmbeddedActiveMQ();
        embeddedBroker.start();
    }

    private void stopEmbeddedBroker() throws Exception {
        embeddedBroker.stop();
    }
}
