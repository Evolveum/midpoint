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

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryLiveSyncExecuteMultithreaded extends TestThresholdsStoryLiveSyncExecute {

    @Override
    protected int getWorkerThreads() {
        return 2;
    }

    /**
     * We imported user4 to user9.
     *
     * LS events received are:
     *
     * - add user4
     * - add user5
     * - add user6
     * - add user7
     * - add user8
     * - add user9
     *
     * Four of them should succeed, 1 or 2 of the other(s) should fail. The rest should not be processed.
     */
    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(4)
                        .assertFailureCount(1, 2)
                    .end();

        assertSyncToken(taskAfter, 3);
    }

    /**
     * We imported user10 to user15.
     *
     * However, the token is still 3. So LS events received are:
     *
     * - add user4
     * - add user5
     * - add user6
     * - add user7
     * - add user8
     * - add user9
     * - add user10
     * - add user11
     * - add user12
     * - add user13
     * - add user14
     * - add user15
     *
     * Some should succeed (some number from the first run, four of this one).
     * 1 or 2 of the other(s) should fail.
     * The rest should not be processed.
     */
    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(4, 8)
                        .assertFailureCount(1, 2)
                    .end();

        assertSyncToken(taskAfter, 3);
    }

    /**
     * Now we disabled users 1 to 6. Note that previous changes were additions of user4 to user15.
     *
     * We start with token of 3, because the token was not updated during previous runs.
     *
     * It looks like OpenDJ provides the following sync events:
     *
     * - add user4
     * - add user5
     * - add user6
     * - add user7
     * - add user8
     * - add user9
     * - add user10
     * - add user11
     * - add user12
     * - add user13
     * - add user14
     * - add user15
     * - disable user1
     * - disable user2
     * - disable user3
     * - disable user4
     * - disable user5
     * - disable user6
     *
     * All "add user" events that we get, will be processed. It is not sure if we get all of them before failing
     * on 3rd "disable" event.
     */
    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertFailureCount(1, 2)
                    .end();

        assertSyncToken(taskAfter, 3);
    }
}
