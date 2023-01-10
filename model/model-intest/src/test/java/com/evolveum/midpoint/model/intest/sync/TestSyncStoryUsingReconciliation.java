/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestSyncStoryUsingReconciliation extends AbstractSynchronizationStoryTest {

    @Override
    protected boolean isReconciliation() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // This should be the default but let's make sure ...
        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).setSyncStyle(DummySyncStyle.NONE);
        getDummyResource().setSyncStyle(DummySyncStyle.NONE);
        getDummyResource(RESOURCE_DUMMY_BLUE_NAME).setSyncStyle(DummySyncStyle.NONE);

        alwaysCheckTimestamp = true;

        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(100L);
        for (TestTask task : getTaskMap().values()) {
            task.init(this, initTask, initResult);
        }
    }

    @Override
    protected String getExpectedChannel() {
        return SchemaConstants.CHANNEL_RECON_URI;
    }

    @Override
    protected int getNumberOfExtraDummyUsers() {
        return 1;
    }
}
