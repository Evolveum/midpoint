/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsReconSimulate extends TestThresholds {

    private static final File TASK_RECONCILE_OPENDJ_SIMULATE_FILE = new File(TEST_DIR, "task-opendj-reconcile-simulate.xml");
    private static final String TASK_RECONCILE_OPENDJ_SIMULATE_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    @Override
    protected File getTaskFile() {
        return TASK_RECONCILE_OPENDJ_SIMULATE_FILE;
    }

    @Override
    protected String getTaskOid() {
        return TASK_RECONCILE_OPENDJ_SIMULATE_OID;
    }

    @Override
    protected int getProcessedUsers() {
        return 0;
    }

    @Override
    protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), 0);
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.testing.story.TestThresholds#assertSynchronizationStatisticsAfterSecondImport(com.evolveum.midpoint.task.api.Task)
     */
    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), 0);
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
    }

    @Override
    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 3);
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);

        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatchedAfter(), 0);
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
    }
}
