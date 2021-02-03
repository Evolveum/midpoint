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
public class TestThresholdsLiveSyncSimulate extends TestThresholds {

    private static final File TASK_LIVESYNC_OPENDJ_SIMULATE_FILE = new File(TEST_DIR, "task-opendj-livesync-simulate.xml");
    private static final String TASK_LIVESYNC_OPENDJ_SIMULATE_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    @Override
    protected File getTaskFile() {
        return TASK_LIVESYNC_OPENDJ_SIMULATE_FILE;
    }

    @Override
    protected String getTaskOid() {
        return TASK_LIVESYNC_OPENDJ_SIMULATE_OID;
    }

    @Override
    protected int getWorkerThreads() {
        return 0;
    }

    @Override
    protected int getProcessedUsers() {
        return 0;
    }

    @Override
    protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

        // user5, user6, user7, user8, user9 (why not user4? -- because token is preset to 4)
        assertEquals((Object) syncInfo.getCountUnmatchedAfter(), 5);
        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
        assertEquals((Object) syncInfo.getCountLinkedAfter(), 0);
        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);

    }

    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        // new users: user5, user6, user7, user8, user9, user10, user11, user12, user13, user14, user15 (11 users)
        assertEquals((Object) syncInfo.getCountUnmatched(), 11);
        assertEquals((Object) syncInfo.getCountDeleted(), 0);
        // existing users: user1, user2 (disabled - passes), user3 (disabled - fails) -- these users were created during initial import
        assertEquals((Object) syncInfo.getCountLinked(), 3);             // 2 + 1
        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.testing.story.TestThresholds#assertSynchronizationStatisticsAfterSecondImport(com.evolveum.midpoint.task.api.Task)
     */
    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        assertSyncToken(taskAfter, 4);

        // user5, user6, user7, user8, user9
        assertEquals((Object) syncInfo.getCountUnmatchedAfter(), 5);
        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
        assertEquals((Object) syncInfo.getCountLinkedAfter(), 0);
        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);
    }
}
