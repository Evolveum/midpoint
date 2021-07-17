/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static com.evolveum.midpoint.report.AbstractReportIntegrationTest.*;

import java.io.File;
import java.io.IOException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportMultiNode extends AbstractModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reports");

    private static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_USERS = new TestResource<>(TEST_DIR,
            "report-object-collection-users.xml", "64e13165-21e5-419a-8d8b-732895109f84");
    private static final TestResource<TaskType> TASK_EXPORT_USERS_MULTINODE = new TestResource<>(TEST_DIR,
            "task-export-users-multinode.xml", "5ab8f8c6-df1a-4580-af8b-a899f240b44f");

    private static final int USERS = 1000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);

        repoAdd(REPORT_OBJECT_COLLECTION_USERS, initResult);

        createUsers(initResult);
    }

    private void createUsers(OperationResult initResult) throws CommonException {
        for (int i = 0; i < USERS; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format("u%06d", i));
            repositoryService.addObject(user.asPrismObject(), null, initResult);
        }
        System.out.printf("%d users created", USERS);
    }

    // TODO deduplicate
    private void commonInitialization(OperationResult initResult)
            throws CommonException, EncryptionException, IOException {
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);

        modelService.postInit(initResult);
        try {
            repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);
    }

    @Test
    public void test100ExportUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        addTask(TASK_EXPORT_USERS_MULTINODE, result);

        when();

        waitForTaskCloseOrSuspend(TASK_EXPORT_USERS_MULTINODE.oid);

        then();

        assertTask(TASK_EXPORT_USERS_MULTINODE.oid, "after")
                .assertSuccess()
                .display();

        // TODO assert the resulting file
    }
}
