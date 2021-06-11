/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks;

import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.task.work.BucketingManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;

public class AbstractRepoCommonTest extends AbstractIntegrationTest {

    private static final TestResource<UserType> USER_ADMINISTRATOR = new TestResource<>(COMMON_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");

    @Autowired protected TaskActivityManager activityManager;
    @Autowired protected BucketingManager bucketingManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_ADMINISTRATOR, initResult);
    }

}
