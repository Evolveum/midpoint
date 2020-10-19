/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.FileNotFoundException;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTask extends AbstractSynchronizationStoryTest {

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

        allwaysCheckTimestamp = true;
    }

    @Override
    protected String getExpectedChannel() {
        return SchemaConstants.CHANNEL_RECON_URI;
    }

    @Override
    protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_GREEN_FILENAME);
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_BLUE_FILENAME);
        } else if (resource == getDummyResourceObject()) {
            importObjectFromFile(TASK_RECONCILE_DUMMY_FILENAME);
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }

    @Override
    protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
        if (resource == getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME)) {
            return TASK_RECONCILE_DUMMY_GREEN_OID;
        } else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
            return TASK_RECONCILE_DUMMY_BLUE_OID;
        } else if (resource == getDummyResourceObject()) {
            return TASK_RECONCILE_DUMMY_OID;
        } else {
            throw new IllegalArgumentException("Unknown resource "+resource);
        }
    }

    protected int getWaitTimeout() {
        return 70000;
    }

    @Override
    protected int getNumberOfExtraDummyUsers() {
        return 1;
    }



}
