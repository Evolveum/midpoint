/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy.evaluator;

import java.util.List;
import java.util.Set;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRuleEvaluationContext;
import com.evolveum.midpoint.repo.common.activity.policy.DataNeed;
import com.evolveum.midpoint.util.LocalizableMessage;

import com.evolveum.midpoint.util.SingleLocalizableMessage;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.task.WallClockTimeComputer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRunRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

@Component
public class ExecutionTimeConstraintEvaluator
        extends DurationConstraintEvaluator<DurationThresholdPolicyConstraintType> {

    @Override
    protected Duration getLocalValue(ActivityPolicyRuleEvaluationContext context) {

        List<ActivityRunRecordType> runRecords =
                context.getActivityRun()
                        .getActivityState()
                        .getLiveItemProcessingStatistics()
                        .getValueCopy()
                        .getRun();

        WallClockTimeComputer computer = WallClockTimeComputer.create(runRecords);

        return XmlTypeConverter.createDuration(computer.getSummaryTime());
    }

    @Override
    protected @Nullable Duration getPreexistingValue(ActivityPolicyRuleEvaluationContext context) {
        return context.getPreexistingExecutionTime();
    }

    @Override
    protected LocalizableMessage createEvaluatorName() {
        return new SingleLocalizableMessage("ExecutionTimeConstraintEvaluator.name", new Object[0],"Execution time");
    }

    @Override
    public Set<DataNeed> getDataNeeds(JAXBElement<DurationThresholdPolicyConstraintType> constraint) {
        return Set.of(DataNeed.EXECUTION_TIME);
    }
}
