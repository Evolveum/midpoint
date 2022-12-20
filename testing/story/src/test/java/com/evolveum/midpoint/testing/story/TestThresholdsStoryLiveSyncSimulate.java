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

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryLiveSyncSimulate extends TestThresholdsStoryLiveSync {

    private static final TestResource<TaskType> TASK_LIVESYNC_OPENDJ_SIMULATE = new TestResource<>(
            TEST_DIR, "task-opendj-livesync-simulate.xml", "02e134e0-a740-4730-be6d-6521e63198e7");

    @Override
    protected TestResource<TaskType> getTaskTestResource() {
        return TASK_LIVESYNC_OPENDJ_SIMULATE;
    }

    @Override
    protected int getWorkerThreads() {
        return 0;
    }

    @Override
    protected boolean isPreview() {
        return true;
    }

    /**
     * We imported user4 to user9.
     *
     * LS events received are:
     *
     * - add user4 - would create a user (succeeds)
     * - add user5 - would create a user (succeeds)
     * - add user6 - would create a user (succeeds)
     * - add user7 - would create a user (succeeds)
     * - add user8 - couldn't pretend to create a user because of a threshold of 5 (fails)
     * - add user9
     */
    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        // @formatter:off
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(4, 1, 0)
                        .assertLastSuccessObjectName("uid=user7,ou=people,dc=example,dc=com")
                        .assertLastFailureObjectName("uid=user8,ou=people,dc=example,dc=com")
                    .end()
                    .synchronizationStatistics()
                        // the transition is recorded as to LINKED because we know it's simulated
                        // TODO decide about how it should be
                        .assertTransition(null, UNMATCHED, LINKED, null, 4, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 0, 1, 0)
                        .assertTransitions(2);
        // @formatter:on
    }

    /**
     * We imported user10 to user15.
     *
     * However, the token is still 3. So LS events received are:
     *
     * - add user4 - would create a user (succeeds)
     * - add user5 - would create a user (succeeds)
     * - add user6 - would create a user (succeeds)
     * - add user7 - would create a user (succeeds)
     * - add user8 - couldn't pretend to create a user because of a threshold of 5 (fails)
     * - add user9
     * - add user10
     * - add user11
     * - add user12
     * - add user13
     * - add user14
     * - add user15
     */

    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        // @formatter:off
        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertTotalCounts(4, 1, 0)
                        .assertLastSuccessObjectName("uid=user7,ou=people,dc=example,dc=com")
                        .assertLastFailureObjectName("uid=user8,ou=people,dc=example,dc=com")
                    .end()
                    .synchronizationStatistics()
                        // See the above method
                        .assertTransition(null, UNMATCHED, LINKED, null, 4, 0, 0)
                        .assertTransition(null, UNMATCHED, UNMATCHED, null, 0, 1, 0)
                        .assertTransitions(2);
        // @formatter:on
    }

    /**
     * Now we disabled users 1 to 6. Note that previous changes were additions of user4 to user15.
     *
     * We start with token of 3, because the token was not updated during previous runs.
     *
     * It looks like OpenDJ provides the following sync events:
     *
     * - add user4 - would create a user because the rule is no longer there (succeeds)
     * - add user5 - would create a user because the rule is no longer there (succeeds)
     * - add user6 - would create a user because the rule is no longer there (succeeds)
     * - add user7 - would create a user because the rule is no longer there (succeeds)
     * - add user8 - would create a user because the rule is no longer there (succeeds)
     * - add user9 - would create a user because the rule is no longer there (succeeds)
     * - add user10 - would create a user because the rule is no longer there (succeeds)
     * - add user11 - would create a user because the rule is no longer there (succeeds)
     * - add user12 - would create a user because the rule is no longer there (succeeds)
     * - add user13 - would create a user because the rule is no longer there (succeeds)
     * - add user14 - would create a user because the rule is no longer there (succeeds)
     * - add user15 - would create a user because the rule is no longer there (succeeds)
     * - disable user1 - would update a user (succeeds)
     * - disable user2 - would update a user (succeeds)
     * - disable user3 - couldn't pretend to update a user because of a threshold
     * - disable user4
     * - disable user5
     * - disable user6
     */
    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
        assertSyncToken(taskAfter, 3);

        assertTask(taskAfter, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                    .assertTotalCounts(14, 1) // see above
                    .assertLastSuccessObjectName("uid=user2,ou=People,dc=example,dc=com")
                    .assertLastFailureObjectName("uid=user3,ou=People,dc=example,dc=com")
                .end();
    }
}
