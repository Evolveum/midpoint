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
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStatsOrClone().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

//        assertEquals((Object) syncInfo.getCountUnmatched(), 5);
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        assertEquals((Object) syncInfo.getCountLinked(), 0);
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
//
//        assertEquals((Object) syncInfo.getCountUnmatchedAfter(), 1);     // this is the one that failed
//        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountLinkedAfter(), getProcessedUsers());
//        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);

    }

    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStatsOrClone().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

//        // It's actually not much clear how these numbers are obtained. The task processes various (yet unprocessed) changes
//        // and stops after seeing third disabled account.
//        assertEquals((Object) syncInfo.getCountUnmatched(), 3);
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        assertEquals((Object) syncInfo.getCountLinked(), 11); // TODO?
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
    }

    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStatsOrClone().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

//        assertEquals((Object) syncInfo.getCountUnmatched(), 5);
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        assertEquals((Object) syncInfo.getCountLinked(), 4);     // this is because LiveSync re-processes changes by default (FIXME)
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
//
//        assertEquals((Object) syncInfo.getCountUnmatchedAfter(), 1);     // this is the one that failed
//        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountLinkedAfter(), 8);    // this is because LiveSync re-processes changes by default (FIXME)
//        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);
    }
}
