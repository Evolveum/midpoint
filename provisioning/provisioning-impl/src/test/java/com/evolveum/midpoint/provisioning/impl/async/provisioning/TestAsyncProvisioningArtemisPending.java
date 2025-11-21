/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Bug: MID-10870
 * Test case for async provisioning with propagated pending operations enabled.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestAsyncProvisioningArtemisPending extends TestAsyncProvisioningArtemisJms {

    protected static final File COMMON_DIR = new File("src/test/resources/common");

    private static final TestObject<UserType> USER_ADMINISTRATOR =
            TestObject.file(COMMON_DIR, "admin.xml", "00000000-0000-0000-0000-000000000002");

    private static final TestObject<RoleType> ROLE_SUPERUSER =
            TestObject.file(COMMON_DIR, "role-superuser.xml", "00000000-0000-0000-0000-000000000004");

    private static final TestObject<TaskType> TASK_PROPAGATION =
            TestObject.file(TEST_DIR, "task-propagation-multi.xml", "01db4542-f224-11e7-8833-bbe6634814e7");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        InternalsConfig.setSanityChecks(true);
        repoAdd(ROLE_SUPERUSER, initResult);
        repoAdd(USER_ADMINISTRATOR, initResult);
    }

    @Override
    protected void customizeResource(PrismObject<ResourceType> resource) {
        ResourceType r = resource.asObjectable();
        r.beginConsistency()
                .operationGroupingInterval(XmlTypeConverter.createDuration("PT2M"))
                .recordPendingOperations(RecordPendingOperationsType.ALL)
                .pendingOperationGracePeriod(XmlTypeConverter.createDuration("PT5M"))
                .end();
    }

    @Override
    protected TestObject<TaskType> getPropagationTask() {
        return TASK_PROPAGATION;
    }

    @Override
    protected String getClockForwardDurationForPropagation() {
        return "PT3M";
    }
}
