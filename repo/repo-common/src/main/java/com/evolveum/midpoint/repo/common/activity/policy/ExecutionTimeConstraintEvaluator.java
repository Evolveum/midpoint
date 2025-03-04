/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityItemProcessingStatistics;
import com.evolveum.midpoint.schema.util.task.WallClockTimeComputer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRunRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

import org.springframework.stereotype.Component;

@Component
public class ExecutionTimeConstraintEvaluator
        extends EvaluatedDurationThresholdConstraintEvaluator<DurationThresholdPolicyConstraintType> {

    @Override
    protected Long getDurationValue(ActivityPolicyRuleEvaluationContext context) {
        // todo MID-10412 handle differently for worker vs for coordinator

        AbstractActivityRun<?, ?, ?> activityRun = context.getActivityRun();

        ActivityItemProcessingStatistics stats = activityRun.getActivityState().getLiveItemProcessingStatistics();
        List<ActivityRunRecordType> runRecords = stats.getValueCopy().getRun();

        WallClockTimeComputer computer = WallClockTimeComputer.create(runRecords);
        return computer.getSummaryTime();
    }
}
