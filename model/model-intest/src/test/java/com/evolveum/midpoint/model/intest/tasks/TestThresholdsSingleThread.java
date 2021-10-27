/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.test.asserter.ActivityStateAsserter;
import com.evolveum.midpoint.test.asserter.TaskAsserter;

public class TestThresholdsSingleThread extends TestThresholdsSingleTask {

    @Override
    int getWorkerThreads() {
        return 0;
    }

    @Override
    void additionalTest100TaskAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
        asserter
                .progress()
                    .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                .end()
                .itemProcessingStatistics()
                    .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
                .end();
    }

    @Override
    void additionalTest100RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
        asserter
                .previewModePolicyRulesCounters()
                    .assertCounter(ruleAddId, USER_ADD_ALLOWED + 2)
                .end()
                .progress()
                    .assertUncommitted(0, 1, 0) // fails immediately because of persistent counters
                .end()
                .itemProcessingStatistics()
                    .assertTotalCounts(USER_ADD_ALLOWED, 2, 0)
                .end();
    }

    @Override
    void additionalTest200Asserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
        asserter
                .itemProcessingStatistics()
                    .assertTotalCounts(USER_MODIFY_ALLOWED*4, 1, 0)
                .end();
    }

    @Override
    void additionalTest200RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
        asserter
                .previewModePolicyRulesCounters()
                    .assertCounter(ruleModifyCostCenterId, USER_MODIFY_ALLOWED + 2)
                .end()
                .itemProcessingStatistics()
                    .assertTotalCounts(USER_MODIFY_ALLOWED*4, 2, 0)
                .end();
    }

    @Override
    void additionalTest400RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
        asserter
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleDeleteId, USER_DELETE_ALLOWED + 2)
                .end();
    }
}
