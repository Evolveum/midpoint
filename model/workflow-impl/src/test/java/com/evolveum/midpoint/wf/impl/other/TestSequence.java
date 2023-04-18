/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Test for using sequences in objects being approved.
 *
 * See MID-7575.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSequence extends AbstractWfTestPolicy {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "sequence");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<SequenceType> SEQUENCE_USER_NAME =
            new TestResource<>(TEST_DIR, "sequence-user-name.xml", "84f9d763-bfd9-4edb-9f5b-a49158580e16");

    private static final TestResource<ObjectTemplateType> TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "template-user.xml", "5e156e0f-5844-44d3-a7f7-78df11e3c98a");

    private String rootCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(SEQUENCE_USER_NAME, initTask, initResult);
        addObject(TEMPLATE_USER, initTask, initResult);
        setDefaultUserTemplate(TEMPLATE_USER.oid);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Request a user be created. It should be named 100000.
     */
    @Test
    public void test100RequestUserCreation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(USER_ADMINISTRATOR_USERNAME);

        when("user creation is requested");
        UserType user = new UserType()
                .fullName("Joe Black");

        addObject(user, task, result);

        then("an approval case should be created");

        // @formatter:off
        CaseType rootCase = assertCase(result, "after")
                .display()
                .displayXml()
                .subcases()
                    .single()
                        .display()
                    .end()
                .end()
                .getObjectable();
        // @formatter:on

        rootCaseOid = rootCase.getOid();

        and("the user to be created should have a name of 100000 (start of sequence)");
        UserType userToCreate = (UserType) ObjectTypeUtil.getObjectFromReference(rootCase.getObjectRef());
        assertUser(userToCreate, "user to create")
                .display()
                .assertName("100000")
                .assertFullName("Joe Black");

        assertNoObjectByName(UserType.class, "100000", task, result);
    }

    /**
     * Approve user creation. The created user should have the same name as the one that was being approved (100000).
     */
    @Test
    public void test110ApproveUserCreation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("creation is approved");
        CaseWorkItemType workItem = getWorkItem(task, result);
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(),
                task,
                result);

        then("user is created with the same name");
        waitForCaseClose(getCase(rootCaseOid), 20000);

        assertUserAfterByUsername("100000")
                .assertFullName("Joe Black");
    }
}
