/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.async;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests async updates using Grouper JSON messages.
 * <p>
 * Currently uses caching.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAsyncUpdateGrouperJson extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async/grouper-amqp091");

    private static final File RESOURCE_GROUPER_FILE = new File(TEST_DIR, "resource-grouper-amqp091.xml");
    protected static final String RESOURCE_GROUPER_ID = "Grouper";
    private static final String RESOURCE_GROUPER_OID = "bbb9900a-b53d-4453-b60b-908725e3950e";

    private static final String BANDERSON_USERNAME = "banderson";
    private static final String JLEWIS685_USERNAME = "jlewis685";
    private static final String ALUMNI_NAME = "ref:alumni";
    private static final String STAFF_NAME = "ref:staff";

    public static final String GROUPER_USER_INTENT = "subject";
    public static final String GROUPER_GROUP_INTENT = "group";

    private static final TestObject<TaskType> TASK_ASYNC_UPDATE = TestObject.file(TEST_DIR, "task-async-update.xml", "3a4d7734-1082-4290-812b-37b6fc7f7f47");

    private static final File CHANGE_100 = new File(TEST_DIR, "change-100-banderson-add-supergroup.json");
    private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.json");
    private static final File CHANGE_110_A = new File(TEST_DIR, "change-110a-staff-add.json");
    private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni.json");
    private static final File CHANGE_210 = new File(TEST_DIR, "change-210-banderson-add-staff.json");
    private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.json");
    private static final File CHANGE_230 = new File(TEST_DIR, "change-230-jlewis685-add-supergroup.json");
    private static final File CHANGE_240 = new File(TEST_DIR, "change-240-banderson-add-staff.json");
    private static final File CHANGE_250 = new File(TEST_DIR, "change-250-banderson-delete-alumni.json");
    private static final File CHANGE_310 = new File(TEST_DIR, "change-310-staff-delete.json");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER_FILE, RESOURCE_GROUPER_OID, initTask, initResult);

        addObject(TASK_ASYNC_UPDATE, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultGrouper = modelService.testResource(RESOURCE_GROUPER_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultGrouper);
    }

    /**
     * The first MEMBERSHIP_ADD event for banderson (supergroup)
     */
    @Test
    public void test100AddAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_100));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                //                        .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits();
    }

    private AsyncUpdateMessageType getAmqp091Message(File file) throws IOException {
        Amqp091MessageType rv = new Amqp091MessageType();
        String json = String.join("\n", IOUtils.readLines(new FileReader(file)));
        rv.setBody(json.getBytes(StandardCharsets.UTF_8));
        return rv;
    }

    /**
     * GROUP_ADD event for ref:alumni and ref:staff.
     */
    @Test
    public void test110AddAlumniAndStaff() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_110));
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_110_A));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertOrgByName(ALUMNI_NAME, "after")
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display()
                .assertKind(ShadowKindType.ENTITLEMENT)
//                        .assertIntent(GROUPER_GROUP_INTENT)
                .assertResource(RESOURCE_GROUPER_OID);

        assertOrgByName(STAFF_NAME, "after")
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display()
                .assertKind(ShadowKindType.ENTITLEMENT)
//                        .assertIntent(GROUPER_GROUP_INTENT)
                .assertResource(RESOURCE_GROUPER_OID);
    }

    /**
     * Adding ref:alumni membership for banderson.
     */
    @Test
    public void test200AddAlumniForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_200));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .display("shadow after")
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Adding ref:staff membership for banderson.
     */
    @Test
    public void test210AddStaffForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_210));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display("shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME, STAFF_NAME);
    }

    /**
     * Adding ref:alumni membership for jlewis685. But this is the first occurrence of jlewis685!
     */
    @Test
    public void test220AddAlumniForLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_220));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display("shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Adding supergroup to jlewis (notification-only change). Should be idempotent.
     */
    @Test
    public void test230AddLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_230));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display("shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Adding ref:staff membership for banderson (again). Should be idempotent.
     */
    @Test
    public void test240AddStaffForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_240));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display("shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME, STAFF_NAME);
    }

    /**
     * Deleting ref:alumni membership for banderson.
     */
    @Test
    public void test250DeleteAlumniForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_250));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                .singleAny()
                .resolveTarget()
                .display("shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                //                            .assertIntent(GROUPER_USER_INTENT)
                .assertResource(RESOURCE_GROUPER_OID)
                .end()
                .end()
                .end()
                .assertOrganizationalUnits(STAFF_NAME);
    }

    /**
     * Deleting etc:staff.
     */
    @Test
    public void test310DeleteStaff() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        assertObjectByName(OrgType.class, STAFF_NAME, task, result);

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_310));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoObjectByName(OrgType.class, STAFF_NAME, task, result);
    }
}
