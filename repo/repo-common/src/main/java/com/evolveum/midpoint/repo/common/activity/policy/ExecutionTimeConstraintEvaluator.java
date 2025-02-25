/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.task.api.Task;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

@Component
public class ExecutionTimeConstraintEvaluator
        extends EvaluatedDurationThresholdConstraintEvaluator<DurationThresholdPolicyConstraintType> {

    @Override
    protected Duration getDurationValue(ActivityPolicyRuleEvaluationContext context) {
        Task task = context.getActivityRun().getRunningTask();

        // todo get from context and from task
        return XmlTypeConverter.createDuration("PT1M");
    }
}
