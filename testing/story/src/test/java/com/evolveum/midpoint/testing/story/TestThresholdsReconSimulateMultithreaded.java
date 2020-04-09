/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;

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
public class TestThresholdsReconSimulateMultithreaded extends TestThresholds {

    private static final File TASK_RECONCILE_OPENDJ_SIMULATE_FILE = new File(TEST_DIR, "task-opendj-reconcile-simulate-multithreaded.xml");
    private static final String TASK_RECONCILE_OPENDJ_SIMULATE_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    private static final int WORKER_THREADS = 3;

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
    protected int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        display(IterativeTaskInformation.format(infoType));
        assertThat(infoType.getTotalFailureCount()).isBetween(1, WORKER_THREADS);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        // user4, user5, user6, user7, user8
        assertThat(syncInfo.getCountUnmatched()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
        assertEquals(syncInfo.getCountDeleted(), 0);
        // jgibbs, hbarbossa, jbeckett, user1, user2, user3
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertThat(syncInfo.getCountUnmatchedAfter()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers());
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
    }

    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        display(IterativeTaskInformation.format(infoType));
        assertThat(infoType.getTotalFailureCount()).isBetween(1, WORKER_THREADS);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        // user4, user5, user6, user7, user8
        assertThat(syncInfo.getCountUnmatched()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
        assertEquals(syncInfo.getCountDeleted(), 0);
        // jgibbs, hbarbossa, jbeckett, user1, user2, user3
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertThat(syncInfo.getCountUnmatchedAfter()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
        assertEquals(syncInfo.getCountDeletedAfter(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
    }

    @Override
    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(syncInfo);

        //user4
        assertEquals(syncInfo.getCountDeleted(), 0);
        // jgibbs, hbarbossa, jbeckett, user1 (disabled-#1), user2 (disabled-#2), user3 (disabled-#3-fails)
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountDeleted(), 0);
        // jgibbs, hbarbossa, jbeckett, user1 (disabled-#1), user2 (disabled-#2), user3 (disabled-#3-fails)
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);
    }
}
