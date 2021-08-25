/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryReconSimulateMultithreaded extends TestThresholdsStoryReconSimulate {

    private static final int WORKER_THREADS = 3;

    @Override
    protected int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
//        assertThat(getReconFailureCount(taskAfter)).isBetween(1, WORKER_THREADS);
//
//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        // user4, user5, user6, user7, user8
//        assertThat(syncInfo.getCountUnmatched()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        // jgibbs, hbarbossa, jbeckett, user1, user2, user3
//        assertEquals((Object) syncInfo.getCountLinked(), getDefaultUsers());
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
//
//        assertThat(syncInfo.getCountUnmatchedAfter()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
//        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountLinkedAfter(), getDefaultUsers());
//        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);
    }

    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
//        assertThat(getReconFailureCount(taskAfter)).isBetween(1, WORKER_THREADS);
//
//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        // user4, user5, user6, user7, user8
//        assertThat(syncInfo.getCountUnmatched()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        // jgibbs, hbarbossa, jbeckett, user1, user2, user3
//        assertEquals((Object) syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
//
//        assertThat(syncInfo.getCountUnmatchedAfter()).isBetween(RULE_CREATE_WATERMARK, RULE_CREATE_WATERMARK + WORKER_THREADS);
//        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);
    }

    @Override
    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
//        assertEquals(getReconFailureCount(taskAfter), 1);
//
//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        //user4
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        // jgibbs, hbarbossa, jbeckett, user1 (disabled-#1), user2 (disabled-#2), user3 (disabled-#3-fails)
//        assertEquals((Object) syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
//
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        // jgibbs, hbarbossa, jbeckett, user1 (disabled-#1), user2 (disabled-#2), user3 (disabled-#3-fails)
//        assertEquals((Object) syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);
    }
}
