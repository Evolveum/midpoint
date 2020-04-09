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
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test with a resource that can only write. It cannot read the accounts. It has to cache the values.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestWriter extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "writer");

    protected static final File RESOURCE_WRITER_FILE = new File(TEST_DIR, "resource-writer.xml");
    protected static final String RESOURCE_WRITER_OID = "607c4616-1a66-11ea-b513-ef360fa00fe1";
    protected static final String RESOURCE_WRITER_DUMMY_NAME = "writer";

    private static final String USER_JACK_FULL_NAME_CAPTAIN = "Captain Jack Sparrow";
    private static final String USER_JACK_LOCALITY = "Seven seas";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyResourceContoller writerDummyController = DummyResourceContoller.create(RESOURCE_WRITER_DUMMY_NAME);
        writerDummyController.extendSchemaPirate();
        dummyResourceCollection.initDummyResource(RESOURCE_WRITER_DUMMY_NAME, writerDummyController);

        PrismObject<ResourceType> resourceWriter = importAndGetObjectFromFile(ResourceType.class, RESOURCE_WRITER_FILE, RESOURCE_WRITER_OID, initTask, initResult);
        writerDummyController.setResource(resourceWriter);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultWriter = modelService.testResource(RESOURCE_WRITER_OID, task);
        TestUtil.assertSuccess(testResultWriter);
    }

    @Test
    public void test100AssignJackDummyAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, createPolyString(USER_JACK_LOCALITY));

        // WHEN
        when();

        assignAccountToUser(USER_JACK_OID, RESOURCE_WRITER_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccountByUsername(RESOURCE_WRITER_DUMMY_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME);
    }

    /**
     * MID-5944
     */
    @Test
    public void test110ModifyCaptainJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, createPolyString(USER_JACK_FULL_NAME_CAPTAIN));

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccountByUsername(RESOURCE_WRITER_DUMMY_NAME, USER_JACK_USERNAME)
                .assertFullName(USER_JACK_FULL_NAME_CAPTAIN);
    }
}
