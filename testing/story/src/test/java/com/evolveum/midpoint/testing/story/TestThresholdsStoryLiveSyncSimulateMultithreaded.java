/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryLiveSyncSimulateMultithreaded extends TestThresholdsStoryLiveSyncSimulate {

    private static final int WORKER_THREADS = 2;

    @Override
    protected int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        // We cannot do assumptions for specific success/failed object names because of multithreading.
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(4)
                        .assertFailureCount(1, WORKER_THREADS) // more workers can fail at once
                    .end();
    }

    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(4)
                        .assertFailureCount(1, WORKER_THREADS) // more workers can fail at once
                    .end();
    }

    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        // Cannot assert success # because users4-6 would succeed now (as they do not exist yet).
                        .assertFailureCount(1, WORKER_THREADS) // more workers can fail at once
                    .end();
    }
}
