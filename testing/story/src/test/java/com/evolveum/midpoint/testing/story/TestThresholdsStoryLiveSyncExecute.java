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

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryLiveSyncExecute extends TestThresholdsStoryLiveSync {

    private static final TestObject<TaskType> TASK_LIVESYNC_OPENDJ_FULL = TestObject.file(TEST_DIR, "task-opendj-livesync-full.xml", "95bd6a35-ab67-46c9-bda6-51f5bfb070da");

    @Override
    protected TestObject<TaskType> getTaskTestResource() {
        return TASK_LIVESYNC_OPENDJ_FULL;
    }

    @Override
    protected int getWorkerThreads() {
        return 0;
    }

    /**
     * We imported user4 to user9.
     *
     * LS events received are:
     *
     * - add user4 - creates a user (succeeds)
     * - add user5 - creates a user (succeeds)
     * - add user6 - creates a user (succeeds)
     * - add user7 - creates a user (succeeds)
     * - add user8 - cannot create a user because of a threshold of 5 (fails)
     * - add user9
     */
    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(4, 1, 0)
                        .assertLastSuccessObjectName("uid=user7,ou=people,dc=example,dc=com")
                        .assertLastFailureObjectName("uid=user8,ou=people,dc=example,dc=com")
                    .end()
                    .synchronizationStatistics()
                        .assertTransition(null, UNMATCHED, LINKED, null, 4, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 0, 1, 0)
                        .assertTransitions(2);

        assertSyncToken(taskAfter, 3);
    }

    /**
     * We imported user10 to user15.
     *
     * However, the token is still 3. So LS events received are:
     *
     * - add user4 - noop (succeeds)
     * - add user5 - noop (succeeds)
     * - add user6 - noop (succeeds)
     * - add user7 - noop (succeeds)
     * - add user8 - creates a user (succeeds)
     * - add user9 - creates a user (succeeds)
     * - add user10 - creates a user (succeeds)
     * - add user11 - creates a user (succeeds)
     * - add user12 - cannot create a user because of a threshold of 5 (fails)
     * - add user13
     * - add user14
     * - add user15
     */
    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(8, 1, 0) // 4 users re-processed because of token not updated by default, 4+1 users new
                        .assertLastSuccessObjectName("uid=user11,ou=people,dc=example,dc=com")
                        .assertLastFailureObjectName("uid=user12,ou=people,dc=example,dc=com")
                    .end()
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0) // users created in the first run (re-processed because of token not updated by default)
                        .assertTransition(UNMATCHED, UNMATCHED, LINKED, null, 1, 0, 0) // user failing in the first run
                        .assertTransition(null, UNMATCHED, LINKED, null, 3, 0, 0) // 3 users succeeding in the current run
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 0, 1, 0) // "threshold" user failing in the current run
                        .assertTransitions(4);

        assertSyncToken(taskAfter, 3);
    }

    /**
     * Now we disabled users 1 to 6. Note that previous changes were additions of user4 to user15.
     *
     * We start with token of 3, because the token was not updated during previous runs.
     *
     * It looks like OpenDJ provides the following sync events:
     *
     * - add user4 - noop (succeeds)
     * - add user5 - noop (succeeds)
     * - add user6 - noop (succeeds)
     * - add user7 - noop (succeeds)
     * - add user8 - noop (succeeds)
     * - add user9 - noop (succeeds)
     * - add user10 - noop (succeeds)
     * - add user11 - noop (succeeds)
     * - add user12 - creates a user (succeeds); rule is no longer there
     * - add user13 - creates a user (succeeds); rule is no longer there
     * - add user14 - creates a user (succeeds); rule is no longer there
     * - add user15 - creates a user (succeeds); rule is no longer there
     * - disable user1 - updates a user (succeeds)
     * - disable user2 - updates a user (succeeds)
     * - disable user3 - fails to update a user because of a threshold
     * - disable user4
     * - disable user5
     * - disable user6
     */
    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(14, 1) // see above
                        .assertLastSuccessObjectName("uid=user2,ou=People,dc=example,dc=com")
                        .assertLastFailureObjectName("uid=user3,ou=People,dc=example,dc=com")
                    .end()
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, 8+2, 1, 0) // failing is user3
                        .assertTransition(UNMATCHED, UNMATCHED, LINKED, null, 1, 0, 0) // user12
                        .assertTransition(null, UNMATCHED, LINKED, null, 3, 0, 0) // user13..15
                        .assertTransitions(3);

        assertSyncToken(taskAfter, 3);
    }
}
