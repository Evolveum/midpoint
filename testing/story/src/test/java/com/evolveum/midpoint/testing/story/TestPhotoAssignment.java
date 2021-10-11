/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPhotoAssignment extends AbstractStoryTest {

    private static final File TEST_DIR = new File("src/test/resources/photo");

    private static final TestResource ARCHETYPE_ID_CARD = new TestResource(TEST_DIR, "archetype-id-card.xml", "fe13a7f8-8b3b-4094-8417-1743e78a0acd");
    private static final TestResource SERVICE_CARD_10001 = new TestResource(TEST_DIR, "service-card-10001.xml", "1d936f27-17de-406e-b2e0-1a6069e801de");
    private static final TestResource USER_JOE = new TestResource(TEST_DIR, "user-joe.xml", "c094f5de-9a32-4d24-baf1-0e6db7fbb28a");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ARCHETYPE_ID_CARD, initTask, initResult);
        addObject(SERVICE_CARD_10001, initTask, initResult);
        addObject(USER_JOE, initTask, initResult);

        //setGlobalTracingOverride(createModelLoggingTracingProfile());
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // don't need these
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Test
    public void test100AssignJoeCard() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assignService(USER_JOE.oid, SERVICE_CARD_10001.oid, task, result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, USER_JOE.oid,
                schemaHelper.getOperationOptionsBuilder().retrieve().build(), result);
        new UserAsserter<>(userAfter)
                .display()
                .assertJpegPhoto();
    }
}
