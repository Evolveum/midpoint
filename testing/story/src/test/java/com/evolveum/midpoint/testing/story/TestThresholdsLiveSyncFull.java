/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;

import java.io.File;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsLiveSyncFull extends TestThresholds {

    private static final File TASK_LIVESYNC_OPENDJ_FULL_FILE = new File(TEST_DIR, "task-opendj-livesync-full.xml");
    private static final String TASK_LIVESYNC_OPENDJ_FULL_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    @Override
    protected File getTaskFile() {
        return TASK_LIVESYNC_OPENDJ_FULL_FILE;
    }

    @Override
    protected String getTaskOid() {
        return TASK_LIVESYNC_OPENDJ_FULL_OID;
    }

    @Override
    protected int getWorkerThreads() {
        return 0;
    }

    @Override
    protected int getProcessedUsers() {
        return 4;
    }

    @Override
    protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), 0);
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 1);     // this is the one that failed
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);

    }

    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        // It's actually not much clear how these numbers are obtained. The task processes various (yet unprocessed) changes
        // and stops after seeing third disabled account.
        assertEquals(syncInfo.getCountUnmatched(), 3);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), 9);
        assertEquals(syncInfo.getCountUnlinked(), 0);
    }

    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), 4);     // this is because LiveSync re-processes changes by default (FIXME)
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 1);     // this is the one that failed
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), 8);    // this is because LiveSync re-processes changes by default (FIXME)
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
    }
}
