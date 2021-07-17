/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static com.evolveum.midpoint.report.AbstractReportIntegrationTest.*;

/**
 * Common superclass for "empty" report integration tests.
 *
 * VERY EXPERIMENTAL
 *
 * TODO reconsider
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmptyReportIntegrationTest extends AbstractModelIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);
    }

    // TODO deduplicate
    void commonInitialization(OperationResult initResult)
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

    void createUsers(int users, OperationResult initResult) throws CommonException {
        for (int i = 0; i < users; i++) {
            UserType user = new UserType(prismContext)
                    .name(String.format("u%06d", i));
            repositoryService.addObject(user.asPrismObject(), null, initResult);
        }
        System.out.printf("%d users created", users);
    }

}
